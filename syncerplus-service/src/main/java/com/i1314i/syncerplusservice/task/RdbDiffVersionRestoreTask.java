package com.i1314i.syncerplusservice.task;

import com.i1314i.syncerplusservice.pool.ConnectionPool;
import com.i1314i.syncerplusservice.pool.RedisClient;
import com.moilioncircle.redis.replicator.rdb.dump.datatype.DumpKeyValuePair;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.concurrent.Callable;


/**
 * 不相同版本之间的数据迁移
 */
@Slf4j
public class RdbDiffVersionRestoreTask implements Callable<Object> {
    private DumpKeyValuePair mkv;
    long ms;
    private RedisClient redisClient;
    private ConnectionPool pool;
    private StringBuffer info;
    private Jedis targetJedis;
    private Jedis sourceJedis;

    public RdbDiffVersionRestoreTask(DumpKeyValuePair mkv, long ms, RedisClient redisClient, ConnectionPool pool,  StringBuffer info, Jedis targetJedis, Jedis sourceJedis) {
        this.mkv = mkv;
        this.ms = ms;
        this.redisClient = redisClient;
        this.pool = pool;
        this.info = info;
        this.targetJedis = targetJedis;
        this.sourceJedis = sourceJedis;
    }


    @Override
    public Object call() throws Exception {

        Object r = null;
        int i = 3;
        try {
            while (i > 0) {


                byte[] data = sourceJedis.get(mkv.getRawKey());
                if (mkv.getExpiredMs() == null) {
                    r = targetJedis.set(mkv.getRawKey(), data);
                } else {
                    r = targetJedis.set(mkv.getRawKey(), data, new SetParams().px(mkv.getExpiredMs()));
                }
                if (r.equals("OK")) {
                    i = -1;
                    info.append(mkv.getKey());
                    info.append("->");
                    info.append(r.toString());
                    log.info(info.toString());
                    break;
                } else {
                    i--;
                }

            }
        } catch (Exception epx) {
            log.info(epx.getMessage() + ": " + i + ":" + mkv.getKey() + ": " + mkv.getExpiredMs());
        } finally {
            if (redisClient != null) {
                pool.release(redisClient);
            }
            if(sourceJedis!=null){
                sourceJedis.close();
            }
            if(targetJedis!=null){
                targetJedis.close();
            }

        }
        return r;
    }
}