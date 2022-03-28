package com.tiny.kv.raft;

import org.junit.Test;

/**
 * @author: leo wang
 * @date: 2022-03-28
 * @description:
 **/
public class TestElastic {

    @Test
    public void testElastic() {
        try {
            RaftNodeBootstrap.startRaft();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
