package com.tiny.kv.raft.common.exception;

/**
 * @author: leo wang
 * @date: 2022-03-23
 * @description:
 **/
public class RaftRemotingException extends RuntimeException {

    public RaftRemotingException() {
    }

    public RaftRemotingException(String message) {
        super(message);
    }
}
