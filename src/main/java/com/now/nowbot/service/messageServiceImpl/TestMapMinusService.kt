package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.skill.Skill6
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_SEPERATOR
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("TEST_SKILL") class TestMapMinusService(
    private val beatmapApiService: OsuBeatmapApiService
) : MessageService<Matcher> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Matcher>): Boolean {

        val m = Instruction.TEST_SKILL.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    override fun handleMessage(event: MessageEvent, param: Matcher): ServiceCallStatistic? {
        val bids = param.group("data").split(REG_SEPERATOR.toRegex()).map { it.toLongOrNull() ?: -1L }

        val files = bids.filter { it != -1L }.map {
                try {
                    OsuFile.getInstance(beatmapApiService.getBeatmapFileString(it) ?: "")
                } catch (_: Exception) {
                    OsuFile.getInstance("")
                }
            }

        val skills = files.map {
            Skill6(it, OsuMode.MANIA)
        }

        val sb = StringBuilder()

        skills.forEach {
            //sb.append(it.graphs.joinToString("\n") { sub -> sub.joinToString(",") })
            sb.append(it.rating).append(",").append(it.skills.joinToString(",")).append("\n")
            //sb.append(it.bases.joinToString(",")).append('\n')
        }

        event.replyFileInGroup(sb.toString().toByteArray(), bids.first().toString() + "s.csv")

        return null
    }
}