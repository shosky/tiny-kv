package com.tiny.kv.rpc.facade;

import com.tiny.kv.rpc.entity.Request;
import com.tiny.kv.rpc.entity.Response;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
public interface VoteFacade {

    Response vote(Request request);
}
