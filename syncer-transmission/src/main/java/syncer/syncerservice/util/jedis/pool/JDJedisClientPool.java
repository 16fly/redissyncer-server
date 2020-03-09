package syncer.syncerservice.util.jedis.pool;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncer.syncerjedis.Jedis;
import syncer.syncerjedis.JedisPool;
import syncer.syncerjedis.JedisPoolConfig;
import syncer.syncerjedis.exceptions.JedisDataException;
import syncer.syncerjedis.exceptions.JedisException;
import syncer.syncerjedis.params.SetParams;
import syncer.syncerplusredis.rdb.datatype.ZSetEntry;
import syncer.syncerservice.util.jedis.JeUtil;
import syncer.syncerservice.util.jedis.ObjectUtils;
import syncer.syncerservice.util.jedis.StringUtils;
import syncer.syncerservice.util.jedis.cmd.JedisProtocolCommand;
import syncer.syncerservice.util.regex.RegexUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 连接池
 */
public class JDJedisClientPool{
    private static Logger logger = LoggerFactory.getLogger(JDJedisClientPool.class);
    private String host;
    private Integer port;
    private JedisPool jedisPool;
    private JedisPoolConfig config;

    public JDJedisClientPool(String host, Integer port, JedisPoolConfig config, String password, Integer db) {
        this.host = host;
        this.port = port;
        if(null==config){
            config=new JedisPoolConfig();
            config.setMaxTotal(20);
            config.setMaxIdle(10);
            config.setMinIdle(5);
            //当池内没有返回对象时，最大等待时间
            config.setMaxWaitMillis(10000);
            config.setTimeBetweenEvictionRunsMillis(30000);
            config.setTestOnReturn(true);
            config.setBlockWhenExhausted(true);
            config.setTestOnBorrow(true);
        }

        this.config = config;
        int timeout = 50000;

        if (org.springframework.util.StringUtils.isEmpty(password)) {
            jedisPool = new JedisPool(this.config, this.host, this.port, timeout);
        } else {
            jedisPool = new JedisPool(this.config, this.host, this.port, timeout, password, db, null);
        }
    }





    /**
     * 首次加载初始化
     */
//    static {
//        host= TemplateUtils.getPropertiesdata(propertiesPath,JEDIS_HOST);
//        port= Integer.valueOf(TemplateUtils.getPropertiesdata(propertiesPath,JEDIS_PORT));
//        JedisPoolConfig config=new JedisPoolConfig();
//        config.setMaxTotal(Integer.parseInt(TemplateUtils.getPropertiesdata(propertiesPath,JEDIS_MAXTOTAL)));
//        config.setMaxIdle(Integer.parseInt(TemplateUtils.getPropertiesdata(propertiesPath,JEDIS_MaxIdle)));
//        config.setMinIdle(Integer.parseInt(TemplateUtils.getPropertiesdata(propertiesPath,JEDIS_MinIdle)));
//        //当池内没有返回对象时，最大等待时间
//        config.setMaxWaitMillis(Integer.parseInt(TemplateUtils.getPropertiesdata(propertiesPath,JEDIS_MaxWaitMillis)));
//
//
//        config.setTimeBetweenEvictionRunsMillis(Integer.parseInt(TemplateUtils.getPropertiesdata(propertiesPath,JEDIS_TimeBetweenEvictionRunsMillis)));
//        config.setTestOnReturn(true);
//        config.setTestOnBorrow(true);
//        int timeout=100000;
//        jedisPool=new JedisPool(config,host,port,timeout);
//
//
//    }


