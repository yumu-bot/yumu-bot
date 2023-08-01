package com.now.nowbot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "osu_mapset", indexes = {
        @Index(name = "raw", columnList = "map_id")
})
public class MapSetLite {
    @Id
    @Column(name = "map_id")
    private Integer id;

    //@Lob
    @Column(columnDefinition = "TEXT")
    private String artist;
    //@Lob
    @Column(name = "artist_unicode", columnDefinition = "TEXT")
    private String artistUTF;
    //四种 covers:{}
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String cover;
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String card;
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String list;
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String slimcover;

    //属性
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String creator;
    @Column(name = "favourite_count")
    private Integer favourite;
    private Boolean nsfw;
    @Column(name = "play_count")
    private Long playCount;
    //@Lob
    @Column(name = "preview_url",columnDefinition = "TEXT")
    private String musicUrl;
    @Column(name = "legacy_thread_url",columnDefinition = "TEXT")
    private String legacyUrl;
    //状态
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String status;

    //@Lob
    @Column(columnDefinition = "TEXT")
    private String source;
    @Column(columnDefinition = "TEXT")
    private String tags;
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String title;
    //@Lob
    @Column(name = "title_unicode", columnDefinition = "TEXT")
    private String titleUTF;

    //    麻婆id?
    @Column(name = "user_id")
    private Integer mapperId;
    //有没有视频
    private Boolean storyboard;
    //是否关闭下载 一般为false true一般是版权原因下架
    @Column(name = "download_disabled")
    private Boolean availabilityDownloadDisable;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getArtistUTF() {
        return artistUTF;
    }

    public void setArtistUTF(String artist_unicode) {
        this.artistUTF = artist_unicode;
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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Integer getFavourite() {
        return favourite;
    }

    public void setFavourite(Integer favourite_count) {
        this.favourite = favourite_count;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public void setNsfw(Boolean nsfw) {
        this.nsfw = nsfw;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long play_count) {
        this.playCount = play_count;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        this.musicUrl = musicUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleUTF() {
        return titleUTF;
    }

    public void setTitleUTF(String title_unicode) {
        this.titleUTF = title_unicode;
    }

    public Integer getMapperId() {
        return mapperId;
    }

    public void setMapperId(Integer user_id) {
        this.mapperId = user_id;
    }

    public Boolean getStoryboard() {
        return storyboard;
    }

    public void setStoryboard(Boolean video) {
        this.storyboard = video;
    }

    public Boolean getAvailabilityDownloadDisable() {
        return availabilityDownloadDisable;
    }

    public void setAvailabilityDownloadDisable(Boolean download_disabled) {
        this.availabilityDownloadDisable = download_disabled;
    }

    public String getLegacyUrl() {
        return legacyUrl;
    }

    public void setLegacyUrl(String legacyUrl) {
        this.legacyUrl = legacyUrl;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
