package com.i1314i.syncerplusservice.util.Jedis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author 平行时空
 * @created 2018-06-14 21:52
 **/
public class ObjectUtils {
    /**
     * 序列化对象
     * @param object
     * @return
     */
    public static byte[] serialize(Object object) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try {
            if (object != null){
                baos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(baos);
                oos.writeObject(object);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 反序列化对象
     * @param bytes
     * @return
     */
    public static Object unserialize(byte[] bytes) {
        ByteArrayInputStream bais = null;
        try {
            if (bytes != null && bytes.length > 0){
                bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais);
                return ois.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }




    /**
     * 获取byte[]类型Key
     * @param key
     * @return
     */
    public synchronized static  byte[] getBytesKey(Object object){
        if(object instanceof String){
            return StringUtils.getBytes((String)object);
        }else{
            return ObjectUtils.serialize(object);
        }
    }

    /**
     * Object转换byte[]类型
     * @param key
     * @return
     */
    public synchronized static  byte[] toBytes(Object object){
        return ObjectUtils.serialize(object);
    }

    /**
     * byte[]型转换Object
     * @param key
     * @return
     */
    public synchronized static Object toObject(byte[] bytes){
        return ObjectUtils.unserialize(bytes);
    }
}
