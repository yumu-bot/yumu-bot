package com.now.nowbot.dao

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.entity.BeatmapExtendLite
import com.now.nowbot.entity.BeatmapLite
import com.now.nowbot.entity.BeatmapLite.BeatmapHitLengthResult
import com.now.nowbot.entity.BeatmapsetExtendLite
import com.now.nowbot.entity.MapSetLite
import com.now.nowbot.entity.TagLite
import com.now.nowbot.mapper.BeatmapExtendRepository
import com.now.nowbot.mapper.BeatmapRepository
import com.now.nowbot.mapper.BeatmapsetExtendLiteRepository
import com.now.nowbot.mapper.BeatmapsetRepository
import com.now.nowbot.mapper.TagRepository
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.Tag
import com.now.nowbot.util.JacksonUtil
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Component
class BeatmapDao(
    private val beatmapsetRepository: BeatmapsetRepository,
    private val beatmapRepository: BeatmapRepository,
    private val tagRepository: TagRepository,
    private val extendBeatmapRepository: BeatmapExtendRepository,
    private val extendBeatmapSetRepository: BeatmapsetExtendLiteRepository,
) {
    fun saveMap(beatmap: Beatmap): BeatmapLite {
        val mapSet = beatmap.beatmapset
        if (mapSet != null) {
            beatmapsetRepository.save(fromMapSetModel(mapSet))
        }
        return beatmapRepository.save(fromBeatmapModel(beatmap))
    }

    fun saveAllMapSet(beatmapset: List<Beatmapset>) {
        val s = beatmapset.map { fromMapSetModel(it) }
        beatmapsetRepository.saveAll(s)
    }

    fun saveAllMap(beatmap: List<Beatmap>) {
        val s = beatmap.map { fromBeatmapModel(it) }
        beatmapRepository.saveAll(s)
    }

    fun saveMapSet(beatmapset: Beatmapset): MapSetLite {
        return beatmapsetRepository.save(fromMapSetModel(beatmapset))
    }

    fun getBeatMapLite(id: Long): BeatmapLite? {
        val lite = beatmapRepository.findById(id)
        if (lite.isEmpty) {
            return null
        }
        return lite.get()
    }

    fun getBeatMapSetLite(id: Long): Optional<MapSetLite> {
        return beatmapRepository.getMapSetByBid(id)
    }

    fun getBeatMapSetLite(id: Int): Optional<MapSetLite> {
        return getBeatMapSetLite(id.toLong())
    }

    fun getBeatMapsHitLength(ids: Collection<Long>): List<BeatmapHitLengthResult> {
        return beatmapRepository.getBeatmapHitLength(ids)
    }

    fun saveTag(tags: Collection<Tag>) {
        tagRepository.saveAll(tags.map { TagLite.from(it) })
    }

    fun getTag(id: Int): Tag? {
        return tagRepository.findById(id).get().toModel()
    }

    fun findSetByUpdateAtAscend(time: LocalDateTime, limit: Int = 500): List<BeatmapsetExtendLite> {
        return extendBeatmapSetRepository.findByUpdateAtAscend(time, limit)
    }

    fun updateFailTimeByBeatmapsetID(beatmapsetID: Long, animeCover: Boolean, favouriteCount: Long, recommendOffset: Short, playCount: Long, spotlight: Boolean, trackID: Int?, discussionLocked: Boolean, rating: Float, ratings: Array<Int>): Int {
        return extendBeatmapSetRepository.updateFailTimeByBeatmapsetID(beatmapsetID, animeCover, favouriteCount, recommendOffset, playCount, spotlight, trackID, discussionLocked, rating, ratings)
    }

    fun findMapByUpdateAtAscend(time: LocalDateTime, limit: Int = 500): List<BeatmapExtendLite> {
        return extendBeatmapRepository.findByUpdateAtAscend(time, limit)
    }

    fun updateFailTimeByBeatmapID(beatmapID: Long, fail: String?): Int {
        return extendBeatmapRepository.updateFailTimeByBeatmapID(beatmapID, fail)
    }

    @Transactional
    fun deleteExtendedBeatmapAndSet(beatmapIDs: Iterable<Long>) {
        extendBeatmapRepository.deleteAllById(beatmapIDs)
        extendBeatmapSetRepository.deleteAllByBeatmapIDs(beatmapIDs)
    }

    fun saveExtendedBeatmap(beatmaps: List<Beatmap>) {
        beatmaps.forEach { saveExtendedBeatmap(it) }
    }

    fun saveExtendedBeatmap(beatmap: Beatmap) {
        val hasBeatmap = extendBeatmapRepository.existsByBeatmapID(beatmap.beatmapID)
        val hasBeatmapset = extendBeatmapSetRepository.existsByBeatmapsetID(beatmap.beatmapsetID)

        if (hasBeatmap && hasBeatmapset) return

        val hasGenreID = beatmap.beatmapset?.genreID != null

        val ranked = beatmap.beatmapset?.ranked

        val stabled = ranked == 1.toByte() || ranked == 2.toByte() || ranked == 4.toByte()

        if (!(hasGenreID && stabled)) return

        val s = beatmap.beatmapset!!

        val savedSet = if (hasBeatmapset) {
            extendBeatmapSetRepository.findByBeatmapsetID(beatmap.beatmapsetID)!!
        } else {
            val set = BeatmapsetExtendLite(
                beatmapsetID = s.beatmapsetID,
                animeCover = s.animeCover,
                artist = s.artist,
                artistUnicode = s.artistUnicode,
                coverID = s.covers.cover
                    .split("?").getOrNull(1)?.toLongOrNull(),
                creator = s.creator,
                favouriteCount = s.favouriteCount,
                genreID = s.genreID,
                creatorID = s.creatorID,
                languageID = s.languageID,
                nsfw = s.nsfw,
                recommendOffset = s.offset,
                playCount = s.playCount,
                source = s.source,
                status = s.status,
                spotlight = s.spotlight,
                title = s.title,
                titleUnicode = s.titleUnicode,
                trackID = s.trackID,
                video = s.video,
                bpm = s.bpm,
                discussionLocked = s.discussionLocked,
                lastUpdated = s.lastUpdated.toLocalDateTime(),
                threadID = s.legacyThreadUrl?.split("/")?.lastOrNull()?.toLongOrNull(),
                nominationsCurrent = s.nominationsSummary?.current,
                nominationsRulesets = s.nominationsSummary?.mode
                    ?.map { mode ->
                        when(mode) {
                            "osu" -> 1
                            "taiko" -> 2
                            "catch", "fruits" -> 4
                            "mania" -> 8
                            else -> 0
                        }
                    }?.sum()?.toByte(),
                nominationsRequiredMain = s.nominationsSummary?.required?.main,
                nominationsRequiredSecondary = s.nominationsSummary?.required?.secondary,
                ranked = s.ranked,
                rankedDate = s.rankedDate?.toLocalDateTime(),
                rating = s.rating,
                storyboard = s.storyboard,
                submittedDate = s.submittedDate.toLocalDateTime(),
                tags = s.tags,
                downloadDisabled = s.availability.downloadDisabled,
                moreInformation = s.availability.moreInformation,
                ratings = s.ratings.toTypedArray(),
            )

            extendBeatmapSetRepository.save(set)
        }

        if (hasBeatmap) {
            extendBeatmapRepository.deleteById(beatmap.beatmapID)
        }

        val now = LocalDateTime.now()

        val lite = BeatmapExtendLite(
            beatmapID = beatmap.beatmapID,
            beatmapset = savedSet,
            failTimes = beatmap.failTimes?.toString(),
            maxCombo = beatmap.maxCombo ?: 0,
            createdAt = now,
            updatedAt = now
        )

        extendBeatmapRepository.save(lite)
    }

    /**
     * 如果成功，就返回这个谱面 ID
     */
    fun extendBeatmap(score: LazerScore): Long? {
        val b = extendBeatmapRepository.findByBeatmapID(score.beatmapID) ?: return null
        val x = b.beatmapset

        val isRanked = x.ranked.toInt() > 0

        fun Byte.isBitSet(bitPosition: Int): Boolean {
            return (this.toInt() and (1 shl bitPosition)) != 0
        }

        score.beatmapset.apply {
            animeCover = x.animeCover
            artist = x.artist
            artistUnicode = x.artistUnicode
            covers = Covers.getCoverFromCacheID(x.beatmapsetID, x.coverID)
            creator = x.creator
            favouriteCount = x.favouriteCount
            genreID = x.genreID
            hype = null
            beatmapsetID = x.beatmapsetID
            languageID = x.languageID
            nsfw = x.nsfw
            offset = x.recommendOffset
            playCount = x.playCount
            previewUrl = "//b.ppy.sh/preview/${x.beatmapsetID}.mp3"
            source = x.source
            spotlight = x.spotlight
            status = x.status
            title = x.title
            titleUnicode = x.titleUnicode
            trackID = x.trackID
            creatorID = x.creatorID
            video = x.video
            bpm = x.bpm
            canBeHyped = !isRanked
            deletedAt = null
            discussionLocked = x.discussionLocked
            scoreable = true
            lastUpdated = x.lastUpdated.atOffset(ZoneOffset.ofHours(8))
            legacyThreadUrl = x.threadID?.let { "https://osu.ppy.sh/community/forums/topics/$it" }
            nominationsSummary = Beatmapset.NominationsSummary(
                x.nominationsCurrent ?: 0,
                x.nominationsRulesets?.let {
                    val rulesets = mutableListOf<String>()

                    if (it.isBitSet(1)) rulesets.add("osu")

                    if (it.isBitSet(2)) rulesets.add("taiko")

                    if (it.isBitSet(3)) rulesets.add("fruits")

                    if (it.isBitSet(4)) rulesets.add("mania")

                    return@let rulesets
                } ?: listOf(), Beatmapset.RequiredMeta(
                    x.nominationsRequiredMain ?: 0,
                    x.nominationsRequiredSecondary ?: 0
                )
            )
            ranked = x.ranked
            rankedDate = x.rankedDate?.atOffset(ZoneOffset.ofHours(8))
            rating = x.rating
            storyboard = x.storyboard
            submittedDate = x.submittedDate.atOffset(ZoneOffset.ofHours(8))
            this.tags = x.tags
            availability = Beatmapset.Availability(
                x.downloadDisabled,
                x.moreInformation
            )
            ratings = x.ratings.toList()
        }

        score.beatmap.apply {
            beatmapsetID = x.beatmapsetID
            failTimes = b.failTimes?.let { JacksonUtil.toNode(it) as? JsonNode }
            maxCombo = b.maxCombo
        }

        return b.beatmapID
    }

    /**
     * 如果成功，就返回这个谱面 ID
     */
    fun extendBeatmap(from: Beatmap): Long? {
        val b = extendBeatmapRepository.findByBeatmapID(from.beatmapID) ?: return null
        val x = b.beatmapset

        val isRanked = x.ranked.toInt() > 0

        fun Byte.isBitSet(bitPosition: Int): Boolean {
            return (this.toInt() and (1 shl bitPosition)) != 0
        }

        val set = (from.beatmapset ?: Beatmapset()).apply {
            animeCover = x.animeCover
            artist = x.artist
            artistUnicode = x.artistUnicode
            covers = Covers.getCoverFromCacheID(x.beatmapsetID, x.coverID)
            creator = x.creator
            favouriteCount = x.favouriteCount
            genreID = x.genreID
            hype = null
            beatmapsetID = x.beatmapsetID
            languageID = x.languageID
            nsfw = x.nsfw
            offset = x.recommendOffset
            playCount = x.playCount
            previewUrl = "//b.ppy.sh/preview/${x.beatmapsetID}.mp3"
            source = x.source
            spotlight = x.spotlight
            status = x.status
            title = x.title
            titleUnicode = x.titleUnicode
            trackID = x.trackID
            creatorID = x.creatorID
            video = x.video
            bpm = x.bpm
            canBeHyped = !isRanked
            deletedAt = null
            discussionLocked = x.discussionLocked
            scoreable = true
            lastUpdated = x.lastUpdated.atOffset(ZoneOffset.ofHours(8))
            legacyThreadUrl = x.threadID?.let { "https://osu.ppy.sh/community/forums/topics/$it" }
            nominationsSummary = Beatmapset.NominationsSummary(
                x.nominationsCurrent ?: 0,
                x.nominationsRulesets?.let {
                    val rulesets = mutableListOf<String>()

                    if (it.isBitSet(1)) rulesets.add("osu")

                    if (it.isBitSet(2)) rulesets.add("taiko")

                    if (it.isBitSet(3)) rulesets.add("fruits")

                    if (it.isBitSet(4)) rulesets.add("mania")

                    return@let rulesets
                } ?: listOf(), Beatmapset.RequiredMeta(
                    x.nominationsRequiredMain ?: 0,
                    x.nominationsRequiredSecondary ?: 0
                )
            )
            ranked = x.ranked
            rankedDate = x.rankedDate?.atOffset(ZoneOffset.ofHours(8))
            rating = x.rating
            storyboard = x.storyboard
            submittedDate = x.submittedDate.atOffset(ZoneOffset.ofHours(8))
            this.tags = x.tags
            availability = Beatmapset.Availability(
                x.downloadDisabled,
                x.moreInformation
            )
            ratings = x.ratings.toList()
        }

        from.apply {
            beatmapsetID = x.beatmapsetID
            beatmapset = set
            failTimes = b.failTimes?.let { JacksonUtil.toNode(it) as? JsonNode }
            maxCombo = b.maxCombo
        }

        return b.beatmapID
    }

    companion object {
        fun fromBeatmapLite(bl: BeatmapLite): Beatmap {
            val b = bl.toBeatMap()
            b.beatmapset = fromBeatMapSetLite(bl.mapSet)
            return b
        }

        fun fromBeatmapModel(b: Beatmap): BeatmapLite {
            val s = BeatmapLite(b)
            var mapSet: MapSetLite? = null
            if (b.beatmapset != null) {
                mapSet = fromMapSetModel(b.beatmapset!!)
            }
            s.mapSet = mapSet
            return s
        }

        private fun fromBeatMapSetLite(set: MapSetLite): Beatmapset {
            val s = Beatmapset()
            s.beatmapsetID = set.id.toLong()
            s.creatorID = set.mapperId.toLong()
            s.creator = set.creator
            s.covers = Covers(set.cover, set.cover, set.card, set.card, set.list, set.list, set.slimcover, set.slimcover)

            s.nsfw = set.nsfw
            s.storyboard = set.storyboard
            s.source = set.source
            s.status = set.status
            s.playCount = set.playCount
            s.favouriteCount = set.favourite.toLong()
            s.title = set.title
            s.titleUnicode = set.titleUTF
            s.artist = set.artist
            s.artistUnicode = set.artistUTF
            s.legacyThreadUrl = set.legacyUrl

            s.fromDatabase = false
            return s
        }

        fun fromMapSetModel(mapSet: Beatmapset): MapSetLite {
            val s = MapSetLite()
            s.id = Math.toIntExact(mapSet.beatmapsetID)
            s.card = mapSet.covers.card2x
            s.cover = mapSet.covers.cover2x
            s.list = mapSet.covers.list2x
            s.slimcover = mapSet.covers.slimcover2x

            s.availabilityDownloadDisable = mapSet.availability.downloadDisabled
            s.nsfw = mapSet.nsfw
            s.storyboard = mapSet.storyboard
            s.legacyUrl = mapSet.legacyThreadUrl

            s.mapperId = mapSet.creatorID.toInt()
            s.creator = mapSet.creator
            s.source = mapSet.source
            s.status = mapSet.status
            s.playCount = mapSet.playCount
            s.favourite = mapSet.favouriteCount.toInt()
            s.title = mapSet.title
            s.titleUTF = mapSet.titleUnicode
            s.artist = mapSet.artist
            s.artistUTF = mapSet.artistUnicode

            return s
        }
    }
}
