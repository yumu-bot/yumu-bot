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

    fun getMapSetLite(id: Long): Optional<MapSetLite> {
        return beatMapRepository.getMapSetByBid(id)
    }

    fun getMapSetLite(id: Int): Optional<MapSetLite> {
        return getMapSetLite(id.toLong())
    }

    fun getAllBeatmapHitLength(ids: Collection<Long>): List<BeatmapHitLengthResult> {
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
            b.beatMapSet = fromMapsetLite(bl.mapSet)
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

        fun fromMapsetLite(mapSet: MapSetLite): BeatMapSet {
            val s = BeatMapSet()
            s.beatMapSetID = mapSet.id.toLong()
            s.creatorID = mapSet.mapperId.toLong()
            s.creator = mapSet.creator
            val cover = Covers()
            cover.cover = mapSet.cover
            cover.cover2x = mapSet.cover
            cover.card = mapSet.card
            cover.card2x = mapSet.card
            cover.list = mapSet.list
            cover.list2x = mapSet.list
            cover.slimcover = mapSet.slimcover
            cover.slimcover2x = mapSet.slimcover
            s.covers = cover

            s.nsfw = mapSet.nsfw
            s.storyboard = mapSet.storyboard
            s.source = mapSet.source
            s.status = mapSet.status
            s.playCount = mapSet.playCount
            s.favouriteCount = mapSet.favourite
            s.title = mapSet.title
            s.titleUnicode = mapSet.titleUTF
            s.artist = mapSet.artist
            s.artistUnicode = mapSet.artistUTF
            s.legacyThreadUrl = mapSet.legacyUrl

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
