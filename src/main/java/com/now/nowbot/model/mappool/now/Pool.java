package com.now.nowbot.model.mappool.now;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.now.nowbot.model.JsonData.BeatMap;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Pool {
    Integer id;
    String  info;
    String  name;
    Integer mode = 0;
    String  banner;

    PoolStatus            status = PoolStatus.OPEN;
    List<CategoryGroupVo> categoryList;
    List<BeatMap>         mapinfo;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public PoolStatus getStatus() {
        return status;
    }

    public void setStatus(PoolStatus status) {
        this.status = status;
    }

    public List<CategoryGroupVo> getCategoryList() {
        return categoryList;
    }

    public void setCategoryList(List<CategoryGroupVo> categoryList) {
        this.categoryList = categoryList;
    }

    public List<BeatMap> getMapinfo() {
        return mapinfo;
    }

    public void setMapinfo(List<BeatMap> mapinfo) {
        this.mapinfo = mapinfo;
    }
}
