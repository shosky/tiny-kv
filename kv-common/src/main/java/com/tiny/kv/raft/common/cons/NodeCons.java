package com.tiny.kv.raft.common.cons;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: 节点常量
 **/
public class NodeCons {
    /**
     * 选举时间间隔基数
     */
    public static volatile long electionTime = 15 * 1000;
    /**
     * 上一次选举时间
     */
    public static volatile long preElectionTime = 0;

    /**
     * 上次一心跳时间戳
     */
    public static volatile long preHeartBeatTime = 0;
    /**
     * 心跳间隔基数
     */
    public static final long heartBeatTick = 5 * 1000;
}
