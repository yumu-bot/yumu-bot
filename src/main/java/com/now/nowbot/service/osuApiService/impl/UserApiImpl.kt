package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.getMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService.TeamInfo
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.JacksonUtil
import io.netty.channel.unix.Errors
import io.netty.handler.timeout.ReadTimeoutException
import kotlinx.io.IOException
import org.codehaus.plexus.util.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Callable
import java.util.regex.Pattern

@Service class UserApiImpl(
    private val base: OsuApiBaseService, private val bindDao: BindDao, private val userInfoDao: OsuUserInfoDao
) : OsuUserApiService {
    override fun getAvatarByte(user: OsuUser): ByteArray {
        return try {
            request { client ->
                client.get().uri(user.avatarUrl).retrieve().bodyToMono(ByteArray::class.java)
            }
        } catch (_: NetworkException) {
            log.error("获取玩家 ${user.userID} 头像失败，尝试返回默认头像")

            request { client ->
                client.get().uri("https://a.ppy.sh/").retrieve().bodyToMono(ByteArray::class.java)
            }
        }
    }

    // 用来确认玩家是否存在于服务器，而无需使用 API 请求。
    override fun isPlayerExist(name: String): Boolean {
        val response = request { client ->
            client.get().uri("https://osu.ppy.sh/users/{name}", name).headers(base::insertHeader).retrieve()
                .bodyToMono(String::class.java).onErrorReturn("")
        }

        return StringUtils.isNotEmpty(response)
    }

    override fun getOauthUrl(state: String, full: Boolean): String {
        return UriComponentsBuilder.fromUriString("https://osu.ppy.sh/oauth/authorize")
            .queryParam("client_id", base.oauthId).queryParam("redirect_uri", base.redirectUrl)
            .queryParam("response_type", "code").queryParam(
                "scope", if (full) "chat.read chat.write chat.write_manage forum.write friends.read identify public"
                else "friends.read identify public"
            ).queryParam("state", state).build().encode().toUriString()
    }

    /**
     * 这个方法适用于需要传递玩家 token 的请求之前——绑定玩家需要有立即可用的 token。
     */
    override fun refreshUserTokenInstant(user: BindUser?, isMyself: Boolean): BindUser {
        if (user == null) {
            if (isMyself) {
                throw BindException.NotBindException.YouNotBind()
            } else {
                throw BindException.NotBindException.UserNotBind()
            }
        }

        if (!user.isAuthorized) {
            throw BindException.Oauth2Exception.UpgradeException()
        }

        if (user.isExpired) {
            try {
                base.refreshUserToken(user, false)
            } catch (_: Exception) {
                throw BindException.Oauth2Exception.RefreshException()
            }
        }

        val result = bindDao.getBindUser(user.userID)!!

        if (!user.isAuthorized) {
            throw BindException.Oauth2Exception.RefreshException()
        }

        return result
    }

    override fun refreshUserToken(user: BindUser): String? {
        if (!user.isAuthorized) return base.botToken
        return base.refreshUserToken(user, false)
    }

    override fun refreshUserTokenFirst(user: BindUser) {
        base.refreshUserToken(user, true)
        val o = getOsuUser(user)
        val uid = o.userID
        user.userID = uid
        user.username = o.username
        user.mode = o.currentOsuMode
    }

    override fun getOsuUser(user: BindUser, mode: OsuMode): OsuUser {
        if (!user.isAuthorized) return getOsuUser(user.userID, mode)

        return request { client -> client
            .get().uri("me/{mode}", mode.shortName)
            .headers { headers ->
                base.insertHeader(headers, user)
            }.retrieve()
                .bodyToMono(OsuUser::class.java).map { data ->
                    userInfoDao.saveUserToday(data, mode)
                    user.userID = data.userID
                    user.username = data.username
                    user.mode = mode
                    data.currentOsuMode = getMode(mode, data.defaultOsuMode)

                    Thread.startVirtualThread {
                        bindDao.updateNameToID(data)
                    }

                    data
                }
        }
    }

    override fun getOsuUser(name: String, mode: OsuMode): OsuUser {
        return request { client ->
            client.get().uri {
                    it.path("users/{data}/{mode}").build("@$name", mode.shortName)
                }.headers(base::insertHeader).retrieve().bodyToMono(OsuUser::class.java).map { data ->
                    userInfoDao.saveUserToday(data, mode)
                    data.currentOsuMode = getMode(mode, data.defaultOsuMode)

                    Thread.startVirtualThread {
                        bindDao.updateNameToID(data)
                    }

                    data
                }
        }
    }

    override fun getOsuUser(id: Long, mode: OsuMode): OsuUser {
        return request { client ->
            client.get().uri {
                    it.path("users/{id}/{mode}").build(id, mode.shortName)
                }.headers(base::insertHeader).retrieve()/*
            .bodyToMono(JsonNode::class.java).map { JacksonUtil.parseObject(it, OsuUser::class.java) }.block()!!
            */.bodyToMono(OsuUser::class.java).map { data: OsuUser ->
                    userInfoDao.saveUserToday(data, mode)
                    data.currentOsuMode = getMode(mode, data.defaultOsuMode)

                    Thread.startVirtualThread {
                        bindDao.updateNameToID(data)
                    }

                    data
                }
        }
    }

    override fun getOsuUsers(ids: List<Long>, mode: OsuMode, batchSize: Int, latencyMillis: Long): List<OsuUser> {
        require(latencyMillis > 0) {
            "延迟必须大于 0"
        }

        val actions = ids.map {
            Callable {
                try {
                    it to getOsuUser(it)
                } catch (_: Exception) {
                    it to OsuUser(-1L)
                }
            }
        }

        val results = AsyncMethodExecutor.awaitBatchCallableExecute(actions)
            .filter { it.second.userID > 0 }
            .toMap()

        return ids.map { results[it] ?: OsuUser(it) }
    }

    override fun getOsuID(name: String): Long {
        val id = bindDao.getOsuID(name)
        if (id != null) {
            return id
        }

        val user = getOsuUser(name)

        Thread.startVirtualThread {
            bindDao.updateNameToID(user)
        }

        return user.userID
    }


    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量无限制
     * @param isVariant 是否获取玩家的多模式信息
     */
    override fun <T : Number> getUsers(users: Iterable<T>, isVariant: Boolean): List<MicroUser> {
        val idChunk = users.chunked(50)

        val callables = idChunk.map {
            Callable {
                getUsersPrivate(it, isVariant = isVariant)
            }
        }

        return AsyncMethodExecutor.awaitCallableExecute(callables).flatten()
    }

    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     * @param isVariant 是否获取玩家的多模式信息
     */
    private fun <T : Number> getUsersPrivate(users: Iterable<T>, isVariant: Boolean): List<MicroUser> {
        return request { client ->
            client.get().uri {
                    it.path("users").queryParam("ids[]", users.toList())
                        .queryParam("include_variant_statistics", isVariant).build()
                }.headers(base::insertHeader).retrieve().bodyToMono(JsonNode::class.java).map {
                    val userList = JacksonUtil.parseObjectList(
                        it["users"], MicroUser::class.java
                    )
                    userInfoDao.saveUsersToday(userList)
                    userList
                }
        }
    }

    override fun getFriendList(user: BindUser): List<LazerFriend> {
        if (!user.isAuthorized) throw UnsupportedOperationException.NotOauthBind()

        return request { client ->
            client.get().uri("friends")
                .headers { headers ->
                    base.insertHeader(headers, user)
                }
                .retrieve().bodyToFlux(LazerFriend::class.java)
                .collectList()
        }
    }

    override fun getUserRecentActivity(id: Long, offset: Int, limit: Int): List<ActivityEvent> {
        return request { client ->
            client.get().uri {
                    it.path("users/{userId}/recent_activity").queryParam("offset", offset).queryParam("limit", limit)
                        .build(id)
                }.headers(base::insertHeader).retrieve().bodyToFlux(ActivityEvent::class.java).collectList()
        }
    }

    override fun getUserKudosu(user: BindUser): KudosuHistory {
        return request { client ->
            client.get().uri("users/{uid}/kudosu").headers { headers ->
                base.insertHeader(headers, user)
            }.retrieve()
                .bodyToMono(KudosuHistory::class.java)
        }
    }

    override fun sendPrivateMessage(sender: BindUser, target: Long, message: String): JsonNode {
        val body: Map<String, Any> = mapOf("target_id" to target, "message" to message, "is_action" to false)
        return request { client ->
            client.post().uri("chat/new").headers { headers ->
                base.insertHeader(headers, sender)
            }.bodyValue(body).retrieve()
                .bodyToMono(JsonNode::class.java)
        }
    }

    override fun acknowledgmentPrivateMessageAlive(user: BindUser, since: Long?): JsonNode {
        return request { client ->
            client.post().uri {
                    it.path("chat/ack").queryParamIfPresent("since", Optional.ofNullable(since)).build()
                }.headers { headers ->
                base.insertHeader(headers, user)
            }.retrieve().bodyToMono(JsonNode::class.java)
        }
    }

    override fun getPrivateMessage(sender: BindUser, channel: Long, since: Long): JsonNode {
        return request { client ->
            client.get().uri("chat/channels/{channel}/messages?since={since}", channel, since)
                .headers { headers ->
                    base.insertHeader(headers, sender)
                }.retrieve().bodyToMono(JsonNode::class.java)
        }
    }

    override fun applyUserForBeatmapset(beatmapsets: List<Beatmapset>) {
        val userSet = (beatmapsets.flatMap { it.beatmaps ?: listOf() }
            .flatMap { it.mapperIDs } + beatmapsets.map { it.creatorID }).toSet()

        val users = getUsers(userSet).associateBy { it.userID }

        beatmapsets.forEach { set ->
            users[set.creatorID]?.let { set.creatorData = OsuUser(it) }

            set.beatmaps?.forEach { b ->
                users[set.creatorID]?.let { b.user = OsuUser(it) }
            }
        }
    }

    /**
     * 错误包装
     */
    private fun <T> request(request: (WebClient) -> Mono<T>): T {
        return try {
            base.request(request)
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<WebClientException>()

            when (ex) {
                is WebClientResponseException.BadRequest -> {
                    throw NetworkException.UserException.BadRequest()
                }

                is WebClientResponseException.Unauthorized -> {
                    throw NetworkException.UserException.Unauthorized()
                }

                is WebClientResponseException.Forbidden -> {
                    throw NetworkException.UserException.Forbidden()
                }

                is WebClientResponseException.NotFound -> {
                    throw NetworkException.UserException.NotFound()
                }

                is WebClientResponseException.TooManyRequests -> {
                    throw NetworkException.UserException.TooManyRequests()
                }

                is WebClientResponseException.BadGateway -> {
                    throw NetworkException.UserException.BadGateWay()
                }

                is WebClientResponseException.ServiceUnavailable -> {
                    throw NetworkException.UserException.ServiceUnavailable()
                }

                else -> if (e.findCauseOfType<Errors.NativeIoException>() != null) {
                    throw NetworkException.UserException.GatewayTimeout()
                } else if (e.findCauseOfType<ReadTimeoutException>() != null) {
                    throw NetworkException.UserException.RequestTimeout()
                } else {
                    throw NetworkException.UserException.Undefined(e)
                }
            }
        }
    }

    private val teamFormedPattern: Pattern = Pattern.compile(
        "<time\\s*class=\"js-tooltip-time\"\\s*data-tooltip-position=\"bottom center\"\\s+title=\"(\\S+)\""
    )
    private val teamUserPattern: Pattern = Pattern.compile("data-user=\"(?<json>.+)\"")
    private val teamModePattern: Pattern = Pattern.compile(
        "(?s)<div class=\"team-info-entry__title\">Default ruleset</div>\\s+<div class=\"team-info-entry__value\">\\s+<span class=\"fal fa-extra-mode-(\\w+)\">"
    )

    // 有点刻晴了
    private val teamNamePattern: Pattern = Pattern.compile(
        "<h1 class=\"profile-info__name\">\\s*<span class=\"u-ellipsis-overflow\">\\s*([\\S\\s]+)\\s*</span>\\s*</h1>"
    )
    private val teamAbbrPattern: Pattern = Pattern.compile(
        "(?s)<p class=\"profile-info__flag\">\\s*\\[(.+)]\\s*</p>"
    )
    private val teamApplicationPattern: Pattern = Pattern.compile(
        "(?s)application</div>\\s*<div class=\"team-info-entry__value\">\\s*(\\S+)\\s*\\((\\d+).+\\s*</div>"
    )

    private val rankPattern: Pattern = Pattern.compile(
        "(?s)<div class=\"team-info-entry__value team-info-entry__value--large\">\\s+#([\\d,]+)\\s+</div>"
    )

    private val ppPattern: Pattern = Pattern.compile(
        "(?s)<div class=\"team-info-entry__title\">\\s+Performance\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
    )
    private val rankedScorePattern: Pattern = Pattern.compile(
        "(?s)<div class=\"team-info-entry__title\">\\s+Ranked Score\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
    )
    private val playCountPattern: Pattern = Pattern.compile(
        "(?s)<div class=\"team-info-entry__title\">\\s+Play Count\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
    )
    private val membersPattern: Pattern = Pattern.compile(
        "(?s)<div class=\"team-info-entry__title\">\\s+Members\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
    )

    private val teamDescriptionPattern: Pattern = Pattern.compile("<div class='bbcode'>(.+)</div>")
    private val teamBannerPattern: Pattern = Pattern.compile("url\\('(https://assets.ppy.sh/teams/header/.+)'\\)")
    private val teamFlagPattern: Pattern = Pattern.compile("url\\('(https://assets.ppy.sh/teams/flag/.+)'\\)")

    override fun getTeamInfo(id: Int): TeamInfo {
        val html = base.request { client: WebClient ->
            client.get().uri("https://osu.ppy.sh/teams/{id}", id).retrieve().bodyToMono(String::class.java)
        }

        val bannerMatcher = teamBannerPattern.matcher(html)
        val banner: String = if (bannerMatcher.find()) {
            bannerMatcher.group(1)
        } else {
            ""
        }

        val nameMatcher = teamNamePattern.matcher(html)
        val name: String = if (nameMatcher.find()) {
            unescapeHTML(nameMatcher.group(1).trim())
        } else {
            ""
        }

        val abbrMatcher = teamAbbrPattern.matcher(html)
        val abbr: String = if (abbrMatcher.find()) {
            unescapeHTML(abbrMatcher.group(1).trim())
        } else {
            ""
        }

        val flagMatcher = teamFlagPattern.matcher(html)
        val flag: String = if (flagMatcher.find()) {
            flagMatcher.group(1)
        } else {
            ""
        }

        val formedMatcher = teamFormedPattern.matcher(html)
        val formed: String = if (formedMatcher.find()) {
            formedMatcher.group(1)
        } else {
            ""
        }

        val modeMatcher = teamModePattern.matcher(html)
        val mode: OsuMode = if (modeMatcher.find()) {
            getMode(modeMatcher.group(1))
        } else {
            OsuMode.DEFAULT
        }

        val application: String
        val available: Int

        val applicationMatcher = teamApplicationPattern.matcher(html)
        if (applicationMatcher.find()) {
            application = applicationMatcher.group(1)
            available = applicationMatcher.group(2)?.toIntOrNull() ?: 0
        } else {
            application = ""
            available = 0
        }

        val rankMatcher = rankPattern.matcher(html)
        val rank: Int = if (rankMatcher.find()) {
            rankMatcher.group(1).replace(",", "").toIntOrNull() ?: 0
        } else {
            0
        }

        val ppMatcher = ppPattern.matcher(html)
        val pp: Int = if (ppMatcher.find()) {
            ppMatcher.group(1).replace(",", "").toIntOrNull() ?: 0
        } else {
            0
        }

        val rankedScoreMatcher = rankedScorePattern.matcher(html)
        val rankedScore: Long = if (rankedScoreMatcher.find()) {
            rankedScoreMatcher.group(1).replace(",", "").toLongOrNull() ?: 0L
        } else {
            0L
        }

        val playCountMatcher = playCountPattern.matcher(html)
        val playCount: Long = if (playCountMatcher.find()) {
            playCountMatcher.group(1).replace(",", "").toLongOrNull() ?: 0L
        } else {
            0L
        }

        val membersMatcher = membersPattern.matcher(html)
        val members: Int = if (membersMatcher.find()) {
            membersMatcher.group(1).replace(",", "").toIntOrNull() ?: 0
        } else {
            0
        }

        val description: String
        val descriptionMatcher = teamDescriptionPattern.matcher(html)
        description = if (descriptionMatcher.find()) {
            descriptionMatcher.group(1)
        } else {
            ""
        }

        val userMatcher = teamUserPattern.matcher(html)
        val users = ArrayList<OsuUser>()

        while (userMatcher.find()) {
            val json = unescapeHTML(userMatcher.group("json"))
            users.add(JacksonUtil.parseObject(json, OsuUser::class.java))
        }

        return TeamInfo(
            id, name, abbr, formed, banner, flag, users, mode, application, available,

            rank, pp, rankedScore, playCount, members,

            description
        )
    }

    @OptIn(ExperimentalStdlibApi::class) override fun asyncDownloadAvatar(users: List<MicroUser>) {
        val path = Path.of(IMG_BUFFER_PATH)
        if (!Files.isDirectory(path) || !Files.isWritable(path)) return

        val actions = users.map { user ->
            return@map AsyncMethodExecutor.Runnable {
                val url = user.avatarUrl

                if (url.isNullOrBlank()) {
                    log.info("异步下载头像：头像不完整")
                    return@Runnable
                }

                val md = MessageDigest.getInstance("MD5")

                try {
                    md.update(url.toByteArray(Charsets.UTF_8))
                } catch (_: Exception) {
                    log.info("异步下载谱面图片：计算 MD5 失败")
                    return@Runnable
                }

                val hex = md.digest().toHexString()

                if (Files.isRegularFile(path.resolve(hex))) {
                    return@Runnable
                } else {
                    val replacePath = url.replace("https://a.ppy.sh/", "")

                    val image = try {
                        base.osuApiWebClient.get().uri {
                                it.scheme("https").host("a.ppy.sh").replacePath(replacePath).build()
                            }.headers(base::insertHeader).retrieve().bodyToMono(ByteArray::class.java).block()!!
                    } catch (e: Exception) {
                        log.error("异步下载头像：任务失败\n", e)
                        return@Runnable
                    }

                    try {
                        Files.write(path.resolve(hex), image)
                    } catch (e: IOException) {
                        log.error("异步下载头像：保存失败\n{}", e.message)
                        return@Runnable
                    }
                }
            }
        }

        AsyncMethodExecutor.asyncRunnableExecute(actions)
    }

    @OptIn(ExperimentalStdlibApi::class) override fun asyncDownloadBackground(users: List<MicroUser>) {
        val path = Path.of(IMG_BUFFER_PATH)
        if (Files.isDirectory(path).not() || Files.isWritable(path).not()) return

        val actions = users.map { user ->
            return@map AsyncMethodExecutor.Runnable {
                val url = user.cover?.url ?: user.coverUrl

                if (url.isNullOrBlank()) {
                    log.info("异步下载背景：背景不完整")
                    return@Runnable
                }

                val md = MessageDigest.getInstance("MD5")

                try {
                    md.update(url.toByteArray(Charsets.UTF_8))
                } catch (_: Exception) {
                    log.info("异步下载背景图片：计算 MD5 失败")
                    return@Runnable
                }

                val hex = md.digest().toHexString()

                if (Files.isRegularFile(path.resolve(hex))) {
                    return@Runnable
                } else {
                    val replacePath = url.replace("https://assets.ppy.sh/", "")

                    val image = try {
                        base.osuApiWebClient.get().uri {
                                it.scheme("https").host("assets.ppy.sh").replacePath(replacePath).build()
                            }.headers(base::insertHeader).retrieve().bodyToMono(ByteArray::class.java).block()!!
                    } catch (e: Exception) {
                        log.error("异步下载背景：任务失败\n", e)
                        return@Runnable
                    }

                    try {
                        Files.write(path.resolve(hex), image)
                    } catch (e: IOException) {
                        log.error("异步下载背景：保存失败\n{}", e.message)
                        return@Runnable
                    }
                }
            }
        }

        AsyncMethodExecutor.asyncRunnableExecute(actions)
    }

    // 反转义字符
    private fun unescapeHTML(str: String): String {
        return str.replace("&amp;".toRegex(), "&").replace("&lt;".toRegex(), "<").replace("&gt;".toRegex(), ">")
            .replace("&quot;".toRegex(), "\"").replace("&apos;".toRegex(), "'").replace("&nbsp;".toRegex(), " ")

            .replace("&#038;".toRegex(), "&").replace("&#034;".toRegex(), "\"").replace("&#039;".toRegex(), "'")
            .replace("&#160;".toRegex(), " ")
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserApiImpl::class.java)

        private val IMG_BUFFER_PATH: String = if (System.getenv("BUFFER_PATH").isNullOrBlank().not()) {
            System.getenv("BUFFER_PATH")
        } else {
            System.getProperty("java.io.tmpdir") + "/n-bot/buffer"
        }
    }
}
