package syncer.syncerservice.filter;

import com.alibaba.fastjson.JSON;
import io.swagger.models.auth.In;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import syncer.syncerplusredis.cmd.impl.DefaultCommand;
import syncer.syncerplusredis.entity.TaskDataEntity;
import syncer.syncerplusredis.event.Event;
import syncer.syncerplusredis.rdb.datatype.DB;
import syncer.syncerplusredis.rdb.dump.datatype.DumpKeyValuePair;
import syncer.syncerplusredis.rdb.iterable.datatype.BatchedKeyValuePair;
import syncer.syncerplusredis.replicator.Replicator;
import syncer.syncerplusredis.util.TaskDataManagerUtils;
import syncer.syncerservice.exception.FilterNodeException;
import syncer.syncerservice.exception.KeyWeed0utException;
import syncer.syncerservice.po.KeyValueEventEntity;
import syncer.syncerservice.util.JDRedisClient.JDRedisClient;
import syncer.syncerservice.util.common.Strings;

import java.util.Arrays;
import java.util.Map;

/**
 * db映射关系Filter
 */
@Builder
@Getter
@Setter
@Slf4j
public class KeyValueEventDBMappingFilter implements CommonFilter {
    private CommonFilter next;
    private JDRedisClient client;
    private String taskId;
    private Integer dbNum=0;

    public KeyValueEventDBMappingFilter(CommonFilter next, JDRedisClient client, String taskId) {
        this.next = next;
        this.client = client;
        this.taskId = taskId;
    }

    public KeyValueEventDBMappingFilter(CommonFilter next, JDRedisClient client, String taskId, Integer dbNum) {
        this.next = next;
        this.client = client;
        this.taskId = taskId;
        this.dbNum = dbNum;
    }

    @Override
    public void run(Replicator replicator, KeyValueEventEntity eventEntity) throws FilterNodeException {
        try {


        //DUMP格式数据
        Event event=eventEntity.getEvent();
        if (event instanceof DumpKeyValuePair) {
            DumpKeyValuePair dumpKeyValuePair= (DumpKeyValuePair) event;

            if(null==dumpKeyValuePair.getValue()||null==dumpKeyValuePair.getKey()){
                return;
            }
            DB db = dumpKeyValuePair.getDb();
            DB newDb=new DB();
            BeanUtils.copyProperties(db,newDb);
            try {
                dbMapping(eventEntity,newDb);
            } catch (KeyWeed0utException e) {
                log.info("全量数据key[{}]不符合DB映射规则，被抛弃..", JSON.toJSONString(eventEntity));
                //抛弃此kv
                return;
            }

          TaskDataManagerUtils.get(taskId).getRealKeyCount().incrementAndGet();
        }

        //分批格式数据
        if (event instanceof BatchedKeyValuePair<?, ?>) {
            BatchedKeyValuePair batchedKeyValuePair = (BatchedKeyValuePair) event;
            if((batchedKeyValuePair.getBatch()==0&&null==batchedKeyValuePair.getValue())||null==batchedKeyValuePair.getValue()){
                return;
            }



            DB db = batchedKeyValuePair.getDb();
            DB newDb=new DB();
            BeanUtils.copyProperties(db,newDb);
            try {
                dbMapping(eventEntity,newDb);
            } catch (KeyWeed0utException e) {
                log.info("全量数据key[{}]不符合DB映射规则，被抛弃..", JSON.toJSONString(eventEntity));
                //抛弃此kv
                return;
            }
        }

        //增量数据
        if (event instanceof DefaultCommand) {
            DefaultCommand defaultCommand = (DefaultCommand) event;
            if(Arrays.equals(defaultCommand.getCommand(),"SELECT".getBytes())) {
                int commDbNum = Integer.parseInt(new String(defaultCommand.getArgs()[0]));
                dbNum=commDbNum;
                try {
                    commanddbMapping(eventEntity,commDbNum,defaultCommand);
                } catch (KeyWeed0utException e) {
                    //抛弃此kv
                    log.info("增量数据key[{}]不符合DB映射规则，被抛弃..", JSON.toJSONString(eventEntity));
                    return;
                }
            }else{
                try {
                    commanddbMapping(eventEntity,dbNum);
                } catch (KeyWeed0utException e) {
                    //抛弃此kv
                    log.info("增量数据key[{}]不符合DB映射规则，被抛弃.. 原因[{}]", JSON.toJSONString(eventEntity),e.getMessage());
                    return;
                }
            }

            TaskDataManagerUtils.get(taskId).getRealKeyCount().incrementAndGet();
        }

        //继续执行下一Filter节点
        toNext(replicator,eventEntity);
        }catch (Exception e){
            throw new FilterNodeException(e.getMessage()+"->KeyValueEventDBMappingFilter",e.getCause());
        }
    }

