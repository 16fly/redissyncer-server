package com.i1314i.syncerplusservice.pool;

import com.i1314i.syncerplusservice.service.exception.TaskRestoreException;
import com.i1314i.syncerplusservice.util.Jedis.IJedisClient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Client;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static redis.clients.jedis.Protocol.Command.RESTORE;
import static redis.clients.jedis.Protocol.toByteArray;


/**
 * RedisClient
 */
@EqualsAndHashCode
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

    public Object send(Protocol.Command cmd, final byte[]... args) throws TaskRestoreException {
        sendCommand(cmd, args);
        Object r=null;
        try{
             r = getOne();
        }catch (Exception e){
            if(e instanceof  JedisDataException)
                throw  new TaskRestoreException(e.getMessage());

        }



        if (r instanceof byte[]) {
            return new String((byte[]) r, UTF_8);
        } else {
            return r;
        }
    }


    public Object send(Protocol.Command cmd) throws TaskRestoreException {
        sendCommand(cmd);
        Object r=null;
        try{
            r = getOne();
        }catch (Exception e){
            if(e instanceof  JedisDataException)
                throw  new TaskRestoreException(e.getMessage());

        }



        if (r instanceof byte[]) {
            return new String((byte[]) r, UTF_8);
        } else {
            return r;
        }
    }



    public static boolean isNull(byte[] bs){
        if(bs.length==0||bs==null){//根据byte数组长度为0判断
            return true;
        }
        return false;
    }

    public Object send(final byte[] cmd, final byte[]... args) throws TaskRestoreException {
        return send(Protocol.Command.valueOf(new String(cmd, UTF_8).toUpperCase()), args);
    }

    public Object send(final byte[] cmd) throws TaskRestoreException {
        return send(Protocol.Command.valueOf(new String(cmd, UTF_8).toUpperCase()));
    }


    public Object restore(byte[] key, long expired, byte[] dumped, boolean replace) throws TaskRestoreException {
        if (!replace) {
            return send(RESTORE, key, toByteArray(expired), dumped);
        } else {
            return send(RESTORE, key, toByteArray(expired), dumped, "REPLACE".getBytes());
        }
    }

    public Object restore(byte[] key, long expired, byte[] dumped, boolean replace, IJedisClient jedisClientPool) throws TaskRestoreException {
        if (!replace) {
            return send(RESTORE, key, toByteArray(expired), dumped);
        } else {
            return send(RESTORE, key, toByteArray(expired), dumped, "REPLACE".getBytes());
        }
    }


}
