package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.UserIDUtil
import com.now.nowbot.util.command.FLAG_NAME
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher

@Service("GUESS")
class GuessService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService
): MessageService<GuessService.GuessParam> {

    companion object {
        private val GUESSING = ConcurrentHashMap<Long, GuessInfo>()
    }

    sealed class GuessParam

    class GuessStartParam(
        val user: OsuUser,
        val beatmaps: List<Beatmap>,
    ): GuessParam()

    class GuessEndParam: GuessParam()

    class GuessOpenParam(val char: Char): GuessParam()

    class GuessingParam(val result: String): GuessParam()

    data class GuessInfo(
        val letters: Set<Char>,
        val expired: OffsetDateTime,
        val user: OsuUser,
        val beatmaps: List<Beatmap>,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<GuessParam>
    ): Boolean {
        val matcher = Instruction.GUESS.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val isGuessing = GUESSING.keys.contains(event.subject.contactID)

        data.value = getParam(event, matcher, isGuessing)
        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: GuessParam
    ): ServiceCallStatistic? {
        val groupID = event.subject.contactID

        when(param) {
            is GuessingParam -> {
                val info = GUESSING[groupID] ?: throw RuntimeException("猜歌异常：没有找到这个猜歌信息。")

                val similarity = DataUtil.getStringSimilarity(param.result, "beatmap title")
            }

            is GuessStartParam -> {
                if (GUESSING.keys.contains(groupID)) {
                    throw RuntimeException("群聊正在猜歌，请等待当前的猜歌流程结束。")
                }

                event.reply("猜歌开始")
            }

            is GuessEndParam -> {
                event.reply("猜歌结束")
            }

            is GuessOpenParam -> {
                val image = imageService.getPanel(param, "A8")

                event.reply(image)
            }
        }

        return null
    }

    fun getParam(event: MessageEvent, matcher: Matcher, isGuessing: Boolean = false): GuessParam {
        if (!isGuessing) {
            return getStartParam(event, matcher)
        }

        return getOtherParam(matcher)
    }

    fun getStartParam(event: MessageEvent, matcher: Matcher): GuessParam {
        val mode = InstructionUtil.getMode(matcher)

        val userID = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

        if (userID != null) {
            val (user, beatmaps) = AsyncMethodExecutor.awaitPair(
                { userApiService.getOsuUser(userID, mode.data!!) },
                { scoreApiService.getBestScores(userID, mode.data!!).map { it.beatmap } },
            )

            return GuessStartParam(user, beatmaps)
        } else {
            val user = InstructionUtil.getUserWithoutRange(event, matcher, mode)
            val beatmaps = scoreApiService.getBestScores(user.userID, mode.data!!).map { it.beatmap }

            return GuessStartParam(user, beatmaps)
        }
    }

    fun getOtherParam(matcher: Matcher): GuessParam {
        val anything = (matcher.group(FLAG_NAME) ?: "").trim()

        return if (anything.isEmpty()) {
            GuessEndParam()
        } else if (anything.length == 1) {
            GuessOpenParam(anything[0])
        } else {
            GuessingParam(anything)
        }
    }
}