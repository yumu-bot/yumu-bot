package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KudosuHistory {
    record Giver(String url, String username) {
    }

    record Post(@Nullable String url, String title) {
    }//It'll be "[deleted beatmap]" for deleted beatmaps.

    Integer id;
    String action;
    Integer amount;
    String model;
    @JsonIgnoreProperties
    LocalDateTime created;
    @JsonIgnoreProperties
    Giver giver;
    @JsonIgnoreProperties
    Post post;

    @JsonProperty("created")
    public void setCreated(String time){
        this.created = LocalDateTime.from(BpInfo.formatter.parse(time));
    }
    @JsonProperty("giver")
    public void setGiver(HashMap<String, String> map){
        if (map != null && !map.isEmpty()){
            var url = map.get("url");
            var username = map.get("username");
            this.giver = new Giver(url, username);
        }
    }
    @JsonProperty("post")
    public void setPost(HashMap<String, String> map){
        if (map != null && !map.isEmpty()){
            var url = map.get("url");
            var title = map.get("title");
            this.post = new Post(url, title);
        }
    }

    public Integer getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getModel() {
        return model;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public Giver getGiver() {
        return giver;
    }

    public Post getPost() {
        return post;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KudosuHistory{");
        sb.append("id=").append(id);
        sb.append(", action='").append(action).append('\'');
        sb.append(", amount=").append(amount);
        sb.append(", model='").append(model).append('\'');
        sb.append(", created=").append(created);
        sb.append(", giver=").append(giver);
        sb.append(", post=").append(post);
        sb.append('}');
        return sb.toString();
    }
}
