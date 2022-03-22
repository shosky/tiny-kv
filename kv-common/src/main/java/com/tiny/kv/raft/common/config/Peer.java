package com.tiny.kv.raft.common.config;

import lombok.Data;

import java.util.Objects;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description: 当前节点的同伴。
 **/
@Data
public class Peer {

    private final String addr;

    public Peer(String addr) {
        this.addr = addr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(addr, peer.addr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addr);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "addr='" + addr + '\'' +
                '}';
    }
}
