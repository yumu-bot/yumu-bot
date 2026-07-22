package com.now.nowbot.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "sb_name_id",
    indexes = [
        Index(name = "sb_find_name", columnList = "name"),
        Index(name = "sb_name_user_id", columnList = "user_id")
    ]
)
class SBNameToIDLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
        // 保持大写规范
        this.name = name.uppercase()
        this.userID = userID
        this.index = index
    }
}
