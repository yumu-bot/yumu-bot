package com.now.nowbot.dao

import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.now.nowbot.entity.*
import com.now.nowbot.entity.BeatmapExtendLite.Companion.toExtendEntity
import com.now.nowbot.entity.BeatmapLite.BeatmapHitLengthResult
import com.now.nowbot.entity.BeatmapLite.Companion.toEntity
import com.now.nowbot.entity.BeatmapsetExtendLite.Companion.toExtendEntity
import com.now.nowbot.entity.BeatmapsetLite.Companion.toEntity
import com.now.nowbot.entity.NanoUserLite.Companion.toEntity
import com.now.nowbot.entity.NanoUserLite.Companion.toModel
import com.now.nowbot.mapper.*
import com.now.nowbot.model.osu.*
import com.now.nowbot.util.IntArrayCompressor
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectRetrievalFailureException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
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
            saveBeatmapAsync(beatmap)

            saveExtendedBeatmapAsync(beatmap)
        }
    }

    fun saveBeatmapsetAsync(beatmapset: Beatmapset) {
        if (beatmapset.beatmapsetID <= 0) return

        Thread.startVirtualThread {
            saveBeatmapset(beatmapset)
        }
        saveBeatmapsAsync(beatmapset.beatmaps.orEmpty())
    }

    fun saveBeatmapsetsAsync(beatmapsets: Collection<Beatmapset>) {
        Thread.startVirtualThread {
            val sets = beatmapsets.toSet()
            val beatmaps = sets.flatMap { it.beatmaps.orEmpty() }.toSet()

            sets.filter { it.beatmapsetID > 0 }.forEach { set ->
                runCatching {
                    saveBeatmapset(set)
                }.onFailure { e ->
                    if (e is DataIntegrityViolationException) return@onFailure
                    log.warn("谱面数据访问对象层：保存 ${set.beatmapsetID} 谱面集失败：", e)
                }
            }

            val setIDs = sets.map { it.beatmapsetID }.toSet()

            beatmaps.forEach { beatmap ->
                runCatching {
                    val parentID = beatmap.beatmapsetID

                    if (parentID !in setIDs && !extendBeatmapSetRepository.existsByBeatmapsetID(parentID)) {
                        log.info("谱面数据访问对象层：放弃保存谱面 ${beatmap.beatmapID}，因其所属的谱面集 $parentID 无论在内存还是数据库中均不存在。")
                        return@forEach
                    }

                    saveBeatmap(beatmap)
                }.onFailure { e ->
                    if (e is DataIntegrityViolationException) return@onFailure
                    if (e is ObjectRetrievalFailureException) return@onFailure

                    log.warn("谱面数据访问对象层：保存 ${beatmap.beatmapID} 谱面失败：", e)
                }
            }
        }
    }

    fun saveBeatmapAsync(beatmap: Beatmap) {
        Thread.startVirtualThread {
            try {
                saveBeatmap(beatmap)
            } catch (e: Exception) {
                log.warn("谱面数据访问对象层：保存 ${beatmap.beatmapID} 谱面失败：", e)
            }
        }
    }

    private fun saveBeatmap(beatmap: Beatmap) {
        val set = beatmap.beatmapset

        if (set != null && set.beatmapsetID > 0) {
            beatmapsetRepository.save(set.toEntity())
            beatmapRepository.save(beatmap.toEntity())
        }
    }

    fun saveBeatmapsAsync(beatmaps: Collection<Beatmap>) {
        Thread.startVirtualThread {
            runCatching {
                saveBeatmapsets(beatmaps.mapNotNull { it.beatmapset }.toSet())
                saveBeatmaps(beatmaps)
            }.onFailure { e ->
                if (e is DataIntegrityViolationException) return@onFailure
                log.warn(
                    "谱面数据访问对象层：保存 ${beatmaps.joinToString(", ") { it.beatmapID.toString() }} 等谱面失败：",
                    e
                )
            }
        }
    }

    private fun saveBeatmaps(beatmaps: Collection<Beatmap>) {
        val exists = beatmapRepository.exists(beatmaps.map { it.beatmapID }).toSet()

        val s = beatmaps.filterNot { it.beatmapID in exists }.map { it.toEntity() }

        beatmapRepository.saveAll(s)
    }

    fun saveBeatmapset(beatmapset: Beatmapset): BeatmapsetLite {
        return beatmapsetRepository.saveAndFlush(beatmapset.toEntity())
    }

    fun saveBeatmapsets(beatmapsets: Collection<Beatmapset>) {
        val exists = beatmapsetRepository.exists(beatmapsets.map { it.beatmapsetID }).toSet()

        val s = beatmapsets.filterNot { it.beatmapsetID in exists }.map { it.toEntity() }
        beatmapsetRepository.saveAllAndFlush(s)
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

//    fun findSetByUpdateAtAscend(time: LocalDateTime, limit: Int = 500): List<BeatmapsetExtendLite> {
//        return extendBeatmapSetRepository.findByUpdateAtAscend(time, limit)
//    }

    fun updateFailTimeByBeatmapsetID(s: Beatmapset): Int {
        return extendBeatmapSetRepository.updateFailTimeByBeatmapsetID(
            s.beatmapsetID, s.animeCover, s.favouriteCount,
            s.offset, s.playCount, s.spotlight,
            s.trackID, s.discussionLocked, s.rating,
            s.ratings.toTypedArray()
        )
    }

    fun findMapByUpdateAtAscend(time: LocalDateTime, limit: Int = 500): List<BeatmapExtendLite> {
        return extendBeatmapRepository.findByUpdateAtAscend(time, limit)
    }

    fun updateFailTimeByBeatmapID(beatmap: Beatmap): Int {
        return extendBeatmapRepository.updateFailTimeByBeatmapID(
            beatmapID = beatmap.beatmapID,
            lazerOnly = beatmap.lazerOnly,
            fails = IntArrayCompressor.intArrayToByteArray(beatmap.retries),
            exits = IntArrayCompressor.intArrayToByteArray(beatmap.fails),
            owners = beatmap.owners?.map { o -> o.toEntity() }
                ?.let { owners -> JacksonUtil.objectToJson(owners) })
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

            val savedSetMap = mutableMapOf<Long, BeatmapsetExtendLite>()

            validBeatmaps.mapNotNull { it.beatmapset }
                .distinctBy { it.beatmapsetID }
                .forEach { s ->
                    val id = s.beatmapsetID

                    val entity = s.toExtendEntity()

                    extendBeatmapSetRepository.upsert(entity)

                    savedSetMap[id] = entity
                }

            validBeatmaps.forEach { beatmap ->
                val associatedSet = savedSetMap[beatmap.beatmapsetID]
                if (associatedSet != null) {
                    runCatching {
                        val entity = beatmap.toExtendEntity(associatedSet)

                        extendBeatmapRepository.upsert(entity)
                    }.onFailure { e ->
                        if (e is DataIntegrityViolationException) return@onFailure
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

        val set = beatmap.beatmapset!!

        val hasBeatmap = extendBeatmapRepository.existsByBeatmapID(beatmap.beatmapID)
        val hasBeatmapset = extendBeatmapSetRepository.existsByBeatmapsetID(beatmap.beatmapsetID)

        if (hasBeatmap && hasBeatmapset) {
            updateFailTimeByBeatmapID(beatmap)
            updateFailTimeByBeatmapsetID(set)
            return
        }

        val setEntity = set.toExtendEntity()
        val mapEntity = beatmap.toExtendEntity(setEntity)

        extendBeatmapSetRepository.upsert(setEntity)
        extendBeatmapRepository.upsert(mapEntity)
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
            failTimes = b.readFailTimesAsData()
            owners =
                b.owners?.let { JacksonUtil.parseObjectList(it, NanoUserLite::class.java) }?.map { it.toModel() }
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
        failTimes = b.readFailTimesAsData()
        maxCombo = b.maxCombo
        owners = b.owners?.let { JacksonUtil.parseObjectList(it, NanoUserLite::class.java) }?.map { it.toModel() }

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

    fun getBeatmapsetIDFromExtend(beatmapID: Long): Long? {
        return extendBeatmapRepository.findByBeatmapID(beatmapID)?.beatmapset?.beatmapsetID
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BeatmapDao::class.java)
    }
}
