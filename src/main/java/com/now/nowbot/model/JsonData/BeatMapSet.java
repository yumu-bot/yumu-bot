package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMapSet {
    @JsonProperty("id")
    Integer id;
    @JsonProperty("user_id")
    Integer userId;
    @JsonProperty("bpm")
    Float bpm;
    @JsonProperty("artist")
    String artist;
    @JsonProperty("artist_unicode")
    String artistUTF;
    @JsonProperty("title")
    String title;
    @JsonProperty("title_unicode")
    String titleUTF;
    @JsonProperty("creator")
    String creator;
    @JsonProperty("favourite_count")
    Integer favourite;
    @JsonProperty("nsfw")
    Boolean nsfw;
    @JsonProperty("play_count")
    Long playCount;
    @JsonProperty("preview_url")
    String musicUrl;
    @JsonProperty("source")
    String source;
    @JsonProperty("status")
    String status;
    @JsonProperty("legacy_thread_url")
    String legacyUrl;
    @JsonProperty("tags")
    String tags;
    @JsonProperty("storyboard")
    Boolean storyboard;
    @JsonProperty("covers")
    Covers covers;

    @Override
    public String toString() {
        return "BeatMapSet{" +
                "id=" + id +
                ", userId=" + userId +
                ", bpm=" + bpm +
                ", artist='" + artist + '\'' +
                ", artistUTF='" + artistUTF + '\'' +
                ", title='" + title + '\'' +
                ", titleUTF='" + titleUTF + '\'' +
                ", creator='" + creator + '\'' +
                ", favourite=" + favourite +
                ", nsfw=" + nsfw +
                ", playCount=" + playCount +
                ", musicUrl='" + musicUrl + '\'' +
                ", source='" + source + '\'' +
                ", status='" + status + '\'' +
                ", legacyUrl='" + legacyUrl + '\'' +
                ", tags='" + tags + '\'' +
                ", storyboard=" + storyboard +
                ", covers=" + covers +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Float getBpm() {
        return bpm;
    }

    public void setBpm(Float bpm) {
        this.bpm = bpm;
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

    public void setArtistUTF(String artistUTF) {
        this.artistUTF = artistUTF;
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

    public void setTitleUTF(String titleUTF) {
        this.titleUTF = titleUTF;
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

    public void setFavourite(Integer favourite) {
        this.favourite = favourite;
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

    public void setPlayCount(Long playCount) {
        this.playCount = playCount;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        this.musicUrl = musicUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Boolean getStoryboard() {
        return storyboard;
    }

    public void setStoryboard(Boolean storyboard) {
        this.storyboard = storyboard;
    }

    public Covers getCovers() {
        return covers;
    }

    public void setCovers(Covers covers) {
        this.covers = covers;
    }
}
