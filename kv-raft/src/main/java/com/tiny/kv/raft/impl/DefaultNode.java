package com.tiny.kv.raft.impl;

import com.tiny.kv.raft.*;
import com.tiny.kv.raft.common.config.NodeConfig;
import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.config.PeerSet;
import com.tiny.kv.raft.common.cons.NodeStatus;
import com.tiny.kv.raft.common.entity.RvoteParam;
import com.tiny.kv.raft.common.entity.RvoteResult;
import com.tiny.kv.raft.common.pools.RaftThreadPools;
import com.tiny.kv.raft.memchange.IClusterMembershipChanges;
import com.tiny.kv.raft.task.ElectionTask;
import com.tiny.kv.raft.task.HeartBeatTask;
import com.tiny.kv.raft.task.ReplicationFailTask;
import com.tiny.kv.rpc.client.VoteRpcClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: 默认服务器节点实现类, 初始为 follower, 角色随时变化.
 **/
@Slf4j
public class DefaultNode implements INode, ILifeCycle, IClusterMembershipChanges {
    /* =============== 节点本身属性 ====================  */
    /* 选举时间间隔基数 */
    public volatile long electionTime = 15 * 1000;
    /* 上一次选举时间 */
    public volatile long preElectionTime = 0;
    /* 上次一心跳时间戳 */
    public volatile long preHeartBeatTime = 0;
    /* 心跳间隔基数 */
    public final long heartBeatTick = 5 * 1000;
    /* 节点当前状态 */
    public volatile int status = NodeStatus.FOLLOWER;
    /* 定时任务是否已经启动 */
    public volatile Boolean started = false;
    /* 状态机实例 */
    public IStateMachine stateMachine;
    /* 一致性实例 */
    public IConsensus consensus;
    /* 集群身份变更实例 */
    public IClusterMembershipChanges delegate;
    /* 节点集合 */
    public PeerSet peerSet;
    /* 投票RPC客户端*/
    public VoteRpcClient voteRpcClient;
    /* 结点配置对象 */
    public NodeConfig nodeConfig;

    /* =============== 定时任务 ====================  */
    private ElectionTask electionTask = new ElectionTask(this);
    private HeartBeatTask heartBeatTask = new HeartBeatTask();
    private ReplicationFailTask replicationFailTask = new ReplicationFailTask();

    /* =============== 所有服务器上持久存在的 ==================== */
    /* 服务器最后一次知道的任期号（初始化为0，持续递增） */
    public volatile long currentTerm = 0;
    /* 在当前获取选票的候选人的 ID */
    public volatile String votedFor;
    /* 日志条目集：每一个条目包含一个用户状态机执行的指令，和收到时的任期号 */
    public ILogModule logModule;

    /* =============== 所有服务器上经常变的 ==================== */
    /* 已知的最大的已经被提交的日志条目的索引值 */
    public volatile long commitIndex;
    /* 最后被应用到状态机的日志条目索引值（初始化为0，持续递增） */
    public volatile long lastApplied = 0;

    /* =============== 在领导人里经常改变的(选举后重新初始化) ==================== */

    /**
     * 所有构造函数
     */
    private DefaultNode() {
    }

    /**
     * 单例获取实例
     *
     * @return
     */
    public static DefaultNode getInstance() {
        return DefaultNodeServiceLazyHolder.INSTANCE;
    }

    @Override
    public void setConfig(NodeConfig config) {
        this.nodeConfig = config;
        logModule = DefaultLogModule.getInstance();

        peerSet = PeerSet.getInstance();
        for (String addr : config.peerAddrs) {
            Peer peer = new Peer(addr);
            peerSet.addPeer(peer);
            if (addr.equals("127.0.0.1:" + config.getSelfPort())) {
                peerSet.setSelf(peer);
            }
        }
    }

    /**
     * 处理请求投票 RPC
     *
     * @param param
     * @return
     */
    @Override
    public RvoteResult handlerRequestVote(RvoteParam param) {
        log.info("handlerRequestVote will be invoke, param info : {}", param);
        return consensus.requestVote(param);
    }

    /**
     * 静态内部类，调用时才会初始化。
     */
    private static class DefaultNodeServiceLazyHolder {
        private static final DefaultNode INSTANCE = new DefaultNode();
    }

    /**
     * Node初始化
     *
     * @throws Throwable
     */
    @Override
    public void init() throws Throwable {
        if (started) {
            return;
        }

        synchronized (this) {
            if (started) {
                return;
            }

            //实例化一致性对象
            consensus = new DefaultConsensus(this);

            //心跳任务,500ms一次(受上一次执行时间影响,如上一次执行未完成,等待上一次执行完成后再等500ms后发送心跳)
            RaftThreadPools.scheduleWithFixedDelay(heartBeatTask, 500);
            //选举任务,系统启动后等待6000ms开始执行,之后每隔500ms触发一次选举
            RaftThreadPools.scheduleAtFixedRate(electionTask, 6000, 500);
            //启动复制失败处理线程
            RaftThreadPools.execute(replicationFailTask);

            started = true;
            log.info("start success, selfId : {} ", peerSet.getSelf());
        }
    }

    @Override
    public void destroy() throws Throwable {

    }

}
