package com.now.nowbot.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class OsuBpListLite {
    @Id
    Integer id;
    Integer OsuId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOsuId() {
        return OsuId;
    }

    public void setOsuId(Integer osuId) {
        OsuId = osuId;
    }
}
