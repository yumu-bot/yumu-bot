package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.BPService.BPParam
import com.now.nowbot.service.MessageServiceImpl.ScorePRService.Companion.getScore4PanelE5
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import com.now.nowbot.util.CmdUtil.getUserWithRange
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

@Service("BP")
class BPService(
    private var beatmapApiService: OsuBeatmapApiService,
    private var scoreApiService: OsuScoreApiService,
    private var imageService: ImageService,
) : MessageService<BPParam>, TencentMessageService<BPParam> {

    data class BPParam(val user: OsuUser?, val BPMap: Map<Int, Score>, val isMyself: Boolean)

    @Throws(Throwable::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<BPParam>,
    ): Boolean {
        val matcher = Instruction.BP.matcher(messageText)
        if (!matcher.find()) return false

        val isMultiple = StringUtils.hasText(matcher.group("s"))
        val isMyself = AtomicBoolean()
        // 处理 range
        val mode = getMode(matcher)
        val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "bp")

        val user = range.data ?: return false

        val bpMap = range.getBPMap(isMultiple, mode.data!!)

        data.value = BPParam(user, bpMap, isMyself.get())

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: BPParam) {
        val image: ByteArray = param.getImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("最好成绩：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "最好成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BPParam? {
        var matcher: Matcher
        val isMultiple: Boolean
        when {
            OfficialInstruction.BP.matcher(messageText).apply { matcher = this }.find() ->
                isMultiple = false

            OfficialInstruction.BPS.matcher(messageText).apply { matcher = this }.find() ->
                isMultiple = true

            else -> return null
        }

        val isMyself = AtomicBoolean()
        // 处理 range
        val mode = getMode(matcher)

        val range = getUserWithRange(event, matcher, mode, isMyself)

        val user = range.data ?: return null

        val bpMap = range.getBPMap(isMultiple, mode.data!!)

        return BPParam(user, bpMap, isMyself.get())
    }

    override fun reply(event: MessageEvent, param: BPParam): MessageChain? =
        QQMsgUtil.getImage(param.getImage())

    private fun CmdRange<OsuUser>.getBPMap(
        isMultiple: Boolean,
        mode: OsuMode,
    ): TreeMap<Int, Score> {
        val offset: Int
        val limit: Int

        if (isMultiple) {
            offset = getOffset(0, true)
            limit = getLimit(20, true)
        } else {
            offset = getOffset(0, false)
            limit = getLimit(1, false)
        }

        val isDefault = offset == 0 && limit == 1

        val bpList = scoreApiService.getBestPerformance(data!!.userID, mode, offset, limit)
        val bpMap = TreeMap<Int, Score>()

        // 检查查到的数据是否为空
        if (bpList.isEmpty()) {
            if (isDefault) {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_PlayerRecord,
                    mode.toString(),
                )
            } else {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_SelectedBP,
                    data!!.username ?: data!!.userID,
                    mode.toString(),
                )
            }
        }

        bpList.forEach(
            ContextUtil.consumerWithIndex { s: Score, index: Int -> bpMap[index + offset] = s }
        )
        return bpMap
    }

    fun BPParam.getImage(): ByteArray =
        try {
            if (BPMap.size > 1) {
                val ranks = ArrayList<Int>()
                val scores = ArrayList<Score?>()
                for ((key, value) in BPMap) {
                    ranks.add(key + 1)
                    scores.add(value)
                }

                beatmapApiService.applySRAndPP(scores)

                imageService.getPanelA4(user, scores, ranks)
            } else {
                var score: Score? = null

                for ((_, value) in BPMap) {
                    score = value
                }

                val e5Param = getScore4PanelE5(user!!, score!!, beatmapApiService)
                imageService.getPanelE5(e5Param)
            }
        } catch (e: Exception) {
            log.error("最好成绩：渲染失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最好成绩")
        }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPService::class.java)
    }
}
