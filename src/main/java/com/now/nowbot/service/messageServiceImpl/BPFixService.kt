package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.json.LazerScoreWithFcPP
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPFixService.BPFixParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithOutRange
import com.now.nowbot.util.CmdUtil.processBP
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.pow

@Service("BP_FIX")
class BPFixService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
) : MessageService<BPFixParam>, TencentMessageService<BPFixParam> {

    data class BPFixParam(val user: OsuUser, val bpMap: Map<Int, LazerScore>, val mode: OsuMode)

    companion object {
        val log: Logger = LoggerFactory.getLogger(BPFixService::class.java)
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<BPFixParam>): Boolean {
        val matcher = Instruction.BP_FIX.matcher(messageText)
        if (!matcher.find()) return false

        val mode = getMode(matcher)

        val user = getUserWithOutRange(event, matcher, mode)

        val bpMap = scoreApiService.getBestScores(user.userID, mode.data, 0, 100)

        data.value = BPFixParam(user, processBP(bpMap), mode.data!!)

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: BPFixParam) {
        val image = param.getImage()
        try {
            event.reply(image)
        } catch (e: java.lang.Exception) {
            log.error("理论最好成绩：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "理论最好成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BPFixParam? {
        val matcher = OfficialInstruction.BP_FIX.matcher(messageText)
        if (!matcher.find()) return null

        val mode = getMode(matcher)

        val user = getUserWithOutRange(event, matcher, mode)

        val bpMap = scoreApiService.getBestScores(user.userID, mode.data, 0, 100)

        return BPFixParam(user, processBP(bpMap), mode.data!!)
    }

    override fun reply(event: MessageEvent, param: BPFixParam): MessageChain? = QQMsgUtil.getImage(param.getImage())

    fun fix(playerPP: Double, bpMap: Map<Int, LazerScore>): Map<String, Any>? {
        val bpList = mutableListOf<LazerScore>()
        val beforeBpSumAtomic = AtomicReference(0.0)

        bpMap.forEach { (index: Int, score: LazerScore) ->
            beforeBpSumAtomic.updateAndGet { v: Double -> v + (score.weight?.PP ?: 0.0) }
            beatmapApiService.applyBeatMapExtendFromDataBase(score)

            val max = score.beatMapCombo
            val combo = score.maxCombo
            val miss = score.statistics.miss ?: 0

            // 断连击，mania 模式不参与此项筛选
            val isChoke = (miss == 0) && (combo < Math.round(max * 0.98)) && (score.mode != OsuMode.MANIA)

            // 含有 <1% 的失误
            val has1pMiss = (miss > 0) && ((1.0 * miss / max) <= 0.01)

            // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
            if (isChoke || has1pMiss) {
                bpList.add(
                    initFixScore(score, index + 1)
                )
            } else {
                bpList.add(score)
            }
        }

        bpList.sortByDescending {
            val pp = if (it is LazerScoreWithFcPP && it.fcPP > 0) {
                it.fcPP
            } else {
                it.PP ?: 0.0
            }

            pp * 100.0
        }

        val afterBpSumAtomic = AtomicReference(0f)

        bpList.forEachIndexed { index, score ->
            val weight: Double = 0.95.pow(index.toDouble())
            val pp: Double
            if (score is LazerScoreWithFcPP) {
                pp = score.fcPP
                score.indexAfter = index + 1
            } else {
                pp = score.PP ?: 0.0
            }
            afterBpSumAtomic.updateAndGet { v: Float -> v + (weight * pp).toFloat() }
        }

        val beforeBpSum = beforeBpSumAtomic.get()
        val afterBpSum = afterBpSumAtomic.get()
        val newPlayerPP = (playerPP + afterBpSum - beforeBpSum).toFloat()

        val scoreList =
            bpList.stream().filter { s: LazerScore? -> s is LazerScoreWithFcPP }.map { s: LazerScore -> s as LazerScoreWithFcPP }.toList()

        if (CollectionUtils.isEmpty(scoreList)) return null

        val result = HashMap<String, Any>(2)
        result["scores"] = scoreList
        result["pp"] = newPlayerPP

        return result
    }

    private fun initFixScore(score: LazerScore, index: Int): LazerScoreWithFcPP {
        val result = LazerScoreWithFcPP.copyOf(score)
        result.index = index + 1
        try {
            val pp = beatmapApiService.getFcPP(score)
            result.fcPP = pp.pp
        } catch (e: Exception) {
            log.error("bp 计算 pp 出错:", e)
        }
        return result
    }

    private fun BPFixParam.getImage(): ByteArray {
        if (CollectionUtils.isEmpty(bpMap)) throw GeneralTipsException(
            GeneralTipsException.Type.G_Null_PlayerRecord,
            mode.getName()
        )

        val pp = Objects.requireNonNullElse(user.pp, 0.0)

        val fixData = fix(pp, bpMap) ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_TheoreticalBP)

        return try {
            imageService.getPanelA7(user, fixData)
        } catch (e: java.lang.Exception) {
            log.error("理论最好成绩：渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "理论最好成绩")
        }
    }
}