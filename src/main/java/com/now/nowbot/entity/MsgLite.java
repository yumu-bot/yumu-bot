package com.now.nowbot.entity;

import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

import javax.persistence.*;

@Entity
@Table(name = "qq_message", indexes = {
        @Index(name = "msg_find", columnList = "row_id,internal,from_id")
})//主要是可能会有其他消息的记录,先设定表名为qq_message
public class MsgLite{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(length = 5000)
    private String content;

    public MsgLite(MessageChain msg){
        var source = msg.get(MessageSource.Key);
        rawId = source.getIds()[0];
        internal = source.getInternalIds()[0];
        time = (long) source.getTime();
        fromId = source.getFromId();
        targetId = source.getTargetId();
        content = MessageChain.serializeToJsonString(msg);
    }

    public MsgLite() {

    }

    public MsgLite(Long id, Integer rawId, Integer internal, Long time, Long fromId, Long targetId, String content) {
        this.id = id;
        this.rawId = rawId;
        this.internal = internal;
        this.time = time;
        this.fromId = fromId;
        this.targetId = targetId;
        this.content = content;
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

    public MessageChain getMessage(){
        return MessageChain.deserializeFromJsonString(content);
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

