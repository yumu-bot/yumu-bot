package com.now.nowbot.dao

import com.now.nowbot.entity.MaiChartLite
import com.now.nowbot.entity.MaiSongLite
import com.now.nowbot.mapper.MaiChartLiteRepository
import com.now.nowbot.mapper.MaiSongLiteRepository
import com.now.nowbot.model.JsonData.MaiSong
import jakarta.persistence.Transient
import org.springframework.stereotype.Component

@Component
class MaiDao (
    val maiSongLiteRepository: MaiSongLiteRepository,
    val maiChartLiteRepository: MaiChartLiteRepository,
) {

    fun findMaiSongById(id: Int): MaiSong {
        val songOpt = maiSongLiteRepository.findById(id)
        if (songOpt.isEmpty) {
            throw IllegalArgumentException("Song not found")
        }
        val song = songOpt.get()
        val charts = maiChartLiteRepository
            .findAllById(song.chartIDs.asList())
            .toCollection(ArrayList())
        song.charts = charts
        return song.toModel()

    }

    @Transient
    fun saveMaiSong(song: MaiSong) {
        val charts = song.getChartLite()
        val chartData = maiChartLiteRepository.saveAll(charts)
        val songLite = MaiSongLite.from(song)
        maiSongLiteRepository.save(songLite)
    }

    @Transient
    fun deleteMaiSongById(id: Int) {
        val songOpt = maiSongLiteRepository.findById(id)
        if (songOpt.isEmpty) return
        val song = songOpt.get()
        deleteMaiChartsByIds(song.chartIDs.toList())
        maiSongLiteRepository.deleteById(id)
    }

    @Transient
    fun deleteMaiChartsByIds(ids: Iterable<Int>) {
        maiChartLiteRepository.deleteAllById(ids)
    }

    fun MaiSong.getChartLite(): List<MaiChartLite> {
        if (this.charts.size != this.chartIDs.size) {
            throw IllegalArgumentException("Chart size not match")
        }

        return charts.mapIndexed { i, chart ->
            MaiChartLite.from(chartIDs[i], chart)
        }
    }
}
