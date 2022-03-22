package com.tiny.kv.raft;

import com.tiny.kv.raft.common.entity.RvoteParam;
import com.tiny.kv.raft.common.entity.RvoteResult;

/**
 * @author: leo wang
 * @date: 2022-03-18
 * @description: 一致性模块接口
 **/
public interface IConsensus {

    /**
     * 请求投票 RPC
     * 接收者实现：
     * 1. 如果term < currentTerm 返回false
     * 2. 如果 votedFor 为空或者就是 candidateId，并且候选人的日志至少和自己一样新，那么就投票给他。
     * @param param
     * @return
     */
    RvoteResult requestVote(RvoteParam param);

    /**
     * 附加日志（多个日志，提高效率） RPC
     * 接收者实现：
     * 1. 如果 term < currentTerm 就返回 false
     * 2. 如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false
     * 3. 如果已经存在的日志条目和新的产生冲突（索引值相同但是任期号不同），删除这一条和之后所有的
     * 4. 附加任何在已有的日志中不存在的条目
     * 5. 如果 leaderCommit > commintIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
     * @param param
     * @return
     */
    //AentryResult appendEntries(AentryParam param);
}
