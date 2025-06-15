package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.model.osu.ActivityEvent
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botException.IMapperException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

@Service("I_MAPPER") class IMapperService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<OsuUser>, TencentMessageService<OsuUser> {

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<OsuUser>): Boolean {
        val matcher = Instruction.I_MAPPER.matcher(messageText)
        if (!matcher.find()) return false

        val mode = CmdObject(OsuMode.DEFAULT)
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

        val user: OsuUser = if (id != null) {
            userApiService.getOsuUser(id, mode.data!!)
        } else {
            getUserWithoutRange(event, matcher, mode)
        }

        data.value = user
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: OsuUser) {
        val map = getIMapperV1(param, userApiService, beatmapApiService)

        val image: ByteArray = try {
            imageService.getPanel(map, "M")
        } catch (e: Exception) {
            log.error("谱师信息：渲染失败", e)
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
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

        val user: OsuUser = if (id != null) {
            userApiService.getOsuUser(id, mode.data!!)
        } else {
            getUserWithoutRange(event, matcher, mode)
        }

        return user
    }

    override fun reply(event: MessageEvent, param: OsuUser): MessageChain? {
        val body = getIMapperV1(param, userApiService, beatmapApiService)

        return MessageChainBuilder().addImage(imageService.getPanel(body, "M")).build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(IMapperService::class.java)

        fun getIMapperV2(user: OsuUser, userApiService: OsuUserApiService, beatmapApiService: OsuBeatmapApiService): Map<String, Any> {
            val query = mapOf(
                "q" to "creator=" + user.userID,
                "sort" to "ranked_desc",
                "s" to "any",
                "page" to 1
            )

            val search = beatmapApiService.searchBeatmapset(query, 10)
            val result1 = search.beatmapSets

            // 这个是补充可能存在的，谱面所有难度都标注了难度作者时，上一个查询会漏掉的谱面
            val query2 = mapOf(
                "q" to user.userID,
                "sort" to "ranked_desc",
                "s" to "any",
                "page" to 1
            )

            val search2 = beatmapApiService.searchBeatmapset(query2, 10)
            val result2 = search2.beatmapSets
                .filter { it.beatmapsetID != user.userID && (it.beatmaps?.all { that -> that.beatmapID != user.id } ?: true) }

            val relatedSets = (result1.toHashSet() + result2.toHashSet()).asSequence()

            val activity: List<ActivityEvent> =
                userApiService.getUserRecentActivity(user.userID, 0, 100).filter { it.isMapping }

            val recentActivity: List<ActivityEvent> = try {
                activity.mapIndexed { i, it ->
                    val before = activity[max(0, i - 1)]

                    if (it != before || i == 0) {
                        it
                    } else {
                        null
                    }
                }.filterNotNull()
            } catch (ignored: NoSuchElementException) {
                listOf()
            } catch (e: Exception) {
                log.error("谱师信息：筛选出错", e)
                listOf()
            }

            val relatedDiffs = relatedSets.map { it.beatmaps!! }.flatten()

            val mySets = relatedSets.filter { it.creatorID == user.userID }
            val otherSets = relatedSets.filter { it.creatorID != user.userID }

            val myDiffs = relatedDiffs.filter { it.mapperID == user.userID } // 包括了别人 Set 里的
            val myGuestDiffs = relatedDiffs.filter { it.mapperID == user.userID && it.beatmapset?.creatorID != user.userID }
            val guestDiffs = relatedDiffs.filter { it.mapperID != user.userID && it.beatmapset?.creatorID == user.userID }

            val relatedUsers = run {
                val idChunk = relatedSets.filter { it.creatorID != user.userID }.map { it.creatorID }.toSet().chunked(50)

                val actions = idChunk.map {
                    return@map AsyncMethodExecutor.Supplier<List<MicroUser>> {
                        userApiService.getUsers(it, false)
                    }
                }

                AsyncMethodExecutor.awaitSupplierExecute(actions).filterNotNull().flatten()
            }

            val guestDifficultyOwners = relatedUsers.map { u ->
                val re = guestDiffs.filter { it.mapperID == u.userID }

                val received: Int = re.count()
                val receivedRanked: Int = re.count { it.ranked > 0 }

                val se = myGuestDiffs.filter { it.beatmapset?.creatorID == u.userID }

                val sent: Int = se.count()

                val sentRanked: Int = se.count { it.ranked > 0 }

                GuestDifficultyService.GuestDifficultyOwner(u, received, receivedRanked, sent, sentRanked)
            }.sortedByDescending {
                it.sent + it.received
            }.sortedByDescending {
                it.sentRanked + it.receivedRanked
            }.take(7)

            val mostPopularBeatmap = mySets
                .sortedByDescending { it.playCount }
                .sortedByDescending { it.ranked }
                .take(6)

            val genreKeys = arrayOf(
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

            val genre = findKey(genreKeys, myDiffs)

            val languageKeys = arrayOf(
                "unspecified",
                "english",
                "chinese",
                "french",
                "german",
                "italian",
                "japanese",
                "korean",
                "spanish",
                "swedish",
                "russian",
                "polish",
                "instrumental",
                "other"
            )

            val language = findKey(languageKeys, myDiffs)

            val lengthArr = IntArray(8)

            run {
                val lengthMaxBoundary = intArrayOf(60, 100, 140, 180, 220, 260, 300, Int.MAX_VALUE)

                for (f in myDiffs.map { it.totalLength }) {
                    for (i in lengthMaxBoundary.indices) {
                        if (f <= lengthMaxBoundary[i]) {
                            lengthArr[i]++
                            break
                        }
                    }
                }
            }

            val diffArr = IntArray(8)

            run {
                val starMaxBoundary = doubleArrayOf(2.0, 2.8, 4.0, 5.3, 6.5, 8.0, 10.0, Double.MAX_VALUE)

                for (d in myDiffs.map { it.starRating }) {
                    for (i in starMaxBoundary.indices) {
                        if (d <= starMaxBoundary[i]) {
                            diffArr[i]++
                            break
                        }
                    }
                }
            }

            val mostRecentRankedBeatmap = mySets
                .filter { it.ranked > 0 }
                .sortedByDescending { it.rankedDate?.toEpochSecond() }
                .take(2)

            val mostRecentRankedGuestDiff = otherSets
                .filter { it.ranked > 0 }
                .sortedByDescending { it.rankedDate?.toEpochSecond() }
                .take(2)


            val favorite = mySets.sumOf { it.favouriteCount }
            val playcount = myDiffs.sumOf { it.playCount }

            return mapOf(
                "user" to user,
                "most_popular_beatmap" to mostPopularBeatmap,
                "genre" to genre,
                "language" to language,
                "difficulty_arr" to diffArr,
                "length_arr" to lengthArr,
                "recent_activity" to recentActivity,

                "most_recent_ranked_beatmap" to mostRecentRankedBeatmap,
                "most_recent_ranked_guest_diff" to mostRecentRankedGuestDiff,

                "favorite" to favorite,
                "playcount" to playcount,

                "guest_differs" to guestDifficultyOwners,

                "total_gds" to myGuestDiffs.count(),
                "total_diffs" to myDiffs.count(),
                )
        }

        fun getIMapperV1(
            user: OsuUser, userApiService: OsuUserApiService, beatmapApiService: OsuBeatmapApiService
        ): Map<String, Any?> {
            val query = mapOf(
                "q" to "creator=" + user.userID, "sort" to "ranked_desc", "s" to "any", "page" to 1
            )

            val search = beatmapApiService.searchBeatmapset(query, 10)
            val result1 = search.beatmapSets

            // 这个是补充可能存在的，谱面所有难度都标注了难度作者时，上一个查询会漏掉的谱面
            val query2 = mapOf(
                "q" to user.userID, "sort" to "ranked_desc", "s" to "any", "page" to 1
            )

            val search2 = beatmapApiService.searchBeatmapset(query2, 10)
            val result2 = search2.beatmapSets
                .filter { it.beatmapsetID != user.userID && (it.beatmaps?.all { that -> that.beatmapID != user.id } ?: true) }

            val result = (result1.toHashSet() + result2.toHashSet()).asSequence()

            val activity: List<ActivityEvent> =
                userApiService.getUserRecentActivity(user.userID, 0, 100).filter { it.isMapping }

            val mappingActivity: List<ActivityEvent> = try {
                activity.mapIndexed { i, it ->
                    val before = activity[max(0, i - 1)]

                    if (it != before || i == 0) {
                        it
                    } else {
                        null
                    }
                }.filterNotNull()
            } catch (ignored: NoSuchElementException) {
                listOf()
            } catch (e: Exception) {
                log.error("谱师信息：筛选出错", e)
                listOf()
            }

            val mostPopularBeatmap =
                result.filter { it.creatorID == user.userID }
                    .sortedByDescending { it.playCount }
                    .sortedByDescending { it.ranked }
                    .take(6)

            val mostRecentRankedBeatmap = result.sortedByDescending { it.rankedDate?.toEpochSecond() }
                .find { it.hasLeaderBoard && user.userID == it.creatorID }

            val mostRecentRankedGuestDiff = result.sortedByDescending { it.rankedDate?.toEpochSecond() }
                .find { it.hasLeaderBoard && user.userID != it.creatorID }

            val beatmaps = result.flatMap { it.beatmaps ?: emptyList() }

            val diffArr = IntArray(8)

            run {
                val diffStar = beatmaps.filter { it.mapperID == user.userID }.map { it.starRating }
                val starMaxBoundary = doubleArrayOf(2.0, 2.8, 4.0, 5.3, 6.5, 8.0, 10.0, Double.MAX_VALUE)

                for (d in diffStar) {
                    for (i in starMaxBoundary.indices) {
                        if (d <= starMaxBoundary[i]) {
                            diffArr[i]++
                            break
                        }
                    }
                }
            }

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

            val genre = IntArray(keywords.size)

            run {
                val hasAnyGenre = AtomicBoolean(false)

                //逻辑应该是先每张图然后再遍历12吧？
                result.forEach {
                    for (i in 1 until keywords.size) {
                        val keyword = keywords[i]

                        if ((it.tags ?: "").lowercase().contains(keyword)) {
                            genre[i]++
                            hasAnyGenre.set(true)
                        }
                    } //0是实在找不到 tag 的时候所赋予的默认值
                    if (!hasAnyGenre.get()) {
                        genre[0]++
                    }
                    hasAnyGenre.set(false)
                }
            }

            val favorite = result.filter { it.creatorID == user.userID }.sumOf { it.favouriteCount }
            val playcount = result.filter { it.creatorID == user.userID }.sumOf { it.playCount }

            val lengthArr = IntArray(8)

            run {
                val lengthAll = beatmaps.filter { it.mapperID == user.userID }.map { it.totalLength }
                val lengthMaxBoundary = intArrayOf(60, 100, 140, 180, 220, 260, 300, Int.MAX_VALUE)

                for (f in lengthAll) {
                    for (i in lengthMaxBoundary.indices) {
                        if (f <= lengthMaxBoundary[i]) {
                            lengthArr[i]++
                            break
                        }
                    }
                }
            }

            return mapOf(
                "user" to user,
                "most_popular_beatmap" to mostPopularBeatmap,
                "most_recent_ranked_beatmap" to mostRecentRankedBeatmap,
                "most_recent_ranked_guest_diff" to mostRecentRankedGuestDiff,
                "difficulty_arr" to diffArr,
                "length_arr" to lengthArr,
                "genre" to genre,
                "recent_activity" to mappingActivity,
                "favorite" to favorite,
                "playcount" to playcount,
            )
        }

        private fun findKey(keys: Array<String>, maps: Sequence<Beatmap>): IntArray {
            val result = IntArray(keys.size)
            val hasAnyGenre = AtomicBoolean(false)

            //逻辑应该是先每张图然后再遍历12吧？
            maps.forEach {
                val str = it.beatmapset?.tags?.lowercase() ?: ""

                keys.forEachIndexed { i, keyword ->
                    if (str.contains(keyword, ignoreCase = true)) {
                        result[i]++

                        if (i > 0) {
                            hasAnyGenre.set(true)
                        }
                    }
                }

                //0是实在找不到 tag 的时候所赋予的默认值
                if (!hasAnyGenre.get()) {
                    result[0] ++
                }

                hasAnyGenre.set(false)
            }

            return result
        }
    }
}
