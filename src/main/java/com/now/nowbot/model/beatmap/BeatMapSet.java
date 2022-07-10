package com.now.nowbot.model.beatmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.Covers;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMapSet {
    Integer id;
    String artist;
    @JsonProperty("artist_unicode")
    String artistUnicode;

    String title;
    @JsonProperty("title_unicode")
    String titleUnicode;

    Covers covers;
    @JsonProperty("creator")
    String mapper;

    @JsonProperty("user_id")
    Integer mapperId;
    @JsonProperty("play_count")
    Long playCount;

    /***
     * rankde ...
     */
    String status;
    Boolean video;
    Boolean nsfw;

    @Override
    public String toString() {
        return "BeatMapSet{" +
                "id=" + id +
                ", artist='" + artist + '\'' +
                ", artistUnicode='" + artistUnicode + '\'' +
                ", title='" + title + '\'' +
                ", titleUnicode='" + titleUnicode + '\'' +
                ", covers=" + covers +
                ", mapper='" + mapper + '\'' +
                ", mapperId=" + mapperId +
                ", playCount=" + playCount +
                ", status='" + status + '\'' +
                ", video=" + video +
                ", nsfw=" + nsfw +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getArtistUnicode() {
        return artistUnicode;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleUnicode() {
        return titleUnicode;
    }

    public Covers getCovers() {
        return covers;
    }

    public String getMapper() {
        return mapper;
    }

    public Integer getMapperId() {
        return mapperId;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getVideo() {
        return video;
    }

    public Boolean getNsfw() {
        return nsfw;
    }
}
