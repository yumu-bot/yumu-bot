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
        val mode = OsuMode.getMode(param.group("mode")).modeValue

        val files = bids.filter { it != -1L }.map {
                try {
                    OsuFile(beatmapApiService.getBeatmapFileString(it) ?: "")
                } catch (_: Exception) {
                    OsuFile("")
                }
            }

        val skills = files.map {
            Skill6(it, OsuMode.MANIA)
        }

        val sb = StringBuilder()

        when (mode.toInt()) {


            2 -> {
                // 写入表头
                sb.append("S,B,J,F,T,U,R,E,V,H,O,G,Y\n")

                skills.forEach { skill ->
                    // 遍历 graphs 中的每一行 (row)
                    val rowsString = skill.graphs.joinToString("\n") { row -> row.joinToString(",") }

                    sb.append(rowsString).append("\n\n") // 写入重组后的所有行，技能之间留空行
                }
            }

            1 -> skills.forEach {
                sb.append(it.rating).append(",")
                    .append(it.skills.take(6).joinToString(",")).append('\n')
            }

            else -> {
                sb.append("S,B,J,F,T,U,R,E,V,H,O,G,Y\n")
                skills.forEach {
                    sb.append(it.bases.joinToString(",")).append('\n')
                }
            }
        }

        val m = when (mode.toInt()) {
            1 -> "skills"
            2 -> "graph"
            else -> "base"
        }

        event.replyFileInGroup(sb.toString().toByteArray(),  "${bids.first()}(${m}).csv")

        return null
    }
}