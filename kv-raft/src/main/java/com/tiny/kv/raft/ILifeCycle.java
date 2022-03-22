package com.tiny.kv.raft;

/**
 * @author: leo wang
 * @date: 2022-03-18
 * @description: 管理所有组件的生命周期
 **/
public interface ILifeCycle {
    /**
     * 组件初始化
     *
     * @throws Throwable
     */
    void init() throws Throwable;

    /**
     * 组件销毁
     *
     * @throws Throwable
     */
    void destroy() throws Throwable;
}
