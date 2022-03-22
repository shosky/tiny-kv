package com.tiny.kv.raft.common.pools;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: Raft工作线程
 **/
@Slf4j
public class RaftThread extends Thread {

    private static final UncaughtExceptionHandler exceptionHandler = (t, e) -> log.warn("Exception occurred from thread {}", t.getName(), e);

    public RaftThread(String threadName, Runnable r) {
        super(r, threadName);
        /**
         * 检测出某个由于未捕获的异常而终结的情况.
         */
        setUncaughtExceptionHandler(exceptionHandler);
    }
}
