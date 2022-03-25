package com.tiny.kv.raft.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest<T> implements Serializable {

    private long requestId;

    //请求投票
    public static final int R_VOTE = 0;

    //附加日志
    public static final int A_ENTEIES = 1;

    //客户端
    public static final int CLIENT_REQ = 2;

    //配置变更->add
    public static final int CHANGE_CONFIG_ADD = 3;

    //配置变更->remove
    public static final int CHANGE_CONFIG_REMOVE = 4;

    //请求类型
    private int cmd = -1;

    /**
     * param
     *
     * @see AntryParam
     * @see RvoteParam
     * @see ClientKvReq
     */
    private T obj;

    String url;


}
