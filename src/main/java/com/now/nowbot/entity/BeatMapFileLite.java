package com.now.nowbot.entity;

import com.now.nowbot.model.enums.OsuMode;
import jakarta.persistence.*;

@Entity
@Table(name = "beatmapfile",indexes = {
        @Index(name = "index_file_sid", columnList = "file_sid"),
})
public class BeatMapFileLite {
    @Id
    @Column(name = "file_bid")
    Long bid;

    @Column(name = "file_sid")
    Long sid;

    String background;

    String audio;

    Integer mode;

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public Long getSid() {
        return sid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public OsuMode getMode() {
        if (mode == null) return OsuMode.DEFAULT;
        return OsuMode.getMode(mode);
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }
}
