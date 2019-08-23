package com.i1314i.syncerplusservice.util;

import com.alibaba.fastjson.JSON;
import com.i1314i.syncerpluscommon.util.common.TemplateUtils;
import com.i1314i.syncerplusservice.constant.KeyValueEnum;
import com.i1314i.syncerplusservice.constant.RedisVersion;
import com.i1314i.syncerplusservice.entity.dto.RedisClusterDto;
import com.i1314i.syncerplusservice.entity.dto.RedisSyncDataDto;
import com.i1314i.syncerplusservice.pool.ConnectionPool;
import com.i1314i.syncerplusservice.pool.Impl.CommonPoolConnectionPoolImpl;
import com.i1314i.syncerplusservice.pool.Impl.ConnectionPoolImpl;
import com.i1314i.syncerplusservice.pool.RedisClient;
import com.i1314i.syncerplusservice.service.exception.TaskMsgException;
import com.i1314i.syncerplusservice.service.exception.TaskRestoreException;
import com.i1314i.syncerplusservice.util.Jedis.TestJedisClient;
import com.i1314i.syncerplusservice.util.Jedis.cluster.JedisClusterClient;
import com.i1314i.syncerplusservice.util.Jedis.cluster.SyncJedisClusterClient;
import com.i1314i.syncerplusservice.util.Jedis.cluster.pipelineCluster.JedisClusterPipeline;
import com.i1314i.syncerplusservice.util.Jedis.pool.JDJedisClientPool;
import com.i1314i.syncerplusservice.util.file.FileUtils;
import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.RedisURI;
import com.moilioncircle.redis.replicator.Replicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static redis.clients.jedis.Protocol.Command.AUTH;

@Slf4j
public class RedisUrlUtils {
    static Map<Double,Integer>rdbVersion=null;
    public static boolean checkRedisUrl(String uri) throws URISyntaxException {
        URI uriplus = new URI(uri);
        if (uriplus.getScheme() != null && uriplus.getScheme().equalsIgnoreCase("redis")) {
            return true;
        }
        return false;
    }

    public static boolean getRedisClientConnectState(String url, String name) throws TaskMsgException {
        RedisURI turi = null;
        RedisClient target = null;
        try {
            turi = new RedisURI(url);

            target = new RedisClient(turi.getHost(), turi.getPort());

            Configuration tconfig = Configuration.valueOf(turi);
            //获取password
            if (tconfig.getAuthPassword() != null) {
                Object auth = target.send(AUTH, tconfig.getAuthPassword().getBytes());
            }
            int i = 3;
            while (i > 0) {
                try {
                    String png = (String) target.send("PING".getBytes());
                    if (png.equals("PONG")) {
                        return true;
                    }
                    i--;
                } catch (Exception e) {
                    return false;
                }

            }


            throw new TaskMsgException("无法连接该reids");

        } catch (URISyntaxException e) {
            throw new TaskMsgException(name + ":连接链接不正确");
        } catch (JedisDataException e) {
            throw new TaskMsgException(name + ":" + e.getMessage());
        } catch (TaskRestoreException e) {
            throw new TaskMsgException(name + ":error:" + e.getMessage());
        } catch (Exception e) {
            throw new TaskMsgException(name + ":error:" + e.getMessage());
        } finally {
            if (null != target) {
                target.close();
            }
        }

    }






    public static RedisVersion selectSyncerVersion(String sourceUri, String targetUri) throws URISyntaxException {
        RedisURI sourceUriplus = new RedisURI(sourceUri);
        RedisURI targetUriplus = new RedisURI(targetUri);
        /**
         * 源目标
         */
        Jedis source = null;
        Jedis target = null;
        double sourceVersion = 0.0;
        double targetVersion = 0.0;
        try {
            source = new Jedis(sourceUriplus.getHost(), sourceUriplus.getPort());
            target = new Jedis(targetUriplus.getHost(), targetUriplus.getPort());
            Configuration sourceConfig = Configuration.valueOf(sourceUriplus);
            Configuration targetConfig = Configuration.valueOf(targetUriplus);

            //获取password
            if (sourceConfig.getAuthPassword() != null) {
                Object sourceAuth = source.auth(sourceConfig.getAuthPassword());
            }

            //获取password
            if (targetConfig.getAuthPassword() != null) {
                Object targetAuth = target.auth(targetConfig.getAuthPassword());
            }

            sourceVersion = TestJedisClient.getRedisVersion(source);
            targetVersion = TestJedisClient.getRedisVersion(target);

        } catch (Exception e) {

        } finally {
            if (source != null)
                source.close();
            if (target != null)
                target.close();
        }
        System.out.println(sourceVersion+": "+targetVersion);
        if (sourceVersion == targetVersion && targetVersion >= 3.0)
            return RedisVersion.SAME;
        else if (sourceVersion == targetVersion && targetVersion < 3.0) {
            return RedisVersion.LOWER;
        } else {
            return RedisVersion.OTHER;
        }
    }


