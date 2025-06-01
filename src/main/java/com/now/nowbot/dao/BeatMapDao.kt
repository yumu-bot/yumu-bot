package com.now.nowbot.dao

import com.now.nowbot.entity.BeatmapLite
import com.now.nowbot.entity.BeatmapLite.BeatmapHitLengthResult
import com.now.nowbot.entity.MapSetLite
import com.now.nowbot.entity.TagLite
import com.now.nowbot.mapper.BeatMapRepository
import com.now.nowbot.mapper.BeatMapSetRepository
import com.now.nowbot.mapper.TagRepository
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.BeatMapSet
import com.now.nowbot.model.json.Covers
import com.now.nowbot.model.json.Tag
import org.springframework.stereotype.Component
import java.util.*

@Component
class BeatMapDao(
    private val beatMapSetRepository: BeatMapSetRepository,
    private val beatMapRepository: BeatMapRepository,
    private val tagRepository: TagRepository
) {
    fun saveMap(beatMap: BeatMap): BeatmapLite {
        val mapSet = beatMap.beatMapSet
        if (mapSet != null) {
            beatMapSetRepository.save(fromMapSetModel(mapSet))
        }
        return beatMapRepository.save(fromBeatmapModel(beatMap))
    }

    fun saveAllMapSet(beatMapSet: List<BeatMapSet>) {
        val s = beatMapSet.map { fromMapSetModel(it) }
        beatMapSetRepository.saveAll(s)
    }

    fun saveAllMap(beatMap: List<BeatMap>) {
        val s = beatMap.map { fromBeatmapModel(it) }
        beatMapRepository.saveAll(s)
    }

    fun saveMapSet(beatMapSet: BeatMapSet): MapSetLite {
        return beatMapSetRepository.save(fromMapSetModel(beatMapSet))
    }

    fun getBeatMapLite(id: Int): BeatmapLite {
        return getBeatMapLite(id.toLong())
    }

    fun getBeatMapLite(id: Long): BeatmapLite {
        val lite = beatMapRepository.findById(id)
        if (lite.isEmpty) {
            throw NullPointerException("not found")
        }
        return lite.get()
    }

    fun getBeatMapSetLite(id: Long): Optional<MapSetLite> {
        return beatMapRepository.getMapSetByBid(id)
    }

    fun getBeatMapSetLite(id: Int): Optional<MapSetLite> {
        return getBeatMapSetLite(id.toLong())
    }

    fun getBeatMapsHitLength(ids: Collection<Long>): List<BeatmapHitLengthResult> {
        return beatMapRepository.getBeatmapHitLength(ids)
    }

    fun saveTag(tags: Collection<Tag>) {
        tagRepository.saveAll(tags.map { TagLite.from(it) })
    }

    fun getTag(id: Int): Tag? {
        return tagRepository.findById(id).get().toModel()
    }

    companion object {
        fun fromBeatmapLite(bl: BeatmapLite): BeatMap {
            val b = bl.toBeatMap()
            b.beatMapSet = fromBeatMapSetLite(bl.mapSet)
            return b
        }

        fun fromBeatmapModel(b: BeatMap): BeatmapLite {
            val s = BeatmapLite(b)
            var mapSet: MapSetLite? = null
            if (b.beatMapSet != null) {
                mapSet = fromMapSetModel(b.beatMapSet!!)
            }
            s.mapSet = mapSet
            return s
        }

        private fun fromBeatMapSetLite(set: MapSetLite): BeatMapSet {
            val s = BeatMapSet()
            s.beatMapSetID = set.id.toLong()
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

        fun fromMapSetModel(mapSet: BeatMapSet): MapSetLite {
            val s = MapSetLite()
            s.id = Math.toIntExact(mapSet.beatMapSetID)
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
