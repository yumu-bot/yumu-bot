package com.now.nowbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMapSet {
    @JsonProperty("id")
    Integer id;
    @JsonProperty("user_id")
    Integer userId;
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
    @JsonProperty("covers")
    Covers covers;

    @Override
    public String toString() {
        return "BeatMapSet{" +
                "id=" + id +
                ", userId=" + userId +
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
                ", covers=" + covers +
                '}';
    }
}
