package com.now.nowbot.entity;

import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

public class MsgLite {
    int id;
    int internal;
    //秒时间戳
    int time;
    long from_id;
    long target_id;
    String msg;

    public MsgLite(MessageChain msg){
        var source = msg.get(MessageSource.Key);;
        id = source.getIds()[0];
        internal = source.getInternalIds()[0];
        time = source.getTime();
        from_id = source.getFromId();
        target_id = source.getTargetId();
        this.msg = MessageChain.serializeToJsonString(msg);
    }
    public MessageChain getMessageChain(){
        return MessageChain.deserializeFromJsonString(msg);
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInternal() {
        return internal;
    }

    public void setInternal(int internal) {
        this.internal = internal;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public long getFrom_id() {
        return from_id;
    }

    public void setFrom_id(long from_id) {
        this.from_id = from_id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public long getTarget_id() {
        return target_id;
    }

    public void setTarget_id(long target_id) {
        this.target_id = target_id;
    }
}
