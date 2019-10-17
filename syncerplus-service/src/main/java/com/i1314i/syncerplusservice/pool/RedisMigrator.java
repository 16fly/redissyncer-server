package com.i1314i.syncerplusservice.pool;


import com.i1314i.syncerplusredis.cmd.CommandName;
import com.i1314i.syncerplusredis.cmd.parser.*;
import com.i1314i.syncerplusredis.rdb.dump.DumpRdbVisitor;
import com.i1314i.syncerplusredis.rdb.iterable.ValueIterableRdbVisitor;
import com.i1314i.syncerplusredis.replicator.Replicator;
import com.i1314i.syncerplusservice.rdb.vister.DefaultRdbPlusVisitor;


public class RedisMigrator {
    public synchronized static Replicator dress(Replicator r) {
        r.getConfiguration();
        r.setRdbVisitor(new DumpRdbVisitor(r));

//        r.addCommandParser(CommandName.name("PING"), new PingParser());
//        r.addCommandParser(CommandName.name("APPEND"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SET"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SETEX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("MSET"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("DEL"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SADD"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("HMSET"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("HSET"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("LSET"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("EXPIRE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("EXPIREAT"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("GETSET"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("HSETNX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("MSETNX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("PSETEX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SETNX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SETRANGE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("HDEL"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("LPOP"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("LPUSH"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("LPUSHX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("LRem"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("RPOP"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("RPUSH"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("RPUSHX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("ZREM"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("RENAME"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("INCR"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("DECR"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("INCRBY"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("DECRBY"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("PERSIST"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SELECT"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("FLUSHALL"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("FLUSHDB"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("HINCRBY"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("ZINCRBY"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("MOVE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SMOVE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("PFADD"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("PFCOUNT"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("PFMERGE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SDIFFSTORE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SINTERSTORE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SUNIONSTORE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("ZADD"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("ZINTERSTORE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("ZUNIONSTORE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("BRPOPLPUSH"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("LINSERT"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("RENAMENX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("RESTORE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("PEXPIRE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("PEXPIREAT"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("GEOADD"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("EVAL"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("EVALSHA"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SCRIPT"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("PUBLISH"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("BITOP"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("BITFIELD"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SETBIT"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SREM"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("UNLINK"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SWAPDB"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("MULTI"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("EXEC"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("ZREMRANGEBYSCORE"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("ZREMRANGEBYRANK"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("ZREMRANGEBYLEX"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("LTRIM"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("SORT"), new DefaultCommandParser());
//        r.addCommandParser(CommandName.name("RPOPLPUSH"), new DefaultCommandParser());
        return addCommandParser(r);
    }


    public synchronized static Replicator commandDress(Replicator r) {
        r.getConfiguration();
//        r.setRdbVisitor(new DefaultRdbPlusVisitor(r));
        return addCommandParser(r);
    }


    public synchronized static Replicator bacthedCommandDress(Replicator r) {
        r.getConfiguration();
        r.setRdbVisitor(new ValueIterableRdbVisitor(r));
        return addCommandParser(r);
    }



    public synchronized static Replicator newBacthedCommandDress(Replicator r) {
        r.getConfiguration();
        return addCommandParser(r);
    }

    public synchronized static Replicator addCommandParser(Replicator r) {
        r.addCommandParser(CommandName.name("PING"), new PingParser());
        r.addCommandParser(CommandName.name("REPLCONF"), new ReplConfParser());
        //
        r.addCommandParser(CommandName.name("APPEND"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SET"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SETEX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("MSET"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("DEL"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SADD"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("HMSET"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("HSET"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("LSET"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("EXPIRE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("EXPIREAT"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("GETSET"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("HSETNX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("MSETNX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("PSETEX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SETNX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SETRANGE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("HDEL"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("LPOP"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("LPUSH"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("LPUSHX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("LRem"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("RPOP"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("RPUSH"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("RPUSHX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZREM"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("RENAME"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("INCR"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("DECR"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("INCRBY"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("DECRBY"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("PERSIST"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SELECT"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("FLUSHALL"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("FLUSHDB"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("HINCRBY"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZINCRBY"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("MOVE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SMOVE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("PFADD"), new DefaultCommandParser());

//        r.addCommandParser(CommandName.name("PFADD"), new PFAddParser());
        r.addCommandParser(CommandName.name("PFCOUNT"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("PFMERGE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SDIFFSTORE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SINTERSTORE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SUNIONSTORE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZADD"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZINTERSTORE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZUNIONSTORE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("BRPOPLPUSH"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("LINSERT"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("RENAMENX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("RESTORE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("PEXPIRE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("PEXPIREAT"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("GEOADD"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("EVAL"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("EVALSHA"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SCRIPT"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("PUBLISH"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("BITOP"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("BITFIELD"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SETBIT"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SREM"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("UNLINK"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SWAPDB"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("MULTI"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("EXEC"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZREMRANGEBYSCORE"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZREMRANGEBYRANK"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZREMRANGEBYLEX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("LTRIM"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("SORT"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("RPOPLPUSH"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZPOPMIN"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("ZPOPMAX"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("XACK"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("XADD"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("XCLAIM"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("XDEL"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("XGROUP"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("XTRIM"), new DefaultCommandParser());
        r.addCommandParser(CommandName.name("XSETID"), new DefaultCommandParser());
        return r;
    }
}
