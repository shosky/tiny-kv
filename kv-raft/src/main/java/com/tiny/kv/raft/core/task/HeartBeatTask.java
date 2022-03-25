package com.tiny.kv.raft.core.task;

import com.alibaba.fastjson.JSON;
import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.cons.NodeStatus;
import com.tiny.kv.raft.common.entity.*;
import com.tiny.kv.raft.common.pools.RaftThreadPools;
import com.tiny.kv.raft.core.impl.DefaultNode;
import com.tiny.kv.raft.rpc.client.RpcClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: 心跳定时任务
 **/
@Slf4j
public class HeartBeatTask implements Runnable {

    private DefaultNode node;

    public HeartBeatTask(DefaultNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        log.info("当前状态:[{}], 当前任期:[{}], 领袖:[{}]", NodeStatus.Enum.code2Text(node.status), node.currentTerm, node.peerSet.getLeader());
        if (node.status != NodeStatus.LEADER) {
            return;
        }

        long current = System.currentTimeMillis();
        if (current - node.preHeartBeatTime < node.heartBeatTick) {
            //当前时间戳-上一次心跳时间戳 < 心跳间隔基数5s
            return;
        }

        log.info("============= 领导人开始发送心跳包 =============");
        for (Peer peer : node.peerSet.getPeersWithOutSelf()) {
            log.info("Peer {} nextIndex={}", peer.getAddr(), node.nextIndexs.get(peer));
        }

        //设置当前心跳时间戳
        node.preHeartBeatTime = System.currentTimeMillis();

        for (Peer peer : node.peerSet.getPeersWithOutSelf()) {
            //心跳只关心 term 和 leaderID
            AentryParam param = AentryParam.builder()
                    .entries(null)
                    .leaderId(node.peerSet.getSelf().getAddr())
                    .serverId(peer.getAddr())
                    .term(node.currentTerm)
                    .build();

            RpcRequest<Object> request = RpcRequest.builder()
                    .cmd(RpcRequest.A_ENTEIES)
                    .obj(param)
                    .url(peer.getAddr()).build();


            RaftThreadPools.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Object result = RpcClient.getInstance().sendSyncMsg(peer.getAddr(), request);
                        RpcResponse<AentryResult> response = (RpcResponse<AentryResult>) result;
                        AentryResult aentryResult = JSON.parseObject(JSON.toJSONString(response.getResult()), AentryResult.class);
                        long term = aentryResult.getTerm();

                        if (term > node.currentTerm) {
                            //发现对方任期比自己高，认怂切换为Follower。
                            log.error("self will become follower, he's term : {}, my term : {}", term, node.currentTerm);
                            node.currentTerm = term;
                            node.votedFor = "";
                            node.status = NodeStatus.FOLLOWER;
                        }
                    } catch (Exception e) {
                        log.error("心跳RPC 失败, url:{}", peer.getAddr(), e);
                    }
                }
            }, false);
        }
    }
}
