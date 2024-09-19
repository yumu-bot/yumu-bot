package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.MaiScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.DivingFishApiService.MaimaiApiService
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

@Service("MAI_SCORE")
class MaiScoreService(private val maimaiApiService: MaimaiApiService) :
    MessageService<MaiScoreService.MaiScoreParam> {

    data class MaiScoreParam(val id : Int)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiScoreParam>,
    ): Boolean {
        val matcher = Instruction.MAI_SCORE.matcher(messageText)

        if (!matcher.find()) {
            return false
        }



        data.value = MaiScoreParam(1)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiScoreParam) {

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
