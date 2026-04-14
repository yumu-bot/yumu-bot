package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.calculate.ETXDuelRating
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionObject
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.UserIDUtil
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("ELITE_DUEL_RATING")
class EliteronixDuelRatingService(
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
): MessageService<EliteronixDuelRatingService.ETXParam>, TencentMessageService<EliteronixDuelRatingService.ETXParam> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ETXParam>
    ): Boolean {

        val m1 = Instruction.ETX.matcher(messageText)
        val m2 = Instruction.ETX_VS.matcher(messageText)

        val isVs: Boolean

        val matcher = if (m1.find()) {
            isVs = false
            m1
        } else if (m2.find()) {
            isVs = true
            m2
        } else return false

        data.value = getParam(event, matcher, isVs)

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: ETXParam
    ): ServiceCallStatistic? {
        val image = imageService.getPanel(param, "B4")

        event.reply(image)

        return ServiceCallStatistic.builds(event, userIDs = listOfNotNull(param.me.userID, param.other?.userID))
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, isVs: Boolean = false): ETXParam {
        val mode = OsuMode.OSU
        val ids = UserIDUtil.get2UserID(event, matcher, InstructionObject(mode), isVs)

        val me: OsuUser
        val other: OsuUser?
        val my: ETXDuelRating
        val others: ETXDuelRating?

        val targetIDs = when {
            ids.first != null && ids.second != null -> listOf(ids.first!!, ids.second!!)
            ids.first != null && !isVs -> listOf(ids.first!!)
            else -> emptyList()
        }

        if (targetIDs.isNotEmpty()) {
            val results = if (targetIDs.size == 2) {
                val async = AsyncMethodExecutor.awaitQuad(
                    { userApiService.getOsuUser(targetIDs[0], mode) },
                    { userApiService.getEliteronixDuelRating(targetIDs[0]) },
                    { userApiService.getOsuUser(targetIDs[1], mode) },
                    { userApiService.getEliteronixDuelRating(targetIDs[1]) }
                )

                listOf(async.first.first to async.first.second, async.second.first to async.second.second)
            } else {
                val async = AsyncMethodExecutor.awaitPair(
                    { userApiService.getOsuUser(targetIDs[0], mode) },
                    { userApiService.getEliteronixDuelRating(targetIDs[0]) },
                )

                listOf(async.first to async.second)
            }

            me = results[0].first
            my = results[0].second
            other = results.getOrNull(1)?.first
            others = results.getOrNull(1)?.second

        } else {
            val users = InstructionUtil.get2User(event, matcher, InstructionObject(mode), isVs)

            me = users.first()
            my = userApiService.getEliteronixDuelRating(me.userID)

            other = users.getOrNull(1)
            others = other?.let { userApiService.getEliteronixDuelRating(it.userID) }
        }

        return ETXParam(me, other, my, others)
    }

    override fun accept(
        event: MessageEvent,
        messageText: String
    ): ETXParam? {
        val m1 = OfficialInstruction.ETX.matcher(messageText)
        val m2 = OfficialInstruction.ETX_VS.matcher(messageText)

        val isVs: Boolean

        val matcher = if (m1.find()) {
            isVs = false
            m1
        } else if (m2.find()) {
            isVs = true
            m2
        } else return null

        return getParam(event, matcher, isVs)
    }

    override fun reply(
        event: MessageEvent,
        param: ETXParam
    ): MessageChain? {
        val image = imageService.getPanel(param, "B4")

        return MessageChain(image)
    }

    data class ETXParam(
        val me: OsuUser,
        val other: OsuUser?,
        val my: ETXDuelRating,
        val others: ETXDuelRating?
    )
}