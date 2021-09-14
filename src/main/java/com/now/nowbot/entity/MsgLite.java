package com.now.nowbot.entity;

import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class MsgLite {
    @Id
    @GeneratedValue
    private Long id;
    @Column(name = "raw_id")
    private Integer rawId;
    private Integer internal;
    //秒时间戳
    private Integer time;
    @Column(name = "from_id")
    private Long fromId;
    @Column(name = "target_id")
    private Long targetId;
    private String msg;

    public MsgLite(MessageChain msg){
        var source = msg.get(MessageSource.Key);;
        rawId = source.getIds()[0];
        internal = source.getInternalIds()[0];
        time = source.getTime();
        fromId = source.getFromId();
        targetId = source.getTargetId();
        this.msg = MessageChain.serializeToJsonString(msg);
    }

    public MsgLite() {

    }

    public MessageChain getMessageChain(){
        return MessageChain.deserializeFromJsonString(msg);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRawId() {
        return rawId;
    }

    public void setRawId(Integer rawId) {
        this.rawId = rawId;
    }

    public Integer getInternal() {
        return internal;
    }

    public void setInternal(Integer internal) {
        this.internal = internal;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Long getFrom_id() {
        return fromId;
    }

    public void setFrom_id(Long from_id) {
        this.fromId = from_id;
    }

    public Long getTarget_id() {
        return targetId;
    }

    public void setTarget_id(Long target_id) {
        this.targetId = target_id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
