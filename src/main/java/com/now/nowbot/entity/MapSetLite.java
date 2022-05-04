package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(name = "osu_mapset", indexes = {
        @Index(name = "raw",columnList = "map_id")
})
public class MapSetLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "map_id")
    private Integer mapset_id;

    private String artist;
    private String artist_unicode;
    //四种 covers:{}
    private String cover;
    private String card;
    private String list;
    private String slimcover;

    //属性
    private String creator;
    private Integer favourite_count;
    private Boolean nsfw;
    private String play_count;
    private String preview_url;
    //状态
    private String status;

    private String source;
    private String title;
    private String title_unicode;

//    麻婆id?
    private Integer user_id;
    //有没有视频
    private Boolean video;
    //是否关闭下载 一般为false true一般是版权原因下架
    private Boolean download_disabled;
    //是否有排名?
    private Boolean is_scoreable;


    public Integer getMapset_id() {
        return mapset_id;
    }

    public void setMapset_id(Integer id) {
        this.mapset_id = id;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getArtist_unicode() {
        return artist_unicode;
    }

    public void setArtist_unicode(String artist_unicode) {
        this.artist_unicode = artist_unicode;
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

    public Integer getFavourite_count() {
        return favourite_count;
    }

    public void setFavourite_count(Integer favourite_count) {
        this.favourite_count = favourite_count;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public void setNsfw(Boolean nsfw) {
        this.nsfw = nsfw;
    }

    public String getPlay_count() {
        return play_count;
    }

    public void setPlay_count(String play_count) {
        this.play_count = play_count;
    }

    public String getPreview_url() {
        return preview_url;
    }

    public void setPreview_url(String preview_url) {
        this.preview_url = preview_url;
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

    public String getTitle_unicode() {
        return title_unicode;
    }

    public void setTitle_unicode(String title_unicode) {
        this.title_unicode = title_unicode;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public Boolean getVideo() {
        return video;
    }

    public void setVideo(Boolean video) {
        this.video = video;
    }

    public Boolean getDownload_disabled() {
        return download_disabled;
    }

    public void setDownload_disabled(Boolean download_disabled) {
        this.download_disabled = download_disabled;
    }
}
