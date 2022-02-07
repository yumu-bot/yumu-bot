package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(name = "osu_name_his", indexes = {
        @Index(name = "osu_name_his_index", columnList = "name"),
})
public class OsuUserNameHisLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long uid;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
