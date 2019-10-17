package com.i1314i.syncerplusservice.service.Impl;
import com.i1314i.syncerplusredis.constant.KeyValueEnum;
import com.i1314i.syncerplusredis.entity.RedisInfo;
import com.i1314i.syncerplusredis.entity.dto.RedisClusterDto;
import com.i1314i.syncerplusredis.entity.dto.RedisSyncDataDto;
import com.i1314i.syncerplusservice.rdbtask.cluster.ClusterDataRestoreTask;
import com.i1314i.syncerplusservice.rdbtask.single.pipeline.SingleDataPipelineRestoreTask;
import com.i1314i.syncerplusservice.service.IRedisReplicatorService;
import com.i1314i.syncerplusredis.exception.TaskMsgException;

import com.i1314i.syncerplusservice.util.RedisUrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.Set;

@Service("redisBatchedReplicatorService")
@Slf4j
public class IRedisBatchedReplicatorServiceImpl implements IRedisReplicatorService {
    @Autowired
    ThreadPoolTaskExecutor threadPoolTaskExecutor;






    @Override
    public void batchedSync(RedisClusterDto clusterDto,String taskId,boolean afresh) throws TaskMsgException {
        Set<String> sourceRedisUris = clusterDto.getSourceUris();
        Set<String> targetRedisUris = clusterDto.getTargetUris();

        for (String sourceUri : sourceRedisUris) {
            RedisUrlUtils.checkRedisUrl(sourceUri, " sourceUri: " + sourceUri);
        }

        for (String targetUri : targetRedisUris) {
            RedisUrlUtils.checkRedisUrl(targetUri, "sourceUri : " + targetUri);
        }


        /**
         * 存在dbMap时检查是否超出数据库大小
         */
        try {
            RedisUrlUtils.doCheckDbNum(sourceRedisUris, clusterDto.getDbNum(), KeyValueEnum.KEY);
        } catch (URISyntaxException e) {
            throw new TaskMsgException(e.getMessage());
        }

        try {
            RedisUrlUtils.doCheckDbNum(targetRedisUris, clusterDto.getDbNum(), KeyValueEnum.VALUE);
        } catch (URISyntaxException e) {
            throw new TaskMsgException(e.getMessage());
        }


        if (clusterDto.getTargetUris().size() == 1) {
            //单机 间或者往京东云集群迁移
            batchedSyncToSingle(clusterDto,taskId,afresh);

        } else if (clusterDto.getSourceUris().size() == 1 && clusterDto.getTargetUris().size() > 1) {



            //单机往cluster迁移
            batchedSyncSingleToCluster(clusterDto,taskId,afresh);
        } else {
            //cluster
            batchedSyncToCluster(clusterDto,taskId,afresh);
        }
    }


    /**
     * 往cluster迁移
     * @param clusterDto
     */
    private void batchedSyncToCluster(RedisClusterDto clusterDto,String taskId,boolean afresh) throws TaskMsgException {
        System.out.println("-----------------batchedSyncToCluster");
        Set<String> sourceRedisUris = clusterDto.getSourceUris();
        Set<String> targetRedisUris = clusterDto.getTargetUris();

        for (String sourceUri : sourceRedisUris) {
            RedisUrlUtils.checkRedisUrl(sourceUri, " sourceUri:" + sourceUri);
        }

        for (String targetUri : targetRedisUris) {
            RedisUrlUtils.checkRedisUrl(targetUri, " sourceUri: " + targetUri);
        }

        int i=0;
        for (String sourceUrl : sourceRedisUris) {
//            threadPoolTaskExecutor.submit(new BatchedKVClusterSyncTask(clusterDto, sourceUrl));
            threadPoolTaskExecutor.execute(new ClusterDataRestoreTask(clusterDto, (RedisInfo) clusterDto.getTargetUriData().toArray()[i],sourceUrl,taskId,afresh));
            i++;
        }


//        if(clusterDto.getPipeline()!=null&&clusterDto.getPipeline().toLowerCase().equals("on")){
//            for (String sourceUrl:sourceRedisUris){
//                threadPoolTaskExecutor.submit(new BatchedKVClusterSyncTask(clusterDto,sourceUrl));
//
//            }
//            log.info("cluster版本（>3.0）版本数据...(开启管道)");
////                log.info("同步不同版本（）版本数据...");
//        }else {
//            for (String sourceUrl:sourceRedisUris){
//                threadPoolTaskExecutor.submit(new ClusterRdbSameVersionJDCloudRestoreTask(clusterDto, sourceUrl));
//
//            }
//
//            log.info("cluster版本（>3.0）版本数据...(非管道)");
//        }

        log.info("--------集群到集群-------");


    }


