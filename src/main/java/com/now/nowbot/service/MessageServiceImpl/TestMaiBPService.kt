package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.MaiScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.DivingFishApiService.MaimaiApiService
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("TEST_MAI_BP")
class TestMaiBPService(private val maimaiApiService: MaimaiApiService) :
    MessageService<TestMaiBPService.MaiBPParam> {

    data class MaiBPParam(val range: Int)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiBPParam>,
    ): Boolean {
        val matcher = Instruction.TEST_MAI_BP.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val range = matcher.group("range")?.toInt() ?: 1

        data.value = MaiBPParam(range)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiBPParam) {
        val scores = MaiBestPerformanceService.getScores(event.sender.id, null, maimaiApiService)
        var score = MaiBestPerformanceService.getScore(param.range, scores)

        event.reply(getMessage(score, maimaiApiService.getMaimaiCover(score.songID)))
    }

    companion object {
        fun getMessage(score: MaiScore, image: ByteArray): MessageChain {
            var sb = MessageChain.MessageChainBuilder()

            sb.addImage(image)
            sb.addText("\n")
            sb.addText("[${score.type}] ${score.title} [${score.difficulty} ${score.level}] (${score.star})\n")
            sb.addText("${String.format("%.4f", score.achievements)}% ${getRank(score.rank)} // ${score.rating} ra\n")
            sb.addText("[${getCombo(score.combo)}] [${getSync(score.sync)}] // id ${score.songID}")

            return sb.build()
        }

        fun getRank(rate: String?): String {
            return (rate ?: "?").uppercase().replace('P', '+')
        }

        fun getCombo(combo: String?): String {
            return when (combo?.lowercase()) {
                "" -> "C"
                "fc" -> "FC"
                "fcp" -> "FC+"
                "ap" -> "AP"
                "app" -> "AP+"
                else -> "?"
            }
        }

        fun getSync(sync: String?): String {
            return when (sync?.lowercase()) {
                "" -> "1P"
                "sync" -> "SY"
                "fs" -> "FS"
                "fsp" -> "FS+"
                "fsd" -> "FDX"
                "fsdp" -> "FDX+"
                else -> "?"
            }
        }
    }
}
