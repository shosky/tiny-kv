package com.tiny.kv.raft.rpc.client;

import com.tiny.kv.raft.common.entity.RpcRequest;
import com.tiny.kv.raft.common.entity.RpcResponse;
import com.tiny.kv.raft.core.impl.DefaultNode;
import com.tiny.kv.raft.rpc.codec.JSONSerializer;
import com.tiny.kv.raft.rpc.codec.RpcDecoder;
import com.tiny.kv.raft.rpc.codec.RpcEncoder;
import com.tiny.kv.raft.rpc.handler.ClientHandler;
import com.tiny.kv.raft.rpc.handler.RpcRequestHolder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author: leo wang
 * @date: 2022-03-23
 * @description:
 **/
@Slf4j
public class RpcClient {
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    private RpcClient() {
    }

    public static RpcClient getInstance() {
        return RpcClientHolder.INSTANCE;
    }

    private static class RpcClientHolder {
        private static RpcClient INSTANCE = new RpcClient();
    }

    /**
     * 启动Client
     */
    public void startRpcClient(DefaultNode node) {
        bootstrap = new Bootstrap();
        bootstrap
                .group(eventLoopGroup)
                // 指定Channel
                .channel(NioSocketChannel.class)
                // 将小的数据包包装成更大的帧进行传送，提高网络的负载,即TCP延迟传输
                .option(ChannelOption.SO_KEEPALIVE, true)
                // 将小的数据包包装成更大的帧进行传送，提高网络的负载,即TCP延迟传输
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new RpcDecoder(RpcResponse.class, new JSONSerializer()))
                                .addLast(new RpcEncoder(RpcRequest.class, new JSONSerializer()))
                                .addLast(new ClientHandler(node));
                    }
                });
    }

    /**
     * 发送消息
     *
     * @param addr
     * @param request
     * @throws InterruptedException
     */
    public Object sendSyncMsg(String addr, RpcRequest request) throws InterruptedException, TimeoutException, ExecutionException {
        String[] addrs = addr.split(":");
        String host = addrs[0];
        int port = Integer.valueOf(addrs[1]);
        RpcFuture<RpcResponse> rpcFuture = new RpcFuture<>(new DefaultPromise<>(new DefaultEventLoop()), 3000);

        ChannelFuture future = bootstrap.connect(host, port).sync();

        Long requestId = RpcRequestHolder.REQUEST_ID_GEN.incrementAndGet();
        request.setRequestId(requestId);
        RpcRequestHolder.REQUEST_MAP.put(requestId, rpcFuture);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("===========发送成功");
                } else {
                    log.error("------------------发送失败");
                }
            }
        });
        future.channel().writeAndFlush(request);
        return rpcFuture.getPromise().get(3000,TimeUnit.MILLISECONDS);
    }

    public void destroy() {
        eventLoopGroup.shutdownGracefully();
    }
}
