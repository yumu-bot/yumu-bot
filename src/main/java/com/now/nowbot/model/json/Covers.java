package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Covers {
    @JsonProperty("cover")
    String cover;
    @JsonProperty("cover@2x")
    String cover2x;
    @JsonProperty("card")
    String card;
    @JsonProperty("card@2x")
    String card2x;
    @JsonProperty("list")
    String list;
    @JsonProperty("list@2x")
    String list2x;
    @JsonProperty("slimcover")
    String slimcover;
    @JsonProperty("slimcover@2x")
    String slimcover2x;

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getCover2x() {
        return cover2x;
    }

    public void setCover2x(String cover2x) {
        this.cover2x = cover2x;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getCard2x() {
        return card2x;
    }

    public void setCard2x(String card2x) {
        this.card2x = card2x;
    }

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getList2x() {
        return list2x;
    }

    public void setList2x(String list2x) {
        this.list2x = list2x;
    }

    public String getSlimcover() {
        return slimcover;
    }

    public void setSlimcover(String slimcover) {
        this.slimcover = slimcover;
    }

    public String getSlimcover2x() {
        return slimcover2x;
    }

    public void setSlimcover2x(String slimcover2x) {
        this.slimcover2x = slimcover2x;
    }

    @Override
    public String toString() {
        return STR."Covers{cover='\{cover}\{'\''}, cover2x='\{cover2x}\{'\''}, card='\{card}\{'\''}, card2x='\{card2x}\{'\''}, list='\{list}\{'\''}, list2x='\{list2x}\{'\''}, slimcover='\{slimcover}\{'\''}, slimcover2x='\{slimcover2x}\{'\''}\{'}'}";
    }
}
