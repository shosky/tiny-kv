package com.tiny.kv.raft.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description: 基础请求参数Class
 **/
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseParam implements Serializable {

    /* 候选人的任期号 */
    public long term;

    /* 被请求者 ID(ip:selfPort) */
    public String serverId;
}
