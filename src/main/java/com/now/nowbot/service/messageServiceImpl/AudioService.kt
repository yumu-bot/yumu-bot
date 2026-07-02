package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.IDType
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.AudioService.AudioParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_ID
import com.now.nowbot.util.command.FLAG_TYPE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.absoluteValue

@Service("AUDIO")
class AudioService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val dao: ServiceCallStatisticsDao,
) : MessageService<AudioParam> {

    data class AudioParam(var id: Long = 0, var type: IDType = IDType.BeatmapID)

    @Throws(Exception::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<AudioParam>,
    ): Boolean {
        val matcher = Instruction.AUDIO.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val (inputType, inputID) = IDType.parse(matcher.group(FLAG_TYPE), matcher.group(FLAG_ID))

        val id: Long
        val type: IDType

        if (inputID == null) {
            val last = dao.getLastBeatmapID(event)
            id = last ?: throw IllegalArgumentException.WrongException.Audio()
            type = IDType.BeatmapID
        } else if (inputID > 0) {
            id = inputID
            type = inputType
        } else {
            id = inputID.absoluteValue
            type = inputType
        }

        data.value = AudioParam(id, type)
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: AudioParam): ServiceCallStatistic? {
        var type: IDType = param.type

        val voice = when (param.type) {
            IDType.BeatmapID -> getVoiceFromBID(param.id) ?: run {
                type = IDType.BeatmapsetID
                getVoiceFromSID(param.id)
            }

            IDType.BeatmapsetID -> getVoiceFromSID(param.id) ?: run {
                type = IDType.BeatmapID
                getVoiceFromBID(param.id)
            }
        }

        if (voice == null) {
            log.info("谱面试听：无法获取 ${type.abbr}${param.id} 的音频")
            throw NoSuchElementException.Audio()
        }

        event.replyVoiceAsync(voice, { e ->
            log.error("谱面试听：发送失败", e)
        })

        return when (type) {
            IDType.BeatmapID -> ServiceCallStatistic.build(event, beatmapID = param.id)
            IDType.BeatmapsetID -> ServiceCallStatistic.build(event, beatmapsetID = param.id)

        }
    }

    private fun getVoiceFromSID(sid: Long): ByteArray? {
        return beatmapApiService.getVoice(sid)
    }

    private fun getVoiceFromBID(bid: Long): ByteArray? {
        val setID = runCatching { beatmapApiService.getBeatmapsetIDFromBeatmapIDByExtend(bid) ?: error("null") }
            .recoverCatching { beatmapApiService.getBeatmapFromDatabase(bid).beatmapsetID }
            .recoverCatching { beatmapApiService.getBeatmap(bid).beatmapsetID }
            .getOrNull() ?: return null

        return beatmapApiService.getVoice(setID)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AudioService::class.java)
    }
}
