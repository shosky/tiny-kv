package com.tiny.kv.storage;

import java.util.Collection;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description:
 **/
public interface IStorageService<T> {

    /**
     * 存储数据
     *
     * @param key
     * @param obj
     * @return
     */
    void add(String key, T obj);

    /**
     * 批量存储数据
     *
     * @param objs
     */
    void batchAdd(Collection<T> objs);

    /**
     * 获取数据
     *
     * @param key
     * @return
     */
    T get(String key);

    /**
     * 删除数据
     *
     * @param key
     * @return
     */
    int delete(String key);
}
