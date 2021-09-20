package com.now.nowbot.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MsgKey implements Serializable {
    private static final long serialVersionUID = -928365924538318201L;
    Integer id;
    Integer internal;
    Integer time;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getInternal() {
        return internal;
    }

    public void setInternal(Integer internal) {
        this.internal = internal;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }else if (this == o) return true;
        if (!(o instanceof MsgKey)) return false;
        MsgKey msgKey = (MsgKey) o;
        return id.equals(msgKey.id) &&
                internal.equals(msgKey.internal) &&
                time.equals(msgKey.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, internal, time);
    }
}