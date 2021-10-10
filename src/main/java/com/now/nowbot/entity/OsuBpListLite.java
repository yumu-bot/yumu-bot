package com.now.nowbot.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "osu_bp_list", indexes = {
        @Index(name = "bp_find", columnList = "osu_id")
})
public class OsuBpListLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "osu_id")
    private Integer OsuId;

    // ','分割的
    private String bpList;

    private LocalDateTime time;

    public Integer getOsuId() {
        return OsuId;
    }

    public void setOsuId(Integer osuId) {
        OsuId = osuId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBpList() {
        return bpList;
    }

    public void setBpList(String bpList) {
        this.bpList = bpList;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
