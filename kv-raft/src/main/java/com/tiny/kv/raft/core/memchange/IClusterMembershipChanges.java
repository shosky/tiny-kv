package com.tiny.kv.raft.core.memchange;

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
    //Result addPeer(Peer newPeer);

    /**
     * 删除节点
     *
     * @param oldPeer
     * @return
     */
    //Result removePeer(Peer oldPeer);
}
