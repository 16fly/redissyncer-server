package syncerservice.syncerplusservice.rdbtask.single;

import syncerservice.syncerpluscommon.config.ThreadPoolConfig;
import syncerservice.syncerpluscommon.util.spring.SpringUtil;
import syncerservice.syncerplusredis.entity.RedisURI;
import syncerservice.syncerplusredis.event.Event;
import syncerservice.syncerplusredis.event.EventListener;
import syncerservice.syncerplusredis.event.PostRdbSyncEvent;
import syncerservice.syncerplusredis.event.PreRdbSyncEvent;
import syncerservice.syncerplusredis.exception.IncrementException;
import syncerservice.syncerplusredis.extend.replicator.listener.ValueDumpIterableEventListener;
import syncerservice.syncerplusredis.extend.replicator.service.JDRedisReplicator;
import syncerservice.syncerplusredis.extend.replicator.visitor.ValueDumpIterableRdbVisitor;
import syncerservice.syncerplusredis.rdb.datatype.DB;
import syncerservice.syncerplusredis.rdb.dump.datatype.DumpKeyValuePair;
import syncerservice.syncerplusredis.rdb.iterable.datatype.BatchedKeyValuePair;
import syncerservice.syncerplusredis.replicator.Replicator;
import syncerservice.syncerplusredis.constant.RedisCommandTypeEnum;
import syncerservice.syncerplusredis.entity.RedisInfo;
import syncerservice.syncerplusredis.entity.dto.RedisSyncDataDto;
import syncerservice.syncerplusredis.entity.thread.OffSetEntity;
import syncerservice.syncerplusservice.pool.ConnectionPool;
import syncerservice.syncerplusservice.pool.RedisMigrator;
import syncerservice.syncerplusservice.rdbtask.enums.RedisCommandType;
import syncerservice.syncerplusservice.rdbtask.single.command.SendRdbCommand;

import syncerservice.syncerplusservice.service.command.SendDefaultCommand;
import syncerservice.syncerplusredis.exception.TaskMsgException;
import syncerservice.syncerplusservice.util.Jedis.pool.JDJedisClientPool;
import syncerservice.syncerplusservice.util.RedisUrlUtils;
import syncerservice.syncerplusredis.util.TaskMsgUtils;

import syncerservice.syncerplusservice.util.SyncTaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Slf4j
public class SingleDataRestoreTask implements Runnable {
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
    private SendDefaultCommand sendDefaultCommand=new SendDefaultCommand();
    private double redisVersion;
    private RedisInfo info;
    private String taskId;
    private boolean afresh;
    private Date time;
    public SingleDataRestoreTask(RedisSyncDataDto syncDataDto, RedisInfo info,String taskId) {
        this.syncDataDto = syncDataDto;
        this.sourceUri = syncDataDto.getSourceUri();
        this.targetUri = syncDataDto.getTargetUri();
        this.threadName = syncDataDto.getTaskName();
        this.info=info;
        this.taskId=taskId;
        this.afresh=syncDataDto.isAfresh();
    }



