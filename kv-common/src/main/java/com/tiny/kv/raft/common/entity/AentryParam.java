package com.tiny.kv.raft.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author: leo wang
 * @date: 2022-03-24
 * @description:
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AentryParam extends BaseParam {

    //领导人Id
    String leaderId;
    //新的日志条目紧随之前的索引值
    long prevLogIndex;
    //prevLogIndex 条目的任期号
    long preLogTerm;
    //准备存储的日志条目(心跳：为空)
    LogEntry[] entries;
    //领导人已经提交的日志索引值
    long leaderCommit;
}
