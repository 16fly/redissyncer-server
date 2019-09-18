### 创建任务接口

    http://116.196.115.143:8080/api/v1/creattask
    
    Method:POST
    
    请求头
    Content-Type:application/json
    
    请求体：
   
       
 * 字段描述
   
| field              | type               | example                                  | description                                                                                                                                                                  | requred |
| ------------------ | ------------------ | ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| dbNum              | map<string,string> | "dbNum": {"1": "1"}                      | redis db映射关系，当由此描述时任务按对应关系同步，未列出db不同步 ;无该字段的情况源与目标db一一对应,无该字段迁移源redis所有db库                                                                       | false   |
| sourceRedisAddress | string             | "sourceRedisAddress": "10.0.0.1:6379"    | 源redis地址，cluster集群模式下地址由';'分割，如"10.0.0.1:6379;10.0.0.2:6379"                                                                                                        | true    |
| sourcePassword     | string             | "sourcePassword": "sourcepasswd"         | 源redis密码默认值为""                                                                                                                                                        | false   |                                                                       | false   |
| targetRedisAddress | string             | "targetRedisAddress": "192.168.0.1:6379" | 目标redis地址 ,当目标redis为单实例或proxy时，填写单一地址即可，当目标redis为集群且需要借助jedis访问集群时地址用';'分割，"192.168.0.1:6379;192.168.0.3:6379;192.168.0.3:6379" | true    |
| targetPassword     | string             | "targetPassword": "xxx"                  | 目标redis密码 ，默认值为""                                                                                                                                                   | false   |
| targetRedisVersion      | string             | "targetversion":"4.0"                    | 目标redis版本, 该参数针对不可获取版本信息的情况，若可获取redis版本信息则按自动获取的版本信息进行处理                                                                         | false   |
| taskName           | string             | "taskname":"product2test"                | 自定义任务名称                                                                                                                                                               | false   |
| autostart          | bool               | "autostart":true                         | 是否创建后自动启动，默认值false                                                                                                                                              | false   |
| afresh          | bool               | "afresh":true                         | 如果之前进行过全量同步并且offset值还在积压缓冲区时，为false时则从offset+1值开始进行增量同步，为true时则进行全量同步，缺省默认值为true (注：创建接口时 afresh字段仅和autostart为true时同时使用，afresh字段当startTask为必填字段)                                                                                                                                              | false   |


### 错误码

| code              | msg               | data                | description |
| ------------------ | ------------------ | ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 4000          | 源/目标redis连接失败                   |                   |                  |
| 4001          | 任务URI信息有误，请检查             |                   |                  |  
| 4002          | 相同配置任务已存在，请修改任务名    |                   |                  |
| 4003          | 无法连接redis,请检查redis配置以及其可用性               |                  |
| 4024          | targetRedisVersion can not be empty /targetRedisVersion error |         |   |
| 4026          | dbMaping中库号超出Redis库的最大大小 |                   |                  |




##### 请求体事例：

    单机往单机迁移（如主从：推荐源redis节点使用从节点--目标redis节点使用目标主从的主节点）
    {
    	"sourcePassword": "password",
    	"sourceRedisAddress": "114.67.100.239:6379",
    	"targetRedisAddress": "127.0.0.1:8002",
    	"targetPassword": "password",
    	"taskName": "test",
    	"targetRedisVersion":2.8, 
    	"autostart":false,
    	"afresh":false
    }    
    
    多节点往单机迁移（如主从：推荐源redis节点使用从节点--目标redis节点使用目标主从的主节点）
    {
    	"sourcePassword": "password",
    	"sourceRedisAddress": "114.67.100.239:6379;114.67.100.238:6379",
    	"targetRedisAddress": "127.0.0.1:6379;",
       	"targetPassword": "password",
       	"taskName": "test",
       	"targetRedisVersion":2.8, 
       	"autostart":false
    }    
        

        cluster从多节点/单节点往集群迁移
        {
            "sourcePassword": "password",
            "sourceRedisAddress": "114.67.100.239:6379;114.67.100.238:6379",
            "targetRedisAddress": "127.0.0.1:8002;127.0.0.1:8002;127.0.0.1:8002;127.0.0.1:8002;127.0.0.1:8002;127.0.0.1:8002",
            "targetPassword": "password",
            "taskName": "test",
            "targetRedisVersion":2.8, 
            "autostart":false,
            "afresh":true
        }    
      
      返回值为：
      
      {
          "msg": "Task created successfully",
          "code": "200",
          "data": {
              "taskid": "89E601A6B23348BCB9B362C67BFB2926"
          }
      }
      

      