    /**
     * 获取redis DB库的数目
     * @param sourceUriList
     * @param dbMap
     * @throws URISyntaxException
     * @throws TaskMsgException
     */
    public static void doCheckDbNum(Set<String> sourceUriList, Map<Integer,Integer> dbMap, KeyValueEnum type) throws URISyntaxException,TaskMsgException {
        if(dbMap==null||dbMap.size()==0){
            return;
        }
        int[]maxInt=getMapMaxInteger(dbMap);
        for (String sourceUri:sourceUriList) {
            RedisURI sourceUriplus = new RedisURI(sourceUri);
            Jedis source = null;
            try {
                source = new Jedis(sourceUriplus.getHost(), sourceUriplus.getPort());
                Configuration sourceConfig = Configuration.valueOf(sourceUriplus);
                //获取password
                if (sourceConfig.getAuthPassword() != null) {
                    Object sourceAuth = source.auth(sourceConfig.getAuthPassword());
                }

                List<String>databases=source.configGet("databases");
                int dbNum=256;
                if(null!=databases&&databases.size()==2){
                    dbNum= Integer.parseInt(databases.get(1));
                }

               if (type.equals(KeyValueEnum.KEY)){
                   if(maxInt[0]>dbNum){
                       throw new TaskMsgException("dbMaping中库号超出Redis库的最大大小 :["+maxInt[0]+"] : "+sourceUri);
                   }
               }

               if(type.equals(KeyValueEnum.VALUE)){
                   if(maxInt[1]>dbNum){
                       throw new TaskMsgException("dbMaping中库号超出Redis库的最大大小 :["+maxInt[1]+"] : "+sourceUri);
                   }
               }


            } catch (Exception e) {
                throw new TaskMsgException("sourceUri is error :"+e.getMessage());
            } finally {
                if (source != null)
                    source.close();
            }
        }
    }


    /**
     * 获取Map<Integer,Integer> key-value的最大值
     * @param dbMap
     * @return
     */
    public static synchronized   int[] getMapMaxInteger(Map<Integer, Integer> dbMap){
        int[]nums=new int[2];


//        List<Integer>list= dbMap.entrySet().stream().map(e ->e.getKey()).collect(Collectors.toList());
        nums[0] = dbMap.entrySet().stream().map(e ->e.getKey())
                .max(Comparator.comparing(i -> i))
                .get();
        nums[1] = dbMap.entrySet().stream().map(e ->e.getValue())
                .max(Comparator.comparing(i -> i))
                .get();

        return nums;
    }

    public static void main(String[] args) throws URISyntaxException, TaskMsgException {
        Map<Integer,Integer>map=new HashMap<>();
        map.put(1,11);
        map.put(2,22);
        map.put(3,33);
//        doCheckDbNum(Stream.of("redis://114.67.100.239:6379?authPassword=redistest0102").collect(Collectors.toList()),map,KeyValueEnum.VALUE);
//        System.out.println(JSON.toJSONString(getMapMaxInteger(map)));
    }
    /**
     * 获取客户端
     *
     * @param syncDataDto
     * @param turi
     * @return
     */

    public static synchronized TestJedisClient getJedisClient(RedisSyncDataDto syncDataDto, RedisURI turi) {
        JedisPoolConfig sourceConfig = new JedisPoolConfig();
        Configuration sourceCon = Configuration.valueOf(turi);
        sourceConfig.setMaxTotal(syncDataDto.getMaxPoolSize());
        sourceConfig.setMaxIdle(syncDataDto.getMinPoolSize());
        sourceConfig.setMinIdle(syncDataDto.getMinPoolSize());
        //当池内没有返回对象时，最大等待时间
        sourceConfig.setMaxWaitMillis(syncDataDto.getMaxWaitTime());
        sourceConfig.setTimeBetweenEvictionRunsMillis(syncDataDto.getTimeBetweenEvictionRunsMillis());
        sourceConfig.setTestOnReturn(true);
        sourceConfig.setTestOnBorrow(true);
        return new TestJedisClient(turi.getHost(), turi.getPort(), sourceConfig, sourceCon.getAuthPassword(), 0);
    }


    /**
     *获取JDJEDISCLIENT 链接
     * @param syncDataDto
     * @param turi
     * @return
     */
    public static synchronized JDJedisClientPool getJDJedisClient(RedisSyncDataDto syncDataDto, RedisURI turi) {
        JedisPoolConfig sourceConfig = new JedisPoolConfig();
        Configuration sourceCon = Configuration.valueOf(turi);
        sourceConfig.setMaxTotal(syncDataDto.getMaxPoolSize());
        sourceConfig.setMaxIdle(syncDataDto.getMinPoolSize());
        sourceConfig.setMinIdle(syncDataDto.getMinPoolSize());
        //当池内没有返回对象时，最大等待时间
        sourceConfig.setMaxWaitMillis(syncDataDto.getMaxWaitTime());
        sourceConfig.setTimeBetweenEvictionRunsMillis(syncDataDto.getTimeBetweenEvictionRunsMillis());
        sourceConfig.setTestOnReturn(true);
        sourceConfig.setTestOnBorrow(true);
        return new JDJedisClientPool(turi.getHost(), turi.getPort(), sourceConfig, sourceCon.getAuthPassword(), 0);
    }

