package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.AudioService.AudioParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.serviceException.AudioException
import com.now.nowbot.util.Instruction
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("AUDIO")
class AudioService(
        private val beatmapApiService: OsuBeatmapApiService,
        private val osuApiWebClient: WebClient,
) : MessageService<AudioParam> {

    class AudioParam {
        var isBid: Boolean = false
        var id: Int = 0
    }

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
        val param = AudioParam()
        val idStr: String? = matcher.group("id")
        val type: String? = matcher.group("type")

        if (idStr == null) {
            throw AudioException(AudioException.Type.SONG_Parameter_NoBid)
        }

        try {
            param.id = idStr.toInt()
        } catch (e: NumberFormatException) {
            throw AudioException(AudioException.Type.SONG_Parameter_BidError)
        }

        param.isBid = type == "b" || type == "bid"

        data.value = param
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: AudioParam) {
        val from = event.subject

        var sid = 0L

        var voice: ByteArray

        if (param.isBid) {
            try {
                sid = beatmapApiService.getBeatMapFromDataBase(param.id).beatMapSetID
            } catch (e: Exception) {
                throw AudioException(AudioException.Type.SONG_Map_NotFound)
            }

            try {
                voice = getVoice(sid)
            } catch (e: Exception) {
                log.error("音频下载失败：", e)
                throw AudioException(AudioException.Type.SONG_Download_Error)
            }
        } else {
            // isSid
            try {
                voice = getVoice(sid)
            } catch (e: Exception) {
                // 输入的不是 SID
                try {
                    sid = beatmapApiService.getBeatMapFromDataBase(param.id).beatMapSetID
                } catch (e1: Exception) {
                    throw AudioException(AudioException.Type.SONG_Map_NotFound)
                }

                try {
                    voice = getVoice(sid)
                } catch (e2: Exception) {
                    log.error("音频下载失败、附加转换失败：", e2)
                    throw AudioException(AudioException.Type.SONG_Download_Error)
                }
            }
        }

        try {
            from.sendVoice(voice)
        } catch (e: Exception) {
            log.error("音频发送失败：", e)
            throw AudioException(AudioException.Type.SONG_Send_Error)
        }
    }

    @Throws(WebClientResponseException::class)
    private fun getVoice(sid: Number): ByteArray {
        val url = "https://b.ppy.sh/preview/${sid}.mp3"

        return osuApiWebClient.get().uri(url).retrieve().bodyToMono(ByteArray::class.java).block()!!
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AudioService::class.java)
    }
}
