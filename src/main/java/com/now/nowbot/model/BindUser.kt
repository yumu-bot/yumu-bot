package com.now.nowbot.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import kotlin.time.Duration.Companion.days

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class BindUser {
    /**
     * 记录在数据库中的 id, 非 uid
     *
     * @return base
     */
    var baseID: Long? = null

    var username: String = ""

    var userID: Long = 0

    /**
     * 当前令牌
     */
    var accessToken: String? = null

    /**
     * 刷新令牌
     */
    var refreshToken: String? = null

    /**
     * 过期时间戳
     */
    var time: Long? = null

    /**
     * 主模式
     */
    var mode: OsuMode = OsuMode.DEFAULT

    constructor() {
        setTimeToNow()
    }

    constructor(refreshToken: String?) {
        this.refreshToken = refreshToken
        setTimeToNow()
    }

    constructor(base: Long) : this() {
        baseID = base
    }

    constructor(userID: Long, name: String) {
        this.userID = userID
        this.username = name
        mode = OsuMode.DEFAULT
        time = 0L
        setTimeToNow()
    }

    constructor(user: OsuUser) {
        this.userID = user.userID
        this.username = user.username
        this.mode = user.currentOsuMode
        time = 0L
        setTimeToNow()
    }

    /**
     * 有 token 也可能是过期的，如果想判断是否过期，请使用 isTokenAvailable
     */
    val hasToken: Boolean
        // 是否绑定过
        get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()

    fun setTimeToNow() {
        time = System.currentTimeMillis()
    }

    fun setTimeToAfter(millis: Long): Long {
        time = System.currentTimeMillis() + millis
        return time!!
    }

    /**
     * - true: 没有过期
     * - false: 可能过期，但可能可以用（6天之内
     * - null: 无效，或是已经过期很久
     */
    val isTokenAvailable: Boolean?
        get() {
            val tokenTime = time ?: return null // 如果 time 为 null 直接无效
            if (!hasToken) return null

            val now = System.currentTimeMillis()
            val gracePeriodMs = 6.days.inWholeMilliseconds // 确保单位统一为毫秒

            return when {
                now < tokenTime -> true                    // 还没过期
                now < (tokenTime + gracePeriodMs) -> false // 已过期，但在 6 天宽限期内
                else -> null                               // 彻底过期或无效
            }
        }

    val isExpired: Boolean
        get() = time == null || System.currentTimeMillis() >= time!!


    // 重写 equals 必须要重写 hashCode, 如果别的地方使用 HashSet/HashMap 会炸
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BindUser) return false

        return userID == other.userID || username.equals(other.username, ignoreCase = true)
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + userID.hashCode()
        return result
    }

    override fun toString(): String {
        return "BindUser(baseID=$baseID, username='$username', userID=$userID, accessToken=$accessToken, refreshToken=$refreshToken, time=$time, mode=$mode, hasToken=$hasToken, isTokenAvailable=$isTokenAvailable, isExpired=$isExpired)"
    }
}
