package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.MicroUser
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
import com.now.nowbot.util.CmdUtil.getMode
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

    override fun HandleMessage(event: MessageEvent, param: GuestParam) {
        val body = param.getBody()

        val image = imageService.getPanel(body, "A11")

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("客串谱师：发送失败", e)
            throw IllegalStateException.Send("客串谱师")
        }
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
        val mode = getMode(matcher)

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself)

        val user: OsuUser
        val relatedSets: Set<Beatmapset>
        val page: Int

        if (id.data != null) {
            val query = mapOf(
                "q" to "creator=" + id.data!!, "sort" to "ranked_desc", "s" to "any", "page" to 1
            )

            // 这个是补充可能存在的，谱面所有难度都标注了难度作者时，上一个查询会漏掉的谱面
            val query2 = mapOf(
                "q" to id.data!!, "sort" to "ranked_desc", "s" to "any", "page" to 1
            )

            val async = AsyncMethodExecutor.awaitTripleCallableExecute(
                { beatmapApiService.searchBeatmapset(query, 10) },
                { beatmapApiService.searchBeatmapset(query2, 10) },
                { userApiService.getOsuUser(id.data!!, mode.data!!) },
            )

            relatedSets = (async.first.beatmapsets.toHashSet() +
                    async.second.beatmapsets.filter {
                        it.beatmapsetID != id.data!! &&
                                (it.beatmaps?.all { that -> that.beatmapID != id.data!! } ?: true)
                    }.toHashSet())

            user = async.third
            page = id.start ?: 1
        } else {
            val range = CmdUtil.getUserWithRange(event, matcher, mode, isMyself)

            user = range.data!!
            val query = mapOf(
                "q" to "creator=" + user.userID, "sort" to "ranked_desc", "s" to "any", "page" to 1
            )

            // 这个是补充可能存在的，谱面所有难度都标注了难度作者时，上一个查询会漏掉的谱面
            val query2 = mapOf(
                "q" to user.userID, "sort" to "ranked_desc", "s" to "any", "page" to 1
            )

            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { beatmapApiService.searchBeatmapset(query, 10) },
                { beatmapApiService.searchBeatmapset(query2, 10) },
            )

            relatedSets = (async.first.beatmapsets.toHashSet() +
                    async.second.beatmapsets.filter {
                        it.beatmapsetID != user.userID &&
                                (it.beatmaps?.all { that -> that.beatmapID != user.id } ?: true)
                    }.toHashSet())

            page = range.start ?: 1
        }

        return GuestParam(user, relatedSets, page)
    }

    private fun GuestParam.getBody(): Map<String, Any> {
        val user = this.user
        val relatedSets = this.relatedSets.asSequence()

        val relatedUsers = run {
            val idChunk = relatedSets.filter { it.creatorID != user.userID }.map { it.creatorID }.toSet().chunked(50)

            val actions = idChunk.map {
                return@map AsyncMethodExecutor.Supplier<List<MicroUser>> {
                    userApiService.getUsers(it)
                }
            }

            AsyncMethodExecutor.awaitSupplierExecute(actions).flatten()
        }

        AsyncMethodExecutor.asyncRunnableExecute {
            userApiService.asyncDownloadAvatar(relatedUsers)
        }

        val relatedDiffs = relatedSets.map { it.beatmaps!! }.flatten()

        val myGuestDiffs = relatedDiffs.filter { it.mapperID == user.userID && it.beatmapset?.creatorID != user.userID }
        val guestDiffs = relatedDiffs.filter { it.mapperID != user.userID && it.beatmapset?.creatorID == user.userID }

        val guestDifficultyOwners = relatedUsers.map { u ->
            val re = guestDiffs.filter { it.mapperID == u.userID }

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
        }

        if (guestDifficultyOwners.isEmpty()) {
            throw NoSuchElementException.GuestDiff()
        }

        // 分页
        val split = DataUtil.splitPage(guestDifficultyOwners, this.page)
        val list = split.first

        userApiService.asyncDownloadAvatar(list.map { it.user })
        userApiService.asyncDownloadBackground(list.map { it.user })

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