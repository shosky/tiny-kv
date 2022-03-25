package com.tiny.kv.raft.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RvoteParam extends BaseParam {

    //请求选票的候选人的 ID(ip:selfPort)
    private String candidateId;

    //候选人的最后日志条目索引值
    private long lastLogIndex;

    //候选人最后日志条目的任期号
    private long lastLogTerm;

    @Override
    public String toString() {
        return "RvoteParam{" +
                "candidateId='" + candidateId + '\'' +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                ", term=" + term +
                ", serverId=" + serverId + '\'' +
                '}';
    }

}
