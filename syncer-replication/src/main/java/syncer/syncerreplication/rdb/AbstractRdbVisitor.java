package syncer.syncerreplication.rdb;

import syncer.syncerreplication.event.Event;
import syncer.syncerreplication.io.stream.RedisInputStream;
import syncer.syncerreplication.rdb.datatype.ContextKeyValuePair;
import syncer.syncerreplication.rdb.datatype.DB;

import java.io.IOException;

/**
 * @author zhanenqiang
 * @Description 描述
 * @Date 2020/4/7
 */
public abstract class AbstractRdbVisitor {

    /*
     * rdb prefix
     */

    public String applyMagic(RedisInputStream in) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public int applyVersion(RedisInputStream in) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public int applyType(RedisInputStream in) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    /*
     * DB
     */

    public DB applySelectDB(RedisInputStream in, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public DB applyResizeDB(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    /*
     * checksum
     */
    public long applyEof(RedisInputStream in, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    /*
     * aux
     */
    public Event applyAux(RedisInputStream in, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyModuleAux(RedisInputStream in, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    /*
     * entity
     */
    public Event applyExpireTime(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyExpireTimeMs(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyFreq(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyIdle(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyString(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyList(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applySet(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyZSet(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyZSet2(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyHash(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyHashZipMap(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyListZipList(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applySetIntSet(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyZSetZipList(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyHashZipList(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyListQuickList(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyModule(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyModule2(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyStreamListPacks(RedisInputStream in, int version, ContextKeyValuePair context) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }
}
