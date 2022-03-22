package com.tiny.kv.rpc.server;

import com.leo.rpc.provider.annotation.TinyRpcService;
import com.tiny.kv.rpc.entity.Request;
import com.tiny.kv.rpc.entity.Response;
import com.tiny.kv.rpc.facade.VoteFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
@Slf4j
@Component
@TinyRpcService(serviceInterface = VoteFacade.class)
public class VoteRpcServer implements VoteFacade {

    @Override
    public Response vote(Request request) {
        log.info("响应投票:{}", request);
        return null;
    }

}
