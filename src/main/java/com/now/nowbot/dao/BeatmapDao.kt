package com.now.nowbot.dao

import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.now.nowbot.entity.BeatmapExtendLite
import com.now.nowbot.entity.BeatmapLite
import com.now.nowbot.entity.BeatmapLite.BeatmapHitLengthResult
import com.now.nowbot.entity.BeatmapsetExtendLite
import com.now.nowbot.entity.BeatmapsetLite
import com.now.nowbot.entity.NanoUserLite
import com.now.nowbot.entity.NanoUserLite.Companion.toNanoUserLite
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.collections.orEmpty
import kotlin.jvm.optionals.getOrNull

@Component
class BeatmapDao(
    private val beatmapsetRepository: BeatmapsetRepository,
    private val beatmapRepository: BeatmapRepository,
    private val tagRepository: TagRepository,
    private val extendBeatmapRepository: BeatmapExtendRepository,
    private val extendBeatmapSetRepository: BeatmapsetExtendLiteRepository,
) {
    fun saveBeatmapsAndSaveExtendAsync(beatmaps: Collection<Beatmap>) {
        saveBeatmapsAsync(beatmaps)
        saveExtendedBeatmapsAsync(beatmaps)
    }

    fun saveBeatmapAndSaveExtendAsync(beatmap: Beatmap) {
        Thread.startVirtualThread {
            try {
                saveBeatmap(beatmap.copy())
            } catch (e: Exception) {
                log.warn("谱面数据访问对象层：保存 ${beatmap.beatmapID} 谱面失败：", e)
            }

            try {
                saveExtendedBeatmap(beatmap.copy())
            } catch (e: Exception) {
                log.warn("谱面数据访问对象层：保存 ${beatmap.beatmapID} 谱面的扩充信息失败：", e)
            }
        }
    }

    fun saveBeatmapsetAsync(beatmapset: Beatmapset) {
        Thread.startVirtualThread {
            try {
                saveBeatmapset(beatmapset.copy())
                beatmapset.beatmaps?.forEach { beatmap ->
                    saveBeatmap(beatmap.copy())
                }
            } catch (e: Exception) {
                log.warn("谱面数据访问对象层：保存 ${beatmapset.beatmapsetID} 谱面集失败：", e)
            }
        }
    }

    fun saveBeatmapsetsAsync(beatmapsets: Collection<Beatmapset>) {
        Thread.startVirtualThread {
            beatmapsets.forEach { beatmapset ->
                try {
                    saveBeatmapset(beatmapset.copy())
                    beatmapset.beatmaps?.forEach { beatmap ->
                        saveBeatmap(beatmap.copy())
                    }
                } catch (e: Exception) {
                    log.warn("谱面数据访问对象层：保存 ${beatmapset.beatmapsetID} 谱面集失败：", e)
                }
            }
        }
    }

    fun saveBeatmapAsync(beatmap: Beatmap) {
        Thread.startVirtualThread {
            try {
                saveBeatmap(beatmap.copy())
            } catch (e: Exception) {
                log.warn("谱面数据访问对象层：保存 ${beatmap.beatmapID} 谱面失败：", e)
            }
        }
    }

    private fun saveBeatmap(beatmap: Beatmap): BeatmapLite {
        val set = beatmap.beatmapset
        if (set != null) {
            beatmapsetRepository.save(fromBeatmapsetModel(set))
        }
        return beatmapRepository.save(fromBeatmapModel(beatmap))
    }

    fun saveBeatmapsAsync(beatmaps: Collection<Beatmap>) {
        Thread.startVirtualThread {
            try {
                saveBeatmaps(beatmaps)
            } catch (e: Exception) {
                log.warn("谱面数据访问对象层：保存 ${beatmaps.joinToString(", ") { it.beatmapID.toString() }} 等谱面失败：", e)
            }
        }
    }

    private fun saveBeatmaps(beatmaps: Collection<Beatmap>) {
        val exists = beatmapRepository.exists(beatmaps.map { it.beatmapID }).toSet()

        val s = beatmaps.filterNot { it.beatmapID in exists }.map { fromBeatmapModel(it) }

        beatmapRepository.saveAll(s)
    }

    fun saveBeatmapset(beatmapset: Beatmapset): BeatmapsetLite {
        return beatmapsetRepository.save(fromBeatmapsetModel(beatmapset))
    }

    fun saveBeatmapsets(beatmapsets: Collection<Beatmapset>) {
        val exists = beatmapsetRepository.findExistingIds(beatmapsets.map { it.beatmapsetID.toInt() }).toSet()

        val s = beatmapsets.filterNot { it.beatmapsetID.toInt() in exists }.map { fromBeatmapsetModel(it) }
        beatmapsetRepository.saveAll(s)
    }

    fun getBeatmapLite(id: Long): BeatmapLite? {
        return beatmapRepository.findById(id).getOrNull()
    }

    fun getBeatmapsetLite(id: Long): BeatmapsetLite? {
        return beatmapRepository.getBeatmapsetByBid(id)
    }

    fun getBeatmapHitLength(ids: Collection<Long>): List<BeatmapHitLengthResult> {
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

    fun updateFailTimeByBeatmapsetID(s: Beatmapset): Int {
        return extendBeatmapSetRepository.updateFailTimeByBeatmapsetID(
            s.beatmapsetID, s.animeCover, s.favouriteCount,
            s.offset, s.playCount, s.spotlight,
            s.trackID, s.discussionLocked, s.rating,
            s.ratings.toTypedArray())
    }

    fun findMapByUpdateAtAscend(time: LocalDateTime, limit: Int = 500): List<BeatmapExtendLite> {
        return extendBeatmapRepository.findByUpdateAtAscend(time, limit)
    }

    fun updateFailTimeByBeatmapID(beatmap: Beatmap): Int {
        return extendBeatmapRepository.updateFailTimeByBeatmapID(
            beatmapID = beatmap.beatmapID,
            lazerOnly = beatmap.lazerOnly,
            fail = beatmap.failTimes?.toString(),
            owners = beatmap.owners?.map { o -> o.toNanoUserLite() }?.let { owners -> JacksonUtil.objectToJson(owners)})
    }

    @Transactional
    fun deleteExtendedBeatmapAndSet(beatmapIDs: Iterable<Long>) {
        extendBeatmapRepository.deleteAllById(beatmapIDs)
        extendBeatmapSetRepository.deleteAllByBeatmapIDs(beatmapIDs)
    }

    fun saveExtendedBeatmapsAsync(beatmaps: Collection<Beatmap>) {
        Thread.startVirtualThread {
            val validBeatmaps = beatmaps.filter { beatmap ->
                val hasGenreID = beatmap.beatmapset?.genreID != null
                val ranked = beatmap.beatmapset?.ranked
                val stabled = ranked != null && ranked in byteArrayOf(1, 2, 4)
                hasGenreID && stabled
            }

            if (validBeatmaps.isEmpty()) return@startVirtualThread

            // 2. 内存去重并批量保存 Beatmapset
            // 用一个 Map 来存储保存成功（或原本就存在）的 BeatmapsetExtendLite 对象，供后面关联使用
            val savedSetMap = mutableMapOf<Long, BeatmapsetExtendLite>()

            validBeatmaps.mapNotNull { it.beatmapset }
                .distinctBy { it.beatmapsetID }
                .forEach { s ->
                    val id = s.beatmapsetID

                    val savedSet = try {
                        saveExtendedBeatmapsetLite(s)
                    } catch (_: Exception) {
                        extendBeatmapSetRepository.findByBeatmapsetID(id)
                    }

                    savedSet?.let { savedSetMap[id] = it }
                }

            // 3. 此时所有需要的 set 都在 savedSetMap 缓存中了，直接调用 saveExtendBeatmapLite 保存谱面
            validBeatmaps.forEach { beatmap ->
                val associatedSet = savedSetMap[beatmap.beatmapsetID]
                if (associatedSet != null) {
                    try {
                        if (extendBeatmapRepository.existsByBeatmapID(beatmap.beatmapID)) {
                            extendBeatmapRepository.deleteByBeatmapID(beatmap.beatmapID)
                        }

                        saveExtendedBeatmapLite(beatmap, associatedSet)
                    } catch (e: Exception) {
                        log.warn("谱面数据访问对象层：保存 ${beatmap.beatmapID} 谱面的扩充信息失败：", e)
                    }
                }
            }
        }
    }

    fun saveExtendedBeatmapAsync(beatmap: Beatmap) {
        Thread.startVirtualThread {
            saveExtendedBeatmap(beatmap)
        }
    }

    private fun saveExtendedBeatmap(beatmap: Beatmap) {
        val hasGenreID = beatmap.beatmapset?.genreID != null
        val ranked = beatmap.beatmapset?.ranked
        val stabled = ranked != null && ranked in byteArrayOf(1, 2, 4)

        if (!(hasGenreID && stabled)) return

        val s = beatmap.beatmapset!!

        val hasBeatmap = extendBeatmapRepository.existsByBeatmapID(beatmap.beatmapID)
        val hasBeatmapset = extendBeatmapSetRepository.existsByBeatmapsetID(beatmap.beatmapsetID)

        if (hasBeatmap && hasBeatmapset) {
            updateFailTimeByBeatmapID(beatmap)
            updateFailTimeByBeatmapsetID(s)
            return
        }

        val savedSet = if (hasBeatmapset) {
            extendBeatmapSetRepository.findByBeatmapsetID(beatmap.beatmapsetID)!!
        } else {
            saveExtendedBeatmapsetLite(s)
        }

        if (hasBeatmap) {
            extendBeatmapRepository.deleteByBeatmapID(beatmap.beatmapID)
        }

        saveExtendedBeatmapLite(beatmap, savedSet)
    }

    private fun saveExtendedBeatmapLite(beatmap: Beatmap, savedSet: BeatmapsetExtendLite): BeatmapExtendLite {

        val now = LocalDateTime.now()

        val lite = BeatmapExtendLite(
            beatmapID = beatmap.beatmapID,
            beatmapset = savedSet,
            lazerOnly = beatmap.lazerOnly,
            failTimes = beatmap.failTimes?.toString(),
            owners = beatmap.owners?.map { nu -> nu.toNanoUserLite() }?.let { JacksonUtil.toJson(it) },
            maxCombo = beatmap.maxCombo ?: 0,
            createdAt = now,
            updatedAt = now
        )

        return extendBeatmapRepository.save(lite)
    }

    private fun saveExtendedBeatmapsetLite(set: Beatmapset): BeatmapsetExtendLite {
        val entity = BeatmapsetExtendLite(
            beatmapsetID = set.beatmapsetID,
            animeCover = set.animeCover,
            artist = set.artist,
            artistUnicode = set.artistUnicode,
            coverID = set.covers.cover
                .split("?").getOrNull(1)?.toLongOrNull(),
            creator = set.creator,
            favouriteCount = set.favouriteCount,
            genreID = set.genreID,
            creatorID = set.creatorID,
            languageID = set.languageID,
            nsfw = set.nsfw,
            recommendOffset = set.offset,
            playCount = set.playCount,
            source = set.source,
            status = set.status,
            spotlight = set.spotlight,
            title = set.title,
            titleUnicode = set.titleUnicode,
            trackID = set.trackID,
            video = set.video,
            bpm = set.bpm,
            discussionLocked = set.discussionLocked,
            lastUpdated = set.lastUpdated.toLocalDateTime(),
            threadID = set.legacyThreadUrl?.split("/")?.lastOrNull()?.toLongOrNull(),
            nominationsCurrent = set.nominationsSummary?.current,
            nominationsRulesets = set.nominationsSummary?.mode
                ?.map { mode ->
                    when(mode) {
                        "osu" -> 1
                        "taiko" -> 2
                        "catch", "fruits" -> 4
                        "mania" -> 8
                        else -> 0
                    }
                }?.sum()?.toByte(),
            nominationsRequiredMain = set.nominationsSummary?.required?.main,
            nominationsRequiredSecondary = set.nominationsSummary?.required?.secondary,
            ranked = set.ranked,
            rankedDate = set.rankedDate?.toLocalDateTime(),
            rating = set.rating,
            storyboard = set.storyboard,
            submittedDate = set.submittedDate.toLocalDateTime(),
            tags = set.tags,
            downloadDisabled = set.availability.downloadDisabled,
            moreInformation = set.availability.moreInformation,
            ratings = set.ratings.toTypedArray(),
        )

        return extendBeatmapSetRepository.save(entity)
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
            scoreAble = true
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
                }.orEmpty(), Beatmapset.RequiredMeta(
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
            lazerOnly = b.lazerOnly
            failTimes = b.failTimes?.let { JacksonUtil.toNode(it) }
            owners = b.owners?.let { JacksonUtil.parseObjectList(it, NanoUserLite::class.java) }?.map { it.toNanoUser() }
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

        from.extend(b, from.beatmapset?.extend(x), x.beatmapsetID)

        return b.beatmapID
    }

    /**
     * 如果成功，就返回这个谱面集 ID
     */
    fun extendBeatmapset(from: Beatmapset): Long? {
        val set = extendBeatmapSetRepository.findByBeatmapsetID(from.beatmapsetID) ?: return null

        val bs = from.beatmaps.orEmpty().map { b ->
            extendBeatmapRepository.findByBeatmapID(b.beatmapID) ?: return null
        }.associateBy { it.beatmapID }

        from.beatmaps.orEmpty().forEach { b ->
            bs[b.beatmapID]?.let { x ->
                b.extend(x, null, set.beatmapsetID)
            }
        }

        from.extend(set)

        return set.beatmapsetID
    }

    @CanIgnoreReturnValue
    private fun Beatmap.extend(b: BeatmapExtendLite, s: Beatmapset? = null, setID: Long? = null): Beatmap {
        setID?.let {
            beatmapsetID = setID
        }

        s?.let {
            beatmapset = s
        }

        lazerOnly = b.lazerOnly
        failTimes = b.failTimes?.let { JacksonUtil.toNode(it) }
        maxCombo = b.maxCombo
        owners = b.owners?.let { JacksonUtil.parseObjectList(it, NanoUserLite::class.java) }?.map { it.toNanoUser() }

        return this
    }

    @CanIgnoreReturnValue
    private fun Beatmapset.extend(x: BeatmapsetExtendLite): Beatmapset {

        fun Byte.isBitSet(bitPosition: Int): Boolean {
            return (this.toInt() and (1 shl bitPosition)) != 0
        }

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
        canBeHyped = x.ranked <= 0.toByte()
        deletedAt = null
        discussionLocked = x.discussionLocked
        scoreAble = true
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
            }.orEmpty(), Beatmapset.RequiredMeta(
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

        return this
    }

    fun existsBeatmapsetFromExtend(beatmapsetID: Long): Boolean {
        return extendBeatmapSetRepository.existsByBeatmapsetID(beatmapsetID)
    }

    fun existsBeatmapFromExtend(beatmapID: Long): Boolean {
        return extendBeatmapRepository.existsByBeatmapID(beatmapID)
    }

    fun getBeatmapsetIDFromExtend(beatmapID: Long): Long? {
        return extendBeatmapRepository.findByBeatmapID(beatmapID)?.beatmapset?.beatmapsetID
    }

    companion object {
        fun fromBeatmapLite(bl: BeatmapLite): Beatmap {
            val b = bl.toBeatmap()
            b.beatmapset = fromBeatmapsetLite(bl.mapSet)
            return b
        }

        fun fromBeatmapModel(b: Beatmap): BeatmapLite {
            val s = BeatmapLite(b)
            var mapSet: BeatmapsetLite? = null
            if (b.beatmapset != null) {
                mapSet = fromBeatmapsetModel(b.beatmapset!!)
            }
            s.mapSet = mapSet
            return s
        }

        private fun fromBeatmapsetLite(set: BeatmapsetLite): Beatmapset {
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

        fun fromBeatmapsetModel(mapSet: Beatmapset): BeatmapsetLite {
            val s = BeatmapsetLite()
            s.id = mapSet.beatmapsetID.toInt()
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

        private val log: Logger = LoggerFactory.getLogger(BeatmapDao::class.java)
    }
}
