package com.tiny.kv.raft.core.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.entity.*;
import com.tiny.kv.raft.common.exception.RaftRemotingException;
import com.tiny.kv.raft.common.pools.RaftThreadPools;
import com.tiny.kv.raft.common.utils.LongConvert;
import com.tiny.kv.raft.core.impl.DefaultNode;
import com.tiny.kv.raft.rpc.client.RpcClient;
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
            log.info("不能发起选举RPC，因为 current-preElectionTime={} < electionTime={}", current - node.preElectionTime, node.electionTime);
            return;
        }

        log.info("================ 开始发起选举RPC ================");

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
                    long lastTerm = 0L;
                    LogEntry last = node.logModule.getLast();
                    if (last != null) {
                        lastTerm = last.getTerm();
                    }
                    //投票对象build
                    RvoteParam param = RvoteParam.builder()
                            .term(node.currentTerm)
                            .candidateId(node.peerSet.getSelf().getAddr())
                            .lastLogTerm(lastTerm)
                            .lastLogIndex(LongConvert.convert(node.logModule.getLastIndex())).build();
                    //Request对象build
                    RpcRequest<Object> request = RpcRequest.builder()
                            .cmd(RpcRequest.R_VOTE)
                            .obj(param)
                            .url(peer.getAddr())
                            .build();
                    try {
                        log.info("ElectionTask Rpc call addr :{}", peer.getAddr());
                        Object result = RpcClient.getInstance().sendSyncMsg(peer.getAddr(), request);
                        return result;
                    } catch (RaftRemotingException e) {
                        log.error("ElectionTask RPC fail url: {}", request.getUrl());
                        return null;
                    }
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
                        RpcResponse<RvoteResult> response = (RpcResponse<RvoteResult>) future.get();
                        if (response == null) {
                            return -1;
                        }
                        RvoteResult voteResult = JSON.parseObject(JSON.toJSONString(response.getResult()), RvoteResult.class);
                        if (voteResult.isVoteGranted()) {
                            electionResult.incrementAndGet();
                        } else {
                            // 更新自己的任期.
                            long resTerm = response.getResult().getTerm();
                            if (resTerm >= node.currentTerm) {
                                node.currentTerm = resTerm;
                            }
                        }
                        return 0;
                    } catch (Exception e) {
                        log.error("future.get exception , e : ", e);
                        return -1;
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

        if (peers.size() >= 2 && success >= peers.size() / 2) {
            //投票过半(包括自己)设置leader
            node.status = NodeStatus.LEADER;
            node.peerSet.setLeader(node.peerSet.getSelf());
            node.votedFor = "";
            node.becomeLeaderToDoThing();
        } else {
            // else 重新选举
            node.votedFor = "";
        }
    }
}
