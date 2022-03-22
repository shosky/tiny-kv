package com.tiny.kv.rpc.server;

import com.leo.rpc.provider.annotation.TinyRpcService;
import com.tiny.kv.rpc.entity.Response;
import com.tiny.kv.rpc.facade.HeartFacade;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
@Slf4j
@TinyRpcService(serviceInterface = HeartFacade.class)
public class HeartRpcServer implements HeartFacade {
    @Override
    public Response call() {
        log.info("heart call");
        return null;
    }
}