    /**
     * 获取资源
     *
     * @return
     * @throws JedisException
     */
    public Jedis getResource() throws JedisException {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
        } catch (JedisException e) {
            logger.warn("getResource.", e);
            returnBrokenResource(jedis);
            throw e;
        }
        return jedis;
    }

    public Jedis getResource(Integer index) throws JedisException {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.select(index);
        } catch (JedisException e) {
            logger.warn("getResource.", e);
            returnBrokenResource(jedis);
            throw e;
        }
        return jedis;
    }


    public Jedis selectDb(Integer index, Jedis jedis) throws JedisException {

        if (jedis == null) {
            return null;
        }

        try {
            jedis.select(index);
        } catch (JedisException e) {
            logger.warn("selectDb.", e);
            returnBrokenResource(jedis);
            throw e;
        }
        return jedis;
    }

    /**
     * 归还资源
     *
     * @param jedis
     * @param
     */
    public void returnBrokenResource(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }


    public static void returnStaticBrokenResource(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

    /**
     * 释放资源
     *
     * @param jedis
     * @param
     */
    public void returnResource(Jedis jedis) {
        if (jedis != null && jedisPool != null) {
            jedis.close();
        }
    }

    /**
     * 获取byte[]类型Key
     *
     * @param
     * @return
     */
    public static byte[] getBytesKey(Object object) {
        if (object instanceof String) {
            return StringUtils.getBytes((String) object);
        } else {
            return ObjectUtils.serialize(object);
        }
    }

    /**
     * Object转换byte[]类型
     *
     * @param object
     * @return
     */
    public static byte[] toBytes(Object object) {
        return ObjectUtils.serialize(object);
    }

    /**
     * byte[]型转换Object
     *
     * @param bytes
     * @return
     */
    public static Object toObject(byte[] bytes) {
        return ObjectUtils.unserialize(bytes);
    }


    /**
     * 获取缓存
     *
     * @param key 键
     * @return
     */

    public String get(String key) {
        String value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                value = jedis.get(key);
                value = StringUtils.isNotBlank(value) && !"nil".equalsIgnoreCase(value) ? value : null;
                logger.debug("get {} = {}", key, value);
            }
        } catch (Exception e) {
            logger.warn("get  {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return value;
    }

    /**
     * 获取缓存对象
     *
     * @param key 键
     * @return
     */

    public Object getObject(String key) {
        Object value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getBytesKey(key))) {
                value = toObject(jedis.get(getBytesKey(key)));
                logger.debug("getObject{} = {}", key, value);
            }
        } catch (Exception e) {
            logger.warn("getObject {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return value;
    }

    /**
     * 设置缓存
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */
    public String set(String key, String value, int cacheSeconds) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.set(key, value);
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("set {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("set {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String set(byte[] key, byte[] value) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.set(key, value);
            logger.debug("set {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("set {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    public String set(byte[] key, byte[] value,long ms) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.set(key, value, SetParams.setParams().px(ms));
            logger.debug("set {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("set {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public Long append(byte[] key, byte[] value){
        Long result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.append(key, value);
            logger.debug("append {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("append {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    public Long lpush(byte[] key, byte[]... value){
        Long result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.lpush(key, value);
            logger.debug("lpush {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("lpush {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    public Long lpush(byte[] key,long ms, byte[]... value){
        Long result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.lpush(key, value);
            jedis.pexpire(key,ms);
            logger.debug("lpush {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("lpush {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public Long sadd(byte[] key, byte[]... members){
        Long result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.sadd(key, members);
            logger.debug("sadd {} = {}", key, members);
        } catch (Exception e) {
            logger.warn("sadd {} = {}", key, members, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public Long sadd(byte[] key, long ms, byte[]... members){
        Long result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.sadd(key, members);
            jedis.pexpire(key,ms);
            logger.debug("sadd {} = {}", key, members);
        } catch (Exception e) {
            logger.warn("sadd {} = {}", key, members, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    public Long zadd(byte[] key, Set<ZSetEntry> value){
        Long result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.zadd(key, ObjectUtils.zsetBytes(value));
            logger.debug("zadd {} = {}", key);
        } catch (Exception e) {
            logger.warn("zadd {} = {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    public Long zadd(byte[] key, long ms,Set<ZSetEntry> value){
        Long result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.zadd(key, ObjectUtils.zsetBytes(value));
            jedis.pexpire(key,ms);
            logger.debug("zadd {} = {}", key);
        } catch (Exception e) {
            logger.warn("zadd {} = {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String hmset(byte[] key, Map<byte[], byte[]> hash){
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.hmset(key,hash);
            logger.debug("hmset {} = {}", key);
        } catch (Exception e) {
            logger.warn("hmset {} = {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    public String hmset(byte[] key, long ms,Map<byte[], byte[]> hash){
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.hmset(key,hash);
            jedis.pexpire(key,ms);
            logger.debug("hmset {} = {}", key);
        } catch (Exception e) {
            logger.warn("hmset {} = {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    public String restore(byte[] key, long ttl, byte[] serializedValue){
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.restore(key,ttl,serializedValue);
            logger.debug("key {} = {}", key);
        } catch (Exception e) {
            logger.warn("key {} = {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String restoreReplace(byte[] key, long ttl, byte[] serializedValue,boolean highVersion){
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if(highVersion){
                result = jedis.restoreReplace(key,ttl,serializedValue);
            }else {
                if (jedis.del(key) >= 0) {
                    result = jedis.restore(key,ttl, serializedValue);
                }else {
                    result = jedis.restore(key, ttl, serializedValue);
                }
            }

            logger.debug("key {} = {}", key);
        } catch (Exception e) {
            logger.warn("key {} = {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    public String restoreReplace(byte[] key, long ttl, byte[] serializedValue){
        return restoreReplace(key,ttl,serializedValue,true);
    }

    public Object send(byte[] cmd, byte[]... args){
        Object result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.sendCommand(JedisProtocolCommand.builder().raw(cmd).build(),args);
            logger.debug("key {} = {}", cmd);
        } catch (Exception e) {
            logger.warn("key {} = {}", cmd, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 查看key列表
     *
     * @param key
     * @return
     */

    public List<String> keys(String key) {
        List<String> result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            Set<String> keyList = jedis.keys(key);
            result = new ArrayList<>(keyList);
        } catch (Exception e) {
            logger.info("select keys {} success", key);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 设置缓存
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */

    public String setObject(String key, Object value, int cacheSeconds) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.set(getBytesKey(key), toBytes(value));
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("setObject {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setObject {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String setbyteObject(byte[] key, byte[] value, Integer cacheSeconds) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.set(key, value);

            if (cacheSeconds != null && cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("setObject{} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setObject{} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String selectDb(Integer db) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (db == null) {
                db = 0;
            }

            jedis.select(db);
            logger.debug("selectDb = {}", db);
        } catch (Exception e) {
            logger.warn("selectDb {} = {}", db, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String restorebyteObject(String key, byte[] value, Integer cacheSeconds) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (cacheSeconds == null) {
                cacheSeconds = 0;
            }
            if (jedis.exists(key)) {
                jedis.del(key);
            }
            result = jedis.restore(key, cacheSeconds, value);


            logger.debug(" restorebyteObject {} = {}", key, value);
        } catch (Exception e) {
            logger.warn(" restorebyteObject {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public static String restorebyteObject(byte[] key, byte[] value, Integer cacheSeconds, Jedis jedis, boolean status) {
        String result = null;
        if (jedis == null) {
            return "error: jedis is null ";
        }
        try {
            if (cacheSeconds == null) {
                cacheSeconds = 0;
            }

            if (!status) {
                result = jedis.restore(key, cacheSeconds, value);
            } else {
                if (jedis.del(key) >= 0) {
                    result = jedis.restore(key, cacheSeconds, value);
                }


            }


            logger.debug("restorebyteObject {} = {}", key, value);
        } catch (JedisDataException e) {
            throw e;
        } finally {
            returnStaticBrokenResource(jedis);
        }
        return result;
    }


    /**
     * 获取List缓存
     *
     * @param key 键
     * @return 值
     */
    public List<String> getList(String key) {
        List<String> value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                value = jedis.lrange(key, 0, -1);
                logger.debug("getList {}  = {}", key, value);
            }
        } catch (Exception e) {
            logger.warn("getList {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return value;
    }

    /**
     * 获取List缓存
     *
     * @param key 键
     * @return 值
     */

    public List<Object> getObjectList(String key) {
        List<Object> value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getBytesKey(key))) {
                List<byte[]> list = jedis.lrange(getBytesKey(key), 0, -1);
                value = Lists.newArrayList();
                for (byte[] bs : list) {
                    value.add(toObject(bs));
                }
                logger.debug("getObjectList {} = {}", key, value);
            }
        } catch (Exception e) {
            logger.warn("getObjectList {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return value;
    }

    /**
     * 设置List缓存
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */

    public long setList(String key, List<String> value, int cacheSeconds) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                jedis.del(key);
            }
            result = jedis.rpush(key, (String[]) value.toArray());
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("setList {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setList {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 设置List缓存
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */

    public long setObjectList(String key, List<Object> value, int cacheSeconds) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getBytesKey(key))) {
                jedis.del(key);
            }
            List<byte[]> list = Lists.newArrayList();
            for (Object o : value) {
                list.add(toBytes(o));
            }
            result = jedis.rpush(getBytesKey(key), (byte[][]) list.toArray());
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("setObjectList {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setObjectList {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 向List缓存中添加值
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public long listAdd(String key, String... value) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.rpush(key, value);
            logger.debug("listAdd {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("listAdd {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 向List缓存中添加值
     *
     * @param key   键
     * @param value 值
     * @return
     */

    public long listObjectAdd(String key, Object... value) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            List<byte[]> list = Lists.newArrayList();
            for (Object o : value) {
                list.add(toBytes(o));
            }
            result = jedis.rpush(getBytesKey(key), (byte[][]) list.toArray());
            logger.debug("listObjectAdd {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("listObjectAdd {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */


    public Set<String> getSet(String key) {
        Set<String> value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                value = jedis.smembers(key);
                logger.debug("getSet {} = {}", key, value);
            }
        } catch (Exception e) {
            logger.warn("getSet {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return value;
    }


    public Set<Object> getObjectSet(String key) {
        Set<Object> value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getBytesKey(key))) {
                value = Sets.newHashSet();
                Set<byte[]> set = jedis.smembers(getBytesKey(key));
                for (byte[] bs : set) {
                    value.add(toObject(bs));
                }
                logger.debug("getObjectSet {} = {}", key, value);
            }
        } catch (Exception e) {
            logger.warn("getObjectSet {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return value;
    }


    public long setSet(String key, Set<String> value, int cacheSeconds) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                jedis.del(key);
            }
            result = jedis.sadd(key, (String[]) value.toArray());
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("setSet {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setSet {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public long setObjectSet(String key, Set<Object> value, int cacheSeconds) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getBytesKey(key))) {
                jedis.del(key);
            }
            Set<byte[]> set = Sets.newHashSet();
            for (Object o : value) {
                set.add(toBytes(o));
            }
            result = jedis.sadd(getBytesKey(key), (byte[][]) set.toArray());
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("setObjectSet {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setObjectSet {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public long setSetAdd(String key, String... value) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.sadd(key, value);
            logger.debug("setSetAdd {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setSetAdd {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public long setSetObjectAdd(String key, Object... value) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            Set<byte[]> set = Sets.newHashSet();
            for (Object o : value) {
                set.add(toBytes(o));
            }
            result = jedis.rpush(getBytesKey(key), (byte[][]) set.toArray());
            logger.debug("setSetObjectAdd {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setSetObjectAdd {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public Map<String, String> getMap(String key) {
        Map<String, String> value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                value = jedis.hgetAll(key);
                logger.debug("getMap {} = {}", key, value);
            }
        } catch (Exception e) {
            logger.warn("getMap {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return value;
    }


    public Map<String, Object> getObjectMap(String key) {
        Map<String, Object> value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getBytesKey(key))) {
                value = Maps.newHashMap();
                Map<byte[], byte[]> map = jedis.hgetAll(getBytesKey(key));
                for (Map.Entry<byte[], byte[]> e : map.entrySet()) {
                    value.put(StringUtils.toString(e.getKey()), toObject(e.getValue()));
                }
                logger.debug("getObjectMap {} = {}", key, value);
            }
        } catch (Exception e) {
            logger.warn("getObjectMap {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return value;
    }


    public String setMap(String key, Map<String, String> value, int cacheSeconds) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                jedis.del(key);
            }
            result = jedis.hmset(key, value);
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("setMap {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setMap {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String setObjectMap(String key, Map<String, Object> value, int cacheSeconds) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getBytesKey(key))) {
                jedis.del(key);
            }
            Map<byte[], byte[]> map = Maps.newHashMap();
            for (Map.Entry<String, Object> e : value.entrySet()) {
                map.put(getBytesKey(e.getKey()), toBytes(e.getValue()));
            }
            result = jedis.hmset(getBytesKey(key), (Map<byte[], byte[]>) map);
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
            logger.debug("setObjectMap  {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("setObjectMap {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String mapPut(String key, Map<String, String> value) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.hmset(key, value);
            logger.debug("mapPut {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("mapPut {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public String mapObjectPut(String key, Map<String, Object> value) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            Map<byte[], byte[]> map = Maps.newHashMap();
            for (Map.Entry<String, Object> e : value.entrySet()) {
                map.put(getBytesKey(e.getKey()), toBytes(e.getValue()));
            }
            result = jedis.hmset(getBytesKey(key), (Map<byte[], byte[]>) map);
            logger.debug("mapObjectPut {} = {}", key, value);
        } catch (Exception e) {
            logger.warn("mapObjectPut {} = {}", key, value, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 移除Map缓存中的值
     *
     * @param key   键
     * @param  mapKey
     * @return
     */
    public long mapRemove(String key, String mapKey) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.hdel(key, mapKey);
            logger.debug("mapRemove {}  {}", key, mapKey);
        } catch (Exception e) {
            logger.warn("mapRemove {}  {}", key, mapKey, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 移除Map缓存中的值
     *
     * @param key   键
     * @param
     * @return
     */
    public long mapObjectRemove(String key, String mapKey) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.hdel(getBytesKey(key), getBytesKey(mapKey));
            logger.debug("mapObjectRemove {}  {}", key, mapKey);
        } catch (Exception e) {
            logger.warn("mapObjectRemove {}  {}", key, mapKey, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 判断Map缓存中的Key是否存在
     *
     * @param key   键
     * @param  mapKey
     * @return
     */
    public boolean mapExists(String key, String mapKey) {
        boolean result = false;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.hexists(key, mapKey);
            logger.debug("mapExists {}  {}", key, mapKey);
        } catch (Exception e) {
            logger.warn("mapExists {}  {}", key, mapKey, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 判断Map缓存中的Key是否存在
     *
     * @param key   键
     * @param  mapKey
     * @return
     */
    public boolean mapObjectExists(String key, String mapKey) {
        boolean result = false;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.hexists(getBytesKey(key), getBytesKey(mapKey));
            logger.debug("mapObjectExists {}  {}", key, mapKey);
        } catch (Exception e) {
            logger.warn("mapObjectExists {}  {}", key, mapKey, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 删除缓存
     *
     * @param key 键
     * @return
     */
    public long del(String key) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                result = jedis.del(key);
                logger.debug("del {}", key);
            } else {
                logger.debug("del {} not exists", key);
            }
        } catch (Exception e) {
            logger.warn("del {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 删除缓存
     *
     * @param key 键
     * @return
     */
    public long delObject(String key) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getBytesKey(key))) {
                result = jedis.del(getBytesKey(key));
                logger.debug("delObject {}", key);
            } else {
                logger.debug("delObject {} not exists", key);
            }
        } catch (Exception e) {
            logger.warn("delObject {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 缓存是否存在
     *
     * @param key 键
     * @return
     */
    public boolean exists(String key) {
        boolean result = false;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.exists(key);
            logger.debug("exists {}", key);
        } catch (Exception e) {
            logger.warn("exists {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 缓存是否存在
     *
     * @param key 键
     * @return
     */
    public boolean existsObject(String key) {
        boolean result = false;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.exists(getBytesKey(key));
            logger.debug(" existsObject {}", key);
        } catch (Exception e) {
            logger.warn("existsObject {}", key, e);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    public void flushlikekey(String key) {
        Jedis jedis = null;
        try {
            jedis = getResource();
            Set<String> keysList = jedis.keys(key + "*");

            if (keysList.size() > 0) {
                String[] keys = new String[keysList.size()];
                keysList.toArray(keys);

                jedis.del(keys);
            } else {
                logger.info("flush like key with {} is fail beacuse list is empty", key);
            }

        } catch (Exception e) {
            logger.info("flush like key with {} is fail", key);
        } finally {
            returnResource(jedis);
        }
    }

    /**
     * 模糊清空缓存
     *
     * @param keys
     */

    public void flushlikekey(String... keys) {
        Jedis jedis = null;
        try {
            if (keys.length > 0) {
                jedis = getResource();
                jedis.del(keys);
            } else {
                logger.info("flush like key with keys is fail beacuse keys is empty");
            }
        } catch (Exception e) {

        } finally {
            returnResource(jedis);
        }
    }

    /**
     * 循环清空缓存
     *
     * @param key
     */

    public void flushlikekey_foreach(String key) {
        Jedis jedis = null;
        try {
            jedis = getResource();
            Set<String> keysList = jedis.keys(key + "*");
            for (String onekey : keysList
            ) {
                jedis.del(onekey);
            }
        } catch (Exception e) {
            logger.info("flush like key with {} is fail", key);
        } finally {
            returnResource(jedis);
        }
    }

    /**
     * 返回剩余时间 毫秒
     *
     * @param key
     * @return
     */

    public long pttl(String key) {
        Jedis jedis = null;
        long time = -1;
        try {
            jedis = jedisPool.getResource();
            time = jedis.pttl(key);
        } catch (JedisException e) {
            logger.warn("getResource.", e);
            returnBrokenResource(jedis);
            throw e;
        }
        return time;
    }

    /**
     * 返回剩余时间 秒
     *
     * @param key
     * @return
     */

    public long ttl(String key) {
        Jedis jedis = null;
        long time = -1;
        try {
            jedis = jedisPool.getResource();
            time = jedis.ttl(key);
        } catch (JedisException e) {
            logger.warn("getResource.", e);
            returnBrokenResource(jedis);
            throw e;
        }
        return time;
    }


    /**
     * 设置生存时间 秒
     *
     * @param key
     * @return
     */

    public long expire(String key, int seconds) {
        Jedis jedis = null;
        long status = 0;
        try {
            jedis = jedisPool.getResource();
            status = jedis.expire(key, seconds);
        } catch (JedisException e) {
            logger.warn("getResource.", e);
            returnBrokenResource(jedis);
            throw e;
        }
        return status;
    }

    /**
     * 设置生存时间 毫秒
     *
     * @param key
     * @return
     */

    public long pexpire(String key, long milliseconds) {
        Jedis jedis = null;
        long status = 0;
        try {
            jedis = jedisPool.getResource();
            status = jedis.pexpire(key, milliseconds);
        } catch (JedisException e) {
            logger.warn("getResource.", e);
            returnBrokenResource(jedis);
            throw e;
        }
        return status;
    }


    public Set<String> allkeys(String redisKeyStartWith) {
        Jedis jedis = null;
        Set<String> set = null;
        try {
            jedis = jedisPool.getResource();
            set = JeUtil.getScanSet(jedis, redisKeyStartWith);
        } catch (Exception e) {
            logger.warn("getResource.", e);
        } finally {
            returnBrokenResource(jedis);
        }
        return set;
    }


    public void deleteRedisKeyStartWith(String redisKeyStartWith) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            Set<String> allKeys = allkeys(redisKeyStartWith);

            String[] allkeys = allKeys.toArray(new String[allKeys.size()]);


            if (allKeys.size() > 0) {
                jedis.del(allkeys);
            }
//            String[] allkeys= (String[]) allKeys.toArray();

        } catch (Exception e) {
            logger.warn("getResource.", e);
        } finally {
            returnResource(jedis);
        }
    }


    public double getRedisVersion() {
        Jedis jedis = null;
        double version = 0.0;
        try {
            jedis = jedisPool.getResource();
            String rgex = "redis_version:(.*?)\r\n";
            version = Double.parseDouble(RegexUtil.getSubUtilSimple(jedis.info(), rgex).substring(0, 3));
        } catch (Exception e) {
            logger.warn("getRedisVersion.", e);
        } finally {
            returnBrokenResource(jedis);
        }
        return version;

    }

    public static double getRedisVersion(Jedis jedisClient) {
        double version = 0.0;
        try {
            String rgex = "redis_version:(.*?)\r\n";
            if(RegexUtil.getSubUtilSimple(jedisClient.info(), rgex).length()>=3){
                version = Double.parseDouble(RegexUtil.getSubUtilSimple(jedisClient.info(), rgex).substring(0, 3));
            }else {
                version=0.0;
            }

        } catch (Exception e) {
            logger.warn("getRedisVersion.", e);
        } finally {
            jedisClient.close();
        }
        return version;

    }


    public  void closePool() {
        if(jedisPool!=null) {
            jedisPool.close();
        }

    }
}
