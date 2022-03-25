package com.tiny.kv.raft.rpc.handler;

import com.tiny.kv.raft.common.entity.RpcResponse;
import com.tiny.kv.raft.core.impl.DefaultNode;
import com.tiny.kv.raft.rpc.client.RpcFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-23
 * @description:
 **/
@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private final DefaultNode node;

    public ClientHandler(DefaultNode node) {
        this.node = node;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        long requestId = msg.getRequestId();
        RpcFuture future = RpcRequestHolder.REQUEST_MAP.remove(requestId);
        future.getPromise().setSuccess(msg);
    }

}
