package com.tiny.kv.raft.core.impl;

import com.alibaba.fastjson.JSON;
import com.tiny.kv.raft.common.config.NodeConfig;
import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.config.PeerSet;
import com.tiny.kv.raft.common.cons.NodeStatus;
import com.tiny.kv.raft.common.entity.*;
import com.tiny.kv.raft.common.pools.RaftThreadPools;
import com.tiny.kv.raft.core.*;
import com.tiny.kv.raft.core.memchange.IClusterMembershipChanges;
import com.tiny.kv.raft.core.task.ElectionTask;
import com.tiny.kv.raft.core.task.HeartBeatTask;
import com.tiny.kv.raft.core.task.ReplicationFailTask;
import com.tiny.kv.raft.rpc.client.RpcClient;
import com.tiny.kv.raft.rpc.server.RpcServer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    /* 结点配置对象 */
    public NodeConfig nodeConfig;

    /* =============== 定时任务 ====================  */
    private ElectionTask electionTask = new ElectionTask(this);
    private HeartBeatTask heartBeatTask = new HeartBeatTask(this);
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
    /* 对于每一个服务器，需要发送给他的下一个日志条目的索引值（初始化为领导人最后索引值加一） */
    public Map<Peer, Long> nextIndexs;
    /* 对于每一个服务器，已经复制给他的日志的最高索引值 */
    public Map<Peer, Long> matchIndexs;


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
        stateMachine = DefaultStateMachine.getInstance();

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
     * 处理日志添加
     *
     * @param param
     * @return
     */
    @Override
    public AentryResult handlerAppendEntries(AentryParam param) {
        log.info("handlerAppendEntries will be invoke, param info : {}", param);
        return consensus.appendEntries(param);
    }

    /**
     * 处理客户端KV请求
     * 1. 客户端的每一条请求都包含一条被复制状态机执行的指令
     * 2. 领导人把这条指令作为一条新的日志条目附加的日志中，然后并行发起附件条目的 RPCs 给其他服务器，让他们复制这条日志。
     * 3. 当这条日志被安全复制，领导人应用这条日志到它的状态机中然后把执行结果返回客户端。
     * 4. 如果跟随者崩溃或运行缓慢，在或者网络丢包，领导人会不断重试知道所有跟随者最终都存储了所有日志条目
     *
     * @param request
     * @return
     */
    @Override
    public ClientKVAck handlerClientRequest(ClientKVReq request) {
        log.info("处理客户端KV请求-> operation:{} , key:{} , value:{}", ClientKVReq.Type.value(request.getType()), request.getKey(), request.getValue());
        if (status != NodeStatus.LEADER) {
            log.info("我不是主节点,转发至主节点处理, leader addr:{} ,myself addr:{}", peerSet.getLeader().getAddr(), peerSet.getSelf().getAddr());
            redirect(request);
        }

        if (request.getType() == ClientKVReq.GET) {
            //处理GET请求
            LogEntry logEntry = stateMachine.get(request.getKey());
            if (logEntry != null) {
                return new ClientKVAck(logEntry.getCommand());
            }
            return new ClientKVAck(null);
        }

        LogEntry logEntry = LogEntry.builder()
                .command(Command.builder()
                        .key(request.getKey())
                        .value(request.getValue())
                        .build())
                .term(currentTerm)
                .build();
        //先写到本地日志
        logModule.write(logEntry);
        log.info("写入本地日志模块成功, logEntry info : {}, log index : {}", logEntry, logEntry.getIndex());


        //复制到其他机器
        int replicationCount = 0;
        List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            replicationCount++;
            //并行发起 RPC 复制
            futureList.add(replication(peer, logEntry));
        }

        final AtomicInteger success = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(futureList.size());
        List<Boolean> resultList = new CopyOnWriteArrayList<>();

        //获取并行异步复制RPC结果
        getRpcAppendResult(futureList, latch, resultList);

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("handlerClientRequest latch error , ", e);
        }

        for (Boolean rpcResult : resultList) {
            if (rpcResult) {
                success.incrementAndGet();
            }
        }

        //过半成功，返回成功
        if (success.get() >= (replicationCount / 2)) {
            //更新已提交索引
            commitIndex = logEntry.getIndex();
            //应用到状态机
            stateMachine.apply(logEntry);
            lastApplied = commitIndex;
            return ClientKVAck.ok();
        } else {
            //未过半，回滚日志，返回失败。
            logModule.removeOnStartIndex(logEntry.getIndex());
            return ClientKVAck.fail();
        }

    }

    /**
     * 请求转发
     * 请求非主节点，转发至主节点处理。
     *
     * @param request
     * @return
     */
    @Override
    public ClientKVAck redirect(ClientKVReq request) {
        RpcRequest<Object> r = RpcRequest.builder()
                .url(peerSet.getLeader().getAddr())
                .cmd(RpcRequest.CLIENT_REQ)
                .obj(request)
                .build();
        try {
            RpcResponse response = (RpcResponse) RpcClient.getInstance().sendSyncMsg(peerSet.getLeader().getAddr(), r);
            return (ClientKVAck) response.getResult();
        } catch (Exception e) {
            log.error("redirect fail ", e);
            return ClientKVAck.fail();
        }
    }

    @Override
    public MembershipChangeResult addPeer(Peer newPeer) {
        return delegate.addPeer(newPeer);
    }

    @Override
    public MembershipChangeResult removePeer(Peer oldPeer) {
        return delegate.removePeer(oldPeer);
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

            //实例化节点扩缩容实例对象
            delegate = new ClusterMembershipChangesImpl(this);

            //心跳任务,500ms一次(受上一次执行时间影响,如上一次执行未完成,等待上一次执行完成后再等500ms后发送心跳)
            RaftThreadPools.scheduleWithFixedDelay(heartBeatTask, 500);
            //选举任务,系统启动后等待6000ms开始执行,之后每隔500ms触发一次选举
            RaftThreadPools.scheduleAtFixedRate(electionTask, 6000, 500);
            //启动复制失败处理线程
            RaftThreadPools.execute(replicationFailTask);

            started = true;
            log.info("启动DefaultNode成功, selfId : {} ", peerSet.getSelf());

            //启动RpcClient
            RpcClient.getInstance().startRpcClient(this);

            //启动RpcServer
            RpcServer.getInstance().startRpcServer(this);
        }
    }

    @Override
    public void destroy() throws Throwable {
        RpcClient.getInstance().destroy();
        RpcServer.getInstance().destory();
    }

    /**
     * 初始化所有的 nextIndex 值为自己的最后一条日志的 index + 1. 如果下次 RPC 时, 跟随者和leader 不一致,就会失败.
     * 那么 leader 尝试递减 nextIndex 并进行重试.最终将达成一致.
     */
    public void becomeLeaderToDoThing() {
        nextIndexs = new ConcurrentHashMap<>();
        matchIndexs = new ConcurrentHashMap<>();
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            nextIndexs.put(peer, logModule.getLastIndex() + 1);
            matchIndexs.put(peer, 0L);
        }
    }

    /**
     * 复制到其他服务器
     * 1. 如果发现对方term比自己大，自己降为Follower
     * 2. 如果对方返回失败，递减nextIndex，再次发送，直到对方返回成功（极端情况下nextIndex会降为1，需要rpc所有的logEntry）。
     *
     * @param peer
     * @param entry
     * @return
     */
    public Future<Boolean> replication(Peer peer, LogEntry entry) {

        return RaftThreadPools.submit(new Callable() {
            @Override
            public Object call() throws Exception {
                //20秒 重试时间
                long start = System.currentTimeMillis(), end = start;
                while (end - start < 20 * 1000l) {
                    AentryParam param = AentryParam.builder()
                            .term(currentTerm)
                            .serverId(peer.getAddr())
                            .leaderId(peerSet.getSelf().getAddr())
                            //commitIndex:已知被提交的最大日志索引值
                            .leaderCommit(commitIndex)
                            .build();
                    //添加nextIndex小于当前entryIndex的所有的日志条目
                    Long nextIndex = nextIndexs.get(peer);
                    LinkedList<LogEntry> logEntries = new LinkedList<>();
                    if (entry.getIndex() > nextIndex) {
                        for (long i = nextIndex; i < entry.getIndex(); i++) {
                            LogEntry before = logModule.read(i);
                            if (before != null) {
                                logEntries.add(before);
                            }
                        }
                    } else {
                        logEntries.add(entry);
                    }

                    //最小那个日志
                    LogEntry preLog = getPreLog(logEntries.getFirst());
                    param.setPreLogTerm(preLog.getTerm());
                    param.setPrevLogIndex(preLog.getIndex());
                    param.setEntries(logEntries.toArray(new LogEntry[0]));

                    RpcRequest<Object> request = RpcRequest.builder()
                            .cmd(RpcRequest.A_ENTEIES)
                            .obj(param)
                            .url(peer.getAddr())
                            .build();

                    Object resObj = RpcClient.getInstance().sendSyncMsg(peer.getAddr(), request);
                    if (resObj == null) {
                        return false;
                    }

                    RpcResponse response = (RpcResponse) resObj;
                    AentryResult result = JSON.parseObject(JSON.toJSONString(response.getResult()), AentryResult.class);
                    if (result != null && result.isSuccess()) {
                        //appendEntry Rpc成功，更新node的追踪值
                        nextIndexs.put(peer, entry.getIndex() + 1);
                        matchIndexs.put(peer, entry.getIndex());
                        return true;
                    } else if (result != null) {
                        if (result.getTerm() > currentTerm) {
                            //对方比我大，设置为Follower
                            currentTerm = result.getTerm();
                            status = NodeStatus.FOLLOWER;
                            return false;
                        } else {
                            //对方没我大，但是response返回失败，说明index不对，应该递减nextIndex，再次发起Rpc，直到返回成功为止。
                            if (nextIndex == 0) {
                                nextIndex = 1l;
                            }
                            nextIndexs.put(peer, nextIndex - 1);
                            //不返回，让while循环持续发起 AppendAentry Rpc
                        }
                    }

                    //更新end
                    end = System.currentTimeMillis();
                }

                //20s内，没有执行完，返回false。 // TODO: 2022-03-25 应该优化下。
                log.error("复制日志失败，原因超时20s未处理完成。nextIndexs:{}", JSON.toJSONString(nextIndexs));
                return false;
            }
        });
    }

    /**
     * 获取前一个LogEntry
     *
     * @param logEntry
     * @return
     */
    public LogEntry getPreLog(LogEntry logEntry) {
        LogEntry preEntry = logModule.read(logEntry.getIndex() - 1);
        if (preEntry == null) {
            preEntry = LogEntry.builder().index(0L).term(0).command(null).build();
        }
        return preEntry;
    }

    /**
     * 获取异步RPC结果
     *
     * @param futureList
     * @param latch
     * @param resultList
     */
    private void getRpcAppendResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> resultList) {
        for (Future<Boolean> future : futureList) {
            RaftThreadPools.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        resultList.add(future.get());
                    } catch (Exception e) {
                        log.error("replication rpc future getResult error , ", e);
                        resultList.add(false);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
    }
}
