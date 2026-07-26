package com.now.nowbot.entity

import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity @Table(name = "osu_bind_user", indexes = [Index(name = "bind_oid", columnList = "osu_id")])
data class OsuBindUserLite(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,

    @Column(name = "osu_id") var userID: Long = 0L,

    @Column(name = "osu_name", columnDefinition = "TEXT") var username: String = "",

    @Column(name = "access_token", columnDefinition = "TEXT") var accessToken: String? = null,

    @Column(name = "refresh_token", columnDefinition = "TEXT") var refreshToken: String? = null,

    @Column(name = "update_count") var updateCount: Int = 0,

    var time: Long? = null,

    //一些额外信息
    //创号时间
    @Column(name = "join_date") var joinDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "main_mode")
    var modeValue: Byte = -1
) {

    @get:Transient
    var mode: OsuMode
        get() = modeValue.toOsuMode()
        set(value) {
            modeValue = value.modeValue
        }

    constructor(data: BindUser) : this(
        data.baseID, data.userID, data.username, data.accessToken, data.refreshToken, 0, data.time, LocalDateTime.now(), data.mode.safeModeValue
    )

    companion object {
        fun OsuBindUserLite.toModel(): BindUser {
            val lite = this

            return BindUser().apply {
                baseID = id
                userID = lite.userID
                username = lite.username
                accessToken = lite.accessToken
                refreshToken = lite.refreshToken
                time = lite.time
                mode = lite.mode
            }
        }

        fun BindUser.toEntity(): OsuBindUserLite {
            return OsuBindUserLite(this)
        }
    }
}
