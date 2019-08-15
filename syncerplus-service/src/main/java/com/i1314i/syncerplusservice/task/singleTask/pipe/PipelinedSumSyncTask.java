package com.i1314i.syncerplusservice.task.singleTask.pipe;

import com.i1314i.syncerplusservice.entity.SyncTaskEntity;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Pipeline;

import java.util.Date;
import java.util.concurrent.Callable;

/**
 * 管道提交检测线程
 */
@Slf4j
public class PipelinedSumSyncTask implements Callable<Object> {
    private Pipeline pipelined;
    private SyncTaskEntity taskEntity;

    public PipelinedSumSyncTask(Pipeline pipelined, SyncTaskEntity taskEntity) {
        this.pipelined = pipelined;
        this.taskEntity = taskEntity;


    }

    @Override
    public Object call() throws Exception {
        while (pipelined!=null){

            if(taskEntity.getSyncNums()>=1000){
                pipelined.sync();
                log.info("将管道中超过 {}个值提交",taskEntity.getSyncNums());
                taskEntity.clear();
            }
//            Thread.sleep(100);
        }
        return null;
    }
}
