package com.now.nowbot.service.osuApiService

import com.fasterxml.jackson.databind.JsonNode
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.service.web.TeamInfo
import com.now.nowbot.service.web.TopPlays
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import org.springframework.web.reactive.function.client.WebClientResponseException

interface OsuUserApiService {
    fun getAvatarByte(user: OsuUser): ByteArray

    fun isPlayerExist(name: String): Boolean

    @Throws(WebClientResponseException::class) fun getOauthUrl(state: String): String {
        return getOauthUrl(state, false)
    }

    @Throws(WebClientResponseException::class) fun getOauthUrl(state: String, full: Boolean): String

    @Throws(BindException::class) fun refreshUserTokenInstant(user: BindUser?, isMyself: Boolean = false): BindUser

    @CanIgnoreReturnValue @Throws(NetworkException.UserException::class) fun refreshUserToken(user: BindUser): String?

    @Throws(WebClientResponseException::class) fun refreshUserTokenFirst(user: BindUser)

    @Throws(WebClientResponseException::class) fun getOsuUser(user: BindUser, mode: OsuMode): OsuUser

    @Throws(WebClientResponseException::class) fun getOsuUser(name: String, mode: OsuMode): OsuUser

    @Throws(WebClientResponseException::class) fun getOsuUser(id: Long, mode: OsuMode): OsuUser

    @Throws(WebClientResponseException::class) fun getOsuUser(user: BindUser): OsuUser {
        return getOsuUser(user, user.mode)
    }

    @Throws(WebClientResponseException::class) fun getOsuUser(name: String): OsuUser {
        return getOsuUser(name, OsuMode.DEFAULT)
    }

    @Throws(WebClientResponseException::class) fun getOsuUser(id: Long): OsuUser {
        return getOsuUser(id, OsuMode.DEFAULT)
    }

    fun getOsuUsers(ids: List<Long>, mode: OsuMode, batchSize: Int = 90, latencyMillis: Long = 60000): List<OsuUser>

    fun getOsuID(name: String): Long

    /**
     * 批量获取用户信息
     *
     * @param users 单次请求量无限制
     * @param isVariant 是否需要额外的四模式信息
     */
    fun <T : Number> getUsers(users: Collection<T>, isVariant: Boolean = false, isBackground: Boolean = false): List<MicroUser>

    fun getFriendList(user: BindUser): List<LazerFriend>

    fun getUserRecentActivity(id: Long, offset: Int, limit: Int): List<ActivityEvent>

    fun getUserKudosu(user: BindUser): KudosuHistory

    fun sendPrivateMessage(sender: BindUser, target: Long, message: String): JsonNode

    fun acknowledgmentPrivateMessageAlive(user: BindUser, since: Long?): JsonNode

    fun acknowledgmentPrivateMessageAlive(user: BindUser): JsonNode {
        return acknowledgmentPrivateMessageAlive(user, null)
    }

    fun getPrivateMessage(sender: BindUser, channel: Long, since: Long): JsonNode

    fun applyUserForBeatmapset(beatmapsets: List<Beatmapset>)

    fun asyncDownloadAvatar(users: List<MicroUser>)

    fun asyncDownloadAvatarFromBeatmapsets(beatmapsets: List<Beatmapset>) {
        val set = beatmapsets.flatMap { it.beatmaps ?: listOf() }.mapNotNull { it.user }.toSet()

        asyncDownloadAvatar(set.map { o -> MicroUser().apply { this.avatarUrl = o.avatarUrl } })
    }

    fun asyncDownloadBackground(users: List<MicroUser>)

    fun getTeamInfo(id: Int): TeamInfo?

    /**
     * @param page 最大 10 页，最小 1 页
     */
    fun getTopPlays(page: Int = 1, mode: OsuMode = OsuMode.OSU): TopPlays?
}