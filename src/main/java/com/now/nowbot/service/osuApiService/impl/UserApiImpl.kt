package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.getMode
import com.now.nowbot.model.json.*
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService.TeamInfo
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.util.JacksonUtil
import org.codehaus.plexus.util.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.util.*
import java.util.regex.Pattern

@Service class UserApiImpl(
    private val base: OsuApiBaseService, private val bindDao: BindDao, private val userInfoDao: OsuUserInfoDao
) : OsuUserApiService {
    // 用来确认玩家是否存在于服务器，而无需使用 API 请求。
    override fun isPlayerExist(name: String): Boolean {
        val response =
            base.osuApiWebClient.get().uri("https://osu.ppy.sh/users/{name}", name)
                .headers {
                base.insertHeader(it!!)
            }.retrieve().bodyToMono(String::class.java).onErrorReturn("").block()

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

    override fun refreshUserToken(user: BindUser): String? {
        if (!user.isAuthorized) return base.botToken
        return base.refreshUserToken(user, false)
    }

    override fun refreshUserTokenFirst(user: BindUser) {
        base.refreshUserToken(user, true)
        val osuInfo = getOsuUser(user)
        val uid = osuInfo.userID
        user.osuID = uid
        user.osuName = user.osuName
        user.osuMode = user.osuMode
    }

    override fun getOsuUser(user: BindUser, mode: OsuMode): OsuUser {
        if (!user.isAuthorized) return getOsuUser(user.osuID, mode)

        return base.osuApiWebClient.get().uri("me/{mode}", mode.shortName).headers(base.insertHeader(user)).retrieve()
            .bodyToMono(OsuUser::class.java).map { data ->
                userInfoDao.saveUser(data, mode)
                user.osuID = data.userID
                user.osuName = data.username
                user.osuMode = mode
                data.currentOsuMode = getMode(mode, data.defaultOsuMode)
                data
            }.block()!!
    }

    override fun getOsuUser(name: String, mode: OsuMode): OsuUser {
        return base.osuApiWebClient.get().uri {
            it.path("users/{data}/{mode}").build("@$name", mode.shortName)
        }.headers { base.insertHeader(it) }.retrieve().bodyToMono(OsuUser::class.java).map { data ->
            userInfoDao.saveUser(data, mode)
            data.currentOsuMode = getMode(mode, data.defaultOsuMode)
            data
        }.block()!!
    }

    override fun getOsuUser(id: Long, mode: OsuMode): OsuUser {
        return base.osuApiWebClient.get().uri {
            it.path("users/{id}/{mode}").build(id, mode.shortName)
        }.headers {
            base.insertHeader(it)
        }.retrieve()
            /*
            .bodyToMono(JsonNode::class.java).map { JacksonUtil.parseObject(it, OsuUser::class.java) }.block()!!
            */
        .bodyToMono(OsuUser::class.java)
        .map { data: OsuUser ->
            userInfoDao.saveUser(data, mode)
                data.currentOsuMode = getMode(mode, data.defaultOsuMode)
                data
        }.block()!!
    }

    override fun getOsuID(name: String): Long {
        val id = bindDao.getOsuID(name)
        if (id != null) {
            return id
        }
        val user = getOsuUser(name)
        bindDao.removeOsuNameToId(user.userID)

        if (user.previousNames.isNullOrEmpty().not()) {
            val names = arrayOf(user.username.uppercase()) + user.previousNames!!.map { it.uppercase() }.toTypedArray()
            bindDao.saveOsuNameToId(user.userID, *names)
        }

        return user.userID
    }

    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     * @param isVariant 是否获取玩家的多模式信息
     */
    override fun <T : Number> getUsers(users: Iterable<T>, isVariant: Boolean): List<MicroUser> {
        return base.osuApiWebClient.get()
            .uri { it.path("users")
                .queryParam("ids[]", users)
                .queryParam("include_variant_statistics", isVariant)
                .build()
            }.headers { base.insertHeader(it)
            }.retrieve().bodyToMono(JsonNode::class.java)
            .map {
                val userList = JacksonUtil.parseObjectList(
                    it["users"], MicroUser::class.java
                )
                userInfoDao.saveUsers(userList)
                userList
            }.block()!!
    }

    override fun getFriendList(user: BindUser): List<LazerFriend> {
        if (!user.isAuthorized) throw TipsRuntimeException("无权限")

        return base.osuApiWebClient.get()
            .uri("friends")
            .headers(base.insertHeader(user))
            .retrieve()
            .bodyToFlux(LazerFriend::class.java)
            .collectList()
            .block()!!
    }

    override fun getUserRecentActivity(id: Long, offset: Int, limit: Int): List<ActivityEvent> {
        return base.osuApiWebClient.get()
            .uri { it.path("users/{userId}/recent_activity")
                .queryParam("offset", offset).queryParam("limit", limit)
                .build(id)
            }.headers { base.insertHeader(it)
            }.retrieve()
            .bodyToFlux(ActivityEvent::class.java)
            .collectList()
            .block()!!
    }

    override fun getUserKudosu(user: BindUser): KudosuHistory {
        return base.osuApiWebClient.get()
            .uri("users/{uid}/kudosu")
            .headers(base.insertHeader(user))
            .retrieve()
            .bodyToMono(KudosuHistory::class.java)
            .block()!!
    }

    override fun sendPrivateMessage(sender: BindUser, target: Long, message: String): JsonNode {
        val body: Map<String, Any> =
            mapOf("target_id" to target, "message" to message, "is_action" to false)
        return base.osuApiWebClient.post()
            .uri("chat/new")
            .headers(base.insertHeader(sender))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block()!!
    }

    override fun acknowledgmentPrivateMessageAlive(user: BindUser, since: Long?): JsonNode {
        return base.osuApiWebClient.post()
            .uri {
                it.path("chat/ack").queryParamIfPresent("since", Optional.ofNullable(since)).build()
            }
            .headers(base.insertHeader(user))
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block()!!
    }

    override fun getPrivateMessage(sender: BindUser, channel: Long, since: Long): JsonNode {
        return base.osuApiWebClient.get()
            .uri("chat/channels/{channel}/messages?since={since}", channel, since)
            .headers(base.insertHeader(sender))
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block()!!
    }

    private val teamFormedPattern: Pattern = Pattern.compile(
        "Formed</div>\\s+<div class=\"team-info-entry__value\">\\s+(.+)\\s+</div>"
    )
    private val teamUserPattern: Pattern = Pattern.compile("data-user=\"(?<json>.+)\"")
    private val teamModePattern: Pattern =
        Pattern.compile("<div class=\"team-info-entry__title\">Default ruleset</div>\\s+<div class=\"team-info-entry__value\">\\s+<span class=\"fal fa-extra-mode-(\\w+)\">")

    // 有点刻晴了
    // "<a\s+class="game-mode-link"\s+href="https://osu.ppy.sh/teams/\d+/(.+)"\s+>"
    // "<div class=\"team-info-entry__title\">Default ruleset</div>\\s+<div class=\"team-info-entry__value\">\\s+<span class=\"fal fa-extra-mode-mania\">[^<]+</span>\\s+(.+)\\s+</div>"
    private val teamNamePattern: Pattern = Pattern.compile(
        "<h1 class=\"profile-info__name\">\\s*<span class=\"u-ellipsis-overflow\">\\s*([\\S\\s]+)\\s*</span>\\s*</h1>"
    )
    private val teamAbbrPattern: Pattern = Pattern.compile(
        "<p class=\"profile-info__flag\">\\s+\\[([\\S\\s]+)]\\s+</p>"
    )
    private val teamApplicationPattern: Pattern = Pattern.compile(
        "application</div>\\s+<div class=\"team-info-entry__value\">\\s+(.+)\\s+</div>"
    )

    private val rankPattern: Pattern = Pattern.compile(
        "<div class=\"team-info-entry__value team-info-entry__value--large\">\\s+#([\\d,]+)\\s+</div>"
    )

    private val ppPattern: Pattern = Pattern.compile(
        "<div class=\"team-info-entry__title\">\\s+Performance\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
    )
    private val rankedScorePattern: Pattern = Pattern.compile(
        "<div class=\"team-info-entry__title\">\\s+Ranked Score\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
    )
    private val playCountPattern: Pattern = Pattern.compile(
        "<div class=\"team-info-entry__title\">\\s+Play Count\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
    )
    private val membersPattern: Pattern = Pattern.compile(
        "<div class=\"team-info-entry__title\">\\s+Members\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
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
        val applicationMatcher = teamApplicationPattern.matcher(html)
        application = if (applicationMatcher.find()) {
            applicationMatcher.group(1)
        } else {
            ""
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
            id, name, abbr, formed, banner, flag, users, mode, application,

            rank, pp, rankedScore, playCount, members,

            description
        )
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
    }
}
