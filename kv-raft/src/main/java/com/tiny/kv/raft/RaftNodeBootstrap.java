package com.tiny.kv.raft;

import com.tiny.kv.raft.common.config.NodeConfig;
import com.tiny.kv.raft.core.impl.DefaultNode;

import java.util.Arrays;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description:
 **/
public class RaftNodeBootstrap {

    public static void main(String[] args) throws Throwable {
        startRaft();
    }

    public static void startRaft() throws Throwable {
        //获取Node实例
        DefaultNode node = DefaultNode.getInstance();

        //设置config信息
        String[] peerAddr = {"127.0.0.1:2781", "127.0.0.1:2782", "127.0.0.1:2783", "127.0.0.1:2784", "127.0.0.1:2785"};
        NodeConfig config = new NodeConfig();
        config.setSelfPort(Integer.valueOf(System.getenv("SERVICE_PORT")));
        config.setPeerAddrs(Arrays.asList(peerAddr));
        node.setConfig(config);

        //初始化节点
        node.init();

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
