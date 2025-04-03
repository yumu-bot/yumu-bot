package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("TEST_TAIKO_SR_CALCULATE") 
class TestTaikoSRCalculateService : MessageService<Matcher> {
    
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Matcher>): Boolean {
        val m = Instruction.TEST_TAIKO_SR_CALCULATE.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }
    
    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val str = matcher.group("data").trim {it <= ' '} .replace("\\s+".toRegex(), "-")
        event.reply("结果：" + getResult(str))
    }
    
    private fun getResult(str: String): String {
        var mainColorChanged = 0
        var mainFingerChanged = 0
        var subColorChanged = 0
        var subFingerChanged = 0
        var complexChanged = 0
        var complexSameColor = 0

        var isComplex = false
        var complexCount = 0

        for (i in str.indices) {
            val char = str[i]

            if (isComplex.not() && (char == '(' || char == '（')) {
                isComplex = true
                complexChanged ++
                continue
            } else if (isComplex && (char == ')' || char == '）')) {
                isComplex = false
                complexCount = 0
                continue
            }

            val index = if (isComplex) complexCount else i

            val frontChar = if (index >= 1) str[index - 1] else '-'
            val front2Char = if (index >= 2) str[index - 2] else '-'

            if (frontChar.toString().matches("[xo]".toRegex()) && char != '-' && frontChar != char) {
                if (i % 2 == 0) mainColorChanged ++ else subColorChanged ++
            }

            if (front2Char.toString().matches("[xo]".toRegex()) && char != '-' && front2Char != char) {
                if (i % 2 == 0) mainFingerChanged ++ else subFingerChanged ++
            }

            if (frontChar.toString().matches("[()（）]".toRegex()) && front2Char.toString().matches("[xo]".toRegex()) && front2Char == char) {
                complexSameColor ++
            }

            if (isComplex) complexCount ++
        }

        val isStartDifferent = if (str.length > 1) {
            str.first() != str.last()
        } else false

        val difficulty = mainColorChanged + mainFingerChanged + 3 * ((subColorChanged + 1) * (subFingerChanged + 1) - 1) + (if (isStartDifferent) 1 else 0)

        return """
            对 $str 的难度分析：
            主手换色：$mainColorChanged
            主手换起指：$mainFingerChanged
            副手换色：$subColorChanged
            副手换起指：$subFingerChanged
            
            预期难度：$difficulty
            
            复合：$complexChanged
            复合前后同色：$complexSameColor
        """.trimIndent()
    }
}