#### 启动任务接口 (只允许单任务)

    当创建任务接口参数 "autostart"设置为true时，创建完成任务会自动执行启动任务，当为flase时需使用本接口通过返回的taskid启动任务
    http://116.196.115.143:8080/api/v1/starttask
        
    Method:POST
    
    请求头
    Content-Type:application/json
    
    请求体：
    
    {
    	"taskid":"89E601A6B23348BCB9B362C67BFB2926",
    	"afresh":false
    }
        
#### 停止任务接口
    停止正在处于运行的迁移同步任务
    http://116.196.115.143:8080/api/v1/stoptask
        
    Method:POST
    
    请求头
    Content-Type:application/json
    
    请求体：
    
    {
    	"taskids":["89E601A6B23348BCB9B362C67BFB2926"]
    }
    
    
### 编辑任务接口

    http://116.196.115.143:8080/api/v1/edittask
    
    Method:POST
    
    请求头
    Content-Type:application/json
    
    请求体：
   
       
 * 字段描述
   
| field              | type               | example                                  | description                                                                                                                                                                  | requred |
| ------------------ | ------------------ | ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| dbNum              | map<string,string> | "dbNum": {"1": "1"}                      | redis db映射关系，当由此描述时任务按对应关系同步，未列出db不同步 ;无该字段的情况源与目标db一一对应,无该字段迁移源redis所有db库                                                                       | false   |
| sourceRedisAddress | string             | "sourceRedisAddress": "10.0.0.1:6379"    | 源redis地址，cluster集群模式下地址由';'分割，如"10.0.0.1:6379;10.0.0.2:6379"                                                                                                        | true    |
| sourcePassword     | string             | "sourcePassword": "sourcepasswd"         | 源redis密码默认值为""                                                                                                                                                        | false   |                                                                       | false   |
| targetRedisAddress | string             | "targetRedisAddress": "192.168.0.1:6379" | 目标redis地址 ,当目标redis为单实例或proxy时，填写单一地址即可，当目标redis为集群且需要借助jedis访问集群时地址用';'分割，"192.168.0.1:6379;192.168.0.3:6379;192.168.0.3:6379" | true    |
| targetPassword     | string             | "targetPassword": "xxx"                  | 目标redis密码 ，默认值为""                                                                                                                                                   | false   |
| targetRedisVersion      | string             | "targetversion":"4.0"                    | 目标redis版本, 该参数针对不可获取版本信息的情况，若可获取redis版本信息则按自动获取的版本信息进行处理                                                                         | false   |
| taskName           | string             | "taskname":"product2test"                | 自定义任务名称 | false   |                                
            
 #### 删除任务接口
 
     删除处于非运行状态的迁移同步任务
     
     请求地址：http://116.196.115.143:8080/api/v1/deletetask
         
     Method:POST
     
     请求头
     Content-Type:application/json
     
     请求体：
     
     {
     	"taskids":["89E601A6B23348BCB9B362C67BFB2926"]
     }
 #### 查看所有任务接口
 
     删除处于非运行状态的迁移同步任务
     请求地址：http://116.196.115.143:8080/api/v1/listtasks
         
     Method:POST
     
     请求头
     Content-Type:application/json
     
 * 字段描述
  
| field | type | example | description |requred |
| ----- | ---- | ------- | ----------- | ------ | 
|regulation|string|"regulation":"all"|任务查询规则all、bynames、byids、bystatus|true|
|tasknames|string[]|"tasknames":["name1","name2"]|byname查询时必填任务名|true when "regulation":"byname" |
|taskids|string[]|"taskids":["id1","id2"]|byid查询时必填任务id|true when "regulation":"byid" |
|taskstatus|string|"taskstatus":"live"|bystatus查询时必填任务状态live(正常运行)、stop(正常停止)、broken(非正常停止)|true when "regulation":"bystatus" |


    请求体事例：
      查询全部任务
    {
    	"regulation":"all"
    }
    
    
