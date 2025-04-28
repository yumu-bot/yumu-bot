package com.now.nowbot.service.osuApiService

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.*
import org.springframework.web.reactive.function.client.WebClientResponseException

interface OsuUserApiService {
    fun isPlayerExist(name: String): Boolean

    @Throws(WebClientResponseException::class) fun getOauthUrl(state: String): String {
        return getOauthUrl(state, false)
    }

    @Throws(WebClientResponseException::class) fun getOauthUrl(state: String, full: Boolean): String

    @CanIgnoreReturnValue @Throws(WebClientResponseException::class) fun refreshUserToken(user: BindUser): String?

    @Throws(WebClientResponseException::class) fun refreshUserTokenFirst(user: BindUser)

    @Throws(WebClientResponseException::class) fun getOsuUser(user: BindUser, mode: OsuMode): OsuUser

    @Throws(WebClientResponseException::class) fun getOsuUser(name: String, mode: OsuMode): OsuUser

    @Throws(WebClientResponseException::class) fun getOsuUser(id: Long, mode: OsuMode): OsuUser

    @Throws(WebClientResponseException::class) fun getOsuUser(user: BindUser): OsuUser {
        return getOsuUser(user, user.osuMode)
    }

    @Throws(WebClientResponseException::class) fun getOsuUser(name: String): OsuUser {
        return getOsuUser(name, OsuMode.DEFAULT)
    }

    @Throws(WebClientResponseException::class) fun getOsuUser(id: Long): OsuUser {
        return getOsuUser(id, OsuMode.DEFAULT)
    }

    fun getOsuID(name: String): Long

    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     */
    @Throws(WebClientResponseException::class) fun <T : Number> getUsers(
        users: Iterable<T>,
        isVariant: Boolean
    ): List<MicroUser>


    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     */
    fun <U : Number> getUsers(users: Iterable<U>): List<MicroUser> {
        return getUsers(users, false)
    }

    @Throws(WebClientResponseException::class) fun getFriendList(user: BindUser): List<LazerFriend>

    fun getUserRecentActivity(id: Long, offset: Int, limit: Int): List<ActivityEvent>

    fun getUserKudosu(user: BindUser): KudosuHistory

    fun sendPrivateMessage(sender: BindUser, target: Long, message: String): JsonNode

    fun acknowledgmentPrivateMessageAlive(user: BindUser, since: Long?): JsonNode

    fun acknowledgmentPrivateMessageAlive(user: BindUser): JsonNode {
        return acknowledgmentPrivateMessageAlive(user, null)
    }

    fun getPrivateMessage(sender: BindUser, channel: Long, since: Long): JsonNode

    fun getTeamInfo(id: Int): TeamInfo?

    @JvmRecord
    data class TeamInfo(
        val id: Int,
        val name: String,
        val abbr: String,
        val formed: String,

        val banner: String,
        val flag: String,

        val users: List<OsuUser>,
        val ruleset: OsuMode,
        val application: String,

        val rank: Int,
        val pp: Int,

        @JsonProperty("ranked_score")
        val rankedScore: Long,

        @JsonProperty("play_count")
        val playCount: Long,

        val members: Int,

        val description: String
    )

    fun asyncDownloadAvatar(users: List<MicroUser>)

    fun asyncDownloadBackground(users: List<MicroUser>)
}