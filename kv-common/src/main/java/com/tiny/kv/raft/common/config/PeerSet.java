package com.tiny.kv.raft.common.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description: 节点集合(去重)
 **/
public class PeerSet implements Serializable {

    private List<Peer> list = new ArrayList();

    private volatile Peer leader;

    private volatile Peer self;

    /**
     * 单例静态获取实例
     */
    private PeerSet() {
    }

    public static PeerSet getInstance() {
        return PeerSetLazyHolder.INSTANCE;
    }

    private static class PeerSetLazyHolder {
        private final static PeerSet INSTANCE = new PeerSet();
    }

    public void addPeer(Peer peer) {
        list.add(peer);
    }

    public void removePeer(Peer peer) {
        list.remove(peer);
    }

    public List<Peer> getPeers() {
        return list;
    }

    /**
     * 获取同伴节点结合
     * @return
     */
    public List<Peer> getPeersWithOutSelf() {
        List<Peer> otherPeers = new ArrayList<>(list);
        otherPeers.remove(self);
        return otherPeers;
    }

    /*--------------get/set----------------*/
    public Peer getLeader() {
        return leader;
    }

    public void setLeader(Peer leader) {
        this.leader = leader;
    }

    public Peer getSelf() {
        return self;
    }

    public void setSelf(Peer self) {
        this.self = self;
    }
}
