package com.tiny.kv.raft.common.entity;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description: 基础请求参数Class
 **/
@Data
@Builder
public class BaseParam implements Serializable {

    /* 候选人的任期号 */
    public long term;

    /* 被请求者 ID(ip:selfPort) */
    public String serverId;
}
