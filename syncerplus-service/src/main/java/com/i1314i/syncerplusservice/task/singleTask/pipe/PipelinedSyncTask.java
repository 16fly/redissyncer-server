package com.i1314i.syncerplusservice.task.singleTask.pipe;

import com.i1314i.syncerplusredis.entity.RedisURI;
import com.i1314i.syncerplusservice.entity.SyncTaskEntity;
import com.i1314i.syncerplusservice.rdbtask.single.pipeline.PipelineLock;
import com.i1314i.syncerplusservice.util.TaskMsgUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * 管道提交检测线程
 */
@Slf4j
public class PipelinedSyncTask implements Callable<Object> {
    private Pipeline pipelined;
    private PipelineLock pipelineLock;
    private SyncTaskEntity taskEntity;
    private LockPipe lockPipe;
    private boolean status=true;
    private String taskId;
    private RedisURI suri;
    private RedisURI turi;
    public PipelinedSyncTask(Pipeline pipelined, SyncTaskEntity taskEntity, LockPipe lockPipe) {
        this.pipelined = pipelined;
        this.taskEntity = taskEntity;
        this.lockPipe = lockPipe;
        taskId=null;
    }

    public PipelinedSyncTask(PipelineLock pipelineLock, SyncTaskEntity taskEntity, LockPipe lockPipe, String taskId, RedisURI suri, RedisURI turi) {
        this.pipelineLock = pipelineLock;
        this.taskEntity = taskEntity;
        this.lockPipe = lockPipe;
        this.taskId=taskId;
        this.suri=suri;
        this.turi=turi;
    }



    @Override
    public Object call() throws Exception {
        while (pipelineLock != null) {
            lockPipe.syncpipe(pipelineLock, taskEntity, 1, false,suri,turi);

            if (TaskMsgUtils.doThreadisCloseCheckTask(taskId)) {
                    if (status) {
                        Thread.currentThread().interrupt();
                        status = false;
                        System.out.println("【"+taskId+"】 PipelinedSyncTask关闭...." + Thread.currentThread().getName());
                        lockPipe.syncpipe(pipelineLock, taskEntity, 1, true,suri,turi);
                        pipelineLock.close();
                        break;
                    }

            }





            Thread.sleep(3000);
        }
        return null;
    }
}
