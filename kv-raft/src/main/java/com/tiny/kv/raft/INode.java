package com.tiny.kv.raft;

import com.tiny.kv.raft.common.config.NodeConfig;
import com.tiny.kv.raft.common.entity.RvoteParam;
import com.tiny.kv.raft.common.entity.RvoteResult;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: Raft节点抽象
 **/
public interface INode {

    /**
     * 设置配置文件
     *
     * @param config
     */
    void setConfig(NodeConfig config);

    /**
     * 处理请求投票 RPC
     *
     * @param param
     * @return
     */
    RvoteResult handlerRequestVote(RvoteParam param);

    /**
     * 处理附加日志请求
     *
     * @param param
     * @return
     */
    //AentryResult handlerAppendEntries(AentryParam param);

    /**
     * 处理客户端请求
     *
     * @param request
     * @return
     */
    //ClientKVAck handlerClientRequest(ClientKVReq request);

    /**
     * 转发给 leader 节点.
     */
    //ClientKVAck redirect(ClientKVReq request);
}
