package com.now.nowbot.entity;

import com.now.nowbot.model.json.BeatMapSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "beat_map_set_info")
public class BeatMapSetInfoLite {
    @Id
    @Column(name = "id", nullable = false)
    private Integer sid;

    //@Lob
    @Column(columnDefinition = "TEXT")
    private String artistUnicode;
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String artist;

    //@Lob
    @Column(columnDefinition = "TEXT")
    private String titleUnicode;
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String title;

    //@Lob
    @Column(columnDefinition = "TEXT")
    private String mapper;

    Integer mapperId;

    //@Lob
    @Column(columnDefinition = "TEXT")
    private String status;

    Boolean video;

    Boolean nsfw;

    //@Lob
    @Column(columnDefinition = "TEXT")
    private String cover;

    //@Lob
    @Column(columnDefinition = "TEXT")
    String card;

    //@Lob
    @Column(columnDefinition = "TEXT")
    String list;

    //@Lob
    @Column(columnDefinition = "TEXT")
    String slimcover;

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer id) {
        this.sid = id;
    }

    public String getArtistUnicode() {
        return artistUnicode;
    }

    public void setArtistUnicode(String artistUnicode) {
        this.artistUnicode = artistUnicode;
    }

    public String getTitleUnicode() {
        return titleUnicode;
    }

    public void setTitleUnicode(String titleUnicode) {
        this.titleUnicode = titleUnicode;
    }

    public String getMapper() {
        return mapper;
    }

    public void setMapper(String mapper) {
        this.mapper = mapper;
    }

    public Integer getMapperId() {
        return mapperId;
    }

    public void setMapperId(Integer mapperId) {
        this.mapperId = mapperId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getVideo() {
        return video;
    }

    public void setVideo(Boolean video) {
        this.video = video;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public void setNsfw(Boolean nsfw) {
        this.nsfw = nsfw;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getSlimcover() {
        return slimcover;
    }

    public void setSlimcover(String slimcover) {
        this.slimcover = slimcover;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static BeatMapSetInfoLite from(BeatMapSet s) {
        var t = new BeatMapSetInfoLite();
        t.sid = Math.toIntExact(s.getBeatMapSetID());

        t.artistUnicode = s.getArtistUnicode();
        t.artist = s.getArtist();
        t.title = s.getTitle();

        t.mapper = s.getCreator();
        t.mapperId = Math.toIntExact(s.getCreatorID());

        t.nsfw = s.getNsfw();
        t.video = s.getVideo();
        t.status = s.getStatus();

        String url = s.getCovers().getCover2x();
        if (url != null && !url.isEmpty()) {
            t.cover = url;
        } else {
            t.cover = s.getCovers().getCover();
        }

        url = s.getCovers().getCard2x();
        if (url != null && !url.isEmpty()) {
            t.card = url;
        } else {
            t.card = s.getCovers().getCard();
        }

        url = s.getCovers().getList2x();
        if (url != null && !url.isEmpty()) {
            t.list = url;
        } else {
            t.list = s.getCovers().getList();
        }

        url = s.getCovers().getSlimcover2x();
        if (url != null && !url.isEmpty()) {
            t.slimcover = url;
        } else {
            t.slimcover = s.getCovers().getSlimcover();
        }
        return t;
    }
}