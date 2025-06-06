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

    @Transient
    private var currentOsuMode: OsuMode? = null
) {

    @get:Transient
    val mode: OsuMode
        get() = currentOsuMode ?: OsuMode.getMode(modeValue.toInt())

    //主模式
    @Column(name = "main_mode")
    private var modeValue: Byte = -1
        get() = currentOsuMode?.modeValue ?: field
        set(value) {
            field = value
            currentOsuMode = OsuMode.getMode(value.toInt())
        }

    fun toSBBindUser(): SBBindUser {
        return SBBindUser(this.id, this.userID, this.username, this.mode, this.time, this.joinDate)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SBBindUserLite

        return userID == other.userID
    }

    override fun hashCode(): Int {
        return userID.hashCode()
    }
}
