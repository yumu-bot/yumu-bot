package com.now.nowbot.entity

import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity @Table(name = "osu_bind_user", indexes = [Index(name = "bind_oid", columnList = "osu_id")])
data class OsuBindUserLite(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,

    @Column(name = "osu_id") var osuID: Long = 0L,

    @Column(name = "osu_name", columnDefinition = "TEXT") var osuName: String = "",

    @Column(name = "access_token", columnDefinition = "TEXT") var accessToken: String? = null,

    @Column(name = "refresh_token", columnDefinition = "TEXT") var refreshToken: String? = null,

    @Column(name = "update_count") var updateCount: Int = 0,

    var time: Long? = null,

    //一些额外信息
    //创号时间
    @Column(name = "join_date") var joinDate: LocalDateTime = LocalDateTime.now(),

    @Transient
    private var currentOsuMode: OsuMode? = null
) {

    @get:Transient
    val mode: OsuMode
        get() = currentOsuMode ?: OsuMode.getMode(modeValue.toInt())

    //主模式
    @Column(name = "main_mode")
    private var modeValue: Byte = -1
        get() {
            if (currentOsuMode != null) {
                field = currentOsuMode!!.modeValue
            }

            return field
        }
        set(value) {
            field = value
            currentOsuMode = OsuMode.getMode(value.toInt())
        }

    constructor(data: BindUser) : this(
        data.baseID, data.userID, data.username, data.accessToken, data.refreshToken, 0, data.time, LocalDateTime.now(), data.mode
    )
}
