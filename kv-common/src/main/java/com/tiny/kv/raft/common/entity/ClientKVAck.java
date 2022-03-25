package com.tiny.kv.raft.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-25
 * @description:
 **/
@Data
@Builder
@NoArgsConstructor
public class ClientKVAck implements Serializable {

    Object result;

    public ClientKVAck(Object result) {
        this.result = result;
    }

    public static ClientKVAck ok() {
        return new ClientKVAck("ok");
    }

    public static ClientKVAck fail() {
        return new ClientKVAck("fail");
    }
}
