package com.i1314i.syncerplusservice.entity;

import com.i1314i.syncerplusservice.constant.RedisCommandTypeEnum;
import com.i1314i.syncerplusservice.entity.thread.EventTypeEntity;
import com.moilioncircle.redis.replicator.cmd.impl.DefaultCommand;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.DB;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter@Setter
@EqualsAndHashCode
public class EventEntity implements Serializable {
    private byte[]key;
    private long ms;
    private DB db;
    private EventTypeEntity typeEntity;
    private RedisCommandTypeEnum redisCommandTypeEnum;
    private DefaultCommand command;
    public EventEntity(byte[] key, long ms,DB db, EventTypeEntity typeEntity,RedisCommandTypeEnum redisCommandTypeEnum) {
        this.key = key;
        this.ms = ms;
        this.db=db;
        this.typeEntity = typeEntity;
        this.redisCommandTypeEnum =redisCommandTypeEnum;
    }

    public EventEntity(DB db, EventTypeEntity typeEntity, RedisCommandTypeEnum redisCommandTypeEnum, DefaultCommand command) {
        this.db = db;
        this.typeEntity = typeEntity;
        this.redisCommandTypeEnum = redisCommandTypeEnum;
        this.command = command;
    }
}
