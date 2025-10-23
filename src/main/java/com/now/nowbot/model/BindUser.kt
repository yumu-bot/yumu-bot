package com.now.nowbot.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class BindUser {
    /**
     * 记录在数据库中的 id, 非 uid
     *
     * @return base
     */
    @JvmField var baseID: Long? = null

    @JvmField var username: String = ""

    @JvmField var userID: Long = 0

    /**
     * 当前令牌
     */
    @JvmField var accessToken: String? = null

    /**
     * 刷新令牌
     */
    @JvmField var refreshToken: String? = null

    /**
     * 过期时间戳
     */
    @JvmField var time: Long? = null

    /**
     * 主模式
     */
    var mode: OsuMode = OsuMode.DEFAULT

    constructor() {
        setTimeToNow()
    }

    constructor(base: Long) : this() {
        baseID = base
    }

    constructor(osuID: Long, name: String) {
        this.userID = osuID
        this.username = name
        mode = OsuMode.DEFAULT
        time = 0L
        setTimeToNow()
    }

    constructor(user: OsuUser) {
        this.userID = user.userID
        this.username = user.username
        this.mode = user.defaultOsuMode
        time = 0L
        setTimeToNow()
    }

    val isAuthorized: Boolean
        // 是否绑定过
        get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()

    fun setTimeToNow() {
        time = System.currentTimeMillis()
    }

    fun setTimeToAfter(millis: Long): Long {
        time = System.currentTimeMillis() + millis
        return time!!
    }

    val isExpired: Boolean
        get() = !isNotExpired

    val isNotExpired: Boolean
        get() = time != null && System.currentTimeMillis() < time!!

    override fun toString(): String {
        return baseID.toString() +
                "," + username +
                "," + userID +
                "," + accessToken +
                "," + refreshToken +
                "," + time +
                "," + mode
    }

    // 重写 equals 必须要重写 hashCode, 如果别的地方使用 HashSet/HashMap 会炸
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BindUser) return false

        return username.equals(other.username, ignoreCase = true) || userID == other.userID
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + userID.hashCode()
        return result
    }

    companion object {
        @JvmStatic fun create(refreshToken: String?): BindUser {
            val user = BindUser()
            user.refreshToken = refreshToken
            user.setTimeToNow()
            return user
        }
    }
}
