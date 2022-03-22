package com.tiny.kv.raft;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description:
 **/
public class TestElectionTask {
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(ThreadLocalRandom.current().nextInt(200));
        }
    }
}
