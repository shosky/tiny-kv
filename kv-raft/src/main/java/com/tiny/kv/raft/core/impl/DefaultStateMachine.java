package com.tiny.kv.raft.core.impl;

import com.alibaba.fastjson.JSON;
import com.tiny.kv.raft.common.entity.Command;
import com.tiny.kv.raft.common.entity.LogEntry;
import com.tiny.kv.raft.core.IStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;

/**
 * @author: leo wang
 * @date: 2022-03-25
 * @description:
 **/
@Slf4j
public class DefaultStateMachine implements IStateMachine {

    private static String dbDir;
    private static String stateMachineDir;
    public static RocksDB machineDb;

    static {
        if (dbDir == null) {
            dbDir = "./rocksDB-raft/" + System.getenv("SERVER_PORT");
        }
        if (stateMachineDir == null) {
            stateMachineDir = dbDir + "/stateMachine";
        }
        RocksDB.loadLibrary();
    }

    private DefaultStateMachine() {
        try {
            File file = new File(stateMachineDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            Options options = new Options();
            options.setCreateIfMissing(true);
            machineDb = RocksDB.open(options, stateMachineDir);
        } catch (RocksDBException e) {
            log.error("RocksDBException e:", e);
        }
    }

    public static DefaultStateMachine getInstance() {
        return DefaultStateMachineLazyHolder.INSTANCE;
    }

    private static class DefaultStateMachineLazyHolder {
        private static final DefaultStateMachine INSTANCE = new DefaultStateMachine();
    }

    @Override
    public void apply(LogEntry logEntry) {
        try {
            Command command = logEntry.getCommand();
            if (command == null) {
                throw new IllegalArgumentException("command can not be null, logEntry : " + logEntry.toString());
            }
            String key = command.getKey();
            machineDb.put(key.getBytes(), JSON.toJSONBytes(logEntry));
        } catch (RocksDBException e) {
            log.info("应用状态机异常,", e);
        }
    }

    @Override
    public LogEntry get(String key) {
        try {
            byte[] bytes = machineDb.get(key.getBytes());
            if (bytes == null) {
                return null;
            }
            return JSON.parseObject(bytes, LogEntry.class);
        } catch (RocksDBException e) {
            log.error("rocksdb get exception:", e);
        }
        return null;
    }

    @Override
    public String getString(String key) {
        try {
            byte[] bytes = machineDb.get(key.getBytes());
            if (bytes != null) {
                return new String(bytes);
            }
        } catch (RocksDBException e) {
            log.error("rocksdb getString exception:", e);
        }
        return "";
    }

    @Override
    public void setString(String key, String value) {
        try {
            machineDb.put(key.getBytes(), value.getBytes());
        } catch (RocksDBException e) {
            log.info(e.getMessage());
        }
    }

    @Override
    public void delString(String... key) {
        try {
            for (String s : key) {
                machineDb.delete(s.getBytes());
            }
        } catch (RocksDBException e) {
            log.info(e.getMessage());
        }
    }
}
