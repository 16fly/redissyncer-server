package com.i1314i.syncerplusservice.task.singleTask.sameVersion;

import com.i1314i.syncerpluscommon.config.ThreadPoolConfig;
import com.i1314i.syncerpluscommon.util.spring.SpringUtil;
import com.i1314i.syncerplusservice.entity.dto.RedisSyncDataDto;
import com.i1314i.syncerplusservice.pool.ConnectionPool;
import com.i1314i.syncerplusservice.pool.RedisClient;
import com.i1314i.syncerplusservice.pool.RedisMigrator;
import com.i1314i.syncerplusservice.task.CommitSendTask;
import com.i1314i.syncerplusservice.task.singleTask.pipe.LockPipe;
import com.i1314i.syncerplusservice.task.singleTask.sameVersion.RdbSameVersionRestoreTask;
import com.i1314i.syncerplusservice.util.RedisUrlUtils;
import com.i1314i.syncerplusservice.util.TaskMonitorUtils;
import com.moilioncircle.redis.replicator.*;

import com.moilioncircle.redis.replicator.cmd.impl.DefaultCommand;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.EventListener;

import com.moilioncircle.redis.replicator.rdb.datatype.DB;
import com.moilioncircle.redis.replicator.rdb.dump.datatype.DumpKeyValuePair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static redis.clients.jedis.Protocol.Command.SELECT;
import static redis.clients.jedis.Protocol.toByteArray;


/**
 * 同步相同版本并且版本号>3数据
 */

@Getter
@Setter
@Slf4j
public class SyncSameTask implements Runnable {

    static ThreadPoolConfig threadPoolConfig;
    static ThreadPoolTaskExecutor threadPoolTaskExecutor;

    static {
        threadPoolConfig = SpringUtil.getBean(ThreadPoolConfig.class);
        threadPoolTaskExecutor = threadPoolConfig.threadPoolTaskExecutor();
    }

    private String sourceUri;  //源redis地址
    private String targetUri;  //目标redis地址
    private int threadCount = 30;  //写线程数
    private boolean status = true;
    private String threadName; //线程名称
    private RedisSyncDataDto syncDataDto;
    private String dbindex="-1";
    private Lock lock = new ReentrantLock();

    public SyncSameTask(RedisSyncDataDto syncDataDto) {
        this.syncDataDto = syncDataDto;
        this.sourceUri = syncDataDto.getSourceUri();
        this.targetUri = syncDataDto.getTargetUri();
        this.threadName = syncDataDto.getThreadName();
        if (status) {
            this.status = false;
        }
    }
    void selectIndex(byte[]index){
        lock.lock();
        try {
            dbindex=new String(index);
        } catch (Exception e) {

        }finally {
            lock.unlock(); //释放锁
        }
    }

    String getIndex(){
        return dbindex;
    }

    @Override
    public void run() {


        //设线程名称
        Thread.currentThread().setName(threadName);
        TaskMonitorUtils.addAliveThread(Thread.currentThread().getName(), Thread.currentThread());
        RedisURI suri = null;
        try {
            suri = new RedisURI(sourceUri);

            RedisURI turi = new RedisURI(targetUri);
            ConnectionPool pools = RedisUrlUtils.getConnectionPool();
            final ConnectionPool pool = pools;

            /**
             * 初始化连接池
             */
            pool.init(syncDataDto.getMinPoolSize(), syncDataDto.getMaxPoolSize(), syncDataDto.getMaxWaitTime(), turi, syncDataDto.getTimeBetweenEvictionRunsMillis(), syncDataDto.getIdleTimeRunsMillis());
            final AtomicInteger dbnum = new AtomicInteger(-1);
            Replicator r = RedisMigrator.dress(new RedisReplicator(suri));


            /**
             * RDB复制
             */
            r.addEventListener(new EventListener() {
                @Override
                public void onEvent(Replicator replicator, Event event) {

                    if (event instanceof DumpKeyValuePair) {
                        DumpKeyValuePair kv = (DumpKeyValuePair) event;


                        RedisUrlUtils.doCheckTask(r, Thread.currentThread());

                        if (RedisUrlUtils.doThreadisCloseCheckTask())
                            return;

                        StringBuffer info = new StringBuffer();
                        // Step1: select db
                        DB db = kv.getDb();
                        int index;
                        RedisClient redisClient = null;
                        try {
                            redisClient = pool.borrowResource();
                        } catch (Exception e) {
                            log.info("RDB复制：从池中获取RedisClient失败：{}" , e.getMessage());

                        }
                        if (db != null && (index = (int) db.getDbNumber()) != dbnum.get()) {
                            status = true;

                            try {
                                redisClient.send(SELECT, toByteArray(index));
                            } catch (Exception e) {
                                log.info("RDB复制： 从池中获取链接失败: {}" ,e.getMessage());
                            }
                            dbnum.set(index);
                            info.append("SELECT:");
                            info.append(index);
                            log.info(info.toString());
                        }

                        info.setLength(0);
                        //threadPoolTaskExecutor.execute(new SyncTask(replicator,kv,target,dbnum));
                        // Step2: restore dump data
                        DumpKeyValuePair mkv = (DumpKeyValuePair) kv;

                        if (mkv.getExpiredMs() == null) {

                            threadPoolTaskExecutor.submit(new RdbSameVersionRestoreTask(mkv, 0L, redisClient, pool, true, info));
                        } else {
                            long ms = mkv.getExpiredMs() - System.currentTimeMillis();
                            if (ms <= 0) return;

//                      threadPoolTaskExecutor.submit(new RdbRestoreTask(mkv, ms, redisClient,pool, true,info,targetJedisplus,sourceJedisplus));
                            threadPoolTaskExecutor.submit(new RdbSameVersionRestoreTask(mkv, ms, redisClient, pool, true, info));
                        }


                    }


                    /**
                     * 命令同步
                     */
                    if (event instanceof DefaultCommand) {
                        // Step3: sync aof command
                        RedisUrlUtils.doCommandCheckTask(r);
                        if (RedisUrlUtils.doThreadisCloseCheckTask()) {
                            return;
                        }

                        RedisClient redisClient = null;
                        try {
                            redisClient = pool.borrowResource();
                        } catch (Exception e) {
                            log.info("命令复制:从池中获取RedisClient失败:{}" , e.getMessage());

                        }
                        StringBuffer info = new StringBuffer();
                        // Step3: sync aof command
                        DefaultCommand dc = (DefaultCommand) event;

                        if(new String(dc.getCommand()).trim().toUpperCase().equals("SELECT")){
                            try {
                                if(dc.getArgs().length>0){
                                    selectIndex(dc.getArgs()[0]);
                                }
                            }catch (Exception e){

                            }

                        }else {
                            if(getDbindex().equals("-1")){
                                threadPoolTaskExecutor.submit(new CommitSendTask(dc, redisClient, pool, info,"0"));
                            }else {

                                threadPoolTaskExecutor.submit(new CommitSendTask(dc, redisClient, pool, info,getIndex()));
                            }

                        }
                    }
                }
            });


            r.addCloseListener(new CloseListener() {
                @Override
                public void handle(Replicator replicator) {
                    if (pool != null) {
                        pool.close();
                    }

                }
            });
            r.open();

        } catch (URISyntaxException e) {
            log.info("redis address is error:{%s} ", e.getMessage());
        } catch (IOException e) {
            log.info("redis address is error:{%s} ", e.getMessage());
        }
    }


}
