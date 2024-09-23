package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchInfo {
    @JsonProperty("c")

    String general;
    @JsonProperty("sort")

    String sort;
    @JsonProperty("s")

    String status;
    @JsonProperty("nsfw")

    Boolean nsfw;
    @JsonProperty("g")

    byte genre;
    @JsonProperty("l")

    byte language;
    @JsonProperty("e")

    String others;
    @JsonProperty("r")

    String rank;
    @JsonProperty("played")

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
