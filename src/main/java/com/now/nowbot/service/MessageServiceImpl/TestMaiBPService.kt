package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.model.JsonData.MaiScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.DivingFishApiService.MaimaiApiService
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("MAI_BP")
@CheckPermission(isSuperAdmin = true)
class TestMaiBPService(private val maimaiApiService: MaimaiApiService) :
    MessageService<TestMaiBPService.MaiBPParam> {

    data class MaiBPParam(val range: Int)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiBPParam>,
    ): Boolean {
        val matcher = Instruction.MAI_BP.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val range = matcher.group("range")?.toInt() ?: 1

        data.value = MaiBPParam(range)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiBPParam) {
        val scores =
            try {
                maimaiApiService.getMaimaiBest50(event.sender.id)
            } catch (e: WebClientResponseException.BadRequest) {
                throw TipsException("找不到您的水鱼绑定账号。")
            } catch (e: WebClientResponseException.Forbidden) {
                throw TipsException("您或者对方不允许其他人查询成绩。")
            }

        var score: MaiScore

        if (param.range > 35) {
            if (param.range - 35 > scores.charts.deluxe.size) {
                throw TipsException("请输入正确的范围！(<${scores.charts.deluxe.size + 35})")
            } else {
                score = scores.charts.deluxe.get(param.range - 36)
            }
        } else {
            if (param.range > scores.charts.standard.size) {
                throw TipsException("请输入正确的范围！(<${scores.charts.standard.size})")
            } else {
                score = scores.charts.standard.get(param.range - 1)
            }
        }

        event.reply(getMessage(score, maimaiApiService.getMaimaiCover(score.songID)))
    }

    companion object {
        fun getMessage(score: MaiScore, image: ByteArray): MessageChain {
            var sb = MessageChain.MessageChainBuilder()

            sb.addImage(image)
            sb.addText("\n")
            sb.addText("[${score.type}] ${score.title} [${score.difficulty} ${score.level}] (${score.star})\n")
            sb.addText("${String.format("%.4f", score.achievements)}% ${getRank(score.rank)} // ${score.rating} ra\n")
            sb.addText("[${getCombo(score.combo)}] [${getSync(score.sync)}] // s${score.songID}")

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
                "", "sync" -> "SY"
                "fs" -> "FSY"
                "fsd" -> "FDX"
                "fsdp" -> "FDX+"
                else -> "?"
            }
        }
    }
}
