package com.tiny.kv.raft.rpc.codec;

import com.alibaba.fastjson.JSON;

/**
 * @author: leo wang
 * @date: 2022-03-24
 * @description:
 **/
public class JSONSerializer implements Serializer {

    @Override
    public byte[] serialize(Object object) {
        return JSON.toJSONBytes(object);
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return JSON.parseObject(bytes, clazz);
    }
}
