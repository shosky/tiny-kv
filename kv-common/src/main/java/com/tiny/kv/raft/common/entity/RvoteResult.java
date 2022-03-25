package com.tiny.kv.raft.common.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description: 请求 RPC 投票返回对象。
 **/
@SuperBuilder
@Data
public class RvoteResult implements Serializable {
    /* 当前任期号，以便于候选人去更新自己的任期 */
    long term;

    /* 候选人赢得了此张选票时为1否则0 */
    @JsonProperty("isVoteGranted")
    boolean voteGranted;

    public RvoteResult(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    public RvoteResult(long term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public static RvoteResult fail() {
        return new RvoteResult(false);
    }

    public static RvoteResult ok() {
        return new RvoteResult(true);
    }

}
