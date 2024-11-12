package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.AudioService.AudioParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.GeneralTipsException
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

        if (idStr == null) {
            throw GeneralTipsException("请输入想要试听的 bid 或者 sid！\n(!a <bid> / !a:s <sid>)")
        }

        val id =
            try {
                idStr.toLong()
            } catch (e: NumberFormatException) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
            }

        val isBID = type != null && (type == "b" || type == "bid")

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
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Audio)
        }

        try {
            event.replyVoice(voice)
        } catch (e: Exception) {
            log.error("谱面试听：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面试听")
        }
    }

    @Throws(WebClientResponseException::class)
    private fun getVoice(sid: Number): ByteArray {
        val url = "https://b.ppy.sh/preview/${sid}.mp3"

        return osuApiWebClient.get().uri(url).retrieve().bodyToMono(ByteArray::class.java).block()!!
    }

    private fun getVoiceFromSID(sid: Long): ByteArray? {
        return try {
            getVoice(sid)
        } catch (e: WebClientException) {
            null
        }
    }

    private fun getVoiceFromBID(bid: Long): ByteArray? {
        val sid =
            try {
                beatmapApiService.getBeatMapFromDataBase(bid).beatMapSetID
            } catch (e: Exception) {
                return null
            }

        return try {
            getVoice(sid)
        } catch (e: WebClientException) {
            null
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AudioService::class.java)
    }
}
