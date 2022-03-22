package com.tiny.kv.raft.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author: leo wang
 * @date: 2022-03-22
 * @description: 日志条目
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LogEntry implements Serializable, Comparable {

    private long index;

    private long term;

    private Command command;

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }
        if (this.getIndex() > ((LogEntry) o).getIndex()) {
            return 1;
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return index == logEntry.index &&
                term == logEntry.term &&
                Objects.equals(command, logEntry.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, term, command);
    }
}
