package com.tiny.kv.raft.task;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: 复制失败重试定时任务
 **/
@Slf4j
public class ReplicationFailTask implements Runnable {
    @Override
    public void run() {
      log.info("replcation fail task.");
    }
}
