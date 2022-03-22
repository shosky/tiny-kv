package com.tiny.kv.raft.impl;

import com.tiny.kv.raft.IConsensus;
import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.cons.NodeStatus;
import com.tiny.kv.raft.common.entity.RvoteParam;
import com.tiny.kv.raft.common.entity.RvoteResult;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description: 默认一致性实现类，处理请求投票RPC 与 附件日志RPC
 **/
@Slf4j
public class DefaultConsensus implements IConsensus {

    private DefaultNode node;
    public final ReentrantLock voteLock = new ReentrantLock();
    public final ReentrantLock appendLock = new ReentrantLock();

    public DefaultConsensus(DefaultNode node) {
        this.node = node;
    }

    /**
     * 请求投票 RPC
     * <p>
     * 接收者实现：
     * 如果term < currentTerm返回false
     * 如果 votedFor 为空或者就是 candidateId，并且候选人的日志至少和自己一样新，那么就投票给他
     *
     * @param param
     * @return
     */
    @Override
    public RvoteResult requestVote(RvoteParam param) {
        try {
            if (!voteLock.tryLock()) {
                return RvoteResult.builder().term(node.currentTerm).voteGranted(false).build();
            }

            //对方任期没有自己新
            if (param.getTerm() < node.currentTerm) {
                return RvoteResult.builder().term(node.currentTerm).voteGranted(false).build();
            }

            if (StringUtil.isNullOrEmpty(node.votedFor) || node.votedFor.equals(param.getCandidateId())) {
                //对方没自己新
                if (node.logModule.getLast().getTerm() > param.getLastLogTerm()) {
                    return RvoteResult.fail();
                }
                //对方没自己新
                if (node.logModule.getLastIndex() > param.getLastLogIndex()) {
                    return RvoteResult.fail();
                }

                //切换为FOLLOWER
                node.status = NodeStatus.FOLLOWER;
                //更新
                node.peerSet.setLeader(new Peer(param.getCandidateId()));
                node.currentTerm = param.getTerm();
                node.votedFor = param.serverId;

                //返回成功
                return RvoteResult.builder().term(node.currentTerm).voteGranted(true).build();
            }

            return RvoteResult.builder().term(node.currentTerm).voteGranted(false).build();
        } finally {
            voteLock.unlock();
        }
    }
}
