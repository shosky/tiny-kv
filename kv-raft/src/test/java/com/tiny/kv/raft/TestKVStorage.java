package com.tiny.kv.raft;

import com.alibaba.fastjson.JSON;
import com.tiny.kv.raft.common.entity.ClientKVReq;
import com.tiny.kv.raft.common.entity.RpcRequest;
import com.tiny.kv.raft.core.impl.DefaultNode;
import com.tiny.kv.raft.rpc.client.RpcClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.rocksdb.RocksDB;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author: leo wang
 * @date: 2022-03-25
 * @description:
 **/
@Slf4j
public class TestKVStorage {

    private static RpcClient rpcClient;
    private static String addr = "127.0.0.1:2781";

    static {
        rpcClient = RpcClient.getInstance();
        rpcClient.startRpcClient(DefaultNode.getInstance());
    }

    @Before
    public void write() {
        RpcRequest<Object> request = RpcRequest.builder()
                .cmd(RpcRequest.CLIENT_REQ)
                .url(addr)
                .obj(ClientKVReq.builder()
                        .key("who")
                        .value("泰勒斯威夫特")
                        .type(ClientKVReq.PUT)
                        .build()).build();
        try {
            Object response = rpcClient.sendSyncMsg(addr, request);
            System.out.println("写入key后，返回结果json:" + JSON.toJSONString(response));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void read() {
        RpcRequest<Object> request = RpcRequest.builder()
                .cmd(RpcRequest.CLIENT_REQ)
                .url(addr)
                .obj(ClientKVReq.builder()
                        .key("who")
                        .type(ClientKVReq.GET)
                        .build()).build();
        try {
            Object response = rpcClient.sendSyncMsg(addr, request);
            System.out.println("读取Key后，返回结果:" + JSON.toJSONString(response));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
