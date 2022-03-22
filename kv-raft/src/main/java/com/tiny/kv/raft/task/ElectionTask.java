package com.tiny.kv.raft.task;

import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.pools.RaftThreadPools;
import com.tiny.kv.raft.impl.DefaultNode;
import lombok.extern.slf4j.Slf4j;
import com.tiny.kv.raft.common.cons.NodeStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: 选举定时任务
 **/
@Slf4j
public class ElectionTask implements Runnable {

    private DefaultNode node;

    public ElectionTask(DefaultNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        if (node.status == NodeStatus.LEADER) {
            return;
        }

        // 基于 RAFT 的随机时间，解决冲突。
        long current = System.currentTimeMillis();
        node.electionTime = node.electionTime + ThreadLocalRandom.current().nextInt(50);
        if (current - node.preElectionTime < node.electionTime) {
            return;
        }

        //设置成候选者
        node.status = NodeStatus.CANDIDATE;

        //设置上次选举时间
        node.preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;

        node.currentTerm = node.currentTerm + 1;

        List<Peer> peers = node.peerSet.getPeersWithOutSelf();
        List<Future> futures = new ArrayList<>();
        AtomicInteger electionResult = new AtomicInteger(0);

        //发送请求
        for (Peer peer : peers) {
            futures.add(RaftThreadPools.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    log.info("peer addr :{}", peer.getAddr());
                    node.voteRpcClient.vote("你好世界");
                    return null;
                }
            }));
        }

        CountDownLatch latch = new CountDownLatch(futures.size());

        //等待结果
        for (Future future : futures) {
            RaftThreadPools.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    try {
                        Object o = future.get();
                        return null;
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("InterruptedException By Master election Task,", e);
        }

        int success = electionResult.get();
        if (node.status == NodeStatus.FOLLOWER) {
            //如果投票期间,有其他服务器发送appendEntry ,就可能变成follower,这时应该停止.
            return;
        }

        //投票过半(包括自己)设置leader
        if (success >= peers.size() / 2) {
            node.status = NodeStatus.LEADER;
            node.peerSet.setLeader(node.peerSet.getSelf());
            node.votedFor = "";
            //become leader todo somthing
        }
    }
}
