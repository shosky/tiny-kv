package com.tiny.kv.raft.common.entity;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description:
 **/
@Data
@SuperBuilder
public class Command implements Serializable {

    String key;

    String value;

    public Command(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
