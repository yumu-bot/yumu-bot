package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

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

    override fun HandleMessage(event: MessageEvent, param: OsuUser) {
        val p = getUserPrefer(param)

        event.reply("正常获取！${p.favorite.size} ${p.bests.size}")
    }

    private fun getUserPrefer(user: OsuUser): UserPrefer {
        var bests: List<LazerScore>
        var favorite: List<Beatmapset>

        val b = scope.async {
            scoreApiService.getBestScores(user, 0, 200)
        }

        val d = scope.async {
            beatmapApiService.getUserBeatmapset(user.userID, "favourite", 0, 100)
        }

        val d2 = scope.async {
            beatmapApiService.getUserBeatmapset(user.userID, "favourite", 100, 100)
        }

        val d3 = scope.async {
            beatmapApiService.getUserBeatmapset(user.userID, "favourite", 200, 100)
        }

        runBlocking {
            bests = b.await()
            favorite = d.await() + d2.await() + d3.await()
        }

        return UserPrefer(bests, favorite)
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(6))
    }
}