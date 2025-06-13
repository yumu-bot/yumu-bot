package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerScoreWithFcPP
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPFixService.BPFixParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.pow
import kotlin.math.roundToInt

@Service("BP_FIX")
class BPFixService(
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
) : MessageService<BPFixParam>, TencentMessageService<BPFixParam> {

    data class BPFixParam(val user: OsuUser, val bpMap: Map<Int, LazerScore>, val mode: OsuMode)

    companion object {
        val log: Logger = LoggerFactory.getLogger(BPFixService::class.java)
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<BPFixParam>): Boolean {
        val matcher = Instruction.BP_FIX.matcher(messageText)
        if (!matcher.find()) return false

        val mode = getMode(matcher)

        val user = getUserWithoutRange(event, matcher, mode)

        val bests100 = scoreApiService.getBestScores(user.userID, mode.data)
        val bests200 = scoreApiService.getBestScores(user.userID, mode.data, 100, 100)
        val bestsMap = listOf(bests100, bests200).flatten()
            .mapIndexed { i, it -> (i + 1) to it }.toMap()

        data.value = BPFixParam(user, bestsMap, mode.data!!)

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: BPFixParam) {
        val image = param.getImage()
        try {
            event.reply(image)
        } catch (e: java.lang.Exception) {
            log.error("理论最好成绩：发送失败", e)
            throw IllegalStateException.Send("理论最好成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BPFixParam? {
        val matcher = OfficialInstruction.BP_FIX.matcher(messageText)
        if (!matcher.find()) return null

        val mode = getMode(matcher)
        val user = getUserWithoutRange(event, matcher, mode)

        val bests100 = scoreApiService.getBestScores(user.userID, mode.data)
        val bests200 = scoreApiService.getBestScores(user.userID, mode.data, 100, 100)
        val bestsMap = listOf(bests100, bests200).flatten()
            .mapIndexed { i, it -> (i + 1) to it }.toMap()

        return BPFixParam(user, bestsMap, mode.data!!)
    }

    override fun reply(event: MessageEvent, param: BPFixParam): MessageChain? = QQMsgUtil.getImage(param.getImage())

    fun fix(playerPP: Double, bestsMap: Map<Int, LazerScore>): Map<String, Any>? {
        val beforeBpSumAtomic = AtomicReference(0.0)

        val actions = (bestsMap.toList()).map { pair ->
            return@map AsyncMethodExecutor.Supplier<LazerScore?> {
                val index = pair.first
                val score = pair.second

                beforeBpSumAtomic.updateAndGet { it + (score.weight?.pp ?: 0.0) }
                beatmapApiService.applyBeatMapExtendFromDataBase(score)

                val max = score.beatmap.maxCombo ?: 1
                val combo = score.maxCombo
                val stat = score.statistics
                val ok = stat.ok
                val meh = stat.meh
                val miss = stat.miss

                // 断连击，mania 模式现在也可以参与此项筛选
                val isChoke = if (score.mode == OsuMode.MANIA) {
                    (ok + meh + miss) / max <= 0.03
                } else {
                    (miss == 0) && (combo < (max * 0.98).roundToInt())
                }

                // 含有 <1% 的失误
                val has1pMiss = (miss > 0) && ((1.0 * miss / max) <= 0.01)

                // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
                return@Supplier if (isChoke || has1pMiss) {
                    initFixScore(score, index)
                } else {
                    score
                }
            }
        }

        val a = AsyncMethodExecutor.awaitSupplierExecute(actions, Duration.ofMinutes(10))
        Thread.sleep(60 * 1000)

        val fixedBests =
            a
                .filterNotNull()
                .sortedByDescending {
                val pp = if (it is LazerScoreWithFcPP && it.fcPP > 0) {
                    it.fcPP
                } else {
                    it.pp
                }

                pp
            }

        /*
        val fixedBests = bestsMap.map { (index, score) ->
            beforeBpSumAtomic.updateAndGet { it + (score.weight?.pp ?: 0.0) }
            beatmapApiService.applyBeatMapExtendFromDataBase(score)

            val max = score.beatmap.maxCombo ?: 1
            val combo = score.maxCombo
            val stat = score.statistics
            val ok = stat.ok
            val meh = stat.meh
            val miss = stat.miss

            // 断连击，mania 模式现在也可以参与此项筛选
            val isChoke = if (score.mode == OsuMode.MANIA) {
                (ok + meh + miss) / max <= 0.03
            } else {
                (miss == 0) && (combo < (max * 0.98).roundToInt())
            }

            // 含有 <1% 的失误
            val has1pMiss = (miss > 0) && ((1.0 * miss / max) <= 0.01)

            // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
            if (isChoke || has1pMiss) {
                initFixScore(score, index)
            } else {
                score
            }
        }.sortedByDescending {
            val pp = if (it is LazerScoreWithFcPP && it.fcPP > 0) {
                it.fcPP
            } else {
                it.pp
            }

            pp * 100.0
        }

         */

        val afterBpSumAtomic = AtomicReference(0.0)

        // 这里的 i 是重排过后的，从 0 开始
        fixedBests.forEachIndexed { index, score ->
            val weight: Double = 0.95.pow(index)
            val pp: Double
            if (score is LazerScoreWithFcPP) {
                pp = score.fcPP
                score.indexAfter = index + 1
            } else {
                pp = score.pp
            }
            afterBpSumAtomic.updateAndGet { it + (weight * pp) }
        }

        val beforeBpSum = beforeBpSumAtomic.get()
        val afterBpSum = afterBpSumAtomic.get()
        val newPlayerPP = (playerPP + afterBpSum - beforeBpSum)

        val scoreList = fixedBests.filterIsInstance<LazerScoreWithFcPP>()

        if (scoreList.isEmpty()) return null

        calculateApiService.applyStarToScores(scoreList)

        return mapOf(
            "scores" to scoreList,
            "pp" to newPlayerPP
        )
    }

    private fun initFixScore(score: LazerScore, index: Int): LazerScoreWithFcPP {
        val result = LazerScoreWithFcPP.copyOf(score)
        result.index = index
        try {
            val pp = calculateApiService.getScoreFullComboPP(score)
            result.fcPP = pp.pp
        } catch (e: Exception) {
            log.error("bp 计算 pp 出错:", e)
        }
        return result
    }

    private fun BPFixParam.getImage(): ByteArray {
        if (bpMap.isEmpty()) {
            throw NoSuchElementException.BestScoreWithMode(this.user.username, this.mode)
        }

        val fixes = fix(user.pp, bpMap) ?: throw NoSuchElementException.BestScoreTheoretical()

        val body = mapOf(
            "user" to user,
            "scores" to fixes["scores"]!!,
            "pp" to fixes["pp"]!!
        )

        return imageService.getPanel(body, "A7")
    }
}