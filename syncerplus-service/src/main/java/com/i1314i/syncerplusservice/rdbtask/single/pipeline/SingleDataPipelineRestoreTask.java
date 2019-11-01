package com.i1314i.syncerplusservice.rdbtask.single.pipeline;

import com.alibaba.fastjson.JSON;
import com.i1314i.syncerpluscommon.config.ThreadPoolConfig;
import com.i1314i.syncerpluscommon.util.spring.SpringUtil;
import com.i1314i.syncerplusredis.cmd.impl.DefaultCommand;
import com.i1314i.syncerplusredis.entity.RedisURI;
import com.i1314i.syncerplusredis.event.Event;
import com.i1314i.syncerplusredis.event.EventListener;
import com.i1314i.syncerplusredis.event.PostRdbSyncEvent;
import com.i1314i.syncerplusredis.event.PreRdbSyncEvent;
import com.i1314i.syncerplusredis.exception.IncrementException;
import com.i1314i.syncerplusredis.extend.replicator.listener.ValueDumpIterableEventListener;
import com.i1314i.syncerplusredis.extend.replicator.service.JDRedisReplicator;
import com.i1314i.syncerplusredis.extend.replicator.visitor.ValueDumpIterableRdbVisitor;
import com.i1314i.syncerplusredis.rdb.datatype.DB;
import com.i1314i.syncerplusredis.rdb.dump.datatype.DumpKeyValuePair;
import com.i1314i.syncerplusredis.rdb.iterable.datatype.BatchedKeyValuePair;
import com.i1314i.syncerplusredis.replicator.Replicator;
import com.i1314i.syncerplusredis.constant.RedisCommandTypeEnum;
import com.i1314i.syncerplusredis.entity.EventEntity;
import com.i1314i.syncerplusredis.entity.RedisInfo;
import com.i1314i.syncerplusredis.entity.SyncTaskEntity;
import com.i1314i.syncerplusredis.entity.dto.RedisSyncDataDto;
import com.i1314i.syncerplusredis.entity.thread.EventTypeEntity;
import com.i1314i.syncerplusredis.entity.thread.OffSetEntity;
import com.i1314i.syncerplusservice.pool.RedisMigrator;
import com.i1314i.syncerplusservice.rdbtask.enums.RedisCommandType;

import com.i1314i.syncerplusredis.exception.TaskMsgException;
import com.i1314i.syncerplusservice.task.clusterTask.command.ClusterProtocolCommand;
import com.i1314i.syncerplusservice.task.singleTask.pipe.LockPipe;
import com.i1314i.syncerplusservice.task.singleTask.pipe.PipelinedSyncTask;
import com.i1314i.syncerplusservice.util.Jedis.JDJedis;
import com.i1314i.syncerplusservice.util.Jedis.pool.JDJedisClientPool;
import com.i1314i.syncerplusservice.util.RedisUrlUtils;
import com.i1314i.syncerplusredis.util.TaskMsgUtils;

