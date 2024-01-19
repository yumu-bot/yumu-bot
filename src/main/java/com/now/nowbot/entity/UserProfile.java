package com.now.nowbot.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class UserProfile {
    @Id
    @JsonIgnore
    Long userId;

    /**
     * 自定义卡片背景
     */
    @JsonProperty("card")
    @Column(columnDefinition = "TEXT")
    String card;
    /**
     * 自定义 banner
     */
    @JsonProperty("banner")
    @Column(columnDefinition = "TEXT")
    String banner;

    /**
     * 头像框
     */
    @JsonProperty("header")
    @Column(name = "header", columnDefinition = "TEXT")
    String headerBorder;
    @JsonProperty("info")
    @Column(name = "panel_info", columnDefinition = "TEXT")
    String infoPanel;
    @JsonProperty("score")
    @Column(name = "panel_score", columnDefinition = "TEXT")
    String scorePanel;
    @JsonProperty("ppm")
    @Column(name = "panel_ppm", columnDefinition = "TEXT")
    String ppmPanel;

    @JsonIgnore
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getInfoPanel() {
        return infoPanel;
    }

    public void setInfoPanel(String infoPanel) {
        this.infoPanel = infoPanel;
    }

    public String getScorePanel() {
        return scorePanel;
    }

    public void setScorePanel(String scorePanel) {
        this.scorePanel = scorePanel;
    }

    public String getPpmPanel() {
        return ppmPanel;
    }

    public void setPpmPanel(String ppmPanel) {
        this.ppmPanel = ppmPanel;
    }
}
