package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.AudioService.AudioParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime

@Service("AUDIO")
class AudioService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val osuApiWebClient: WebClient,
    private val dao: ServiceCallStatisticsDao,
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

        var isBID = !(type != null && (type == "s" || type == "sid"))

        val id = idStr?.toLongOrNull()
            ?: run {
                val last = dao.getLastBeatmapID(
                    groupID = event.subject.id,
                    name = null,
                    from = LocalDateTime.now().minusHours(24L)
                )

                if (last != null) {
                    isBID = true
                }

                last ?: throw IllegalArgumentException.WrongException.Audio()
            }


        data.value = AudioParam(id, isBID)
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: AudioParam): ServiceCallStatistic? {
        var currentType: Boolean = param.isBid

        val voice =
            if (param.isBid) {
                // 先 b 再 s
                getVoiceFromBID(param.id) ?: run {
                    currentType = false
                    getVoiceFromSID(param.id)
                }
            } else {
                // 先 s 再 b
                getVoiceFromSID(param.id) ?: run {
                    currentType = true
                    getVoiceFromSID(param.id)
                }
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

        return if (currentType) {
            ServiceCallStatistic.build(event, beatmapID = param.id)
        } else {
            ServiceCallStatistic.build(event, beatmapsetID = param.id)
        }

        /*
        // 这两种都可以, 选其中一个
        // val old = ServiceCallStatisticLite.build(event, param)
        val new = ServiceCallStatistic.build(event /* , param=xxx */) {
            val key = if (param.isBid) {
                "bid"
            } else{
                "sid"
            }
            setParam(mapOf(key to param.id))
        }
        return new

         */
    }

    @Throws(WebClientResponseException::class)
    private fun getVoice(sid: Number): ByteArray {
        val url = "https://b.ppy.sh/preview/${sid}.mp3"

        return osuApiWebClient.get().uri(url).retrieve().bodyToMono(ByteArray::class.java).block()!!
    }

    private fun getVoiceFromSID(sid: Long): ByteArray? {

        return try {
            getVoice(sid)
        } catch (_: WebClientException) {
            null
        }
    }

    private fun getVoiceFromBID(bid: Long): ByteArray? {
        val b =
            try {
                beatmapApiService.getBeatmap(bid)
            } catch (_: Exception) {
                return null
            }

        return try {
            getVoice(b.beatmapsetID)
        } catch (_: WebClientException) {
            null
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AudioService::class.java)
    }
}
