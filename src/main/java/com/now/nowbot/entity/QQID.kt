package com.now.nowbot.entity

import jakarta.persistence.*

@Entity
@Table(name = "qq_id")
class QQID {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    //true group   |    false friend
    @Column(name = "type")
    var isGroup: Boolean? = null

    @Column(name = "permission_id")
    var permissionID: Long? = null

    @Column(name = "qq")
    var QQ: Long? = null
}
