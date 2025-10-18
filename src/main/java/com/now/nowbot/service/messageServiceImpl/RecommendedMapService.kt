package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import java.util.concurrent.Callable

//@Service("RECOMMEND_MAP")
class RecommendedMapService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService
): MessageService<OsuUser> {

    enum class Interests {
        DEFAULT, PERFORMANCE,
    }

    data class UserPrefer(
        val bests: List<LazerScore> = listOf(),
        val favorite: List<Beatmapset> = listOf(),
    )

    data class TagWithPrefer(
        val prefer: Double,
        val tag: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TagWithPrefer

            return tag == other.tag
        }

        override fun hashCode(): Int {
            return tag.hashCode()
        }
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<OsuUser>): Boolean {
        val matcher = Instruction.RECOMMEND.matcher(messageText)

        if (!matcher.find()) return false

        val mode = CmdUtil.getMode(matcher)
        val user = CmdUtil.getUserWithoutRange(event, matcher, mode)

        data.value = user

        return true
    }

    override fun handleMessage(event: MessageEvent, param: OsuUser): ServiceCallStatistic? {
        val p = getUserPrefer(param)

        event.reply("正常获取！${p.favorite.size} ${p.bests.size}")

        return ServiceCallStatistic.building(event)
    }

    private fun getUserPrefer(user: OsuUser): UserPrefer {
        val favorite: List<Beatmapset>

        val tasks = (0..4).toList().map {
            Callable {
                beatmapApiService.getUserBeatmapset(user.userID, "favourite", it * 100, 100)
            }
        }

        favorite = AsyncMethodExecutor.awaitCallableExecute(tasks).flatten()

        val bests: List<LazerScore> = scoreApiService.getBestScores(user)

        return UserPrefer(bests, favorite)
    }
}