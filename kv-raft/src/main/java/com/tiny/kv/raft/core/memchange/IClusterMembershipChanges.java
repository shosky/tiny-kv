package com.tiny.kv.raft.core.memchange;

import com.tiny.kv.raft.common.config.Peer;
import com.tiny.kv.raft.common.entity.MembershipChangeResult;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: 集群配置变更接口
 **/
public interface IClusterMembershipChanges {

    /**
     * 添加节点
     *
     * @param newPeer
     * @return
     */
    MembershipChangeResult addPeer(Peer newPeer);

    /**
     * 删除节点
     *
     * @param oldPeer
     * @return
     */
    MembershipChangeResult removePeer(Peer oldPeer);
}
