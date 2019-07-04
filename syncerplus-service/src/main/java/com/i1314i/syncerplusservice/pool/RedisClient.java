package com.i1314i.syncerplusservice.pool;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Client;
import redis.clients.jedis.Protocol;

import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static redis.clients.jedis.Protocol.Command.RESTORE;
import static redis.clients.jedis.Protocol.toByteArray;


/**
 * RedisClient
 */
public class RedisClient extends Client {
    @Setter@Getter
    private Date createTime;
    @Setter@Getter
    private Date lastTime;
    @Setter@Getter
    private boolean aliveStatus;

    public RedisClient(final String host, final int port) {
        super(host, port);
        this.createTime=new Date();
        this.lastTime=new Date();
        this.aliveStatus=true;
    }

    public Object send(Protocol.Command cmd, final byte[]... args) {
        sendCommand(cmd, args);
        Object r = getOne();
        if (r instanceof byte[]) {
            return new String((byte[]) r, UTF_8);
        } else {
            return r;
        }
    }

    public Object send(final byte[] cmd, final byte[]... args) {
        return send(Protocol.Command.valueOf(new String(cmd, UTF_8).toUpperCase()), args);
    }

    public Object restore(byte[] key, long expired, byte[] dumped, boolean replace) {
        if (!replace) {
            return send(RESTORE, key, toByteArray(expired), dumped);
        } else {
            return send(RESTORE, key, toByteArray(expired), dumped, "REPLACE".getBytes());
        }
    }
}
