package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.springframework.stereotype.Service

@Service("MAI_SEEK")
class MaiSeekService(private val maimaiApiService: MaimaiApiService) : MessageService<String> {
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<String>,
    ): Boolean {
        val matcher = Instruction.MAI_SEEK.matcher(messageText)
        if (!matcher.find()) {
            return false
        }
        
        val str: String? = matcher.group(FLAG_NAME)

        if (str.isNullOrEmpty()) {
            throw IllegalArgumentException.WrongException.PlayerName()
        }

        data.value = str
        return true
    }

    override fun handleMessage(event: MessageEvent, param: String) {
        if (param.matches("\\s*$REG_NUMBER_MORE\\s*".toRegex()) && !param.contains(REG_QUOTATION.toRegex())) {
            val rating = param.toIntOrNull() ?: 0
            
            val surrounding = maimaiApiService.getMaimaiSurroundingRank(rating)
            
            val sb = StringBuilder("搜索结果：\n")

            var i = 1
            
            for (e in surrounding) {
                val name = e.key
                val achievement = e.value

                val delta = achievement - rating
                val index = if (delta > 0) "+" else ""

                sb.append("#${i}: ")
                    .append(index)
                    .append(delta)
                    .append(" ")
                    .append(name)
                    .append(" ")
                    .append("[${achievement}]")
                    .append("\n")

                i++

                if (i > 15) break
            }

            event.reply(sb.toString())
            return
        }
        
        val rankMap = maimaiApiService.getMaimaiRank()
        val nameMap = rankMap.keys.associateBy { DataUtil.getStandardisedString(it).replace("\"", "") }

        val similarities = mutableListOf<Pair<String, Double>>()

        val paramNoQuote = param.replace(REG_QUOTATION.toRegex(), "")

        for (std in nameMap.keys) {
            val y = DataUtil.getStringSimilarity(paramNoQuote, std, standardised = false)

            if (y >= 0.4) {
                similarities.add(Pair(std, y))
            }
        }

        if (similarities.isEmpty()) {
            throw NoSuchElementException.Result()
        }
        val sort = similarities.sortedByDescending { it.second }
            //.stream().sorted(Comparator.comparingDouble<Pair<String, Double>?> { it.second }.reversed()).toList()

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
