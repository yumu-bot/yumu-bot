package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.NanoUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.GuestDifficultyService.GuestParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("GUEST_DIFFICULTY")
class GuestDifficultyService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService
): MessageService<GuestParam>, TencentMessageService<GuestParam> {

    data class GuestDifficultyOwner(
        @get:JsonProperty("user")
        val user: NanoUser,

        @get:JsonProperty("received")
        val received: Int,

        @get:JsonProperty("received_ranked")
        val receivedRanked: Int,

        @get:JsonProperty("sent")
        val sent: Int,

        @get:JsonProperty("sent_ranked")
        val sentRanked: Int,
    )

    data class GuestParam(val user: OsuUser, val relatedSets: Set<Beatmapset>, val page: Int)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<GuestParam>
    ): Boolean {
        val matcher = Instruction.GUEST_DIFFICULTY.matcher(messageText)
        if (!matcher.find()) return false

        data.value = getParam(event, matcher)

        return true
    }

    override fun handleMessage(event: MessageEvent, param: GuestParam): ServiceCallStatistic? {
        val body = param.getBody()

        val image = imageService.getPanel(body, "A11")

        try {
            event.replyAsync(image)
        } catch (e: Exception) {
            log.error("客串谱师：发送失败", e)
            throw IllegalStateException.Send("客串谱师")
        }

        return ServiceCallStatistic.build(event, userID = param.user.userID)
    }

    override fun accept(event: MessageEvent, messageText: String): GuestParam? {
        val matcher = OfficialInstruction.GUEST_DIFFICULTY.matcher(messageText)
        if (!matcher.find()) return null

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: GuestParam): MessageChain? {
        return MessageChain(imageService.getPanel(param.getBody(), "A11"))
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): GuestParam {

        // 其实这个功能下的 mode 和 isMyself 不重要
        val isMyself = AtomicBoolean(true)
        val mode = InstructionUtil.getMode(matcher)

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself)

        val user: OsuUser
        val relatedSets: Set<Beatmapset>
        val page: Int

        if (id.data != null) {
            val query = mapOf(
                "q" to "creator=" + id.data!!, "sort" to "ranked_desc", "s" to "any", "page" to 1
            )

            val async = AsyncMethodExecutor.awaitPair(
                { beatmapApiService.searchBeatmapsetParallel(query) },
                { userApiService.getOsuUser(id.data!!, mode.data!!) },
            )

            // 注意，从 search 返回的 beatmapset 包含的 beatmap 会缺谱师信息
            val sets = async.first.beatmapsets

            beatmapApiService.applyBeatmapsetExtend(sets)

            relatedSets = sets.toHashSet()

            user = async.second
            page = id.start ?: 1
        } else {
            val range = InstructionUtil.getUserWithRange(event, matcher, mode, isMyself)

            user = range.data!!
            val query = mapOf(
                "q" to "creator=" + user.userID, "sort" to "ranked_desc", "s" to "any", "page" to 1
            )

            // 注意，从 search 返回的 beatmapset 包含的 beatmap 会缺谱师信息
            val sets = beatmapApiService.searchBeatmapsetParallel(query).beatmapsets

            beatmapApiService.applyBeatmapsetExtend(sets)

            relatedSets = sets.toHashSet()

            page = range.start ?: 1
        }

        return GuestParam(user, relatedSets, page)
    }

    private fun GuestParam.getBody(): Map<String, Any> {
        val user = this.user
        val relatedSets = this.relatedSets.asSequence()

        val relatedUsers = relatedSets
            .filter { it.creatorID != user.userID }
            .flatMap { it.beatmaps.orEmpty() }
            .flatMap { it.mappers }
            .distinctBy { it.userID }
            .filter { it.userID != user.userID }

        val (mySets, otherSets) = relatedSets.partition { it.creatorID == user.userID }

        val myOwnedDiffs = mySets.flatMap { it.beatmaps.orEmpty() }
        val otherOwnedDiffs = otherSets.flatMap { it.beatmaps.orEmpty() }

        val guestDiffs = myOwnedDiffs.filter { diff ->
            diff.mapperIDs.any { it != user.userID }
        }
        val myGuestDiffs = otherOwnedDiffs.filter { user.userID in it.mapperIDs }

        val guestDifficultyOwners = relatedUsers.map { u ->
            val re = guestDiffs.filter { it.mapperIDs.contains(u.userID) }

            val received: Int = re.count()
            val receivedRanked: Int = re.count { it.hasLeaderBoard }

            val se = myGuestDiffs.filter { it.beatmapset?.creatorID == u.userID }

            val sent: Int = se.count()

            val sentRanked: Int = se.count { it.hasLeaderBoard }

            GuestDifficultyOwner(u, received, receivedRanked, sent, sentRanked)
        }.sortedByDescending {
            it.sent + it.received
        }.sortedByDescending {
            it.sentRanked + it.receivedRanked
        }.toList()

        if (guestDifficultyOwners.isEmpty()) {
            throw NoSuchElementException.GuestDiff()
        }

        // 分页
        val split = DataUtil.splitPage(guestDifficultyOwners, this.page)
        val list = split.first

        // userApiService.asyncDownloadAvatar(list.map { it.user })
        // userApiService.asyncDownloadBackground(list.map { it.user })

        return mapOf(
            "user" to user,
            "guest_differs" to list,
            "page" to split.second,
            "max_page" to split.third
        )

    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GuestDifficultyService::class.java)
    }
}