    /**
     * 线程检查
     *
     * @param r
     */
    public static synchronized void doCheckTask(Replicator r, Thread thread) {
        /**
         * 当aliveMap中不存在此线程时关闭
         */
        if (!TaskMonitorUtils.getAliveThreadHashMap().containsKey(thread.getName())) {
            try {
                System.out.println("线程正准备关闭...." + thread.getName());
                if (!Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                }

                r.close();
                /**
                 * 清楚所有线程记录
                 */
                TaskMonitorUtils.removeAliveThread(thread.getName(), Thread.currentThread());
            } catch (IOException e) {
                TaskMonitorUtils.addDeadThread(thread.getName(), Thread.currentThread());
                log.info("数据同步关闭失败");
                e.printStackTrace();
            }
        }

    }


    public static synchronized void doCommandCheckTask(Replicator r) {
        /**
         * 当aliveMap中不存在此线程时关闭
         */
        if (!TaskMonitorUtils.getAliveThreadHashMap().containsKey(Thread.currentThread().getName())) {
            try {
                System.out.println("线程正准备关闭....");
                if (!Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                }

                r.close();
                /**
                 * 清楚所有线程记录
                 */
                TaskMonitorUtils.removeAliveThread(Thread.currentThread().getName(), Thread.currentThread());
            } catch (IOException e) {
                TaskMonitorUtils.addDeadThread(Thread.currentThread().getName(), Thread.currentThread());
                log.info("数据同步关闭失败");
                e.printStackTrace();
            }
        }

    }


    public static synchronized boolean doThreadisCloseCheckTask() {
        /**
         * 当aliveMap中不存在此线程时关闭
         */
        if (!TaskMonitorUtils.getAliveThreadHashMap().containsKey(Thread.currentThread().getName()))
            return true;

        return false;
    }

    public void clearPool(ConnectionPool pools, TestJedisClient targetJedisClientPool, TestJedisClient sourceJedisClientPool) {
        if (pools != null) {
            pools.close();
        }

        if (targetJedisClientPool != null) {
            targetJedisClientPool.closePool();
        }

        if (sourceJedisClientPool != null) {
            sourceJedisClientPool.closePool();
        }
    }

    public static synchronized boolean doThreadisCloseCheckTask(String name) {
        name = getOldName(name);
        /**
         * 当aliveMap中不存在此线程时关闭
         */
        if (!TaskMonitorUtils.getAliveThreadHashMap().containsKey(name))
            return true;

        return false;
    }


    public static synchronized ConnectionPool getConnectionPool() {
        ConnectionPool pools = null;

        if (StringUtils.isEmpty(TemplateUtils.getPropertiesdata("other.properties", "redispool.type")) || TemplateUtils.getPropertiesdata("other.properties", "redispool.type").trim().equals("commonpool")) {
            pools = new CommonPoolConnectionPoolImpl();
        } else if (TemplateUtils.getPropertiesdata("other.properties", "redispool.type").trim().equals("selefpool")) {
            pools = new ConnectionPoolImpl();
        }
        return pools;
    }

    public static synchronized SyncJedisClusterClient getConnectionClusterPool(RedisClusterDto syncDataDto) throws ParseException {
//        JedisClusterClient pools = new JedisClusterClient(syncDataDto.getTargetRedisAddress(),syncDataDto.getTargetPassword(),
//                syncDataDto.getMaxPoolSize(),
//               3000,
//                syncDataDto.getTimeBetweenEvictionRunsMillis(),15000);

        SyncJedisClusterClient pools=new SyncJedisClusterClient( syncDataDto.getTargetRedisAddress(),syncDataDto.getTargetPassword(),syncDataDto.getMaxPoolSize(),syncDataDto.getTargetUris().size(),syncDataDto.getMaxWaitTime(),10000);

        return pools;
    }





    public synchronized static String getNewName(String name) {
        return name + "SyncerO";
    }

    public synchronized static String getOldName(String name) {
        if (name.endsWith("SyncerO"))
            return name.substring(0, name.length() - 7);
        return name;
    }


    public static synchronized Integer getRdbVersion(double redisVersion){
        if(rdbVersion==null){
            rdbVersion= (Map<Double, Integer>) JSON.parse(FileUtils.getText(TemplateUtils.class.getClassLoader().getResourceAsStream(
                    "rdbsetting.json")));
        }
        return rdbVersion.get(redisVersion);
    }






}
