package com.tiny.kv.raft.rpc.server;

import com.tiny.kv.raft.common.entity.RpcRequest;
import com.tiny.kv.raft.common.entity.RpcResponse;
import com.tiny.kv.raft.core.impl.DefaultNode;
import com.tiny.kv.raft.rpc.codec.JSONSerializer;
import com.tiny.kv.raft.rpc.codec.RpcDecoder;
import com.tiny.kv.raft.rpc.codec.RpcEncoder;
import com.tiny.kv.raft.rpc.handler.ClientHandler;
import com.tiny.kv.raft.rpc.handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-23
 * @description:
 **/
@Slf4j
public class RpcServer {
    EventLoopGroup boss;
    EventLoopGroup worker;
    private ServerBootstrap bootstrap;
    private String serverAddress;
    private Integer serverPort;

    private RpcServer() {
    }

    public static RpcServer getInstance() {
        return RaftRpcServerHolder.INSTANCE;
    }

    private static class RaftRpcServerHolder {
        private static RpcServer INSTANCE = new RpcServer();
    }

    /**
     * 服务提供者采用的是主从 Reactor 线程模型，启动过程包括配置线程池、Channel 初始化、端口绑定三个步骤。
     *
     * @throws Exception
     */
    public void startRpcServer(DefaultNode node) throws Exception {
        boss = new NioEventLoopGroup();
        worker = new NioEventLoopGroup();
        serverAddress = "127.0.0.1";
        serverPort = Integer.valueOf(System.getenv("SERVICE_PORT"));
        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new RpcDecoder(RpcRequest.class, new JSONSerializer()))
                                    .addLast(new RpcEncoder(RpcResponse.class, new JSONSerializer()))
                                    .addLast(new ServerHandler(node));
                        }
                    }).childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelFuture = bootstrap.bind(serverAddress, serverPort).sync();
            log.info("server addr {} started on port {}", serverAddress, serverPort);
            channelFuture.channel().closeFuture().sync();
        } finally {
            destory();
        }
    }

    public void destory() {
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }
}