    @Override
    public void toNext(Replicator replicator, KeyValueEventEntity eventEntity) throws FilterNodeException {
        if(null!=next) {
            next.run(replicator,eventEntity);
        }
    }

    @Override
    public void setNext(CommonFilter nextFilter) {
        this.next=nextFilter;
    }

    /**
     * 全量KV DBNUM映射
     * @param eventEntity
     * @param db
     * @throws KeyWeed0utException
     */
    void dbMapping(KeyValueEventEntity eventEntity,DB db) throws KeyWeed0utException {
        Event event=eventEntity.getEvent();
        long dbbnum=db.getDbNumber();
        int dbNumInt= Math.toIntExact(db.getDbNumber());

        if (null != eventEntity.getDbMapper() && eventEntity.getDbMapper().size() > 0) {
            if (eventEntity.getDbMapper().containsKey(dbNumInt)) {
                dbbnum = eventEntity.getDbMapper().get(dbNumInt);
            } else {
                //忽略本key
                throw new KeyWeed0utException("key抛弃");
            }
        }

        if(event instanceof DumpKeyValuePair) {
            DumpKeyValuePair dumpKeyValuePair= (DumpKeyValuePair) event;

//            DB dbn=dumpKeyValuePair.getDb();
//            dbn.setDbNumber(dbbnum);
//            dumpKeyValuePair.setDb(dbn);

            db.setDbNumber(dbbnum);
            dumpKeyValuePair.setDb(db);

            eventEntity.setEvent(dumpKeyValuePair);
        }else if(event instanceof BatchedKeyValuePair<?, ?>){
            BatchedKeyValuePair batchedKeyValuePair = (BatchedKeyValuePair) event;
//            DB dbn=batchedKeyValuePair.getDb();
//            dbn.setDbNumber(dbbnum);

            db.setDbNumber(dbbnum);
            batchedKeyValuePair.setDb(db);
            eventEntity.setEvent(batchedKeyValuePair);
        }
        eventEntity.setDbNum(dbbnum);

    }

    /**
     * 增量KV DBNUM映射
     * @param eventEntity
     * @param correntDbNum
     * @throws KeyWeed0utException
     */
    void commanddbMapping(KeyValueEventEntity eventEntity,Integer correntDbNum,DefaultCommand command) throws KeyWeed0utException {

        if (null != eventEntity.getDbMapper() && eventEntity.getDbMapper().size() > 0) {
            if (eventEntity.getDbMapper().containsKey(correntDbNum)) {
               Integer  newNum = eventEntity.getDbMapper().get(correntDbNum);
                byte[][]dbData=command.getArgs();
                dbData[0]=String.valueOf(newNum).getBytes();
                command.setArgs(dbData);
            } else {
                //忽略本key
                throw new KeyWeed0utException("key抛弃");
            }
        }
        eventEntity.setEvent(command);
    }

    void commanddbMapping(KeyValueEventEntity eventEntity,Integer correntDbNum) throws KeyWeed0utException {
        if (null != eventEntity.getDbMapper() && eventEntity.getDbMapper().size() > 0) {
            if (!eventEntity.getDbMapper().containsKey(correntDbNum)) {
                //忽略本key
                throw new KeyWeed0utException("key抛弃");
            }
        }
    }
}
