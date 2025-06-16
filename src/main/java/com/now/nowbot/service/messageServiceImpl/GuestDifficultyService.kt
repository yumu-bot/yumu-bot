package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.GuestDifficultyService.GuestDifferParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service("GUEST_DIFFICULTY")
class GuestDifficultyService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService
): MessageService<GuestDifferParam>, TencentMessageService<GuestDifferParam> {

    data class GuestDifficultyOwner(
        @get:JsonProperty("user")
        val user: MicroUser,

        @get:JsonProperty("received")
        val received: Int,

        @get:JsonProperty("received_ranked")
        val receivedRanked: Int,

        @get:JsonProperty("sent")
        val sent: Int,

        @get:JsonProperty("sent_ranked")
        val sentRanked: Int,
    )

    data class GuestDifferParam(val user: OsuUser, val page: Int)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<GuestDifferParam>
    ): Boolean {
        val matcher = Instruction.GUEST_DIFFICULTY.matcher(messageText)
        if (!matcher.find()) return false

        val isMyself = AtomicBoolean(true)
        val cmd = CmdUtil.getUserWithRange(event, matcher, CmdObject(OsuMode.DEFAULT), isMyself)

        data.value = GuestDifferParam(cmd.data!!, cmd.start ?: 1)

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: GuestDifferParam) {
        val body = param.getBody()

        val image = imageService.getPanel(body, "A11")

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("客串谱师：发送失败", e)
            throw IllegalStateException.Send("客串谱师")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): GuestDifferParam? {
        val matcher = OfficialInstruction.GUEST_DIFFICULTY.matcher(messageText)
        if (!matcher.find()) return null

        val isMyself = AtomicBoolean(true)
        val cmd = CmdUtil.getUserWithRange(event, matcher, CmdObject(OsuMode.DEFAULT), isMyself)

        return GuestDifferParam(cmd.data!!, cmd.start ?: 1)
    }

    override fun reply(event: MessageEvent, param: GuestDifferParam): MessageChain? {
        return QQMsgUtil.getImage(imageService.getPanel(param.getBody(), "A11"))
    }

    private fun GuestDifferParam.getBody(): Map<String, Any> {
        val user = this.user

        val query = mapOf(
            "q" to "creator=" + user.userID,
            "sort" to "ranked_desc",
            "s" to "any",
            "page" to 1
        )

        val search = beatmapApiService.searchBeatmapset(query, 10)
        val result1 = search.beatmapSets

        // 这个是补充可能存在的，谱面所有难度都标注了难度作者时，上一个查询会漏掉的谱面
        val query2 = mapOf(
            "q" to user.userID,
            "sort" to "ranked_desc",
            "s" to "any",
            "page" to 1
        )

        val search2 = beatmapApiService.searchBeatmapset(query2, 10)
        val result2 = search2.beatmapSets
            .filter { it.beatmapsetID != user.userID && (it.beatmaps?.all { that -> that.beatmapID != user.id } ?: true) }

        val relatedSets = (result1.toHashSet() + result2.toHashSet()).asSequence()

        val relatedUsers = run {
            val idChunk = relatedSets.filter { it.creatorID != user.userID }.map { it.creatorID }.toSet().chunked(50)

            val actions = idChunk.map {
                return@map AsyncMethodExecutor.Supplier<List<MicroUser>> {
                    userApiService.getUsers(it, false)
                }
            }

            AsyncMethodExecutor.awaitSupplierExecute(actions).flatten()
        }

        val relatedDiffs = relatedSets.map { it.beatmaps!! }.flatten()

        val myGuestDiffs = relatedDiffs.filter { it.mapperID == user.userID && it.beatmapset?.creatorID != user.userID }
        val guestDiffs = relatedDiffs.filter { it.mapperID != user.userID && it.beatmapset?.creatorID == user.userID }

        val guestDifficultyOwners = relatedUsers.map { u ->
            val re = guestDiffs.filter { it.mapperID == u.userID }

            val received: Int = re.count()
            val receivedRanked: Int = re.count { it.ranked > 0 }

            val se = myGuestDiffs.filter { it.beatmapset?.creatorID == u.userID }

            val sent: Int = se.count()

            val sentRanked: Int = se.count { it.ranked > 0 }

            GuestDifficultyOwner(u, received, receivedRanked, sent, sentRanked)
        }.sortedByDescending {
            it.sent + it.received
        }.sortedByDescending {
            it.sentRanked + it.receivedRanked
        }

        if (guestDifficultyOwners.isEmpty()) throw NoSuchElementException.GuestDiff()

        // 分页
        val page = this.page
        val maxPage = ceil(guestDifficultyOwners.size * 1.0 / MAX_PER_PAGE).roundToInt()

        val start = max(min(page, maxPage) * MAX_PER_PAGE - MAX_PER_PAGE, 0)
        val end = min(min(page, maxPage) * MAX_PER_PAGE, guestDifficultyOwners.size)

        val list = guestDifficultyOwners.subList(start, end)

        userApiService.asyncDownloadAvatar(list.map { it.user })
        userApiService.asyncDownloadBackground(list.map { it.user })

        return mapOf(
            "user" to user,
            "guest_differs" to list,
            "page" to page,
            "max_page" to maxPage
        )

    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GuestDifficultyService::class.java)

        const val MAX_PER_PAGE = 50
    }
}