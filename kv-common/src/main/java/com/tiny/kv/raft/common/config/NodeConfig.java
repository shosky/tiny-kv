package com.tiny.kv.raft.common.config;

import lombok.Data;

import java.util.List;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
@Data
public class NodeConfig {

    /* 自身的selfPort */
    private int selfPort;
    /* 所有结点地址*/
    public List<String> peerAddrs;
}