import com.i1314i.syncerplusservice.util.SyncTaskUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Pipeline;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class SingleDataPipelineRestoreTask implements Runnable {

    static ThreadPoolConfig threadPoolConfig;
    static ThreadPoolTaskExecutor threadPoolTaskExecutor;

    static {
        threadPoolConfig = SpringUtil.getBean(ThreadPoolConfig.class);
        threadPoolTaskExecutor = threadPoolConfig.threadPoolTaskExecutor();
    }
    private String sourceUri;  //源redis地址
    private String targetUri;  //目标redis地址
    private boolean status = true;
    private String threadName; //线程名称
    private RedisSyncDataDto syncDataDto;
    private SendPipelineRdbCommand sendDefaultCommand = new SendPipelineRdbCommand();
//    private SendDefaultCommand sendCommand=new SendDefaultCommand();
    private RedisInfo info;
    private String taskId;
    private boolean afresh;
    private boolean syncStatus = true;


    Pipeline pipelined = null;

    private Lock lock = new ReentrantLock();
    @Getter
    @Setter
    private String dbindex="-1";
    private final AtomicLong dbNum = new AtomicLong(0);
    //判断增量是否可写
    private final AtomicBoolean commandDbStatus=new AtomicBoolean(true);
    private LockPipe lockPipe = new LockPipe();
    private SyncTaskEntity taskEntity = new SyncTaskEntity();

    private Date time;
    private int  batchSize;
    private String offsetPlace;
    private String type;
    public SingleDataPipelineRestoreTask(RedisSyncDataDto syncDataDto, RedisInfo info, String taskId,int batchSize,boolean afresh) {

        this.syncDataDto = syncDataDto;
        this.sourceUri = syncDataDto.getSourceUri();
        this.targetUri = syncDataDto.getTargetUri();
        this.threadName = syncDataDto.getTaskName();
        this.info = info;
        this.taskId = taskId;
        this.afresh=syncDataDto.isAfresh();
        this.batchSize=batchSize;
        this.afresh=afresh;
        this.offsetPlace=syncDataDto.getOffsetPlace();
        this.type=syncDataDto.getType();
    }


    public SingleDataPipelineRestoreTask(RedisSyncDataDto syncDataDto, RedisInfo info, String taskId) {
        this.syncDataDto = syncDataDto;
        this.sourceUri = syncDataDto.getSourceUri();
        this.targetUri = syncDataDto.getTargetUri();
        this.threadName = syncDataDto.getTaskName();
        this.info = info;
        this.taskId = taskId;
        this.afresh=syncDataDto.isAfresh();
        this.batchSize=1000;
    }

    @Override
    public void run() {
        if(batchSize==0){
            batchSize=1000;
        }
        //设线程名称
        Thread.currentThread().setName(threadName);
        System.out.println("batchSize:"+batchSize);
        try {
            RedisURI suri = new RedisURI(sourceUri);
            RedisURI turi = new RedisURI(targetUri);
            JDJedisClientPool targetJedisClientPool = RedisUrlUtils.getJDJedisClient(syncDataDto, turi);


            final JDJedis targetJedisplus = targetJedisClientPool.getResource();
            if (pipelined == null) {
                pipelined = targetJedisplus.pipelined();
            }
            PipelineLock pipelineLock=new PipelineLock(pipelined,taskEntity,taskId,targetJedisplus,targetJedisClientPool);





//            ConnectionPool pools = RedisUrlUtils.getConnectionPool();
//            final ConnectionPool pool = pools;
//
//
//            /**
//             * 初始化连接池
//             */
//            pool.init(syncDataDto.getMinPoolSize(), syncDataDto.getMaxPoolSize(), syncDataDto.getMaxWaitTime(), turi, syncDataDto.getTimeBetweenEvictionRunsMillis(), syncDataDto.getIdleTimeRunsMillis());

            
            Replicator replicator=new JDRedisReplicator(suri,afresh);




//            if(!afresh){
//                replicator=new JDRedisReplicator(suri,false);
//            }else {
//                replicator=new JDRedisReplicator(suri);
//            }

            int dbbnum = 0;
            final Replicator r = RedisMigrator.newBacthedCommandDress(replicator);

            TaskMsgUtils.getThreadMsgEntity(taskId).addReplicator(r);


            r.setRdbVisitor(new ValueDumpIterableRdbVisitor(r, info.getRdbVersion()));
//            1036363

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

            if(type.trim().toLowerCase().equals("partial")){
                String[]data=RedisUrlUtils.selectSyncerBuffer(sourceUri,offsetPlace);
                long offsetNum=0L;
                try {
                     offsetNum= Long.parseLong(data[0]);

                     if(offsetPlace.trim().toLowerCase().equals("beginbuf")){
                         offsetNum-=1;
                     }
                }catch (Exception e){

                }

                if(offsetNum!=0L&&!StringUtils.isEmpty(data[1])){


                    r.getConfiguration().setReplOffset(offsetNum);
                    r.getConfiguration().setReplId(data[1]);
                }


//                r.getConfiguration().setReplOffset(14105376832);
//                r.getConfiguration().setReplId("e1399afce9f5b5c35c5315ae68e4807fe81e764f");
            }

            final OffSetEntity baseOffSet= TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().get(sourceUri);
//            TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().put(taskId,offset);
//            r.getConfiguration().setReplOffset(14105376832);
//            r.getConfiguration().setReplId("e1399afce9f5b5c35c5315ae68e4807fe81e764f");



            r.addEventListener(new ValueDumpIterableEventListener(batchSize, new EventListener() {
                @Override
                public void onEvent(Replicator replicator, Event event) {

//                    r.getConfiguration().getReplOffset()
//                    lockPipe.syncpipe(pipelined, taskEntity, 1000, true);
                    lockPipe.syncpipe(pipelineLock, taskEntity, batchSize, true,suri,turi);

                    if (SyncTaskUtils.doThreadisCloseCheckTask(taskId)) {

                        try {
                            r.close();
//                            pools.close();
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


                    /**
                     * 管道的形式
                     */
                    if (syncStatus) {
//                        threadPoolTaskExecutor.submit(new PipelinedSyncTask(pipelined, taskEntity,lockPipe));
                        threadPoolTaskExecutor.submit(new PipelinedSyncTask(pipelineLock, taskEntity,lockPipe,taskId,suri,turi));
                        syncStatus = false;
                    }

                    if (event instanceof PreRdbSyncEvent) {
                        time=new Date();
                        log.info("【{}】 :全量同步启动",taskId);
                    }


                    if (event instanceof PostRdbSyncEvent) {

                        if(r.getConfiguration().getReplOffset()>=0){
                            baseOffSet.setReplId(r.getConfiguration().getReplId());
                            baseOffSet.getReplOffset().set(r.getConfiguration().getReplOffset());
                        }

                        if(type.trim().toLowerCase().equals("full")){
                            try {
                                Map<String, String> msg = SyncTaskUtils.stopCreateThread(Arrays.asList(taskId));
                            } catch (TaskMsgException e) {
                                e.printStackTrace();
                            }
                            log.warn("任务Id【{}】full全量同步结束", taskId);

                            return;
                        }
//                        if(type)
//                        System.out.println("时间"+ (new Date().getTime()-time.getTime()));
//                        TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().put(taskId,baseOffSet);
                        log.info("【{}】 :全量同步结束 时间：{}",taskId,(new Date().getTime()-time.getTime()));
                    }

                    if (event instanceof BatchedKeyValuePair<?, ?>) {

                        BatchedKeyValuePair event1 = (BatchedKeyValuePair) event;


                        DB db = event1.getDb();
                        Long ms;
                        if (event1.getExpiredMs() == null) {
                            ms = 0L;
                        } else {
//                            ms = event1.getExpiredMs();
                            ms = event1.getExpiredMs() - System.currentTimeMillis();
                            if(ms<0L){
                                return;
                            }
                        }
                        if (event1.getValue() != null) {


                            int dbbnum = (int) db.getDbNumber();
                            if (null != syncDataDto.getDbNum() && syncDataDto.getDbNum().size() > 0) {
                                if (syncDataDto.getDbNum().containsKey((int) db.getDbNumber())) {

                                    dbbnum = syncDataDto.getDbNum().get((int) db.getDbNumber());
                                } else {
                                    return;
                                }
                            }

                            if (lockPipe.getDbNum() != dbbnum) {
                                pipelined.select(dbbnum);

                                EventEntity eventEntity=new EventEntity("SELECT".getBytes(),ms,null, EventTypeEntity.USE,RedisCommandTypeEnum.STRING);
                                taskEntity.addKey(eventEntity);

                                lockPipe.setDbNum(dbbnum);
                                taskEntity.add();

                            }


                            try {

                                sendDefaultCommand.sendSingleCommand(ms,RedisCommandType.getRedisCommandTypeEnum(event1.getValueRdbType()),event,pipelineLock,new String((byte[]) event1.getKey()),syncDataDto.getRedisVersion(),taskEntity);

//                                sendDefaultCommand.sendSingleCommand(ms,RedisCommandType.getRedisCommandTypeEnum(event1.getValueRdbType()),event,pipelined,new String((byte[]) event1.getKey()),syncDataDto.getRedisVersion(),taskEntity);
//                                threadPoolTaskExecutor.submit(new SendRdbCommand(ms, RedisCommandType.getRedisCommandTypeEnum(event1.getValueRdbType()), event, RedisCommandType.getJDJedis(targetJedisClientPool, event, syncDataDto.getDbNum()), new String((byte[]) event1.getKey()), syncDataDto.getRedisVersion()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        }
                    }
                    if (event instanceof DumpKeyValuePair) {
//                        System.out.println(r.getConfiguration().getReplId()+":"+r.getConfiguration().getReplOffset());

                        DumpKeyValuePair valuePair = (DumpKeyValuePair) event;
                        if (valuePair.getValue() != null) {
                            Long ms;
                            if (valuePair.getExpiredMs() == null) {
                                ms = 0L;
                            } else {

                                ms = valuePair.getExpiredMs() - System.currentTimeMillis();
                                if(ms<0L){
                                    return;
                                }
                            }

                            DB db = valuePair.getDb();

                            int dbbnum = (int) db.getDbNumber();
                            if (null != syncDataDto.getDbNum() && syncDataDto.getDbNum().size() > 0) {
                                if (syncDataDto.getDbNum().containsKey((int) db.getDbNumber())) {

                                    dbbnum = syncDataDto.getDbNum().get((int) db.getDbNumber());
                                } else {
                                    return;
                                }
                            }

                            try {

                                if (lockPipe.getDbNum() != dbbnum) {
                                    pipelined.select(dbbnum);
                                    lockPipe.setDbNum(dbbnum);
                                    EventEntity eventEntity=new EventEntity("SELECT".getBytes(),ms,null, EventTypeEntity.USE,RedisCommandTypeEnum.STRING);
                                    taskEntity.addKey(eventEntity);
                                    taskEntity.add();
                                }

                                sendDefaultCommand.sendSingleCommand(ms,RedisCommandTypeEnum.DUMP,event,pipelineLock,new String((byte[]) valuePair.getKey()),syncDataDto.getRedisVersion(),taskEntity);


//                                threadPoolTaskExecutor.submit(new SendRdbCommand(ms, RedisCommandTypeEnum.DUMP, event, RedisCommandType.getJDJedis(targetJedisClientPool, event, syncDataDto.getDbNum()), new String((byte[]) valuePair.getKey()), syncDataDto.getRedisVersion()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }


                    /**
                     * 命令同步
                     */


                    /**
                     * 命令同步
                     */
//                    sendCommand.sendDefaultCommand(TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().get(sourceUri),event,r,pool,threadPoolTaskExecutor,syncDataDto);


                    if (event instanceof DefaultCommand) {


//                        System.out.println(r.getConfiguration().getReplId()+":"+r.getConfiguration().getReplOffset());
                        baseOffSet.setReplId(r.getConfiguration().getReplId());
                        baseOffSet.getReplOffset().set(r.getConfiguration().getReplOffset());
//                        TaskMsgUtils.getThreadMsgEntity(taskId).getOffsetMap().put(taskId,baseOffSet);
                        DefaultCommand dc = (DefaultCommand) event;


                        if(Arrays.equals(dc.getCommand(),"SELECT".getBytes())){
                            int commDbNum= Integer.parseInt(new String(dc.getArgs()[0]));


                            if(syncDataDto.getDbNum()==null||syncDataDto.getDbNum().size()==0){
                                dbNum.set(commDbNum);
                                commandDbStatus.set(true);
                            }else {
                                if(syncDataDto.getDbNum().containsKey(commDbNum)){
                                    byte[][]dbData=dc.getArgs();
                                    dbData[0]=String.valueOf(syncDataDto.getDbNum().get(commDbNum)).getBytes();
                                    dc.setArgs(dbData);
                                    dbNum.set(syncDataDto.getDbNum().get(commDbNum));
                                    commandDbStatus.set(true);
                                }else {
                                    commandDbStatus.set(false);
                                    return ;
                                }

                            }

                        }else {

                            if(!commandDbStatus.get()&&!Arrays.equals(dc.getCommand(),"FLUSHALL".getBytes())){
                                return;
                            }

                            EventEntity eventEntity=new EventEntity(new DB(dbNum.get()),EventTypeEntity.USE,RedisCommandTypeEnum.COMMAND,dc);
//                            EventEntity eventEntity=new EventEntity(dc.getArgs()[0],new DB(dbNum.get()),EventTypeEntity.USE,RedisCommandTypeEnum.COMMAND);
                            taskEntity.addKey(eventEntity);

                        }
                        if(!commandDbStatus.get()&&!Arrays.equals(dc.getCommand(),"FLUSHALL".getBytes())){
                            return;
                        }

                        taskEntity.add();
                        pipelineLock.sendCommand(new ClusterProtocolCommand(dc.getCommand()), dc.getArgs());


                    }
//                    sendDefaultCommand.sendDefaultCommand(event, r, pool, threadPoolTaskExecutor, syncDataDto);


                }
            }));
            r.open(taskId);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (EOFException ex) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId));
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, ex.getMessage());
        } catch (NoRouteToHostException p) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId));
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, p.getMessage());
        } catch (ConnectException cx) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId));
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, cx.getMessage());
        }catch (AssertionError er){
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId));
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, er.getMessage());
        }catch (JedisConnectionException ty){
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId));
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, ty.getMessage());
        }catch (SocketException ii){
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId));
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, ii.getMessage());
        }

        catch (IOException et) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId));
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, et.getMessage());
        } catch (IncrementException et) {
            try {
                Map<String, String> msg = SyncTaskUtils.brokenCreateThread(Arrays.asList(taskId));
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, et.getMessage());
        }
    }


}
