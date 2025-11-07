package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.permission.TokenBucketRateLimiter
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.lxnsApiService.LxMaiApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service("MAI_AUDIO")
class MaiAudioService(
    private val lxMaiApiService: LxMaiApiService,
    private val dao: ServiceCallStatisticsDao,
): MessageService<MaiFindService.MaiFindParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiFindService.MaiFindParam>
    ): Boolean {

        val matcher = Instruction.MAI_AUDIO.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val any: String = (matcher.group(FLAG_NAME) ?: "").trim()

        if (any.isBlank()) {
            val last = dao.getLastMaiSongID(event.subject.id, null, LocalDateTime.now().minusHours(24))

            if (last != null) {
                val song = lxMaiApiService.getMaiSong(last.toInt()) ?: throw NoSuchElementException.Song(last)

                data.value = MaiFindService.MaiFindParam(
                    listOf(song), 1, 1, 1
                )
                return true
            }
        }

        data.value = MaiFindService.getParam(matcher, lxMaiApiService)
        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: MaiFindService.MaiFindParam
    ): ServiceCallStatistic? {
        tokenBucketRateLimiter.checkOrThrow("MAI_AUDIO", event.subject.id)

        val first = param.songs.map { it.songID }.first()

        val voice = try {
            lxMaiApiService.getAudio(first)
        } catch (e: Exception) {
            log.error("舞萌试听：获取失败", e)
            throw e as? NetworkException.LxnsException ?: IllegalStateException.Fetch("舞萌试听")
        }

        try {
            event.reply(getVoiceInfo(param.songs))

            event.replyVoice(voice)
        } catch (e: Exception) {
            log.error("舞萌试听：发送失败", e)
            throw IllegalStateException.Send("舞萌试听")
        }

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mais" to listOf(first)
            ))
        }
    }

    companion object {
        private val tokenBucketRateLimiter = TokenBucketRateLimiter(3, 30.toDuration(DurationUnit.MINUTES))

        private fun getVoiceInfo(songs: List<MaiSong>): String {
            if (songs.isEmpty()) return "没有需要播放的舞萌歌曲。"

            val currentSong = songs.first()

            val sb = StringBuilder(
                """
                正在为您播放：
                ${currentSong.getSongPreviewInfo()}
            """.trimIndent()
            )

            if (songs.size > 1) {
                sb.append('\n').append('\n').append("其他结果：")

                songs.drop(1).take(10).forEach { song ->
                    sb.append('\n').append(song.getSongPreviewInfo())
                }
            }

            return sb.toString()
        }

        private fun MaiSong.getSongPreviewInfo(): String {
            val it = this

            return "${it.songID}: ${it.info.artist} - ${it.info.title} ${it.level.joinToString(", ", "[", "]")}"
        }


        private val log: Logger = LoggerFactory.getLogger(MaiAudioService::class.java)
    }
}