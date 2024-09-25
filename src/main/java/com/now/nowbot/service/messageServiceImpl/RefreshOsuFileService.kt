package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.GeneralTipsException
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
            data.value = matcher.group("bid").toLong()
            true
        } else {
            false
        }
    }

    override fun HandleMessage(event: MessageEvent, param: Long) {
        val sid =
            try {
                osuBeatmapApiService.getBeatMapFromDataBase(param).setID
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Map)
            }

        val s =
            try {
                osuBeatmapApiService.getBeatMapSet(sid)
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Map)
            }
        var count = 0

        for (b in s.beatMaps!!) try {
            if (osuBeatmapApiService.refreshBeatMapFileFromDirectory(b.beatMapID)) {
                count++
            }
        } catch (e: IOException) {
            log.error("刷新文件：IO 异常", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_IOException, "刷新文件")
        }

        if (count == 0) {
            event.reply(GeneralTipsException(GeneralTipsException.Type.G_Null_MapFile))
        } else {
            event.reply(GeneralTipsException(GeneralTipsException.Type.G_Success_RefreshFile, param, count))
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RefreshOsuFileService::class.java)
    }
}
