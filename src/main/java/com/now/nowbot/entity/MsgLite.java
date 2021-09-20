package com.now.nowbot.entity;

import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "qq_message")//主要是可能会有其他消息的记录,先设定表名为qq_message
@IdClass(MsgKey.class)
public class MsgLite{
    //id internal time共为主键
    @Id
    private Integer id;
    @Id
    private Integer internal;
    @Id
    // 秒时间戳
    private Integer time;
    @Column(name = "from_id")
    private Long fromId;
    @Column(name = "target_id")
    private Long targetId;
    @Column(name = "qq_message")
    private String msg;

    public MsgLite(MessageChain msg){
        var source = msg.get(MessageSource.Key);;
        id = source.getIds()[0];
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

