package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.InfoService.InfoParam
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.InstructionUtil.getMode
import com.now.nowbot.util.UserIDUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("SB_INFO")
class SBInfoService(
    private val sbUserApiService: SBUserApiService,
    private val sbScoreApiService: SBScoreApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val infoDao: OsuUserInfoDao,
    private val imageService: ImageService,
): MessageService<InfoParam> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<InfoParam>): Boolean {
        val matcher = Instruction.SB_INFO.matcher(messageText)

        if (!matcher.find()) return false

        data.value = getParam(event, matcher)
        return true
    }

    override fun handleMessage(event: MessageEvent, param: InfoParam): ServiceCallStatistic? {
        val message = param.getMessageChain()
        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("玩家信息：发送失败", e)
            throw IllegalStateException.Send("玩家信息")
        }

        return ServiceCallStatistic.build(event, userID = param.user.userID, mode = param.user.currentOsuMode)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, version: Int = 3): InfoParam {
        val isMyself = AtomicBoolean(false)

        val mode = getMode(matcher)
        val user: OsuUser
        val bests: List<LazerScore>

        val id = UserIDUtil.getSBUserIDWithoutRange(event, matcher, mode, isMyself)

        if (id != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { sbUserApiService.getUser(id) },
                { sbScoreApiService.getBestScore(
                    id = id, mode = mode.data!!) }
            )

            user = async.first?.toOsuUser(mode.data!!) ?: throw NoSuchElementException.Player(id.toString())
            bests = async.second.map { it.toLazerScore() }
        } else {
            user = InstructionUtil.getSBUserWithoutRange(event, matcher, mode, isMyself)
                .toOsuUser(mode.data!!)

            bests = sbScoreApiService.getBestScore(id = user.userID, mode = mode.data!!)
                .map { it.toLazerScore() }
        }

        AsyncMethodExecutor.awaitPairCallableExecute(
            { calculateApiService.applyBeatMapChanges(bests.take(6)) },
            { calculateApiService.applyStarToScores(bests.take(6)) }
        )

        /*
        val day = (matcher.group(FLAG_DAY) ?: "").toLongOrNull() ?: 1L

        val historyUser =
            infoDao.getLastFrom(
                user.userID,
                user.currentOsuMode,
                LocalDate.now().minusDays(day)
            )?.let { OsuUserInfoDao.fromArchive(it) }

         */

        val currentMode = OsuMode.getMode(mode.data!!, user.currentOsuMode)

        val percentiles = infoDao.getPercentiles(user, user.currentOsuMode)

        return InfoParam(user, bests, currentMode, historyUser = null, isMyself.get(), version, percentiles)
    }

    private fun InfoParam.getMessageChain(): MessageChain {
        val name: String

        when(version) {
            1 -> {
                name = "D"
            }

            2 -> {
                name = "D2"
                calculateApiService.applyStarToScores(bests.take(6))
            }

            3 -> {
                name = "D3"
                calculateApiService.applyStarToScores(bests.take(6))
            }

            else -> {
                name = "D"
            }
        }

        return try {
            MessageChain(imageService.getPanel(this.toMap(), name))
        } catch (_: NetworkException) {
            log.info("玩家信息：渲染失败")

            val avatar = sbUserApiService.getAvatarByte(user)

            // 变化不大就不去拿了
            /*
            val h = if (historyUser == null || (historyUser.pp - user.pp).absoluteValue <= 0.5) {
                null
            } else {
                historyUser
            }

             */

            UUIService.getUUInfo(user, avatar, null)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBInfoService::class.java)
    }
}