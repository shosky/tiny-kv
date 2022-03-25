package com.tiny.kv.raft.rpc.handler;

import com.tiny.kv.raft.common.entity.RpcResponse;
import com.tiny.kv.raft.rpc.client.RpcFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: leo wang
 * @date: 2022-03-24
 * @description:
 **/
public class RpcRequestHolder {

    public final static AtomicLong REQUEST_ID_GEN = new AtomicLong(0);

    /**
     * 异步回调Map
     */
    public static final Map<Long, RpcFuture<RpcResponse>> REQUEST_MAP = new ConcurrentHashMap<>();
}
