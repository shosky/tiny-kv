package com.tiny.kv.raft.common.cons;

import lombok.Getter;

/**
 * @author: leo wang
 * @date: 2022-03-21
 * @description: 节点状态
 **/
public interface NodeStatus {

    /**
     * 追随者
     */
    int FOLLOWER = 0;
    /**
     * 候选者
     */
    int CANDIDATE = 1;
    /**
     * 领袖
     */
    int LEADER = 2;

    @Getter
    enum Enum {
        FOLLOWER(0), CANDIDATE(1), LEADER(2);

        Enum(int code) {
            this.code = code;
        }

        int code;

        public static Enum value(int i) {
            for (Enum value : Enum.values()) {
                if (value.code == i) {
                    return value;
                }
            }
            return null;
        }

    }
}
