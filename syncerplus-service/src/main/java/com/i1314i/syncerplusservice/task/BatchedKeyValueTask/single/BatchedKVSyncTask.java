package com.i1314i.syncerplusservice.task.BatchedKeyValueTask.single;

import com.i1314i.syncerpluscommon.config.ThreadPoolConfig;
import com.i1314i.syncerpluscommon.util.spring.SpringUtil;
import com.i1314i.syncerplusservice.entity.SyncTaskEntity;
import com.i1314i.syncerplusservice.entity.dto.RedisSyncDataDto;
import com.i1314i.syncerplusservice.pool.ConnectionPool;
import com.i1314i.syncerplusservice.pool.RedisMigrator;
import com.i1314i.syncerplusservice.service.command.SendDefaultCommand;
import com.i1314i.syncerplusservice.service.rdb.SendDumpKeyDiffVersionCommand;
import com.i1314i.syncerplusservice.util.Jedis.pool.JDJedisClientPool;
import com.i1314i.syncerplusservice.util.RedisUrlUtils;
import com.i1314i.syncerplusservice.util.TaskMonitorUtils;
import com.moilioncircle.redis.replicator.CloseListener;
import com.moilioncircle.redis.replicator.RedisReplicator;
import com.moilioncircle.redis.replicator.RedisURI;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.rdb.iterable.ValueIterableEventListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Slf4j
public class BatchedKVSyncTask implements Runnable {

    static ThreadPoolConfig threadPoolConfig;
    static ThreadPoolTaskExecutor threadPoolTaskExecutor;


    static {
        threadPoolConfig = SpringUtil.getBean(ThreadPoolConfig.class);
        threadPoolTaskExecutor = threadPoolConfig.threadPoolTaskExecutor();
    }
    private SendDefaultCommand sendDefaultCommand=new SendDefaultCommand();
    private String sourceUri;  //源redis地址
    private String targetUri;  //目标redis地址
    private int threadCount = 30;  //写线程数
    private boolean status = true;
    private String threadName; //线程名称
    private RedisSyncDataDto syncDataDto;
    private Date startTime = new Date();
    private boolean syncStatus = true;
    int commandNum = 0;
    Pipeline pipelined = null;
    private SyncTaskEntity taskEntity = new SyncTaskEntity();

    private RdbCommand sendDumpKeyDiffVersionCommand=new RdbCommand();
    public BatchedKVSyncTask(RedisSyncDataDto syncDataDto) {
        this.syncDataDto = syncDataDto;
        this.sourceUri = syncDataDto.getSourceUri();
        this.targetUri = syncDataDto.getTargetUri();
        this.threadName = syncDataDto.getThreadName();
        if (status) {
            this.status = false;
        }
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
            Replicator r = RedisMigrator.bacthedCommandDress(new RedisReplicator(suri));
            JDJedisClientPool targetJedisClientPool = RedisUrlUtils.getJDJedisClient(syncDataDto, turi);

            /**
             * RDB复制
             */
            r.addEventListener(new ValueIterableEventListener(200,new EventListener() {
                @Override
                public void onEvent(Replicator replicator, Event event) {
                    RedisUrlUtils.doCheckTask(r, Thread.currentThread());
                    if (RedisUrlUtils.doThreadisCloseCheckTask())
                        return;

                    /**
                     * 全量同步
                     */

                    sendDumpKeyDiffVersionCommand.sendRestoreDumpData(event,r,threadPoolTaskExecutor,targetJedisClientPool,threadName,syncDataDto.getDbNum());



                    /**
                     * 命令同步
                     */
                    sendDefaultCommand.sendDefaultCommand(event,r,pool,threadPoolTaskExecutor,syncDataDto);


                }
            }));


            r.addCloseListener(new CloseListener() {
                @Override
                public void handle(Replicator replicator) {
                    if (targetJedisClientPool != null) {
                        targetJedisClientPool.closePool();
                    }



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
        }catch (Exception e){
            log.info(e.getMessage());
        }
    }

}
