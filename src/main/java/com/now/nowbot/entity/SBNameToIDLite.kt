package com.now.nowbot.entity

import jakarta.persistence.*

@Entity @Table(name = "sb_name_id", indexes = [Index(name = "sbfindname", columnList = "name")])
class SBNameToIDLite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    //@Lob
    @Column(columnDefinition = "TEXT")
    var name: String? = null

    @Column(name = "userid")
    var userID: Long? = null

    @Column(name = "idx")
    private var index: Int? = null

    constructor()

    constructor(userID: Long?, name: String, index: Int?) {
        this.name = name.uppercase()
        this.userID = userID
        this.index = index
    }
}
