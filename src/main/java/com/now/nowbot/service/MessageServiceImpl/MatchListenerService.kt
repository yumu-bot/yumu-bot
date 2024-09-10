package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.multiplayer.MatchAdapter
import com.now.nowbot.model.multiplayer.MatchCalculate
import com.now.nowbot.model.multiplayer.NewMatch
import com.now.nowbot.model.multiplayer.NewMatchListener
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageServiceImpl.MatchMapService.PanelE7Param
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuMatchApiService
import com.now.nowbot.throwable.ServiceException.MatchListenerException
import com.now.nowbot.throwable.ServiceException.MatchRoundException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.DataUtil.getOriginal
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.yumu.core.extensions.isNotNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException


class MatchListenerService(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<MatchListenerService.ListenerParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ListenerParam?>
    ): Boolean {
        val matcher = Instruction.MATCH_LISTENER.matcher(messageText)
        if (!matcher.find()) return false

        val operate = when (matcher.group("operate")) {
            "stop", "p", "end", "e", "off", "f" -> Status.STOP
            "start", "s", "on", "o" -> Status.START
            "list", "l", "info", "i" -> Status.INFO
            null -> Status.START
            else -> throw MatchListenerException(MatchListenerException.Type.ML_Instructions)
        }

        val id = when {
            StringUtils.hasText(matcher.group("matchid")) -> matcher.group("matchid").toLong()
            operate != Status.INFO -> try {
                val md = getMarkdownFile("Help/listen.md")
                val image = imageService.getPanelA6(md, "help")
                QQMsgUtil.sendImage(event, image)
                return false
            } catch (e: Exception) {
                throw MatchListenerException(MatchListenerException.Type.ML_Instructions)
            }

            else -> 0
        }

        data.value = ListenerParam(id, operate)

        return true
    }

    override fun HandleMessage(event: MessageEvent, data: ListenerParam) {
        val from = event.subject
        val match: NewMatch

        if (event !is GroupMessageEvent) {
            throw TipsException(MatchListenerException.Type.ML_Send_NotGroup.message)
        }

        when (data.operate) {
            Status.INFO -> {
                var list = senderSet
                    .filter { it.first == event.group.id }
                    .map { it.third.matchID }
                val message = if (list.isEmpty()) {
                    MatchListenerException.Type.ML_Info_NoListener.message
                } else {
                    val allID = list.joinToString { "\n" }
                    String.format(MatchListenerException.Type.ML_Info_List.message, allID)
                }
                from.sendMessage(message)
                return
            }

            Status.START -> {
                match = try {
                    matchApiService.getNewMatchInfo(data.id)
                } catch (e: Exception) {
                    throw MatchListenerException(MatchListenerException.Type.ML_MatchID_NotFound)
                }

                // 结束了直接提示
                if (match.isMatchEnd) {
                    throw MatchListenerException(MatchListenerException.Type.ML_Match_End)
                }

                from.sendMessage(String.format(MatchListenerException.Type.ML_Listen_Start.message, data.id))
            }

            Status.STOP -> {
                consoleListener(
                    event.group.id,
                    Permission.isSuperAdmin(event.sender.id),
                    data.id
                )
            }

            else -> {}
        }

        regectListener(
            event.group.id,
            event.sender.id,
            MatchListenerImplement(
                beatmapApiService,
                imageService,
                from,
                data.id
            ),
            matchApiService,
            beatmapApiService,
        )
    }

    class MatchListenerImplement(
        val beatmapApiService: OsuBeatmapApiService,
        val imageService: ImageService,
        val contact: Contact,
        val matchID: Long,
    ) : MatchAdapter {
        var round = 0

        /**
         * 判断是否继续
         */
        fun hasNext(): Boolean {
            if (round <= BREAK_ROUND) return true
            round++
            val message = """
                比赛($matchID)已经监听${round}轮, 如果要继续监听, 请60秒内任意一人回复
                "$matchID" (不要带引号)
                """.trimIndent()
            contact.sendMessage(message)
            val lock = ASyncMessageUtil.getLock(contact.id, null, 60 * 1000) {
                it.rawMessage.equals(matchID.toString())
            }
            return lock.get().isNotNull()
        }

        override fun onStart() {}

        override fun onGameStart(event: MatchAdapter.GameStartEvent) = with(event) {
            if (!isTeamVS && !hasNext()) {
                consoleListener(contact.id, false, matchID)
            }
            val calculate = MatchCalculate()
            val objectGroup = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val e7 = PanelE7Param(
                calculate,
                mode,
                mods.map { it.abbreviation },
                users,
                beatmap,
                objectGroup,
                getOriginal(beatmap),
            )

            val image = try {
                imageService.getPanelE7(e7)
            } catch (e: WebClientResponseException) {
                log.error(e) { "获取图片失败" }
                throw TipsRuntimeException(
                    String.format(
                        MatchListenerException.Type.ML_Match_Start.message,
                        matchID, beatmap.id
                    )
                )
            }
            contact.sendImage(image)
            return@with
        }

        override fun onGameEnd(event: MatchAdapter.GameEndEvent) = with(event) {
            game.scores = game.scores.filter { s-> s.score >= 1000 }
            val index = 1
            val image = try {
                val stat = NewMatch.MatchStat(matchID, startTime, null, name)
                imageService.getPanelF2(stat, game, index)
            } catch (e: java.lang.Exception) {
                MatchListenerServiceOld.log.error("对局信息图片渲染失败：", e)
                throw MatchRoundException(MatchRoundException.Type.MR_Fetch_Error)
            }
            contact.sendImage(image)
            return@with
        }

        override fun onMatchEnd(type: NewMatchListener.StopType) {
            if (type == NewMatchListener.StopType.SERVICE_STOP || type == NewMatchListener.StopType.USER_STOP) return
            consoleListener(contact.id, false, matchID)
            val message = String.format(
                MatchListenerException.Type.ML_Listen_Stop.message,
                matchID,
                type.tips
            )
            contact.sendMessage(message)
        }

        override fun onError(e: Exception) {
            log.error(e) { "比赛监听异常" }
            contact.sendMessage("监听期间出现错误, id: $matchID")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MatchListenerImplement) return false

            if (matchID != other.matchID) return false
            if (contact.id != other.contact.id) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contact.id.hashCode()
            result = 31 * result + matchID.hashCode()
            return result
        }

    }

    enum class Status { INFO, START, STOP, END }
    data class ListenerParam(val id: Long, val operate: Status = Status.END)
    companion object {
        val log = KotlinLogging.logger { }
        const val BREAK_ROUND: Int = 8
        const val USER_MAX = 3
        const val GROUP_MAX = 3

        val listeners = mutableMapOf<Long, NewMatchListener>()

        // group user listener
        val senderSet = mutableSetOf<Triple<Long, Long, MatchListenerImplement>>()

        fun regectListener(
            group: Long,
            user: Long,
            listener: MatchListenerImplement,
            matchApiService: OsuMatchApiService,
            beatmapApiService: OsuBeatmapApiService,
        ) {
            if (countByGroup(group) >= GROUP_MAX) {
                throw TipsRuntimeException(MatchListenerException.Type.ML_Listen_MaxInstanceGroup.message)
            }
            if (countByUser(user) >= USER_MAX) {
                throw TipsRuntimeException(MatchListenerException.Type.ML_Listen_MaxInstance.message)
            }

            val key = Triple(group, user, listener)
            senderSet.add(key)
            val l = listeners.computeIfAbsent(listener.matchID) {
                val match = matchApiService.getNewMatchInfo(it)
                NewMatchListener(match, beatmapApiService, matchApiService, listener)
            }
            l.addListener(listener)
        }

        fun consoleListener(
            group: Long,
            isSuper: Boolean,
            matchID: Long,
        ) {
            val triples = senderSet.filter {
                it.third.matchID == matchID && (isSuper || it.first == group)
            }
            if (triples.isEmpty()) {
                return
            }

            val stopType = if (isSuper) NewMatchListener.StopType.SUPER_STOP else NewMatchListener.StopType.USER_STOP

            triples.forEach { (_, _, l) ->
                l.onMatchEnd(stopType)
                val removable = listeners[l.matchID]?.removeListener(l) == true
                if (removable) {
                    listeners.remove(l.matchID)
                }
            }
        }

        fun stopAllListener() {
            listeners.forEach { (_, u) ->
                u.stop(NewMatchListener.StopType.SERVICE_STOP)
            }
        }

        private fun countByGroup(group: Long): Int {
            return senderSet.count { it.first == group }
        }

        private fun countByUser(user: Long): Int {
            return senderSet.count { it.second == user }
        }
    }
}