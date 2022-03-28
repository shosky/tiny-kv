package com.tiny.kv.raft.core.impl;

import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.cons.NodeStatus;
import com.tiny.kv.raft.common.entity.LogEntry;
import com.tiny.kv.raft.common.entity.MembershipChangeResult;
import com.tiny.kv.raft.common.entity.RpcRequest;
import com.tiny.kv.raft.common.entity.RpcResponse;
import com.tiny.kv.raft.core.memchange.IClusterMembershipChanges;
import com.tiny.kv.raft.rpc.client.RpcClient;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author: leo wang
 * @date: 2022-03-28
 * @description: 集群配置变更接口默认实现
 **/
@Slf4j
public class ClusterMembershipChangesImpl implements IClusterMembershipChanges {

    private final DefaultNode node;

    public ClusterMembershipChangesImpl(DefaultNode node) {
        this.node = node;
    }

    /**
     * 必须同步，一次只能添加一个节点
     *
     * @param newPeer
     * @return
     */
    @Override
    public MembershipChangeResult addPeer(Peer newPeer) {
        if (node.peerSet.getPeersWithOutSelf().contains(newPeer)) {
            //已存在，不允许再次添加。
            return new MembershipChangeResult();
        }

        //当前节点添加peerSet
        node.peerSet.getPeersWithOutSelf().add(newPeer);

        if (node.status == NodeStatus.LEADER) {
            node.nextIndexs.put(newPeer, 0L);
            node.matchIndexs.put(newPeer, 0L);

            //复制主节点logEntry
            for (long i = 0; i < node.logModule.getLastIndex(); i++) {
                LogEntry read = node.logModule.read(i);
                if (read != null) {
                    node.replication(newPeer, read);
                }
            }

            //同步到其他节点(node.peerSet)
            for (Peer item : node.peerSet.getPeersWithOutSelf()) {
                RpcRequest request = RpcRequest.builder()
                        .cmd(RpcRequest.CHANGE_CONFIG_ADD)
                        .url(item.getAddr())
                        .obj(newPeer)
                        .build();

                try {
                    Object o = RpcClient.getInstance().sendSyncMsg(item.getAddr(), request);
                    if (o != null) {
                        RpcResponse response = (RpcResponse) o;
                        MembershipChangeResult result = (MembershipChangeResult) response.getResult();
                        if (result != null && result.getStatus() == MembershipChangeResult.Status.SUCCESS.getCode()) {
                            log.info("添加节点成功 , peer:{}, newServer:{}", newPeer, newPeer);
                        } else {
                            log.error("添加节点失败 , peer:{}, newServer:{}", newPeer, newPeer);
                        }
                    }
                } catch (Exception e) {
                    log.error("增加节点，同步至其他节点失败。", e);
                    throw new RuntimeException(e);
                }

            }

        }
        return null;
    }

    /**
     * 必须同步，一次只能删除一个节点
     *
     * @param oldPeer
     * @return
     */
    @Override
    public MembershipChangeResult removePeer(Peer oldPeer) {
        node.peerSet.getPeersWithOutSelf().remove(oldPeer);
        node.nextIndexs.remove(oldPeer);
        node.matchIndexs.remove(oldPeer);
        return new MembershipChangeResult();
    }
}
