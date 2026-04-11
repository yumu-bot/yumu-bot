package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchAdapter
import com.now.nowbot.model.match.MatchListener
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.MatchMapService.PanelE7Param
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.throwable.botRuntimeException.*
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.BeatmapUtil
import com.now.nowbot.util.Instruction
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@Service("MATCH_LISTENER")
class MatchListenerService(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<MatchListenerService.ListenerParam>
{

    // 状态管理移至实例或单例管理器中 (此处暂留 Service 内但改用并发容器)
    companion object {
        private val log = LoggerFactory.getLogger(MatchListenerService::class.java)
        const val BREAK_ROUND = 20
        private const val USER_MAX = 3
        private const val GROUP_MAX = 3

        private val listeners = ConcurrentHashMap<Long, MatchListener>()
        private val listenerData = CopyOnWriteArraySet<ListenerData>()

        suspend fun stopAllListenerFromReboot() {
            val items = listeners.values

            log.info("开始停止 ${items.size} 个监听器...")

            coroutineScope {
                items.forEachIndexed { index, listener ->
                    launch {
                        try {
                            if (items.size < 20) {
                                val delayTime = (index / 5) * 500L
                                if (delayTime > 0) {
                                    delay(delayTime.milliseconds)
                                }
                            }

                            listener.stop(MatchListener.StopType.SERVER_REBOOT)
                        } catch (e: Exception) {
                            log.error("停止监听 ${listener.match.statistics.matchID} 时出错", e)
                        }
                    }
                }
            }

            listeners.clear()
            listenerData.clear()

            log.info("所有监听器已清理完毕")
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ListenerParam>,
    ): Boolean {
        val matcher = Instruction.MATCH_LISTENER.matcher(messageText)
        if (!matcher.find()) return false

        val operate = parseOperation(matcher.group("operate"))
        val idStr = matcher.group("matchid") ?: ""
        val id = idStr.toLongOrNull() ?: 0L
        val skip = matcher.group("skip")?.toIntOrNull() ?: 0
        val isSuper = Permission.isSuperAdmin(event.sender.contactID)

        return when {
            operate == Operation.STOP && id == 0L -> {
                handleGlobalStop(event, isSuper)
                false
            }

            id != 0L -> {
                data.value = ListenerParam(id, operate, skip)
                true
            }

            idStr.isNotBlank() -> throw IllegalArgumentException.WrongException.MatchID()

            else -> throw MatchException.NormalOperate.Instructions()
        }
    }

    private fun handleGlobalStop(event: MessageEvent, isSuper: Boolean) {
        if (isSuper) {
            val stopped = stopAllListenerFromSuper()
            event.reply(MatchException.NormalOperate.StopAll(stopped))
        } else {
            val stopped = stopByGroup(event.subject.contactID)
            event.reply(MatchException.NormalOperate.StopGroup(stopped))
        }
    }

    override fun handleMessage(event: MessageEvent, param: ListenerParam): ServiceCallStatistic? {
        if (event !is GroupMessageEvent) throw UnsupportedOperationException.NotGroup()

        when (param.operate) {
            Operation.INFO -> {
                val groupMatchIDs =
                    listenerData.filter { it.groupID == event.group.contactID }.map { it.listener.matchID }

                val msg = if (groupMatchIDs.isEmpty()) {
                    MatchException.NoListener()
                } else {
                    MatchException.ListListener(groupMatchIDs)
                }

                event.reply(msg)
                return null
            }

            Operation.START -> {
                val match = fetchMatch(param.id)

                if (match.isMatchEnd) {
                    throw MatchException.MatchAlreadyEnd(match.statistics.matchID)
                }

                register(event, param)
                event.reply(MatchException.NormalOperate.Start(match))
            }

            Operation.STOP -> {
                cancelListener(event.group.contactID, param.id, Permission.isSuperAdmin(event.sender.contactID))
            }

            else -> {}
        }

        return ServiceCallStatistic.building(event) { setParam(mapOf("mids" to listOf(param.id))) }
    }

    // --- 逻辑提取 ---

    private fun fetchMatch(id: Long) = runCatching {
        matchApiService.getMatch(id)
    }.getOrElse {
        throw NoSuchElementException.Match()
    }

    private fun register(event: GroupMessageEvent, param: ListenerParam) {
        val groupID = event.group.contactID
        val userID = event.sender.contactID

        if (listenerData.count { it.groupID == groupID } >= GROUP_MAX)
            throw MatchException.MaxListenerInGroup()
        if (listenerData.count { it.userID == userID } >= USER_MAX)
            throw MatchException.MaxListenerByUser()

        if (listenerData.any { it.groupID == groupID && it.listener.matchID == param.id }) {
            throw MatchException.MatchAlreadyListen(param.id)
        }

        val implement = MatchAdapterImpl(
            beatmapApiService, calculateApiService, imageService, event, param.id, param.skip
        )

        listenerData.add(ListenerData(groupID, userID, implement))

        listeners.computeIfAbsent(param.id) {
            val match = matchApiService.getMatch(it)
            MatchListener(match, matchApiService, implement).apply {
                beforeGame = this@MatchListenerService::initBeatmapAndUser
                start()
            }
        }.addListener(implement)
    }

    private fun initBeatmapAndUser(event: Match.MatchEvent, listener: MatchListener) {
        val game = event.round ?: return

        // 完善谱面信息
        game.beatmap = game.beatmap?.let {
            beatmapApiService.getBeatmap(it.beatmapID)
        } ?: Beatmap().apply { this.beatmapID = game.beatmapID }

        game.beatmap?.let {
            calculateApiService.applyStarToBeatmap(it, game.mode, LazerMod.getModsList(game.mods))
        }

        // 批量获取用户信息
        val missingUserIds = game.scores.map { it.userID }.filterNot { listener.userMap.containsKey(it) }
        if (missingUserIds.isNotEmpty()) {
            userApiService.getUsers(missingUserIds).forEach { listener.userMap[it.userID] = it }
        }
    }

    // --- 内部类: 核心处理器 ---

    inner class MatchAdapterImpl(
        private val beatmapApiService: OsuBeatmapApiService,
        private val calculateApiService: OsuCalculateApiService,
        private val imageService: ImageService,
        private val messageEvent: MessageEvent,
        val matchID: Long,
        private val skipCount: Int,
    ) : MatchAdapter {
        private var roundCounter = 0
        override lateinit var match: Match

        private fun createRating(isSkipping: Boolean): MatchRating {
            val actualSkip = min(skipCount, max(match.events.count { it.round != null } - 1, 0))
            return MatchRating(
                match, MatchRating.RatingParam(skip = actualSkip),
                beatmapApiService, calculateApiService, isSkipping
            ).apply { calculate() }
        }

        override fun onStart() {}

        override fun onGameStart(event: MatchAdapter.GameStartEvent) {
            if (!event.isTeamVS && !checkContinue()) {
                cancelListener(messageEvent.subject.contactID, matchID, false)
                return
            }

            val isSkipping = skipCount >= match.events.count { it.round != null }
            val mr = createRating(isSkipping)

            // 谱面增强处理
            var bm = event.beatmap

            if (bm.cs == null) {
                bm = beatmapApiService.getBeatmap(bm.beatmapID)
            }

            BeatmapUtil.applyBeatmapChanges(bm, event.mods)

            val param = PanelE7Param(
                mr, event.mode, event.mods, event.users, bm,
                beatmapApiService.getBeatmapObjectGrouping26(bm), BeatmapUtil.getDetailMap(bm)
            )

            val fallback = MatchException.NormalOperate.Start(match).message

            renderAndReply(param, "E7", fallback)
        }

        override fun onGameEnd(event: MatchAdapter.GameEndEvent) {
            val isSkipping = skipCount >= match.events.count { it.round != null }
            val mr = createRating(isSkipping)

            val round = mr.rounds.find { it.roundID == event.game.roundID }
                ?: mr.rounds.lastOrNull()
                ?: throw NoSuchElementException.MatchRound()

            BeatmapUtil.applyBeatmapChanges(round.beatmap, LazerMod.getModsList(round.mods))

            // 排序逻辑
            round.scores = if (round.scores.size > 2) {
                round.scores.sortedByDescending { it.score }

            } else {
                round.scores.sortedBy { it.playerStat?.slot }
            }

            val body = mapOf("match" to mr, "round" to round, "index" to mr.rounds.indexOf(round), "panel" to "RR")
            renderAndReply(body, "F3")
        }

        private fun renderAndReply(param: Any, panelType: String, fallback: String? = null) {
            try {
                val image = imageService.getPanel(param, panelType)
                messageEvent.reply(image)
            } catch (e: Exception) {
                log.error("比赛监听：$panelType 图片渲染失败。", e)

                fallback?.let { messageEvent.reply(it) }
                    ?: throw IllegalStateException.Render("比赛监听")
            }
        }

        private fun checkContinue(): Boolean {
            if (roundCounter <= BREAK_ROUND) {
                roundCounter++
                return true
            }

            messageEvent.reply(MatchException.NormalOperate.Continue(matchID, roundCounter))

            val lock = ASyncMessageUtil.getLock(messageEvent.subject.contactID, null, 60000) {
                it?.rawMessage?.contains("OK", ignoreCase = true) == true
            }

            return lock.get() != null
        }

        override fun onMatchEnd(type: MatchListener.StopType) {
            // 1. 消息过滤逻辑：只有特定类型才发消息
            val shouldNotify = when (type) {
                MatchListener.StopType.SERVER_REBOOT,
                MatchListener.StopType.USER_STOP -> false

                else -> true
            }

            if (shouldNotify) {
                messageEvent.reply(MatchException.NormalOperate.Stop(matchID, type))
            }

        }

        override fun onGameAbort(beatmapID: Long) {
            messageEvent.reply(MatchException.MatchAborted(matchID, beatmapID))
        }

        override fun onError(e: Throwable) {
            log.warn("比赛监听：发生错误: $matchID", e)
            when (e) {
                is HttpClientErrorException -> return
                is TipsRuntimeException -> messageEvent.reply(e.message ?: "未知错误")
                else -> messageEvent.reply("监听期间出现错误, id: $matchID")
            }
        }
    }

    // --- 工具方法 ---

    private fun parseOperation(str: String?): Operation = when (str?.trim()?.lowercase()) {
        "stop", "p", "end", "e", "off", "f" -> Operation.STOP
        "start", "s", "on", "o", null, "" -> Operation.START
        "list", "l", "info", "i" -> Operation.INFO
        else -> throw MatchException.NormalOperate.Instructions()
    }

    private fun stopByGroup(groupID: Long): List<Long> {
        val targets = listenerData.filter { it.groupID == groupID }

        val removedMatchIDs = targets.mapNotNull { data ->
            val mid = data.listener.matchID
            var removed: Long? = null

            listeners[mid]?.let { matchListener ->
                if (matchListener.removeListener(data.listener)) {
                    listeners.remove(mid)
                    removed = mid
                }
            }

            removed
        }.distinct()

        // 3. 清理全局群组索引
        listenerData.removeIf { it.groupID == groupID }

        return removedMatchIDs
    }

    private fun stopAllListenerFromSuper(): List<Long> {
        val ids = listeners.keys().toList()

        listenerData.clear()
        listeners.forEach { (_, u) -> u.stop(MatchListener.StopType.SUPER_STOP) }
        listeners.clear()

        return ids
    }

    fun cancelListener(groupID: Long, matchID: Long, isSuper: Boolean) {
        val stopType = if (isSuper) {
            MatchListener.StopType.SUPER_STOP
        } else {
            MatchListener.StopType.USER_STOP
        }

        listenerData.filter { it.listener.matchID == matchID && (isSuper || it.groupID == groupID) }
            .forEach { data ->
                data.listener.onMatchEnd(stopType)
                if (listeners[matchID]?.removeListener(data.listener) == true) {
                    listeners.remove(matchID)
                }
            }

        listenerData.removeIf { it.groupID == groupID && it.listener.matchID == matchID }
    }

    enum class Operation { INFO, START, STOP, END }
    data class ListenerParam(val id: Long, val operate: Operation = Operation.END, val skip: Int = 0)
    data class ListenerData(val groupID: Long, val userID: Long, val listener: MatchAdapterImpl)
}