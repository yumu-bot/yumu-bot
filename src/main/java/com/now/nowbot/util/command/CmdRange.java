package com.now.nowbot.util.command;

import org.jetbrains.annotations.Nullable;

public class CmdRange<T> {
    @Nullable
    T data;
    Integer start;
    Integer end;

    public CmdRange(@Nullable T data, Integer start, Integer end) {
        this.data = data;
        this.start = start;
        this.end = end;
    }

    @Nullable
    public T getData() {
        return data;
    }

    public void setData(@Nullable T data) {
        this.data = data;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }
}
