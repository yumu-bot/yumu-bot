package com.now.nowbot.entity

import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "sb_bind_user", indexes = [Index(name = "bind_uid", columnList = "user_id")])
data class SBBindUserLite(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id")
    var userID: Long,

    @Column(name = "username", columnDefinition = "TEXT")
    var username: String,

    var time: Long = 0L,

    //一些额外信息
    //创号时间
    @Column(name = "join_date")
    var joinDate: OffsetDateTime = OffsetDateTime.now(),

    //主模式
    @Column(name = "main_mode")
    var mainMode: OsuMode = OsuMode.DEFAULT

) {
    fun toSBBindUser(): SBBindUser {
        return SBBindUser(this.id, this.userID, this.username, this.mainMode, this.time, this.joinDate)
    }
}
