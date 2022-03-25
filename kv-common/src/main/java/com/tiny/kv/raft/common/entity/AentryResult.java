package com.tiny.kv.raft.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-24
 * @description:
 **/
@Data
@SuperBuilder
public class AentryResult implements Serializable {
    //当前任期号，用于领导人去更新自己
    long term;

    boolean success;

    public AentryResult(boolean success) {
        this.success = success;
    }

    public static AentryResult fail() {
        return new AentryResult(false);
    }

    public static AentryResult ok() {
        return new AentryResult(true);
    }
}
