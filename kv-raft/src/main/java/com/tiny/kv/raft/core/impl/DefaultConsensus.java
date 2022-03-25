package com.tiny.kv.raft.core.impl;

import com.tiny.kv.raft.common.entity.*;
import com.tiny.kv.raft.core.IConsensus;
import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.cons.NodeStatus;
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

                if (node.logModule.getLast() != null) {
                    //对方没自己新
                    if (node.logModule.getLast().getTerm() > param.getLastLogTerm()) {
                        return RvoteResult.fail();
                    }
                    //对方没自己新
                    if (node.logModule.getLastIndex() > param.getLastLogIndex()) {
                        return RvoteResult.fail();
                    }
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

    @Override
    public AentryResult appendEntries(AentryParam param) {
        AentryResult result = AentryResult.fail();
        try {
            if (!appendLock.tryLock()) {
                return result;
            }
            //返回当前任期
            result.setTerm(node.currentTerm);

            if (param.getTerm() < node.currentTerm) {
                //发送心跳一方比自己任期还小，不够额。
                return result;
            }

            node.preHeartBeatTime = System.currentTimeMillis();
            //更新上一次选举时间(领导者上线时:每次心跳都会更新该值，领导下线:此值不会更新就给了其他追随者发起选举机会)
            node.preElectionTime = System.currentTimeMillis();
            //设置当前Leader是发送心跳一方
            node.peerSet.setLeader(new Peer(param.getLeaderId()));

            // 够格
            if (param.getTerm() >= node.currentTerm) {
                log.debug("节点 {} 成为 FOLLOWER, 当前任期 : {}, 参数任期 : {}, 参数serverId",
                        node.peerSet.getSelf(), node.currentTerm, param.getTerm(), param.getServerId());
                // 认怂
                node.status = NodeStatus.FOLLOWER;
            }
            // 使用对方的 term.
            node.currentTerm = param.getTerm();

            //心跳
            if (param.getEntries() == null || param.getEntries().length == 0) {
                log.info("节点 {} 添加心跳成功 , 它的任期 : {}, 我的任期 : {}",
                        param.getLeaderId(), param.getTerm(), node.currentTerm);
                return AentryResult.builder().term(node.currentTerm).success(true).build();
            }

            //真实日志

            //第一次
            if (node.logModule.getLastIndex() != 0 && param.getPrevLogIndex() != 0) {
                LogEntry logEntry = node.logModule.read(param.getPrevLogIndex());
                if (logEntry != null) {
                    //如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false
                    if (logEntry.getTerm() != param.getPreLogTerm()) {
                        return result;
                    }
                } else {
                    //index不对，需要递减nextIndex，返回false
                    return result;
                }
            }

            LogEntry existLog = node.logModule.read(param.getPrevLogIndex() + 1);
            if (existLog != null && existLog.getTerm() != param.getEntries()[0].getTerm()) {
                //如果已经存在这条日志，删除这一条之后所有的，以Leader的日志为准。
                node.logModule.removeOnStartIndex(param.getPrevLogIndex() + 1);
            } else if (existLog != null) {
                //已经存在不允许写入
                result.setSuccess(true);
                return result;
            }

            //应用到状态机
            for (LogEntry entry : param.getEntries()) {
                node.logModule.write(entry);
                node.stateMachine.apply(entry);
                result.setSuccess(true);
            }

            if (param.getLeaderCommit() > node.commitIndex) {
                //如果 leaderCommit > commitIndex，令commintIndex为leaderCommint与日志条目最小的一个。
                int commitIndex = (int) Math.min(param.getLeaderCommit(), node.logModule.getLastIndex());
                node.commitIndex = commitIndex;
                node.lastApplied = commitIndex;
            }
            result.setTerm(node.currentTerm);
            node.status = NodeStatus.FOLLOWER;

            return result;
        } finally {
            appendLock.unlock();
        }
    }
}
