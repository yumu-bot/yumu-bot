package com.now.nowbot.util.command;

public class CmdObject<T> {
    T data;

    public CmdObject(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
