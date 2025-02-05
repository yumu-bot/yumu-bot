package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service("MAI_COUPLE")
class MaiCoupleService(private val maimaiApiService: MaimaiApiService) : MessageService<String> {
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<String>,
    ): Boolean {
        val matcher = Instruction.MAI_COUPLE.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = matcher.group(FLAG_NAME)
        return true
    }

    override fun HandleMessage(event: MessageEvent, input: String?) {
        if (input.isNullOrEmpty())
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_UserName)

        val rankMap = maimaiApiService.getMaimaiRank()
        val nameMap =
                rankMap.keys
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        { DataUtil.getStandardisedString(it) },
                                        { it },
                                        { _, v2 -> v2 },
                                )
                        )

        val similarities = mutableListOf<Pair<String, Double>>()

        for (std in nameMap.keys) {
            val y = DataUtil.getStringSimilarityStandardised(input, std)

            if (y >= 0.4) {
                similarities.add(Pair(std, y))
            }
        }

        if (similarities.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Result)
        }
        val sort = similarities.stream().sorted(Comparator.comparingDouble<Pair<String, Double>?> { it.second }.reversed()).toList()

        val sb = StringBuilder("搜索结果：\n")

        var i = 1
        for (e in sort) {
            val name = nameMap[e.first]
            val achievement = rankMap[nameMap[e.first]]

            sb.append("#${i}: ")
                    .append("${String.format("%.0f", e.second * 100)}%")
                    .append(" ")
                    .append("$name")
                    .append(" ")
                    .append("[${achievement}]")
                    .append("\n")

            i++

            if (i > 15) break
        }

        event.reply(sb.toString())
    }
}
