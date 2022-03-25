package com.tiny.kv.raft.rpc.handler;

import com.alibaba.fastjson.JSON;
import com.tiny.kv.raft.common.entity.*;
import com.tiny.kv.raft.core.impl.DefaultNode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-23
 * @description:
 **/
@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private DefaultNode node;

    public ServerHandler(DefaultNode node) {
        this.node = node;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        log.info(JSON.toJSONString(msg));
        RpcResponse response = new RpcResponse();
        response.setRequestId(msg.getRequestId());
        if (msg.getCmd() == RpcRequest.R_VOTE) {
            RvoteResult rvoteResult = node.handlerRequestVote(JSON.parseObject(JSON.toJSONString(msg.getObj()), RvoteParam.class));
            response.setResult(rvoteResult);
        } else if (msg.getCmd() == RpcRequest.A_ENTEIES) {
            AentryResult aentryResult = node.handlerAppendEntries(JSON.parseObject(JSON.toJSONString(msg.getObj()), AentryParam.class));
            response.setResult(aentryResult);
        } else if (msg.getCmd() == RpcRequest.CLIENT_REQ) {
            //客户端请求
            ClientKVAck clientKVAck = node.handlerClientRequest(JSON.parseObject(JSON.toJSONString(msg.getObj()), ClientKVReq.class));
            response.setResult(clientKVAck);
        }
        ctx.writeAndFlush(response);
    }
}
