package com.tiny.kv.raft.common.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
@Data
@Builder
@NoArgsConstructor
public class RpcResponse<T> implements Serializable {

    private long requestId;

    private T result;

    public RpcResponse(T result) {
        this.result = result;
    }

    public RpcResponse(long requestId, T result) {
        this.requestId = requestId;
        this.result = result;
    }

    public static RpcResponse ok() {
        return new RpcResponse<>("ok");
    }

    public static RpcResponse fail() {
        return new RpcResponse<>("fail");
    }

}
