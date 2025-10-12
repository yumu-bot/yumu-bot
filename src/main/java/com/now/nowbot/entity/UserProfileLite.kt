package com.now.nowbot.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class UserProfileLite {

    @Id
    @JsonIgnore
    var id: Long? = null

    @JsonIgnore
    var userId: Long? = null

    /**
     * 自定义卡片背景
     */
    @JsonProperty("card")
    @Column(columnDefinition = "TEXT", updatable = true, nullable = true)
    var card: String? = null

    /**
     * 自定义横幅
     */
    @JsonProperty("banner")
    @Column(columnDefinition = "TEXT", updatable = true, nullable = true)
    var banner: String? = null

    @JsonProperty("mascot")
    @Column(columnDefinition = "TEXT", updatable = true, nullable = true)
    var mascot: String? = null

    /**
     * 头像框
     */
    @JsonProperty("header") @Column(name = "header", columnDefinition = "TEXT", updatable = true, nullable = true)
    var avatarFrame: String? = null

    @JsonProperty("info") @Column(name = "panel_info", columnDefinition = "TEXT", updatable = true, nullable = true)
    var infoPanel: String? = null

    @JsonProperty("score") @Column(name = "panel_score", columnDefinition = "TEXT", updatable = true, nullable = true)
    var scorePanel: String? = null

    @JsonProperty("ppm") @Column(name = "panel_ppm", columnDefinition = "TEXT", updatable = true, nullable = true)
    var ppmPanel: String? = null
}