    @Override
    public void run() {

        //设线程名称
        Thread.currentThread().setName(threadName);


        try {
            RedisURI suri = new RedisURI(sourceUri);
            RedisURI turi = new RedisURI(targetUri);
            JDJedisClientPool targetJedisClientPool = RedisUrlUtils.getJDJedisClient(syncDataDto, turi);


            ConnectionPool pools = RedisUrlUtils.getConnectionPool();
            final ConnectionPool pool = pools;


            /**
             * 初始化连接池
             */
            pool.init(syncDataDto.getMinPoolSize(), syncDataDto.getMaxPoolSize(), syncDataDto.getMaxWaitTime(), turi, syncDataDto.getTimeBetweenEvictionRunsMillis(), syncDataDto.getIdleTimeRunsMillis());



            final Replicator r  = RedisMigrator.newBacthedCommandDress(new JDRedisReplicator(suri));
            TaskMsgUtils.getThreadMsgEntity(taskId).addReplicator(r);

            r.setRdbVisitor(new ValueDumpIterableRdbVisitor(r,info.getRdbVersion()));

            OffSetEntity offset= TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().get(sourceUri);
            if(offset==null){

                offset=new OffSetEntity();
                TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().put(sourceUri,offset);
            }else {

                if(StringUtils.isEmpty(offset.getReplId())){
                    offset.setReplId(r.getConfiguration().getReplId());
                }else if(offset.getReplOffset().get()>-1){
                    if(!afresh){
                        r.getConfiguration().setReplOffset(offset.getReplOffset().get());
                        r.getConfiguration().setReplId(offset.getReplId());
                    }

                }
            }

            final OffSetEntity baseOffSet= TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().get(sourceUri);

//            1036363
//
//            r.getConfiguration().setReplOffset(10000);
//            r.getConfiguration().setReplId("e1399afce9f5b5c35c5315ae68e4807fe81e764f");
            r.addEventListener(new ValueDumpIterableEventListener(1000, new EventListener() {
                @Override
                public void onEvent(Replicator replicator, Event event) {

//                    r.getConfiguration().getReplOffset()


                    if (SyncTaskUtils.doThreadisCloseCheckTask(taskId)) {

                        try {
                            r.close();
                            if (status) {
                                Thread.currentThread().interrupt();
                                status = false;
                                System.out.println(" 线程正准备关闭..." + Thread.currentThread().getName());
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }



                    if (event instanceof PreRdbSyncEvent) {
                        time=new Date();
                        log.info("{} :全量同步启动");
                    }


                    if (event instanceof PostRdbSyncEvent) {
                        if(r.getConfiguration().getReplOffset()>=0){
                            baseOffSet.setReplId(r.getConfiguration().getReplId());
                            baseOffSet.getReplOffset().set(r.getConfiguration().getReplOffset());
                        }
                        System.out.println("时间"+ (new Date().getTime()-time.getTime()));
                        log.info("{} :全量同步结束");
                    }

                    if (event instanceof BatchedKeyValuePair<?, ?>) {

                        BatchedKeyValuePair event1 = (BatchedKeyValuePair) event;
                        DB db=event1.getDb();
                        int dbbnum= (int) db.getDbNumber();
                        Long ms;
                        if(event1.getExpiredMs()==null){
                            ms =0L;
                        }else {
                            ms =event1.getExpiredMs()-System.currentTimeMillis();
                            if(ms<0L){
                                return;
                            }
                        }
                        if (event1.getValue() != null) {

                            if(null!=syncDataDto.getDbMapper()&&syncDataDto.getDbMapper().size()>0){
                                if(syncDataDto.getDbMapper().containsKey((int)db.getDbNumber())){
                                    dbbnum=syncDataDto.getDbMapper().get((int)db.getDbNumber());
                                }else {
                                    return;
                                }
                            }

                            try {
                                threadPoolTaskExecutor.submit(new SendRdbCommand(taskId,ms, RedisCommandType.getRedisCommandTypeEnum(event1.getValueRdbType()),event,RedisCommandType.getJDJedis(targetJedisClientPool,event,syncDataDto.getDbMapper()),new String((byte[]) event1.getKey()),syncDataDto.getRedisVersion()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }


                    }

                    if (event instanceof DumpKeyValuePair) {
                        DumpKeyValuePair valuePair = (DumpKeyValuePair) event;

                        if(valuePair.getValue()!=null){
                            Long ms;
                            if(valuePair.getExpiredMs()==null){
                                ms =0L;
                            }else {
                                ms =valuePair.getExpiredMs()-System.currentTimeMillis();
//                                ms=ms/1000;
//                                System.out.println(ms);
                                if(ms<=0L){
                                    return;
                                }
                            }

                            DB db=valuePair.getDb();
                            int dbbnum= (int) db.getDbNumber();

                            if(null!=syncDataDto.getDbMapper()&&syncDataDto.getDbMapper().size()>0){
                                if(syncDataDto.getDbMapper().containsKey((int)db.getDbNumber())){
                                    dbbnum=syncDataDto.getDbMapper().get((int)db.getDbNumber());
                                }else {
                                    return;
                                }
                            }
                            try {
                                threadPoolTaskExecutor.submit(new SendRdbCommand(taskId,ms, RedisCommandTypeEnum.DUMP,event,RedisCommandType.getJDJedis(targetJedisClientPool,event,syncDataDto.getDbMapper()),new String((byte[]) valuePair.getKey()),syncDataDto.getRedisVersion()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }


                    /**
                     * 命令同步
                     */
                    sendDefaultCommand.sendDefaultCommand(TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().get(sourceUri),event,r,pool,threadPoolTaskExecutor,syncDataDto);


                }
            }));
            r.open();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (EOFException ex) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),ex.getMessage());
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, ex.getMessage());
        } catch (NoRouteToHostException p) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),p.getMessage());
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, p.getMessage());
        } catch (ConnectException cx) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),cx.getMessage());
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, cx.getMessage());
        }catch (AssertionError er){
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),er.getMessage());
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, er.getMessage());
        }catch (JedisConnectionException ty){
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),ty.getMessage());
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, ty.getMessage());
        }catch (SocketException ii){
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),ii.getMessage());
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, ii.getMessage());
        }

        catch (IOException et) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),et.getMessage());
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, et.getMessage());
        } catch (IncrementException et) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),et.getMessage());
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, et.getMessage());
        }catch (Exception e){
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId),e.getMessage());
            } catch (TaskMsgException ep) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, e.getMessage());
        }
    }


}
