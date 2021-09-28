package com.now.nowbot.entity;

import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

//@Entity
//@Table(name = "qq_message")//主要是可能会有其他消息的记录,先设定表名为qq_message
public class MsgLite{
    @Id
    @GeneratedValue
    private Long id;
    @Column(name = "row_id")
    private Integer rawId;
    private Integer internal;
    // 毫秒时间戳
    private Long time;
    @Column(name = "from_id")
    private Long fromId;
    @Column(name = "target_id")
    private Long targetId;
    private String content;

    public MsgLite(MessageChain msg){
        var source = msg.get(MessageSource.Key);
        rawId = source.getIds()[0];
        internal = source.getInternalIds()[0];
        time = (long) source.getTime();
        fromId = source.getFromId();
        targetId = source.getTargetId();
        content = msg.contentToString();
    }

    public MsgLite() {

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getInternal() {
        return internal;
    }

    public void setInternal(Integer internal) {
        this.internal = internal;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
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

}

