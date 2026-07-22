package com.now.nowbot.entity

import jakarta.persistence.*

@Entity @Table(name = "osu_name_id",
    indexes = [
        // Index(name = "osufindname", columnList = "name"),
        // CREATE INDEX idx_osu_name_id_trgm_name ON osu_name_id USING gin (name gin_trgm_ops);
        Index(name = "osu_name_user_id", columnList = "user_id")
    ])
class OsuNameToIDLite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "name", length = 32)
    var name: String? = null

    @Column(name = "user_id")
    var userID: Long? = null

    @Column(name = "index")
    private var index: Int? = null

    @Suppress("UNUSED")
    constructor()

    constructor(userID: Long?, name: String, index: Int?) {
        this.name = name
        this.userID = userID
        this.index = index
    }
}
