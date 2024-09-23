package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.*
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.serviceException.IMapperException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil.getUserWithOutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service("I_MAPPER")
class IMapperService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<OsuUser>, TencentMessageService<OsuUser> {

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<OsuUser>): Boolean {
        val m = Instruction.I_MAPPER.matcher(messageText)
        if (!m.find()) return false
        val mode = CmdObject(OsuMode.DEFAULT)
        val osuUser = getUserWithOutRange(event, m, mode)
        data.value = osuUser
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, osuUser: OsuUser) {
        val map = parseData(
            osuUser,
            userApiService,
            beatmapApiService
        )

        val image: ByteArray

        try {
            image = imageService.getPanelM(map)
        } catch (e: Exception) {
            log.error("谱师信息：图片渲染失败", e)
            throw IMapperException(IMapperException.Type.IM_Fetch_Error)
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("谱师信息：发送失败", e)
            throw IMapperException(IMapperException.Type.IM_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): OsuUser? {
        val matcher = OfficialInstruction.I_MAPPER.matcher(messageText)
        if (!matcher.find()) return null
        val mode = CmdObject(OsuMode.DEFAULT)
        return getUserWithOutRange(event, matcher, mode)
    }

    override fun reply(event: MessageEvent, param: OsuUser): MessageChain? {
        val map = parseData(
            param,
            userApiService,
            beatmapApiService
        )

        return QQMsgUtil.getImage(imageService.getPanelM(map))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(IMapperService::class.java)

        fun parseData(
            user: OsuUser,
            userApiService: OsuUserApiService,
            beatmapApiService: OsuBeatmapApiService
        ): Map<String, Any?> {
            var page = 1
            val query = HashMap<String, Any>()
            query["q"] = "creator=" + user.userID
            query["sort"] = "ranked_desc"
            query["s"] = "any"
            query["page"] = page

            var search: BeatMapSetSearch? = null

            //依据QualifiedMapService 的逻辑来多次获取
            run {
                var resultCount = 0
                do {
                    if (Objects.isNull(search)) {
                        search = beatmapApiService.searchBeatMapSet(query)
                        resultCount += search!!.beatmapSets.size
                    } else {
                        page++
                        query["page"] = page
                        val result = beatmapApiService.searchBeatMapSet(query)
                        resultCount += result.beatmapSets.size
                        search!!.beatmapSets.addAll(result.beatmapSets)
                    }
                } while (resultCount < search!!.total && page < 10)
            }

            val result = search!!.beatmapSets

            val activity: List<ActivityEvent>
            val mappingActivity: MutableList<ActivityEvent> = ArrayList()

            try {
                activity = userApiService.getUserRecentActivity(user.userID, 0, 100)

                activity.filter { it.isMapping }.forEach {
                    if (mappingActivity.isEmpty()) {
                        mappingActivity += it
                        return@forEach
                    }
                    if (it == mappingActivity.last()) return@forEach
                    mappingActivity += it
                }
            } catch (ignored: NoSuchElementException) {
            } catch (e: Exception) {
                log.error("谱师信息：筛选出错", e)
            }

            val mostPopularBeatmap = result
                .filter { it.creatorID == user.userID }
                .sortedByDescending { it.playCount }
                .take(6)

            var mostRecentRankedBeatmap = result
                .find { it.hasLeaderBoard() && user.userID == it.creatorID }

            if (Objects.isNull(mostRecentRankedBeatmap) && user.rankedCount > 0) {
                try {
                    val newQuery = HashMap<String, Any>()
                    newQuery["q"] = user.userID.toString()
                    newQuery["sort"] = "ranked_desc"
                    newQuery["s"] = "any"
                    newQuery["page"] = 1

                    val newSearch = beatmapApiService.searchBeatMapSet(newQuery)
                    mostRecentRankedBeatmap = newSearch.beatmapSets.find { it.hasLeaderBoard() }
                } catch (ignored: Exception) {
                }
            }

            val mostRecentRankedGuestDiff = result
                .find { it.hasLeaderBoard() && user.userID != it.creatorID }

            val beatMapSum =
                search!!.beatmapSets.flatMap { it.beatMaps ?: emptyList() }

            val diffArr = IntArray(8)
            run {
                val diffStar =
                    beatMapSum.filter { it.mapperID == user.userID }
                        .map { it.starRating }
                        .toFloatArray()
                val starMaxBoundary = doubleArrayOf(2.0, 2.8, 4.0, 5.3, 6.5, 8.0, 10.0, Double.MAX_VALUE)
                for (d in diffStar) {
                    for (i in 0..7) {
                        if (d <= starMaxBoundary[i]) {
                            diffArr[i]++
                            break
                        }
                    }
                }
            }

            var genre: IntArray
            run {
                val keywords = arrayOf(
                    "unspecified",
                    "video game",
                    "anime",
                    "rock",
                    "pop",
                    "other",
                    "novelty",
                    "hip hop",
                    "electronic",
                    "metal",
                    "classical",
                    "folk",
                    "jazz"
                )
                genre = IntArray(keywords.size)
                val hasAnyGenre = AtomicBoolean(false)

                //逻辑应该是先每张图然后再遍历12吧？
                if (!CollectionUtils.isEmpty(search!!.beatmapSets)) {
                    search!!.beatmapSets.forEach {
                        for (i in 1 until keywords.size) {
                            val keyword = keywords[i]

                            if (it.tags.lowercase(Locale.getDefault()).contains(keyword)) {
                                genre[i]++
                                hasAnyGenre.set(true)
                            }
                        }
                        //0是实在找不到 tag 的时候所赋予的默认值
                        if (!hasAnyGenre.get()) {
                            genre[0]++
                        }
                        hasAnyGenre.set(false)
                    }
                }
            }

            var favorite = 0
            var playcount = 0
            if (!CollectionUtils.isEmpty(search!!.beatmapSets)) {
                for (i in search!!.beatmapSets.indices) {
                    val v = search!!.beatmapSets[i]

                    if (v.creatorID == user.userID) {
                        favorite += v.favouriteCount
                        playcount += v.playCount.toInt()
                    }
                }
            }

            val lengthArr = IntArray(8)
            run {
                val lengthAll =
                    beatMapSum.filter { it.mapperID == user.userID }
                        .map { it.totalLength }
                        .toIntArray()
                val lengthMaxBoundary = intArrayOf(60, 100, 140, 180, 220, 260, 300, Int.MAX_VALUE)
                for (f in lengthAll) {
                    for (i in 0..7) {
                        if (f <= lengthMaxBoundary[i]) {
                            lengthArr[i]++
                            break
                        }
                    }
                }
            }

            val body = HashMap<String, Any?>()

            body["user"] = user
            body["most_popular_beatmap"] = mostPopularBeatmap
            body["most_recent_ranked_beatmap"] = mostRecentRankedBeatmap
            body["most_recent_ranked_guest_diff"] = mostRecentRankedGuestDiff
            body["difficulty_arr"] = diffArr
            body["length_arr"] = lengthArr
            body["genre"] = genre
            body["recent_activity"] = mappingActivity
            body["favorite"] = favorite
            body["playcount"] = playcount

            return body
        }
    }
}
