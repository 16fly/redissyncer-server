package syncer.syncerplusredis.dao;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;
import syncer.syncerplusredis.model.RdbVersionModel;


import java.util.List;

/**
 * @author zhanenqiang
 * @Description 描述
 * @Date 2020/3/10
 */

//@Component
@Mapper
public interface RdbVersionMapper  {
    // 根据 ID 查询
    @Select("SELECT * FROM t_rdb_version")
    List<RdbVersionModel> selectAll()throws Exception;

    @Select("SELECT * FROM t_rdb_version WHERE id =#{id}")
    RdbVersionModel findRdbVersionModelById(@Param("id") Integer id)throws Exception;

    @Select("SELECT * FROM t_rdb_version WHERE redis_version =#{redisVersion}")
    RdbVersionModel findRdbVersionModelByRedisVersion(@Param("redisVersion") String redisVersion)throws Exception;

    @Select("SELECT * FROM t_rdb_version WHERE redis_version =#{redisVersion} and rdb_version =#{rdbVersion}")
    RdbVersionModel findRdbVersionModelByRedisVersionAndRdbVersion(@Param("redisVersion") String redisVersion,@Param("rdbVersion")Integer rdbVersion)throws Exception;

    @Select("SELECT * FROM t_rdb_version WHERE rdb_version =#{rdbVersion}")
    List<RdbVersionModel>findTaskByRdbVersion(@Param("rdbVersion")Integer rdbVersion)throws  Exception;

    @Insert("INSERT INTO t_rdb_version(redis_version,rdb_version) VALUES(#{redis_version},#{rdb_version})")
    boolean insertRdbVersionModel(RdbVersionModel rdbVersionModel)throws Exception;

    @Select("select count(*) from t_rdb_version")
    int countItem()throws Exception;

    @Select("UPDATE  t_rdb_version  set redis_version=#{redisVersion}, rdb_version =#{rdbVersion} WHERE id =#{id}")
    boolean updateRdbVersionModelById(@Param("id")Integer id,@Param("redisVersion") String redisVersion,@Param("rdbVersion")Integer rdbVersion)throws Exception;


    @Insert({
            "<script>",
            "insert into t_rdb_version(redis_version,rdb_version) values ",
            "<foreach collection='rdbVersionModelList' item='item' index='index' separator=','>",
            "(#{item.redis_version}, #{item.rdb_version})",
            "</foreach>",
            "</script>"
    })
    int insertRdbVersionModelList(@Param(value="rdbVersionModelList") List<RdbVersionModel> rdbVersionModelList);


    @Delete("DELETE FROM t_rdb_version WHERE id=#{id}")
    boolean deleteRdbVersionModelById(@Param("id")Integer id)throws Exception;

    @Delete("DELETE FROM t_rdb_version WHERE redis_version=#{redisVersion}")
    int deleteRdbVersionModelByRedisVersion(@Param("redisVersion") String redisVersion)throws Exception;

    @Delete("DELETE FROM t_rdb_version")
    int deleteAllRdbVersionModel();
}
