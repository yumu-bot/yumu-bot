package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMapSet {
    @JsonProperty("id")
    Integer id;
    @JsonProperty("user_id")
    Integer mapperId;
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

    Boolean video;
    @JsonProperty("covers")
    Covers covers;

    @JsonProperty("ratings")
    List<Integer> ratings;

    @JsonIgnoreProperties
    Boolean availabilityDownloadDisable;
    @JsonIgnoreProperties
    @Nullable
    String availabilityInformation;
    @JsonProperty("availability")
    public void setAvt(HashMap<String, String> map){
        availabilityDownloadDisable = Boolean.valueOf(map.get("download_disabled"));
        availabilityInformation = map.get("more_information");
    }

    @JsonIgnoreProperties
    boolean fromDatabases = false;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMapperId() {
        return mapperId;
    }

    public void setMapperId(Integer mapperId) {
        this.mapperId = mapperId;
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

    public Boolean getAvailabilityDownloadDisable() {
        return availabilityDownloadDisable;
    }

    public void setAvailabilityDownloadDisable(Boolean availabilityDownloadDisable) {
        this.availabilityDownloadDisable = availabilityDownloadDisable;
    }

    @Nullable
    public String getAvailabilityInformation() {
        return availabilityInformation;
    }

    public boolean isFromDatabases() {
        return fromDatabases;
    }

    public void FromDatabases() {
        this.fromDatabases = true;
    }

    public Boolean getVideo() {
        return video;
    }

    public void setVideo(Boolean video) {
        this.video = video;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("BeatMapSet{");
        sb.append("id=").append(id);
        sb.append(", userId=").append(mapperId);
        sb.append(", bpm=").append(bpm);
        sb.append(", artist='").append(artist).append('\'');
        sb.append(", artistUTF='").append(artistUTF).append('\'');
        sb.append(", title='").append(title).append('\'');
        sb.append(", titleUTF='").append(titleUTF).append('\'');
        sb.append(", creator='").append(creator).append('\'');
        sb.append(", favourite=").append(favourite);
        sb.append(", nsfw=").append(nsfw);
        sb.append(", playCount=").append(playCount);
        sb.append(", musicUrl='").append(musicUrl).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append(", legacyUrl='").append(legacyUrl).append('\'');
        sb.append(", tags='").append(tags).append('\'');
        sb.append(", storyboard=").append(storyboard);
        sb.append(", covers=").append(covers);
        sb.append(", availabilityDownloadDisable=").append(availabilityDownloadDisable);
        sb.append(", availabilityInformation='").append(availabilityInformation).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
