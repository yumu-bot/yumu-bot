package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.multiplayer.MatchAdapter
import com.now.nowbot.model.multiplayer.NewMatch
import com.now.nowbot.model.multiplayer.NewMatchListener
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuMatchApiService
import com.now.nowbot.throwable.ServiceException.MatchListenerException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.yumu.core.extensions.isNotNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.util.StringUtils
import java.util.concurrent.atomic.AtomicReference


class MatchListenerService1(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<MatchListenerService1.ListenerParam> {
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
        val status = AtomicReference(Status.START)

        if (event !is GroupMessageEvent) {
            throw TipsException(MatchListenerException.Type.ML_Send_NotGroup.message)
        }
        val senderID = event.sender.id
        when (data.operate) {
            Status.INFO -> {
                var list = mutableListOf<Long>()
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

                return
            }

            else -> {}
        }
    }

    class MatchListenerImplament(
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

        override fun onGameStart(event: MatchAdapter.GameStartEvent) {

        }

        override fun onGameEnd(event: MatchAdapter.GameEndEvent) {

        }

        override fun onMatchEnd(type: NewMatchListener.StopType) {
            String.format(
                MatchListenerException.Type.ML_Listen_Stop.message,
                matchID,
                type.tips
            )
        }

        override fun onError(e: Exception) {
            log.error(e) { "比赛监听异常" }
            contact.sendMessage("监听期间出现错误, id: $matchID")
        }
    }

    enum class Status { INFO, START, WAITING, RESULT, STOP, END }
    data class ListenerParam(val id: Long, val operate: Status = Status.END)
    companion object {
        val log = KotlinLogging.logger { }
        const val BREAK_ROUND: Int = 8

        val listeners = mutableMapOf<Int, NewMatchListener>()
    }
}