package com.tiny.kv.rpc.client;

import com.leo.rpc.consumer.annotation.TinyRpcReference;
import com.tiny.kv.rpc.facade.VoteFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
@Slf4j
@Component(value = "voteRpcClient")
public class VoteRpcClient {

    @TinyRpcReference
    private VoteFacade voteFacade;

    public void vote(String args) {
        log.info("请求投票:{}", args);
        voteFacade.vote(args);
    }

}
