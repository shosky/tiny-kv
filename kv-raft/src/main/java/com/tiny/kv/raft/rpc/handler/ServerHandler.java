package com.tiny.kv.raft.rpc.handler;

import com.alibaba.fastjson.JSON;
import com.tiny.kv.raft.common.config.Peer;
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
        switch (msg.getCmd()) {
            case RpcRequest.R_VOTE:
                RvoteResult rvoteResult = node.handlerRequestVote(JSON.parseObject(JSON.toJSONString(msg.getObj()), RvoteParam.class));
                response.setResult(rvoteResult);
                break;
            case RpcRequest.A_ENTEIES:
                AentryResult aentryResult = node.handlerAppendEntries(JSON.parseObject(JSON.toJSONString(msg.getObj()), AentryParam.class));
                response.setResult(aentryResult);
                break;
            case RpcRequest.CLIENT_REQ:
                //客户端请求
                ClientKVAck clientKVAck = node.handlerClientRequest(JSON.parseObject(JSON.toJSONString(msg.getObj()), ClientKVReq.class));
                response.setResult(clientKVAck);
                break;
            case RpcRequest.CHANGE_CONFIG_ADD:
                MembershipChangeResult addPeerResult = node.addPeer(JSON.parseObject(JSON.toJSONString(msg.getObj()), Peer.class));
                response.setResult(addPeerResult);
                break;
            case RpcRequest.CHANGE_CONFIG_REMOVE:
                MembershipChangeResult removePeerResult = node.removePeer(JSON.parseObject(JSON.toJSONString(msg.getObj()), Peer.class));
                response.setResult(removePeerResult);
                break;
            default:
                log.error("未知的请求协议，拒绝处理");
                throw new RuntimeException("未知的请求协议");
        }
        ctx.writeAndFlush(response);
    }
}
