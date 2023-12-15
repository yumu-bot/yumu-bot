package com.now.nowbot.model.mappool.now;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CategoryGroupVo {
    String         name;
    String         info;
    Integer        color;
    Integer        modsOptional;
    Integer        modsRequired;
    List<Category> category;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public Integer getModsOptional() {
        return modsOptional;
    }

    public void setModsOptional(Integer modsOptional) {
        this.modsOptional = modsOptional;
    }

    public Integer getModsRequired() {
        return modsRequired;
    }

    public void setModsRequired(Integer modsRequired) {
        this.modsRequired = modsRequired;
    }

    public List<Category> getCategory() {
        return category;
    }

    public void setCategory(List<Category> category) {
        this.category = category;
    }

    public record Category(String name, Long bid, Long creater) {
    }
}
