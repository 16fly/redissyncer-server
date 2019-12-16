package syncer.syncerservice.util.JDRedisClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import redis.clients.jedis.params.SetParams;
import syncer.syncerpluscommon.config.ThreadPoolConfig;
import syncer.syncerpluscommon.util.spring.SpringUtil;
import syncer.syncerplusredis.rdb.datatype.ZSetEntry;
import syncer.syncerservice.cmd.ClusterProtocolCommand;
import syncer.syncerservice.util.jedis.ObjectUtils;
import syncer.syncerservice.util.jedis.cluster.SyncJedisClusterClient;
import syncer.syncerservice.util.jedis.cluster.extendCluster.JedisClusterPlus;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class JDRedisJedisClusterClient implements JDRedisClient {

    static ThreadPoolConfig threadPoolConfig;
    static ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private String host;
    //任务id
    private String taskId;
    private JedisClusterPlus redisClient;
    static {
        threadPoolConfig = SpringUtil.getBean(ThreadPoolConfig.class);
        threadPoolTaskExecutor = threadPoolConfig.threadPoolTaskExecutor();
    }


    public JDRedisJedisClusterClient(String host,  String password,String taskId) {
        this.host = host;
        this.taskId = taskId;

        try {
            SyncJedisClusterClient pool=new SyncJedisClusterClient( host,password,10,5000,5000,5000);
            redisClient=pool.jedisCluster();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String set(Long dbNum, byte[] key, byte[] value) {
        return redisClient.set(key,value);
    }

    @Override
    public String set(Long dbNum, byte[] key, byte[] value, long ms) {
        return redisClient.set(key,value, SetParams.setParams().px(ms));
    }

    @Override
    public Long append(Long dbNum, byte[] key, byte[] value) {
        return redisClient.append(key, value);
    }

    @Override
    public Long lpush(Long dbNum, byte[] key, byte[]... value) {
        return redisClient.lpush(key,value);
    }

    @Override
    public Long lpush(Long dbNum, byte[] key, long ms, byte[]... value) {
        Long res= redisClient.lpush(key,value);
        redisClient.pexpire(key,ms);
        return res;
    }

    @Override
    public Long lpush(Long dbNum, byte[] key, List<byte[]> value) {
        return  redisClient.lpush(key, ObjectUtils.listBytes(value));
    }

    @Override
    public Long lpush(Long dbNum, byte[] key, long ms, List<byte[]> value) {
        Long res= redisClient.lpush(key,ObjectUtils.listBytes(value));
        redisClient.pexpire(key,ms);
        return res;
    }

    @Override
    public Long sadd(Long dbNum, byte[] key, byte[]... members) {
        return redisClient.sadd(key,members);
    }

    @Override
    public Long sadd(Long dbNum, byte[] key, long ms, byte[]... members) {
        Long res= redisClient.sadd(key,members);
        redisClient.pexpire(key,ms);
        return res;
    }

    @Override
    public Long sadd(Long dbNum, byte[] key, Set<byte[]> members) {
        Long res= redisClient.sadd(key,ObjectUtils.setBytes(members));
        return res;
    }

    @Override
    public Long sadd(Long dbNum, byte[] key, long ms, Set<byte[]> members) {
        Long res= redisClient.sadd(key,ObjectUtils.setBytes(members));
        redisClient.pexpire(key,ms);
        return res;
    }

    @Override
    public Long zadd(Long dbNum, byte[] key, Set<ZSetEntry> value) {
        return redisClient.zadd(key,ObjectUtils.zsetBytes(value));
    }

    @Override
    public Long zadd(Long dbNum, byte[] key, Set<ZSetEntry> value, long ms) {
        Long res= redisClient.zadd(key,ObjectUtils.zsetBytes(value));
        redisClient.pexpire(key,ms);
        return res;
    }

    @Override
    public String hmset(Long dbNum, byte[] key, Map<byte[], byte[]> hash) {
        return redisClient.hmset(key,hash);
    }

    @Override
    public String hmset(Long dbNum, byte[] key, Map<byte[], byte[]> hash, long ms) {
        String res= redisClient.hmset(key,hash);
        redisClient.pexpire(key,ms);
        return res;
    }

    @Override
    public String restore(Long dbNum, byte[] key, int ttl, byte[] serializedValue) {
        return redisClient.restore(key,ttl,serializedValue);
    }

    @Override
    public String restoreReplace(Long dbNum, byte[] key, int ttl, byte[] serializedValue) {
        return redisClient.restoreReplace(key,ttl,serializedValue);
    }

    @Override
    public String restoreReplace(Long dbNum, byte[] key, int ttl, byte[] serializedValue, boolean highVersion) {
        return redisClient.restoreReplace(key,ttl,serializedValue);
    }

    @Override
    public Object send(byte[] cmd, byte[]... args) {
        return redisClient.sendCommand(args[0], ClusterProtocolCommand.builder().raw(cmd).build(),args);
    }

    @Override
    public void select(Integer dbNum) {

    }
}
