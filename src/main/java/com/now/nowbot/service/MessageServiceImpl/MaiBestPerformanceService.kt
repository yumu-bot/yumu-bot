package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.MaiBestPerformance
import com.now.nowbot.model.JsonData.MaiFit.ChartData
import com.now.nowbot.model.JsonData.MaiFit.DiffData
import com.now.nowbot.model.JsonData.MaiScore
import com.now.nowbot.model.JsonData.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.AtMessage
import com.now.nowbot.service.DivingFishApiService.MaimaiApiService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.yumu.core.extensions.isNotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("MAI_BP")
class MaiBestPerformanceService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService,
) : MessageService<MaiBestPerformanceService.MaiScoreParam> {

    data class MaiScoreParam(val name: String?, val qq: Long?, val range: Int)

    @JvmRecord
    data class PanelMEParam(
        val user: MaiBestPerformance.User,
        val score: MaiScore,
        val song: MaiSong,
        val chart: ChartData,
        val diff: DiffData,
    ) {
        fun toMap(): Map<String, Any> {
            var out = mutableMapOf<String, Any>()

            out["user"] = user
            out["score"] = score
            out["song"] = song
            out["chart"] = chart
            out["diff"] = diff
            return out
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiScoreParam>,
    ): Boolean {
        val matcher = Instruction.MAI_BP.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        // TODO 这里用老式的获取方法，但是最好写成一个通用的，类似于 cmdUtil
        val range =
            if (StringUtils.hasText(matcher.group("range"))) {
                matcher.group("range").toInt()
            } else {
                1
            }

        val at = QQMsgUtil.getType(event.message, AtMessage::class.java)

        if (StringUtils.hasText(matcher.group("name"))) {
            val name = matcher.group("name").trim()
            if (name.contains(Regex("\\s+"))) {
                val strs = name.split(Regex("\\s+"))

                if (strs.size == 2 && Regex("\\d{1,3}").matches(strs.last())) {
                    data.value = MaiScoreParam(strs.first().trim(), null, strs.last().toInt())
                    return true
                }
            } else if (Regex("\\d{1,3}").matches(name)) {
                data.value = MaiScoreParam(null, event.sender.id, name.toInt())
                return true
            }

            data.value = MaiScoreParam(matcher.group("name").trim(), null, range)
        } else if (StringUtils.hasText(matcher.group("qq"))) {
            data.value = MaiScoreParam(null, matcher.group("qq").toLong(), range)
        } else if (at != null) {
            data.value = MaiScoreParam(null, at.target, range)
        } else {
            data.value = MaiScoreParam(null, event.sender.id, range)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiScoreParam) {
        val scores = getScores(param.qq, param.name, maimaiApiService)
        val score = getScore(param.range, scores)

        val user = scores.user
        val song = maimaiApiService.getMaimaiSong(score.songID, true)
        score.setMax(song)
        val fit = maimaiApiService.getMaimaiFit(true)

        val chart = fit.getChartData(score.songID?.toString(), score.index)
        val diff = fit.getDiffData(chart)

        val image = imageService.getPanelME(PanelMEParam(user, score, song, chart, diff))
        event.reply(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MaiBestPerformanceService::class.java)

        fun getScores(
            qq: Long?,
            name: String?,
            maimaiApiService: MaimaiApiService,
        ): MaiBestPerformance {

            return if (qq.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiBest50(qq)
                } catch (e: WebClientResponseException.BadRequest) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_QQBadRequest)
                } catch (e: WebClientResponseException.Forbidden) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_Forbidden)
                }
            } else if (name.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiBest50(name)
                } catch (e: WebClientResponseException.BadRequest) {
                    log.error("?", e)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_NameBadRequest)
                } catch (e: WebClientResponseException.Forbidden) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_Forbidden)
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            }
        }

        @JvmStatic
        fun getScore(range: Int, bp: MaiBestPerformance): MaiScore {
            return if (range > 35) {
                if (range - 35 > bp.charts.deluxe.size) {
                    throw TipsException("请输入正确的范围！(<= ${bp.charts.deluxe.size + 35})")
                } else {
                    bp.charts.deluxe.get(range - 36)
                }
            } else {
                if (range > bp.charts.standard.size) {
                    throw TipsException("请输入正确的范围！(<= ${bp.charts.standard.size})")
                } else {
                    bp.charts.standard.get(range - 1)
                }
            }
        }
    }
}
