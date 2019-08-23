package com.i1314i.syncerplusservice.task.BatchedKeyValueTask.single;


import com.i1314i.syncerplusservice.constant.RedisCommandTypeEnum;
import com.i1314i.syncerplusservice.rdbtask.enums.RedisCommandType;
import com.i1314i.syncerplusservice.task.singleTask.diffVersion.defaultVersion.RdbDiffVersionInsertPlusRestoreTask;
import com.i1314i.syncerplusservice.util.Jedis.JDJedis;
import com.i1314i.syncerplusservice.util.Jedis.pool.JDJedisClientPool;

import com.moilioncircle.redis.replicator.Constants;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.PostRdbSyncEvent;
import com.moilioncircle.redis.replicator.event.PreRdbSyncEvent;
import com.moilioncircle.redis.replicator.rdb.datatype.DB;

import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyValuePair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Date;
import java.util.Map;



@Slf4j
public class RdbCommand {
    private boolean status = true;

    public RdbCommand() {
        if (status) {
            this.status = false;
        }
    }

    public void sendRestoreDumpData(Event event, Replicator r, ThreadPoolTaskExecutor threadPoolTaskExecutor, JDJedisClientPool targetJedisClientPool, String threadName, Map<Integer, Integer> dbMap) {

        if (event instanceof PreRdbSyncEvent) {
            log.info("{} :全量同步启动", threadName);
        }

        if (event instanceof PostRdbSyncEvent) {
            log.info("{} :全量同步结束", threadName);
        }

        if (event instanceof BatchedKeyValuePair<?, ?>) {
            BatchedKeyValuePair event1 = (BatchedKeyValuePair) event;
                if (event1.getDb() == null)
                    return;
                StringBuffer info = new StringBuffer();
                DB db=event1.getDb();

                JDJedis targetJedisplus= null;
                try {

                    targetJedisplus = RedisCommandType.getJDJedis(targetJedisClientPool,event1,dbMap);

                    long newTime=0L;
                    if(event1.getExpiredMs()!=null){
                        newTime=event1.getExpiredMs()-System.currentTimeMillis();
                    }

                    if(event1.getValue()!=null){

                        threadPoolTaskExecutor.submit(new BatchedRestoreTask(event, newTime, new String((byte[]) event1.getKey()), info, targetJedisplus, RedisCommandType.getRedisCommandTypeEnum(event1.getValueRdbType())));
                    }


                } catch (Exception e) {
                    //mapping映射中不存在关系，放弃当前 kv数据同步
                }
        }

    }






}
