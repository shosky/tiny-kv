package com.tiny.kv.raft.task;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: 心跳定时任务
 **/
@Slf4j
public class HeartBeatTask implements Runnable{
    @Override
    public void run() {
        log.info("heart task.");
    }
}
