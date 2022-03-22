package com.tiny.kv.raft.impl;

import com.tiny.kv.raft.ILogModule;
import com.tiny.kv.raft.common.entity.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description: 默认的日志实现. 日志模块不关心 key, 只关心 index.
 * <p>
 * 日志实现方式：rocksdb
 **/
@Slf4j
public class DefaultLogModule implements ILogModule {

    public static String dbDir;
    private static String logsDir;
    private static RocksDB logDb;
    private final static byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();

    static {
        if (dbDir == null) {
            dbDir = "./rocksDB-raft/" + System.getenv("SERVER_PORT");
        }
        if (logsDir == null) {
            logsDir = dbDir + "/logModule";
        }
        RocksDB.loadLibrary();
    }

    private DefaultLogModule() {
        Options options = new Options();
        options.setCreateIfMissing(true);

        File file = new File(logsDir);
        boolean succ = false;
        if (!file.exists()) {
            succ = file.mkdirs();
        }
        if (succ) {
            log.warn("mkdir a new dir : " + logsDir);
        }

        try {
            logDb = RocksDB.open(options, logsDir);
        } catch (RocksDBException e) {
            log.error("RocksDBException " + e);
        }

    }

    public static DefaultLogModule getInstance() {
        return DefaultLogModuleHolder.INSTANCE;
    }

    private static class DefaultLogModuleHolder {
        private static final DefaultLogModule INSTANCE = new DefaultLogModule();
    }

    @Override
    public void write(LogEntry logEntry) {

    }

    @Override
    public LogEntry read(Long index) {
        return null;
    }

    @Override
    public void removeOnStartIndex(Long startIndex) {

    }

    @Override
    public LogEntry getLast() {
        return null;
    }

    @Override
    public Long getLastIndex() {
        return null;
    }
}
