package com.i1314i.syncerpluswebapp.controller.v1.api;

import com.alibaba.fastjson.JSON;
import com.i1314i.syncerpluscommon.entity.ResultMap;
import com.i1314i.syncerpluscommon.util.common.TemplateUtils;
import com.i1314i.syncerplusredis.constant.ThreadStatusEnum;
import com.i1314i.syncerplusredis.entity.RedisPoolProps;
import com.i1314i.syncerplusredis.entity.dto.RedisClusterDto;
import com.i1314i.syncerplusredis.entity.dto.RedisFileDataDto;
import com.i1314i.syncerplusredis.entity.thread.ThreadMsgEntity;
import com.i1314i.syncerplusredis.exception.TaskMsgException;
import com.i1314i.syncerplusredis.util.TaskMsgUtils;
import com.i1314i.syncerplusservice.service.IRedisReplicatorService;
import com.i1314i.syncerpluswebapp.util.DtoCheckUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping(value = "/api/v1/file")
@Validated
public class RedisFileReplicatorController {
    @Autowired
    IRedisReplicatorService redisBatchedReplicatorService;
    @Autowired
    RedisPoolProps redisPoolProps;
    @RequestMapping(value = "/creattask",method = {RequestMethod.POST},produces="application/json;charset=utf-8;")

    public ResultMap test(@RequestBody @Validated  RedisFileDataDto redisFileDataDto) throws TaskMsgException {

        redisFileDataDto.setRedisFileDataDto(redisPoolProps.getMinPoolSize(),
                redisPoolProps.getMaxPoolSize(),
                redisPoolProps.getMaxWaitTime(),
                redisPoolProps.getTimeBetweenEvictionRunsMillis(),
                redisPoolProps.getIdleTimeRunsMillis());


        RedisClusterDto redisClusterDto=new RedisClusterDto(redisPoolProps.getMinPoolSize(),
                redisPoolProps.getMaxPoolSize(),
                redisPoolProps.getMaxWaitTime(),
                redisPoolProps.getTimeBetweenEvictionRunsMillis(),
                redisPoolProps.getIdleTimeRunsMillis());
        redisClusterDto.setDbNum(redisFileDataDto.getDbNum());
        DtoCheckUtils.updateUri(redisFileDataDto);

        BeanUtils.copyProperties(redisFileDataDto,redisClusterDto);

        redisClusterDto.setSourceRedisAddress(redisFileDataDto.getFileAddress());
        redisClusterDto.setTargetUriData(redisFileDataDto.getTargetUriData());
        redisClusterDto.setTargetUris(redisFileDataDto.getTargetUris());



        String threadId= TemplateUtils.uuid();


        String threadName=redisFileDataDto.getTaskName();
        if(StringUtils.isEmpty(threadName)){
            threadName=threadId;
            redisFileDataDto.setTaskName(threadId);
        }

        ThreadMsgEntity msgEntity=ThreadMsgEntity.builder().id(threadId)
                .status(ThreadStatusEnum.CREATE)
                .taskName(threadName)
                .redisClusterDto(redisClusterDto)
                .build();
        try {
            TaskMsgUtils.addAliveThread(threadId, msgEntity);

        if(redisClusterDto.isAutostart()){
            redisBatchedReplicatorService.filebatchedSync(redisClusterDto,threadId);
            msgEntity.setStatus(ThreadStatusEnum.RUN);
        }else {
            msgEntity.getRedisClusterDto().setAfresh(true);
        }

        }catch (TaskMsgException ex){
            throw ex;
        } catch (Exception e){
            return  ResultMap.builder().code("1000").msg("Failed to create task");
        }

        HashMap msg=new HashMap();
        msg.put("taskid",threadId);
        return  ResultMap.builder().code("2000").msg("Task created successfully").data(msg);
//        return ResultMap.getInstance().data(redisClusterDto);
    }
}