    /**
     * 单机往cluster迁移
     * @param clusterDto
     */
    private void batchedSyncSingleToCluster(RedisClusterDto clusterDto,String taskId,boolean afresh) throws TaskMsgException {
        System.out.println("-----------------batchedSyncSingleToCluster");
        Set<String> sourceRedisUris = clusterDto.getSourceUris();
        Set<String> targetRedisUris = clusterDto.getTargetUris();

        for (String sourceUri : sourceRedisUris) {
            RedisUrlUtils.checkRedisUrl(sourceUri, " sourceUri: " + sourceUri);
        }

        for (String targetUri : targetRedisUris) {
            RedisUrlUtils.checkRedisUrl(targetUri, " sourceUri: " + targetUri);
        }


        threadPoolTaskExecutor.execute(new ClusterDataRestoreTask(clusterDto, (RedisInfo) clusterDto.getTargetUriData().toArray()[0], (String) sourceRedisUris.toArray()[0],taskId,afresh));
        log.info("--------单机到集群-------");


    }


    private void batchedSyncToSingle(RedisClusterDto clusterDto, String taskId,boolean afresh) {
        /**
         * 获取所有地址并处理新建线程进行同步
         */
        Set<String> sourceRedisUris = clusterDto.getSourceUris();
        Set<String> targetRedisUris = clusterDto.getTargetUris();
        int i = 0;
        for (String source : sourceRedisUris
        ) {
            RedisSyncDataDto syncDataDto = new RedisSyncDataDto();
            BeanUtils.copyProperties(clusterDto, syncDataDto);
            //set进去数据 copy完后线程池的信息都有 ----------疑问密码如何赋值？？？？？
            syncDataDto.setSourceUri(source);
            syncDataDto.setTargetUri(String.valueOf(targetRedisUris.toArray()[0]));
            //进行数据同步
            try {
                if (i == 0) {
                    syncTask(syncDataDto, source, String.valueOf(targetRedisUris.toArray()[0]), true,taskId,clusterDto.getBatchSize(),afresh);
                } else {
                    syncTask(syncDataDto, source, String.valueOf(targetRedisUris.toArray()[0]), false,taskId,clusterDto.getBatchSize(),afresh);
                }
                i++;
            } catch (TaskMsgException e) {
                e.printStackTrace();
            }
        }
    }




    /*
     * zzj add 提出判断连接的部分
     * */


    public void syncTask(RedisSyncDataDto syncDataDto, String sourceUri, String targetUri, boolean status,String taskId,int batchSize,boolean afresh) throws TaskMsgException {
//        if (status) {
//            if (TaskMonitorUtils.containsKeyAliveMap(syncDataDto.getThreadName())) {
//                throw new TaskMsgException(TaskMsgConstant.Task_MSG_PARSE_ERROR_CODE);
//            }
//        }

//SingleDataPipelineRestoreTask

        threadPoolTaskExecutor.execute(new SingleDataPipelineRestoreTask(syncDataDto, (RedisInfo) syncDataDto.getTargetUriData().toArray()[0],taskId,batchSize,afresh));

//        threadPoolTaskExecutor.execute(new SingleDataRestoreTask(syncDataDto, (RedisInfo) syncDataDto.getTargetUriData().toArray()[0],taskId));
    }
}
