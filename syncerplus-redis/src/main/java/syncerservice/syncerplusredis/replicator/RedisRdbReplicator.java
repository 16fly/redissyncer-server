/*
 * Copyright 2016-2018 Leon Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package syncerservice.syncerplusredis.replicator;

import syncerservice.syncerplusredis.entity.Configuration;
import syncerservice.syncerplusredis.exception.IncrementException;
import syncerservice.syncerplusredis.exception.TaskMsgException;
import syncerservice.syncerplusredis.io.RedisInputStream;
import syncerservice.syncerplusredis.rdb.RdbParser;
import syncerservice.syncerplusredis.util.TaskMsgUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * @author Leon Chen
 * @since 2.1.0
 */

@Slf4j
public class RedisRdbReplicator extends AbstractReplicator {
    
    public RedisRdbReplicator(File file, Configuration configuration) throws FileNotFoundException {
        this(new FileInputStream(file), configuration);
    }
    
    public RedisRdbReplicator(InputStream in, Configuration configuration) {
        Objects.requireNonNull(in);
        Objects.requireNonNull(configuration);
        this.configuration = configuration;
        this.inputStream = new RedisInputStream(in, this.configuration.getBufferSize());
        this.inputStream.setRawByteListeners(this.rawByteListeners);
        if (configuration.isUseDefaultExceptionListener())
            addExceptionListener(new DefaultExceptionListener());
    }


    public RedisRdbReplicator(String filePath, Configuration configuration, String taskId) {
        InputStream in = null;
        try {
            in = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            try {
                Map<String, String> msg = TaskMsgUtils.brokenCreateThread(Arrays.asList(taskId),"文件读取异常");
            } catch (TaskMsgException ex) {
                ex.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, "文件下载异常");
        }

        Objects.requireNonNull(in);
        Objects.requireNonNull(configuration);
        this.configuration = configuration;
        this.inputStream = new RedisInputStream(in, this.configuration.getBufferSize());
        this.inputStream.setRawByteListeners(this.rawByteListeners);
        if (configuration.isUseDefaultExceptionListener())
            addExceptionListener(new DefaultExceptionListener());
    }

    
    @Override
    public void open() throws IOException, IncrementException {
        super.open();
        if (!compareAndSet(Status.DISCONNECTED, Status.CONNECTED)) return;
        try {
            doOpen();
        } catch (UncheckedIOException e) {
            if (!(e.getCause() instanceof EOFException)) throw e.getCause();
        } finally {
            doClose();
            doCloseListener(this);
        }
    }

    @Override
    public void open(String taskId) throws IOException, IncrementException {
        super.open();
        if (!compareAndSet(Status.DISCONNECTED, Status.CONNECTED)) return;
        try {
            doOpen(taskId);
        } catch (UncheckedIOException e) {
            if (!(e.getCause() instanceof EOFException)) throw e.getCause();
        } finally {
            doClose();
            doCloseListener(this);
        }
    }

    protected void doOpen() throws IOException {
        try {
            new RdbParser(inputStream, this).parse();
        } catch (EOFException ignore) {
        }
    }

    protected void doOpen(String taskId) throws IOException {
        try {
            new RdbParser(inputStream, this).parse();
        } catch (EOFException ignore) {
            try {
                Map<String, String> msg = TaskMsgUtils.brokenCreateThread(Arrays.asList(taskId),ignore.getMessage());
            } catch (TaskMsgException ex) {
                ex.printStackTrace();
            }
            log.warn("任务Id【{}】异常停止，停止原因【{}】", taskId, ignore.getMessage());
        }
    }
}
