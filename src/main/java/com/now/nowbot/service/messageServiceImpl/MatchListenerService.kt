package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.model.multiplayer.MatchAdapter
import com.now.nowbot.model.multiplayer.MatchCalculate
import com.now.nowbot.model.multiplayer.MatchListener
import com.now.nowbot.model.multiplayer.MonitoredMatch
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.MatchMapService.PanelE7Param
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.throwable.serviceException.MatchListenerException
import com.now.nowbot.throwable.serviceException.MatchRoundException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.DataUtil.getOriginal
import com.now.nowbot.util.Instruction
import com.yumu.core.extensions.isNotNull
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.internal.toImmutableMap
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.stream.Collectors

@Service("MATCH_LISTENER")
class MatchListenerService(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<MatchListenerService.ListenerParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ListenerParam?>,
    ): Boolean {
        val matcher = Instruction.MATCH_LISTENER.matcher(messageText)
        if (!matcher.find()) return false

        val operate = getStatus(matcher.group("operate"))

        try {
            if (StringUtils.hasText(matcher.group("matchid"))) {
                val id = matcher.group("matchid").toLong()

                data.value = ListenerParam(id, operate)
                return true
            } else if (operate == Operation.STOP && Permission.isSuperAdmin(event.sender.id)) {
                stopAllListenerFromSuperAdmin()
                event.reply(MatchListenerException.Type.ML_Listen_StopAll.message)
                return false
            } else {
                throw MatchListenerException(MatchListenerException.Type.ML_Instructions)
            }
        } catch (e: Exception) {
            log.error { "比赛监听：无法确定对局编号！" }
            throw MatchListenerException(MatchListenerException.Type.ML_MatchID_Null)
        }
    }

    override fun HandleMessage(event: MessageEvent, data: ListenerParam) {
        val match: MonitoredMatch

        if (event !is GroupMessageEvent) {
            throw TipsException(MatchListenerException.Type.ML_Send_NotGroup.message)
        }

        when (data.operate) {
            Operation.INFO -> {
                val list = senderSet.filter { it.first == event.group.id }.map { it.third.matchID }
                val message =
                    if (list.isEmpty()) {
                        MatchListenerException.Type.ML_Info_NoListener.message
                    } else {
                        val allID = list.joinToString { "\n" }
                        String.format(MatchListenerException.Type.ML_Info_List.message, allID)
                    }
                event.reply(message)
                return
            }

            Operation.START -> {
                match =
                    try {
                        matchApiService.getMonitoredMatchInfo(data.id)
                    } catch (e: Exception) {
                        throw MatchListenerException(
                            MatchListenerException.Type.ML_MatchID_NotFound
                        )
                    }

                // 结束了直接提示
                if (match.isMatchEnd) {
                    throw MatchListenerException(MatchListenerException.Type.ML_Match_End)
                }

                event.reply(
                    String.format(MatchListenerException.Type.ML_Listen_Start.message, data.id)
                )
            }

            Operation.STOP -> {
                consoleListener(event.group.id, Permission.isSuperAdmin(event.sender.id), data.id)
            }

            else -> {}
        }

        regentListener(
            event.group.id,
            event.sender.id,
            MatchListenerImplement(beatmapApiService, imageService, event, data.id),
            matchApiService,
            this,
        )
    }

    private fun initBeatmapAndUser(event: MonitoredMatch.MatchEvent, listener: MatchListener) {
        val game = event.game ?: return
        with(game) {
            if (beatmap != null) {
                beatmap = beatmapApiService.getBeatMap(beatmapID)
                calculateApiService.applyStarToBeatMap(beatmap!!, mode, LazerMod.getModsList(mods))
            } else {
                val b = BeatMap()
                b.beatMapID = beatmapID

                beatmap = b
            }
        }

        val nonUserID = listener.usersIDSet.filter { !listener.userMap.containsKey(it) }
        if (nonUserID.isEmpty()) return
        val users = userApiService.getUsers(nonUserID)
        users.forEach {
            listener.userMap[it.id] = it
        }
    }

    class MatchListenerImplement(
        val beatmapApiService: OsuBeatmapApiService,
        val imageService: ImageService,
        val messageEvent: MessageEvent,
        val matchID: Long,
    ) : MatchAdapter {
        var round = 0
        override lateinit var match: MonitoredMatch

        /** 判断是否继续 */
        fun hasNext(): Boolean {
            if (round <= BREAK_ROUND) return true
            round++
            val message =
                """
                比赛 ($matchID) 已经监听 $round 轮, 如果要继续监听, 请在 60 秒内回复
                "$matchID" (不要带引号)
                """
                    .trimIndent()
            messageEvent.reply(message)
            val lock =
                ASyncMessageUtil.getLock(messageEvent.subject.id, null, 60 * 1000) {
                    it.rawMessage.equals(matchID.toString())
                }
            return lock.get().isNotNull()
        }

        override fun onStart() {}

        override fun onGameAbort(beatmapID: Long) {
            messageEvent.reply(MatchListenerException.Type.ML_Listen_Aborted.message)
        }

        override fun onGameStart(event: MatchAdapter.GameStartEvent) =
            with(event) {
                if (!isTeamVS && !hasNext()) {
                    consoleListener(messageEvent.subject.id, false, matchID)
                }

                val calculate = MatchCalculate(match.toMatch(), beatmapApiService)

                val objectGroup = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
                val e7 =
                    PanelE7Param(
                        calculate,
                        mode,
                        mods.map { it.acronym },
                        users,
                        beatmap,
                        objectGroup,
                        getOriginal(beatmap),
                    )

                val image =
                    try {
                        imageService.getPanelE7(e7)
                    } catch (e: WebClientResponseException) {
                        log.error(e) { "获取图片失败" }
                        throw TipsRuntimeException(
                            String.format(
                                MatchListenerException.Type.ML_Match_Start.message,
                                matchID,
                                beatmap.beatMapID,
                            )
                        )
                    }
                messageEvent.reply(image)
                return@with
            }

        override fun onGameEnd(event: MatchAdapter.GameEndEvent) =
            with(event) {
                game.scores = game.scores.filter { s -> s.score >= 1000 }

                val userMap =
                    match.users
                        .stream()
                        .distinct()
                        .collect(Collectors.toMap(MicroUser::getUserID) { it })
                        .toImmutableMap()

                for (s in game.scores) {
                    val u = userMap[s.userID]

                    if (u != null && u.id != 0L) {
                        s.user = u
                    }
                }

                val index = 1
                val image =
                    try {
                        val stat = match.statistics
                        imageService.getPanelF2(stat, game, index)
                    } catch (e: java.lang.Exception) {
                        log.error(e) { "对局信息图片渲染失败：" }
                        throw MatchRoundException(MatchRoundException.Type.MR_Fetch_Error)
                    }
                messageEvent.reply(image)
                return@with
            }

        override fun onMatchEnd(type: MatchListener.StopType) {
            if (
                type == MatchListener.StopType.SERVER_REBOOT ||
                type == MatchListener.StopType.USER_STOP
            )
                return
            consoleListener(messageEvent.subject.id, false, matchID)
            val message =
                String.format(
                    MatchListenerException.Type.ML_Listen_Stop.message,
                    matchID,
                    type.tips,
                )
            messageEvent.reply(message)
        }

        override fun onError(e: Exception) {
            if (e is WebClientResponseException) {
                // 网络错误, 忽略
                log.error(e) { "比赛监听：网络错误" }
                return
            }
            log.error(e) { "比赛监听：出现错误" }
            messageEvent.reply("监听期间出现错误, id: $matchID")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MatchListenerImplement) return false

            if (matchID != other.matchID) return false
            if (messageEvent.subject.id != other.messageEvent.subject.id) return false

            return true
        }

        override fun hashCode(): Int {
            var result = messageEvent.subject.id.hashCode()
            result = 31 * result + matchID.hashCode()
            return result
        }
    }

    enum class Operation {
        INFO,
        START,
        STOP,
        END,
    }

    data class ListenerParam(val id: Long, val operate: Operation = Operation.END)

    companion object {
        val log = KotlinLogging.logger {}
        const val BREAK_ROUND: Int = 16
        const val USER_MAX = 3
        const val GROUP_MAX = 3

        val listeners = mutableMapOf<Long, MatchListener>()

        // group user listener
        val senderSet = mutableSetOf<Triple<Long, Long, MatchListenerImplement>>()

        fun regentListener(
            group: Long,
            user: Long,
            listener: MatchListenerImplement,
            matchApiService: OsuMatchApiService,
            self: MatchListenerService,
        ) {
            if (countByGroup(group) >= GROUP_MAX) {
                throw TipsRuntimeException(
                    MatchListenerException.Type.ML_Listen_MaxInstanceGroup.message
                )
            }
            if (countByUser(user) >= USER_MAX) {
                throw TipsRuntimeException(
                    MatchListenerException.Type.ML_Listen_MaxInstance.message
                )
            }

            val key = Triple(group, user, listener)
            senderSet.add(key)
            val l =
                listeners.computeIfAbsent(listener.matchID) {
                    val match = matchApiService.getMonitoredMatchInfo(it)
                    val monitoredMatchListener =
                        MatchListener(match, matchApiService, listener)
                    monitoredMatchListener.beforeGame = self::initBeatmapAndUser
                    monitoredMatchListener.start()
                    monitoredMatchListener
                }
            l.addListener(listener)
        }

        fun consoleListener(group: Long, isSuper: Boolean, matchID: Long) {
            val triples =
                senderSet.filter {
                    it.third.matchID == matchID && (isSuper || it.first == group)
                }
            if (triples.isEmpty()) {
                return
            }

            val stopType =
                if (isSuper) {
                    MatchListener.StopType.SUPER_STOP
                } else {
                    MatchListener.StopType.USER_STOP
                }

            triples.forEach { (_, _, l) ->
                l.onMatchEnd(stopType)
                val removable = listeners[l.matchID]?.removeListener(l) == true
                if (removable) {
                    listeners.remove(l.matchID)
                }
            }
        }

        fun stopAllListenerFromSuperAdmin() {
            listeners.forEach { (_, u) -> u.stop(MatchListener.StopType.SUPER_STOP) }
        }

        // TODO 按群移除监听实例
        fun stopGroupListenerFromSuperAdmin() {
            listeners.forEach { (_, u) -> u.stop(MatchListener.StopType.SUPER_STOP) }
        }

        fun stopAllListenerFromReboot() {
            listeners.forEach { (_, u) -> u.stop(MatchListener.StopType.SERVER_REBOOT) }
        }

        private fun countByGroup(group: Long): Int {
            return senderSet.count { it.first == group }
        }

        private fun countByUser(user: Long): Int {
            return senderSet.count { it.second == user }
        }

        private fun getStatus(str: String?): Operation {
            return when (str?.trim()) {
                "stop",
                "p",
                "end",
                "e",
                "off",
                "f" -> Operation.STOP

                "start",
                "s",
                "on",
                "o" -> Operation.START

                "list",
                "l",
                "info",
                "i" -> Operation.INFO

                null,
                "" -> Operation.START

                else -> throw MatchListenerException(MatchListenerException.Type.ML_Instructions)
            }
        }
    }
}
