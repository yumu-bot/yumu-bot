package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.CoverType
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
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Matcher
import kotlin.math.*

@Service("BP_FIX")
class BPFixService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
) : MessageService<BPFixParam>, TencentMessageService<BPFixParam> {

    data class BPFixParam(val user: OsuUser, val bests: Map<Int, LazerScore>, val mode: OsuMode, val page: Int = 1)

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<BPFixParam>): Boolean {
        val matcher = Instruction.BP_FIX.matcher(messageText)
        if (!matcher.find()) return false

        data.value = getParam(event, matcher)
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: BPFixParam): ServiceCallStatistic? {
        val image = param.getImage()
        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("理论最好成绩：发送失败", e)
            throw IllegalStateException.Send("理论最好成绩")
        }

        val scores = param.bests.toList()

        return ServiceCallStatistic.builds(
            event,
            beatmapIDs = scores.map { it.second.beatmapID },
            beatmapsetIDs = scores.map { it.second.beatmapset.beatmapsetID },
            userIDs =  listOf(param.user.userID),
            modes = listOf(param.mode)
        )
    }

    override fun accept(event: MessageEvent, messageText: String): BPFixParam? {
        val matcher = OfficialInstruction.BP_FIX.matcher(messageText)
        if (!matcher.find()) return null

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: BPFixParam): MessageChain? = MessageChain(param.getImage())

    private fun getParam(event: MessageEvent, matcher: Matcher): BPFixParam {
        val isMyself = AtomicBoolean(true)
        val mode = getMode(matcher)

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself)

        val user: OsuUser
        val scores: Map<Int, LazerScore>
        val page: Int

        // 高效的获取方式
        if (id.data != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(id.data!!, mode.data!!) },
                { scoreApiService.getBestScores(id.data!!, mode.data)
                    .mapIndexed { i, it ->
                        (i + 1) to it
                    }.toMap() }
            )

            user = async.first
            scores = async.second
            page = id.start ?: 1
        } else {
            // 经典的获取方式
            val range = CmdUtil.getUserWithRange(event, matcher, mode, isMyself)

            user = range.data!!

            val bests = scoreApiService.getBestScores(user.userID, mode.data)

            scores = bests.mapIndexed { i, it -> (i + 1) to it }.toMap()

            page = range.start ?: 1
        }

        if (scores.isEmpty()) {
            throw NoSuchElementException.BestScore(user.username)
        }

        return BPFixParam(user, scores, mode.data!!, page)
    }

    private fun fix(user: OsuUser, bestsMap: Map<Int, LazerScore>): Pair<List<LazerScoreWithFcPP>, Double> {
        if (bestsMap.isEmpty()) {
            throw NoSuchElementException.BestScoreWithMode(user.username, user.currentOsuMode)
        }

        val playerPP = user.pp
        val beforeBpSumAtomic = AtomicReference(0.0)

        AsyncMethodExecutor.awaitPairCallableExecute(
            { beatmapApiService.applyBeatmapExtendFromDatabase(bestsMap.map { it.value }) },
            { calculateApiService.applyStarToScores(bestsMap.map { it.value }) }
        )

        val tasks = bestsMap.map { entry ->
            val index = entry.key
            val score = entry.value

            beforeBpSumAtomic.updateAndGet { it + (score.weight?.pp ?: 0.0) }

            AsyncMethodExecutor.Supplier {
                val max = score.beatmap.maxCombo ?: 1
                val combo = score.maxCombo
                val stat = score.statistics
                val ok = stat.ok
                val meh = stat.meh
                val miss = stat.miss

                /**
                 * 断连击
                 * mania 模式现在也可以参与此项筛选
                 * catch 的 choke 会被归纳到 has1pMiss 中
                 */
                val isChoke = when(score.mode) {
                    OsuMode.MANIA -> (ok + meh + miss) / max <= 0.03
                    OsuMode.CATCH_RELAX, OsuMode.CATCH -> false
                    else -> (miss == 0) && (combo < (max * 0.98).roundToInt())
                }

                // 含有 <1% 的失误
                val has1pMiss = (miss > 0) && ((1.0 * miss / max) <= 0.01)

                // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
                if (isChoke || has1pMiss) {
                    initFixScore(score, index)
                } else {
                    score
                }
            }
        }

        val fixedBests = AsyncMethodExecutor.awaitSupplierExecute(tasks)
            .sortedByDescending {
            if (it is LazerScoreWithFcPP && it.fcPP > 0) {
                it.fcPP
            } else {
                it.pp
            }
        }

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

        val scores = fixedBests.filterIsInstance<LazerScoreWithFcPP>()

        if (scores.isEmpty()) throw NoSuchElementException.BestScoreTheoretical()

        AsyncMethodExecutor.asyncRunnableExecute {
            scoreApiService.asyncDownloadBackgroundFromScores(scores, listOf(CoverType.LIST, CoverType.COVER))
        }

        return scores to newPlayerPP
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
        val fixes = fix(user, bests)

        // 分页
        val split = DataUtil.splitPage(fixes.first, this.page)

        val body = mapOf(
            "user" to user,
            "scores" to split.first,
            "pp" to fixes.second,
            "page" to split.second,
            "max_page" to split.third
        )

        return imageService.getPanel(body, "A7")
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(BPFixService::class.java)
    }
}