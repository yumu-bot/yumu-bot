package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.mapminus.PPMinus4
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher

@Service("TEST_MAP_MINUS") class TestMapMinusService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<Matcher> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Matcher>): Boolean {

        val m = Instruction.TEST_MAP_MINUS.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    override fun HandleMessage(event: MessageEvent?, param: Matcher) {
        val bids = param.group("data").split("[,，、|:：\\s]+".toRegex()).map { it.toLongOrNull() ?: -1L }

        val files = bids.filter { it != -1L }.map {
                try {
                    OsuFile.getInstance(beatmapApiService.getBeatMapFileString(it))
                } catch (e: Exception) {
                    OsuFile.getInstance("")
                }
            }

        val mapMinuses = files.map {
            PPMinus4.getInstance(it)
        }

        val sb = StringBuilder()

        mapMinuses.forEach {
            sb.append(it.toString()).append('\n')
        }

        val p = Path.of(NowbotConfig.RUN_PATH, "debug")
        Files.write(p.resolve(bids.first().toString() + "s.csv"), sb.toString().toByteArray())
    }
}