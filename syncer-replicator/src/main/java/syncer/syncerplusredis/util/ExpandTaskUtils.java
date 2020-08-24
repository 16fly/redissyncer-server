package syncer.syncerplusredis.util;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import syncer.syncerplusredis.entity.TaskDataEntity;
import syncer.syncerplusredis.model.ExpandTaskModel;
import syncer.syncerplusredis.model.TaskModel;

/**
 * @author zhanenqiang
 * @Description 扩展字段工具
 * @Date 2020/8/24
 */
@Slf4j
public class ExpandTaskUtils {
    public static synchronized void  loadingExpandTaskData(TaskModel taskModel, TaskDataEntity dataEntity){
        ExpandTaskModel expand=null;
        try {
            expand= JSON.parseObject(taskModel.getExpandJson(), ExpandTaskModel.class);
        }catch (Exception e){
            expand=new ExpandTaskModel();
            log.error("[{}]任务扩展JSON逆序列化失败,重新置零",taskModel.getId());
            e.printStackTrace();

        }
        dataEntity.setExpandTaskModel(expand);
    }
}
