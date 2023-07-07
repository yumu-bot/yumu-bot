package com.now.nowbot.qq.message;

public class ReplayMessage extends Message{
    final long id;
    public ReplayMessage(long messageId){
        id = messageId;
    }

    public long getId() {
        return id;
    }
}
