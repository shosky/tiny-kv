package com.tiny.kv.raft;

import com.tiny.kv.raft.common.entity.LogEntry;

/**
 * @author: leo wang
 * @date: 2022-03-18
 * @description: 日志模块接口
 **/
public interface ILogModule {

    /**
     * 写入日志
     *
     * @param logEntry
     */
    void write(LogEntry logEntry);

    /**
     * 读取日志
     *
     * @param index
     * @return
     */
    LogEntry read(Long index);

    /**
     * 删除从索引起始日志
     *
     * @param startIndex
     */
    void removeOnStartIndex(Long startIndex);

    LogEntry getLast();

    Long getLastIndex();
}
