package com.now.nowbot.dao

import com.now.nowbot.entity.BeatmapLite
import com.now.nowbot.entity.BeatmapLite.BeatmapHitLengthResult
import com.now.nowbot.entity.MapSetLite
import com.now.nowbot.entity.TagLite
import com.now.nowbot.mapper.BeatmapRepository
import com.now.nowbot.mapper.BeatmapsetRepository
import com.now.nowbot.mapper.TagRepository
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.Tag
import org.springframework.stereotype.Component
import java.util.*

@Component
class BeatmapDao(
    private val beatmapsetRepository: BeatmapsetRepository,
    private val beatmapRepository: BeatmapRepository,
    private val tagRepository: TagRepository
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

    fun getBeatMapLite(id: Int): BeatmapLite {
        return getBeatMapLite(id.toLong())
    }

    fun getBeatMapLite(id: Long): BeatmapLite {
        val lite = beatmapRepository.findById(id)
        if (lite.isEmpty) {
            throw NullPointerException("not found")
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
            s.favouriteCount = set.favourite
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

            if (mapSet.availability != null) {
                s.availabilityDownloadDisable = mapSet.availability!!.downloadDisabled
            }
            s.nsfw = mapSet.nsfw
            s.storyboard = mapSet.storyboard
            s.legacyUrl = mapSet.legacyThreadUrl

            s.mapperId = mapSet.creatorID.toInt()
            s.creator = mapSet.creator
            s.source = mapSet.source
            s.status = mapSet.status
            s.playCount = mapSet.playCount
            s.favourite = mapSet.favouriteCount
            s.title = mapSet.title
            s.titleUTF = mapSet.titleUnicode
            s.artist = mapSet.artist
            s.artistUTF = mapSet.artistUnicode

            return s
        }
    }
}
