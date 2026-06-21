package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuGenre
import com.now.nowbot.model.enums.OsuLanguage
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.model.osu.ActivityEvent
import com.now.nowbot.model.osu.ActivityEvent.Companion.filterIsMapping
import com.now.nowbot.model.osu.ActivityEvent.Companion.squash
import com.now.nowbot.model.osu.Beatmap
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
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("I_MAPPER") class IMapperService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<IMapperService.IMapperParam>, TencentMessageService<IMapperService.IMapperParam> {

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<IMapperParam>): Boolean {
        val matcher = Instruction.I_MAPPER.matcher(messageText)
        if (!matcher.find()) return false

        data.value = getIMapperParam(event, matcher, userApiService, beatmapApiService)
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: IMapperParam): ServiceCallStatistic? {
        val map = getIMapperV2(param)

        val image: ByteArray = try {
            imageService.getPanel(map, "M2")
        } catch (e: Exception) {
            log.error("谱师信息：渲染失败", e)
            throw IllegalStateException.Render("谱师信息")
        }

        event.replyAsync(image, { e ->
            log.error("谱师信息：发送失败", e)
        })

        return ServiceCallStatistic.build(event, userID = param.user.userID)
    }

    override fun accept(event: MessageEvent, messageText: String): IMapperParam? {
        val matcher = OfficialInstruction.I_MAPPER.matcher(messageText)
        if (!matcher.find()) return null

        return getIMapperParam(event, matcher, userApiService, beatmapApiService)
    }

    override fun reply(event: MessageEvent, param: IMapperParam): MessageChain? {
        val map = getIMapperV2(param)

        return MessageChainBuilder().addImage(imageService.getPanel(map, "M2")).build()
    }

    data class IMapperParam(
        val user: OsuUser,
        val relatedSets: Sequence<Beatmapset>,
        val activities: List<ActivityEvent>,
    )


    internal data class RecentlyRankedBeatmap(
        @field:JsonProperty("id")
        val beatmapID: Long,

        @field:JsonProperty("difficulty_rating")
        val starRating: Double,
    ) {
        constructor(beatmap: Beatmap) : this(
            beatmapID = beatmap.beatmapID,
            starRating = beatmap.starRating
        )
    }

    internal data class RecentlyRankedBeatmapset(

        @field:JsonProperty("type")
        val type: String = "host",

        @field:JsonProperty("id")
        val beatmapsetID: Long,

        @field:JsonProperty("title")
        val title: String,

        @field:JsonProperty("covers")
        val covers: Covers,

        @field:JsonProperty("creator")
        var creator: String = "",

        @field:JsonProperty("beatmaps")
        val beatmaps: List<RecentlyRankedBeatmap>,

        @field:JsonProperty(value = "ranked_date")
        val rankedDate: OffsetDateTime? = null
    ) {
        constructor(set: Beatmapset, type: String = "host") : this(
            type = type,
            beatmapsetID = set.beatmapsetID,
            title = set.title,
            covers = set.covers,
            creator = set.creator,
            beatmaps = set.beatmaps.orEmpty().sortedBy { it.starRating }.map { RecentlyRankedBeatmap(it) },
            rankedDate = set.rankedDate
        )
    }

    internal data class MostPopularBeatmapset(

        @field:JsonProperty("id")
        val beatmapsetID: Long,

        @field:JsonProperty("covers")
        val covers: Covers = Covers(),

        @field:JsonProperty("play_count")
        val playCount: Long = 0,

        @field:JsonProperty("favourite_count")
        val favouriteCount: Long = 0,

        @field:JsonProperty("ranked")
        val ranked: Byte = 0,

        @field:JsonProperty("rating")
        val rating: Float = 0f,

    ) {
        constructor(set: Beatmapset) : this(
            beatmapsetID = set.beatmapsetID,
            covers = set.covers,
            playCount = set.playCount,
            favouriteCount = set.favouriteCount,
            ranked = set.ranked,
            rating = set.rating,
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(IMapperService::class.java)

        fun getIMapperV2(param: IMapperParam): Map<String, Any> {
            val user = param.user
            val relatedSets = param.relatedSets
            val activity = param.activities

            val relatedUsers = relatedSets
                .filter { it.creatorID != user.userID }
                .flatMap { it.beatmaps.orEmpty() }
                .flatMap { it.mappers }
                .distinctBy { it.userID }
                .filter { it.userID != user.userID }

            val recentActivity: List<ActivityEvent> = try {
                activity.filterIndexed { i, it ->
                    i == 0 || it != param.activities[i - 1]
                }
            } catch (e: Exception) {
                log.error("谱师信息：筛选出错", e)
                emptyList()
            }

            val (mySets, otherSets) = relatedSets.partition { it.creatorID == user.userID }

            val myOwnedDiffs = mySets.flatMap { it.beatmaps.orEmpty() }
            val otherOwnedDiffs = otherSets.flatMap { it.beatmaps.orEmpty() }

            val myDiffs = myOwnedDiffs.filter { user.userID in it.mapperIDs } +
                    otherOwnedDiffs.filter { user.userID in it.mapperIDs }

            val guestDiffs = myOwnedDiffs.filter { diff ->
                diff.mapperIDs.any { it != user.userID }
            }
            val myGuestDiffs = otherOwnedDiffs.filter { user.userID in it.mapperIDs }


            val guestDifficultyOwners = relatedUsers.map { u ->
                val re = guestDiffs.filter { u.userID in it.mapperIDs }
                val received = re.count()
                val receivedRanked = re.count { it.ranked > 0 }

                val se = myGuestDiffs.filter { it.beatmapset?.creatorID == u.userID }
                val sent = se.count()
                val sentRanked = se.count { it.ranked > 0 }

                GuestDifficultyService.GuestDifficultyOwner(u, received, receivedRanked, sent, sentRanked)
            }

                // 过滤掉总数和 Ranked 数均为 0 的用户
                .filter { it.sent + it.received > 0 }

                //复合排序（先按具有资格的谱面数排序，相同再按总谱面数排序）
                .sortedWith(
                    compareByDescending<GuestDifficultyService.GuestDifficultyOwner> { it.sentRanked + it.receivedRanked }
                        .thenByDescending { it.sent + it.received }
                )
                .take(7)

            val mostPopularBeatmapset = mySets
                .sortedWith(
                    compareByDescending<Beatmapset> { if (it.ranked > 0.toByte()) 1.toByte() else it.ranked }
                        .thenByDescending { it.playCount }
                )
                .take(6)
                .map { MostPopularBeatmapset(it) }

            val genre = findGenreCounts( myDiffs)

            val language = findLanguageCounts( myDiffs)

            val lengthArr = IntArray(8)

            val lengthMaxBoundary = intArrayOf(60, 100, 140, 180, 220, 260, 300, Int.MAX_VALUE)

            for (f in myDiffs.map { it.totalLength }) {
                for (i in lengthMaxBoundary.indices) {
                    if (f <= lengthMaxBoundary[i]) {
                        lengthArr[i]++
                        break
                    }
                }
            }

            val diffArr = IntArray(8)

            val starMaxBoundary = doubleArrayOf(2.0, 2.8, 4.0, 5.3, 6.5, 8.0, 10.0, Double.MAX_VALUE)

            for (d in myDiffs.map { it.starRating }) {
                for (i in starMaxBoundary.indices) {
                    if (d <= starMaxBoundary[i]) {
                        diffArr[i]++
                        break
                    }
                }
            }

            val mostRecentlyRankedBeatmapset = (mySets
                .filter { it.ranked > 0 }
                .map { RecentlyRankedBeatmapset(it, "host") }
                    + otherSets
                        .filter { it.ranked > 0 }
                        .map { RecentlyRankedBeatmapset(it, "guest") }
                    )
                .sortedByDescending { it.rankedDate?.toEpochSecond() }
                .take(5)

            val favorite = mySets.sumOf { it.favouriteCount }
            val playcount = myDiffs.sumOf { it.playCount }

            val averageRating = mySets.filter { it.ranked > 0 }.map { it.rating }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            val averageLength = myDiffs.map { it.totalLength }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            val averageDifficulty = myDiffs.map { it.starRating }.takeIf { it.isNotEmpty() }?.average() ?: 0.0

            return mapOf(
                "user" to user,
                "most_popular_beatmapset" to mostPopularBeatmapset,
                "genre" to genre,
                "language" to language,
                "difficulty_arr" to diffArr,
                "length_arr" to lengthArr,
                "recent_activity" to recentActivity,

                "most_recent_ranked_beatmapset" to mostRecentlyRankedBeatmapset,

                "favorite" to favorite,
                "playcount" to playcount,

                "guest_owners" to guestDifficultyOwners,
                "associated_bids" to myDiffs.map { it.beatmapID }.toSet(),

                "total_guest" to myGuestDiffs.count(),
                "total_guest_ranked" to myGuestDiffs.count { it.ranked > 0 },
                "total_diff" to myDiffs.count(),

                "average_rating" to averageRating,
                "average_length" to averageLength,
                "average_difficulty" to averageDifficulty
                )
        }

        @Deprecated("use getImapperV2")
        fun getIMapperV1(
            param: IMapperParam
        ): Map<String, Any?> {
            val result = param.relatedSets
            val user = param.user
            val activity = param.activities

            val mappingActivity: List<ActivityEvent> = try {
                activity.filterIndexed { i, it ->
                    i == 0 || it != param.activities[i - 1]
                }
            } catch (e: Exception) {
                log.error("谱师信息：筛选出错", e)
                emptyList()
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

            val beatmaps = result.flatMap { it.beatmaps.orEmpty() }

            val diffArr = IntArray(8)

            run {
                val diffStar = beatmaps.filter { it.mapperIDs.contains(user.userID)  }.map { it.starRating }
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

                        if (it.tags.lowercase().contains(keyword)) {
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
                val lengthAll = beatmaps.filter { it.mapperIDs.contains(user.userID) }.map { it.totalLength }
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



        fun getIMapperParam(event: MessageEvent, matcher: Matcher, userApiService: OsuUserApiService, beatmapApiService: OsuBeatmapApiService): IMapperParam {
            val mode = InstructionObject(OsuMode.DEFAULT)
            val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

            val user: OsuUser
            val relatedSets: Sequence<Beatmapset>
            val activity: List<ActivityEvent>

            if (id != null) {
                val query = mapOf(
                    "q" to "creator=${id}", "sort" to "ranked_desc", "s" to "any", "page" to 1
                )

                val async = AsyncMethodExecutor.awaitTriple(
                    { beatmapApiService.searchBeatmapsetParallel(query) },
                    { userApiService.getUserRecentActivity(id).filterIsMapping().squash() },
                    { userApiService.getOsuUser(id, mode.data!!) },
                )
                val sets = async.first.beatmapsets

                beatmapApiService.applyBeatmapsetExtend(sets)

                relatedSets = sets.asSequence()

                activity = async.second
                user = async.third
            } else {
                user = InstructionUtil.getUserWithoutRange(event, matcher, mode)

                val query = mapOf(
                    "q" to "creator=${user.userID}", "sort" to "ranked_desc", "s" to "any", "page" to 1
                )

                val async = AsyncMethodExecutor.awaitPair(
                    { beatmapApiService.searchBeatmapsetParallel(query) },
                    { userApiService.getUserRecentActivity(user.userID).filterIsMapping().squash() }
                )

                val sets = async.first.beatmapsets

                beatmapApiService.applyBeatmapsetExtend(sets)

                relatedSets = sets.asSequence()

                activity = async.second
            }

            return IMapperParam(user, relatedSets, activity)
        }

        private fun findGenreCounts(maps: Collection<Beatmap>): IntArray {
            val genres = OsuGenre.entries
            val result = IntArray(genres.size)

            maps.forEach { map ->
                val tags = map.beatmapset?.tags?.lowercase() ?: ""
                var matchedAnyGenre = false

                for (i in 1 until genres.size) {
                    val genre = genres[i]
                    if (tags.contains(genre.formalName)) {
                        result[i]++
                        matchedAnyGenre = true
                    }
                }

                if (!matchedAnyGenre) {
                    result[0]++
                }
            }

            return result
        }

        private fun findLanguageCounts(maps: Collection<Beatmap>): IntArray {
            val languages = OsuLanguage.entries
            val result = IntArray(languages.size)

            maps.forEach { map ->
                val tags = map.beatmapset?.tags?.lowercase() ?: ""
                var matchedAnyLanguage = false

                for (i in 1 until languages.size) {
                    val lang = languages[i]
                    if (tags.contains(lang.formalName)) {
                        result[i]++
                        matchedAnyLanguage = true
                    }
                }

                if (!matchedAnyLanguage) {
                    result[0]++
                }
            }

            return result
        }
    }
}
