package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.AudioService.AudioParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*

@Service("AUDIO")
class AudioService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val osuApiWebClient: WebClient,
) : MessageService<AudioParam> {

    data class AudioParam(var id: Long = 0, var isBid: Boolean = false)

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
        val idStr: String? = matcher.group("id")
        val type: String? = matcher.group("type")

        val id = idStr?.toLongOrNull() ?: throw IllegalArgumentException.WrongException.Audio()

        val isBID = !(type != null && (type == "s" || type == "sid"))

        data.value = AudioParam(id, isBID)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: AudioParam) {
        val voice =
            if (param.isBid) {
                // 先 b 再 s
                getVoiceFromBID(param.id) ?: getVoiceFromSID(param.id)
            } else {
                // 先 s 再 b
                getVoiceFromSID(param.id) ?: getVoiceFromBID(param.id)
            }

        if (voice == null) {
            val type = if (param.isBid) 'B' else 'S'
            log.info("谱面试听：无法获取 ${type}${param.id} 的音频")
            throw NoSuchElementException.Audio()
        }

        try {
            event.replyVoice(voice)
        } catch (e: Exception) {
            log.error("谱面试听：发送失败", e)
            throw IllegalStateException.Send("谱面试听")
        }
    }

    @Throws(WebClientResponseException::class)
    private fun getVoice(sid: Number): ByteArray {
        val url = "https://b.ppy.sh/preview/${sid}.mp3"

        return osuApiWebClient.get().uri(url).retrieve().bodyToMono(ByteArray::class.java).block()!!
    }

    private fun getVoiceFromSID(sid: Long): ByteArray? {
        val s =
            try {
                beatmapApiService.getBeatMapSet(sid)
            } catch (e: Exception) {
                return null
            }

        if (s.nsfw) {
            throw UnsupportedOperationException.AudioNotSafeForWork()
        }

        return try {
            getVoice(sid)
        } catch (e: WebClientException) {
            null
        }
    }

    private fun getVoiceFromBID(bid: Long): ByteArray? {
        val b =
            try {
                beatmapApiService.getBeatMap(bid)
            } catch (e: Exception) {
                return null
            }

        if (b.beatmapset?.nsfw == true) {
            throw UnsupportedOperationException.AudioNotSafeForWork()
        }

        return try {
            getVoice(b.beatmapsetID)
        } catch (e: WebClientException) {
            null
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AudioService::class.java)
    }
}
