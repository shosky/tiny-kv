package com.tiny.kv.raft;

import com.tiny.kv.raft.common.config.NodeConfig;
import com.tiny.kv.raft.impl.DefaultNode;
import com.tiny.kv.rpc.client.VoteRpcClient;
import com.tiny.kv.rpc.entity.Request;
import com.tiny.kv.rpc.util.SpringUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description:
 **/
@EnableConfigurationProperties
@SpringBootApplication
@ComponentScan(value = {"com.leo", "com.tiny"})
public class RaftNodeBootstrap {

    public static void main(String[] args) throws Throwable {

        SpringApplication.run(RaftNodeBootstrap.class, args);

        startRaft();
    }

    private static void startRaft() throws Throwable {
        DefaultNode node = DefaultNode.getInstance();

        //设置config信息
        String[] peerAddr = {"127.0.0.1:2781", "127.0.0.1:2782", "127.0.0.1:2783"};
        NodeConfig config = new NodeConfig();
        config.setSelfPort(Integer.valueOf(System.getenv("SERVICE_PORT")));
        config.setPeerAddrs(Arrays.asList(peerAddr));
        node.setConfig(config);

        //初始化节点
        node.init();
        node.voteRpcClient = (VoteRpcClient) SpringUtil.getBean("voteRpcClient");

        //线程结束钩子,销毁node。
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                node.destroy();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }));
    }
}
