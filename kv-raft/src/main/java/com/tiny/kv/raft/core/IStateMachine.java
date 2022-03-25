package com.tiny.kv.raft.core;

import com.tiny.kv.raft.common.entity.LogEntry;

/**
 * @author: leo wang
 * @date: 2022-03-18
 * @description: 状态机接口
 **/
public interface IStateMachine {

    /**
     * 将数据应用到状态机
     *
     * @param logEntry
     */
    void apply(LogEntry logEntry);

    /**
     * 获取日志条目
     *
     * @param key
     * @return
     */
    LogEntry get(String key);

    /**
     * 获取日志中数据
     *
     * @param key
     * @return
     */
    String getString(String key);

    /**
     * 存储数据
     *
     * @param key
     * @param value
     */
    void setString(String key, String value);

    /**
     * 删除数据
     *
     * @param key
     */
    void delString(String... key);
}
