package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.multiplayer.Match
import com.now.nowbot.model.multiplayer.MatchAdapter
import com.now.nowbot.model.multiplayer.MatchListener
import com.now.nowbot.model.multiplayer.MatchRating
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
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException

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
        data: MessageService.DataValue<ListenerParam>,
    ): Boolean {
        val matcher = Instruction.MATCH_LISTENER.matcher(messageText)
        if (!matcher.find()) return false

        val operate = getStatus(matcher.group("operate"))

        val id = matcher.group("matchid").toLongOrNull()
        val isSuper = Permission.isSuperAdmin(event.sender.id)

        if (operate == Operation.STOP) {
            if (id == null) {
                if (isSuper) {
                    stopAllListener(true)
                    event.reply(MatchListenerException(MatchListenerException.Type.ML_Listen_StopAll))
                    return false
                } else {
                    stopGroupListener(event.subject.id, false)
                    event.reply(MatchListenerException(MatchListenerException.Type.ML_Listen_StopGroup))
                    return false
                }
            } else {
                data.value = ListenerParam(id, Operation.STOP)
                return true
            }
        } else if (id != null) {
            data.value = ListenerParam(id, operate)
            return true
        } else if (matcher.group("matchid").isNullOrBlank().not()) {
            throw MatchListenerException(MatchListenerException.Type.ML_MatchID_Null)
        } else {
            throw MatchListenerException(MatchListenerException.Type.ML_Instructions)
        }
    }

    override fun HandleMessage(event: MessageEvent, param: ListenerParam) {
        val match: Match

        if (event !is GroupMessageEvent) {
            throw TipsException(MatchListenerException.Type.ML_Send_NotGroup.message)
        }

        when (param.operate) {
            Operation.INFO -> {
                val list = listenerData.filter { it.groupID == event.group.id }.map { it.listener.matchID }
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
                        matchApiService.getMonitoredMatchInfo(param.id)
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
                    String.format(MatchListenerException.Type.ML_Listen_Start.message, param.id)
                )
            }

            Operation.STOP -> {
                cancelListener(event.group.id, param.id, Permission.isSuperAdmin(event.sender.id))
            }

            else -> {}
        }

        registerListener(
            event.group.id,
            event.sender.id,
            MatchListenerImplement(
                beatmapApiService,
                matchApiService,
                calculateApiService,
                imageService,
                event,
                param.id
            ),
            matchApiService,
            this,
        )
    }

    private fun initBeatmapAndUser(event: Match.MatchEvent, listener: MatchListener) {
        val game = event.round ?: return
        with(game) {
            if (beatMap != null) {
                beatMap = beatmapApiService.getBeatMap(beatMapID)
                calculateApiService.applyStarToBeatMap(beatMap!!, mode, LazerMod.getModsList(mods))
            } else {
                val b = BeatMap()
                b.beatMapID = beatMapID

                beatMap = b
            }
        }

        val nonUserID = game
            .scores
            .map { it.userID }
            .filterNot { listener.userMap.containsKey(it) }
        if (nonUserID.isEmpty()) return
        val users = userApiService.getUsers(nonUserID)
        users.forEach {
            listener.userMap[it.id] = it
        }
    }

    class MatchListenerImplement(
        private val beatmapApiService: OsuBeatmapApiService,
        private val matchApiService: OsuMatchApiService,
        private val calculateApiService: OsuCalculateApiService,
        val imageService: ImageService,
        val messageEvent: MessageEvent,
        val matchID: Long,
    ) : MatchAdapter {
        var round = 0
        override lateinit var match: Match

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
            return lock.get() != null
        }

        override fun onStart() {
            for (i in 0..5) {
                val firstEvent = match.events.first()
                if (firstEvent.type == Match.EventType.MatchCreated) return
                match += matchApiService.getMatchInfoBefore(matchID, firstEvent.eventID)
            }
        }

        override fun onGameAbort(beatmapID: Long) {
            messageEvent.reply(MatchListenerException.Type.ML_Listen_Aborted.message)
        }

        override fun onGameStart(event: MatchAdapter.GameStartEvent) =
            with(event) {
                if (!isTeamVS && !hasNext()) {
                    cancelListener(messageEvent.subject.id, matchID, false)
                }

                val mr = MatchRating(
                    match,
                    beatmapApiService,
                    calculateApiService
                )
                mr.calculate()

                // 需要拓展
                if (beatmap.CS == null) beatmap = beatmapApiService.getBeatMap(beatmapID)

                calculateApiService.applyBeatMapChanges(beatmap, LazerMod.getModsList(event.mods.map { it.acronym }))

                val objectGroup = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
                val e7 =
                    PanelE7Param(
                        mr,
                        mode,
                        mods.map { it.acronym },
                        users,
                        beatmap,
                        objectGroup,
                        getOriginal(beatmap),
                    )

                val image =
                    try {
                        imageService.getPanel(e7, "E7")
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
                val mr = MatchRating(match, beatmapApiService, calculateApiService)

                // 其实这个就是 game
                val round = mr.rounds.last { it.roundID == game.roundID }

                calculateApiService.applyBeatMapChanges(round.beatMap, LazerMod.getModsList(round.mods))

                // 手动调位置
                if (round.scores.size > 2) {
                    round.scores = round.scores.sortedByDescending { it.score }
                } else {
                    round.scores = round.scores.sortedBy { it.playerStat.slot }
                }

                val index = mr.rounds.map { it.roundID }.indexOf(round.roundID)

                val image =
                    try {
                        val body = mapOf(
                            "match" to mr,
                            "round" to round,
                            "index" to index,
                            "panel" to "RR"
                        )

                        imageService.getPanel(body, "F3")
                    } catch (e: Exception) {
                        log.error(e) { "对局信息图片渲染失败：" }
                        throw MatchRoundException(MatchRoundException.Type.MR_Fetch_Error)
                    }
                messageEvent.reply(image)
                return@with
            }

        override fun onMatchEnd(type: MatchListener.StopType) {
            if (type == MatchListener.StopType.SERVER_REBOOT || type == MatchListener.StopType.USER_STOP) return

            cancelListener(messageEvent.subject.id, matchID, false)

            val message =
                String.format(
                    MatchListenerException.Type.ML_Listen_Stop.message,
                    matchID,
                    type.tips,
                )
            messageEvent.reply(message)
        }

        override fun onError(e: Throwable) {
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

    data class ListenerData(val groupID: Long, val userID: Long, val listener: MatchListenerImplement) {
        override fun hashCode(): Int {
            return ((groupID shl 11) + userID).hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ListenerData

            if (groupID != other.groupID) return false
            if (userID != other.userID) return false

            return true
        }
    }

    companion object {
        val log = KotlinLogging.logger {}
        const val BREAK_ROUND: Int = 16
        private const val USER_MAX = 3
        private const val GROUP_MAX = 3

        /**
         * matchID : listener
         */
        private val listeners = mutableMapOf<Long, MatchListener>()

        // group user listener
        val listenerData = mutableSetOf<ListenerData>()

        fun registerListener(
            group: Long,
            user: Long,
            listener: MatchListenerImplement,
            matchApiService: OsuMatchApiService,
            self: MatchListenerService,
        ) {
            if (countByGroupID(group) >= GROUP_MAX) {
                throw TipsRuntimeException(
                    MatchListenerException.Type.ML_Listen_MaxInstanceGroup.message
                )
            }
            if (countByUserID(user) >= USER_MAX) {
                throw TipsRuntimeException(
                    MatchListenerException.Type.ML_Listen_MaxInstance.message
                )
            }

            val key = ListenerData(group, user, listener)
            listenerData.add(key)
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

        fun cancelListener(groupID: Long, matchID: Long, isSuper: Boolean) {
            val filteredSet = listenerData.filter {
                it.listener.matchID == matchID && (isSuper || it.groupID == groupID)
            }

            if (filteredSet.isEmpty()) {
                return
            }

            listenerData.removeIf { it.groupID == groupID }

            val stopType =
                if (isSuper) {
                    MatchListener.StopType.SUPER_STOP
                } else {
                    MatchListener.StopType.USER_STOP
                }

            filteredSet.forEach {
                val l = it.listener

                l.onMatchEnd(stopType)
                val removable = listeners[l.matchID]?.removeListener(l) == true
                if (removable) {
                    listeners.remove(l.matchID)
                }
            }
        }

        fun stopAllListener(isSuper: Boolean = false) {
            val type = if (isSuper) MatchListener.StopType.SUPER_STOP else MatchListener.StopType.USER_STOP

            listenerData.clear()
            listeners.forEach { (_, u) -> u.stop(type) }
        }

        fun stopGroupListener(groupID: Long, isSuper: Boolean = false) {
            val matches = listenerData.filter { it.groupID == groupID }.map { it.listener.matchID }
            val type = if (isSuper) MatchListener.StopType.SUPER_STOP else MatchListener.StopType.USER_STOP

            listenerData.removeIf { it.groupID == groupID }
            listeners.filter { matches.contains(it.key) }.forEach { it.value.stop(type) }
        }

        @JvmStatic
        fun stopAllListenerFromReboot() {
            listenerData.clear()
            listeners.forEach { it.value.stop(MatchListener.StopType.SERVER_REBOOT) }
        }

        private fun countByGroupID(groupID: Long): Int {
            return listenerData.count { it.groupID == groupID }
        }

        private fun countByUserID(userID: Long): Int {
            return listenerData.count { it.userID == userID }
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
