package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

public class SearchInfo {
    @JsonProperty("c")
    @Nullable
    String general;
    @JsonProperty("sort")
    @Nullable
    String sort;
    @JsonProperty("s")
    @Nullable
    String status;
    @JsonProperty("nsfw")
    @Nullable
    Boolean nsfw;
    @JsonProperty("g")
    @Nullable
    byte genre;
    @JsonProperty("l")
    @Nullable
    byte language;
    @JsonProperty("e")
    @Nullable
    String others;
    @JsonProperty("r")
    @Nullable
    String rank;
    @JsonProperty("played")
    @Nullable
    String played;

    public String getGeneral() {
        return general;
    }
    public void setGeneral(String general) {
        this.general = general;
    }
    public String getSort() {
        return sort;
    }
    public void setSort(String sort) {
        this.sort = sort;
    }
    @Nullable
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Boolean getNsfw() {
        return nsfw;
    }
    public void setNsfw(Boolean nsfw) {
        this.nsfw = nsfw;
    }
    public byte getGenre() {
        return genre;
    }
    public void setGenre(byte genre) {
        this.genre = genre;
    }
    public byte getLanguage() {
        return language;
    }
    public void setLanguage(byte language) {
        this.language = language;
    }
    public String getOthers() {
        return others;
    }
    public void setOthers(String others) {
        this.others = others;
    }
    @Nullable
    public String getRank() {
        return rank;
    }
    public void setRank(String rank) {
        this.rank = rank;
    }
    public String getPlayed() {
        return played;
    }
    public void setPlayed(String played) {
        this.played = played;
    }

}
