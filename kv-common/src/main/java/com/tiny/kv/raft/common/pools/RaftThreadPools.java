package com.tiny.kv.raft.common.pools;

import java.util.concurrent.*;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: Raft线程池
 **/
public class RaftThreadPools {

    private static final int cpu = Runtime.getRuntime().availableProcessors();
    private static final int maxPoolSize = cpu * 2;
    private static final int queueSize = 1024;
    private static final long keepTime = 1000 * 60;
    private static TimeUnit keepTimeUnit = TimeUnit.MILLISECONDS;

    private static ThreadPoolExecutor te = getThreadPool();
    private static ScheduledExecutorService ss = getScheduled();

    /**
     * 带线程池的执行器
     *
     * @return
     */
    private static ThreadPoolExecutor getThreadPool() {
        return new RaftThreadPoolExecutor(
                cpu,
                maxPoolSize,
                keepTime,
                keepTimeUnit,
                new LinkedBlockingDeque<>(queueSize),
                new NameThreadFactory()
        );
    }

    /**
     * 在ExecutorService接口上再扩展，额外增加了定时、周期执行的能力
     *
     * @return
     */
    private static ScheduledExecutorService getScheduled() {
        return new ScheduledThreadPoolExecutor(cpu, new NameThreadFactory());
    }

    static class NameThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new RaftThread("Raft thread", r);
            t.setDaemon(true);
            t.setPriority(5);
            return t;
        }
    }

    /**
     * 创建一个给定初始延迟的间隔性的任务，之后的每次任务执行时间为 初始延迟 + N * delay(间隔)  。
     * 这里的N为首次任务执行之后的第N个任务，N从1开始，意识就是 首次执行任务的时间为12:00 那么下次任务的执行时间是固定的 是12:01 下下次为12:02。
     * 与scheduleWithFixedDelay 最大的区别就是 ，scheduleAtFixedRate  不受任务执行时间的影响。
     *
     * @param r
     * @param initDelay: 系统启动后，需要等待多久才开始执行。
     * @param period:    固定周期时间，按照一定频率来重复执行任务。
     */
    public static void scheduleAtFixedRate(Runnable r, long initDelay, long period) {
        ss.scheduleAtFixedRate(r, initDelay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 创建一个给定初始延迟的间隔性的任务，之后的下次执行时间是上一次任务从执行到结束所需要的时间+给定的间隔时间。
     * 举个例子：比如我给定任务的初始延迟(long initialdelay)是12:00， 间隔为1分钟 。
     * 那么这个任务会在12:00 首次创建并执行，如果该任务从执行到结束所需要消耗的时间为1分钟，那么下次任务执行的时间理应从12：01 再加上设定的间隔1分钟，
     * 那么下次任务执行时间是12:02 。这里的间隔时间（delay） 是从上次任务执行结束开始算起的。
     *
     * @param r
     * @param delay
     */
    public static void scheduleWithFixedDelay(Runnable r, long delay) {
        ss.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }

    public static <T> Future<T> submit(Callable r) {
        return te.submit(r);
    }

    public static void execute(Runnable r) {
        te.execute(r);
    }

    public static void execute(Runnable r, boolean sync) {
        if (sync) {
            r.run();
        } else {
            te.execute(r);
        }
    }

}
