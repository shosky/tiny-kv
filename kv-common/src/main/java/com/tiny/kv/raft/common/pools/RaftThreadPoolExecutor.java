package com.tiny.kv.raft.common.pools;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: Raft线程池执行器
 **/
@Slf4j
public class RaftThreadPoolExecutor extends ThreadPoolExecutor {

    /*处理耗时监听线程局部变量*/
    private static final ThreadLocal<Long> COST_TIME_WATCH = ThreadLocal.withInitial(System::currentTimeMillis);
    /*处理任务数*/
    private final AtomicLong numTasks = new AtomicLong();
    /*总耗时*/
    private final AtomicLong totalTime = new AtomicLong();

    public RaftThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                  TimeUnit unit, BlockingQueue<Runnable> workQueue, RaftThreadPools.NameThreadFactory nameThreadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, nameThreadFactory);
    }


    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        COST_TIME_WATCH.get();
        log.debug(String.format("Thread %s: start %s", t, r));
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        long endTime = System.currentTimeMillis();
        long taskTime = endTime - COST_TIME_WATCH.get();
        numTasks.incrementAndGet();
        totalTime.addAndGet(taskTime);
        log.debug(String.format("Thread %s: end %s, time=%dns", t, r, taskTime));
        COST_TIME_WATCH.remove();
    }

    @Override
    protected void terminated() {
        log.info("active count : {}, queueSize : {}, poolSize : {}, avg: {}", getActiveCount(), getQueue().size(), getPoolSize(), totalTime.get() / COST_TIME_WATCH.get());
    }
}
