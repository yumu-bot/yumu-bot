package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.JsonData.ScoreWithFcPP
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.BPFixService.BPFixParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.pow

@Service("BP_FIX")
class BPFixService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
) : MessageService<BPFixParam>, TencentMessageService<BPFixParam> {

    data class BPFixParam(val user: OsuUser, val bpMap: Map<Int, Score>, val mode: OsuMode)

    companion object {
        val log: Logger = LoggerFactory.getLogger(BPFixService::class.java)
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<BPFixParam>): Boolean {
        val matcher = Instruction.BP_FIX.matcher(messageText)
        if (!matcher.find()) return false

        val mode = getMode(matcher)

        val user = getUserWithOutRange(event, matcher, mode)

        val bpMap = scoreApiService.getBestPerformance(user.userID, mode.data, 0, 100)

        data.value = BPFixParam(user, processBP(bpMap), mode.data!!)

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: BPFixParam) {
        val from = event.subject
        val image = param.getImage()
        try {
            from.sendImage(image)
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

        val bpMap = scoreApiService.getBestPerformance(user.userID, mode.data, 0, 100)

        return BPFixParam(user, processBP(bpMap), mode.data!!)
    }

    override fun reply(event: MessageEvent, param: BPFixParam): MessageChain? = QQMsgUtil.getImage(param.getImage())

    fun fix(playerPP: Double, bpMap: Map<Int, Score>): Map<String, Any>? {
        val bpList = ArrayList<Score>(bpMap.size)
        val beforeBpSumAtomic = AtomicReference(0f)

        bpMap.forEach { (index: Int, score: Score) ->
            beforeBpSumAtomic.updateAndGet { v: Float -> v + score.weightedPP }
            beatmapApiService.applyBeatMapExtendFromDataBase(score)

            val max = score.beatMap.maxCombo
            val combo = score.maxCombo

            val miss = score.statistics.countMiss
            val all = Objects.requireNonNullElse(score.statistics.getCountAll(score.mode), 1)

            // 断连击，mania 模式不参与此项筛选
            val isChoke = (miss == 0) && (combo < Math.round(max * 0.98f)) && (score.mode != OsuMode.MANIA)

            // 含有 <1% 的失误
            val has1pMiss = (miss > 0) && ((1f * miss / all) <= 0.01f)

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
            val pp = if (it is ScoreWithFcPP) it.fcPP else it.pp
            pp * 100
        }

        val afterBpSumAtomic = AtomicReference(0f)

        bpList.forEachIndexed { index, score ->
            val weight: Double = 0.95.pow(index.toDouble())
            val pp: Float
            if (score is ScoreWithFcPP) {
                pp = score.fcPP
                score.indexAfter = index + 1
            } else {
                pp = score.pp
            }
            afterBpSumAtomic.updateAndGet { v: Float -> v + (weight * pp).toFloat() }
        }

        val beforeBpSum = beforeBpSumAtomic.get()
        val afterBpSum = afterBpSumAtomic.get()
        val newPlayerPP = (playerPP + afterBpSum - beforeBpSum).toFloat()

        val scoreList =
            bpList.stream().filter { s: Score? -> s is ScoreWithFcPP }.map { s: Score -> s as ScoreWithFcPP }.toList()

        if (CollectionUtils.isEmpty(scoreList)) return null

        val result = HashMap<String, Any>(2)
        result["scores"] = scoreList
        result["pp"] = newPlayerPP

        return result
    }

    private fun initFixScore(score: Score, index: Int): ScoreWithFcPP {
        val result = ScoreWithFcPP.copyOf(score)
        result.index = index + 1
        try {
            val pp = beatmapApiService.getFcPP(score)
            result.fcPP = pp.pp.toFloat()
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