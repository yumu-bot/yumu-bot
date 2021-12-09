package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.enums.OsuMode;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OsuUser {
    Integer id;
    Double pp;
    @JsonProperty("statistics")
    public Statustucs statustucs;


    public record Country(String  countryCode, String  countryName){}
    public record Kudosu(Integer total, Integer available){}
    public record RankHistory(OsuMode mode, List<Integer> history){}

    String occupation;
    @JsonProperty("unranked_beatmapset_count")
    Integer beatmapSetCountUnranked;
    @JsonProperty("ranked_beatmapset_count")
    Integer beatmapSetCountRanked;
    @JsonProperty("ranked_and_approved_beatmapset_count")
    Integer beatmapSetCountRankedAndApproved;
    /** 这是什么我也不知道 */
    @JsonProperty("beatmap_playcounts_count")
    Integer beatmapSetCountPlaycounts;
    @JsonProperty("mapping_follower_count")
    Integer beatmapSetCountFlollower;
    @JsonProperty("has_supported")
    Boolean hasSupported;
    @JsonProperty("is_bot")
    Boolean isBot;
    @JsonProperty("pm_friends_only")
    Boolean pmFriendsOnly;
    Cover cover;
    @JsonProperty("profile_order")
    List<String> profileOrder;
    @JsonProperty("previous_usernames")
    List<String> previousName;
    @JsonProperty("join_date")
    String joinDate;
    @JsonProperty("max_friends")
    Integer maxFriends;
    @JsonProperty("comments_count")
    Integer commentsCount;
    @JsonProperty("support_level")
    short supportLeve;
    @JsonProperty("post_count")
    Integer postCount;
    @JsonProperty("raw")
    public String page;


    @JsonIgnore
    Country country;
    @JsonProperty("country")
    void setCountry(Map<String,String> country){
        this.country = new Country(country.get("code"),country.get("name"));
    }
    @JsonIgnore
    Kudosu kudosu;
    @JsonProperty("kudosu")
    void setKudosu(Map<String,Integer> kudosu){
        this.kudosu = new Kudosu(kudosu.get("total"),kudosu.get("available"));
    }
    @JsonIgnore
    RankHistory rankHistory;
    @JsonProperty("rank_history")
    void setRankHistory(Map<String,Object> map){
        this.rankHistory = new RankHistory(OsuMode.getMode((String) map.get("mode")), (List<Integer>) map.get("data"));
    }
}
