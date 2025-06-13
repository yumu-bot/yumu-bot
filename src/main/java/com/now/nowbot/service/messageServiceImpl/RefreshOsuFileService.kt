package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import okio.IOException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("REFRESH_FILE")
class RefreshOsuFileService(private val osuBeatmapApiService: OsuBeatmapApiService) :
    MessageService<Long> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Long>,
    ): Boolean {
        val matcher = Instruction.REFRESH_FILE.matcher(messageText)
        return if (matcher.find()) {
            data.value = matcher.group("bid")?.toLongOrNull() ?: throw IllegalArgumentException.WrongException.BeatmapID()
            true
        } else {
            false
        }
    }

    override fun HandleMessage(event: MessageEvent, param: Long) {
        val sid =
            try {
                osuBeatmapApiService.getBeatMapFromDataBase(param).beatmapsetID
            } catch (e: Exception) {
                throw NoSuchElementException.Beatmap(param)
            }

        val s =
            try {
                osuBeatmapApiService.getBeatMapSet(sid)
            } catch (e: Exception) {
                throw NoSuchElementException.Beatmap(sid)
            }

        var count = 0

        for (b in s.beatmaps!!) try {
            if (osuBeatmapApiService.refreshBeatMapFileFromDirectory(b.beatmapID)) {
                count++
            }
        } catch (e: IOException) {
            log.error("刷新文件：IO 异常", e)
            throw IllegalStateException.ReadFile("刷新文件")
        }

        if (count == 0) {
            event.reply(NoSuchElementException.BeatmapCache(s.previewName))
        } else {
            event.reply("已成功刷新谱面 ${s.previewName} 的所有相关联的 $count 个缓存文件。")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RefreshOsuFileService::class.java)
    }
}
