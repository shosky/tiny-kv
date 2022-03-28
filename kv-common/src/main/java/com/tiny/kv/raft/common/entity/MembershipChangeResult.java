package com.tiny.kv.raft.common.entity;

import lombok.*;

/**
 * @author: leo wang
 * @date: 2022-03-28
 * @description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipChangeResult {

    public static final int FAIL = 0;
    public static final int SUCCESS = 1;

    int status;

    //领导暗示
    String leaderHint;

    @Getter
    public enum Status {

        FAIL(0), SUCCESS(1);

        Status(int code) {
            this.code = code;
        }

        int code;

        public static Status value(int v) {
            for (Status i : values()) {
                if (i.code == v) {
                    return i;
                }
            }
            return null;
        }
    }
}
