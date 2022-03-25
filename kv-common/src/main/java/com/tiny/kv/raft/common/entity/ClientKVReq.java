package com.tiny.kv.raft.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author: leo wang
 * @date: 2022-03-25
 * @description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientKVReq implements Serializable {

    public static int GET = 0;
    public static int PUT = 1;

    /**
     * 请求类型GET/PUT
     */
    int type;

    /**
     * 键
     */
    String key;

    /**
     * 值
     */
    String value;

    public enum Type {
        PUT(1), GET(0);
        int code;

        Type(int code) {
            this.code = code;
        }

        public static Type value(int code) {
            for (Type type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }
}
