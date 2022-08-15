package com.now.nowbot.entity;

import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Table(name = "osu_name_id", indexes = {
        @Index(name = "osufindname", columnList = "name"),
})

public class OsuNameToIdLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String name;
    private Long uid;
    @Column(name = "idx")
    private Integer index;

    public OsuNameToIdLite() {
    }

    public OsuNameToIdLite(Long uid, String name, Integer index) {
        this.name = name.toUpperCase();
        this.uid = uid;
        this.index = index;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toUpperCase();
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }
}
