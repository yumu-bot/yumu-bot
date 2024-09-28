package com.now.nowbot.dao

import com.now.nowbot.entity.MaiChartLite
import com.now.nowbot.entity.MaiFitChartLite
import com.now.nowbot.entity.MaiFitDiffLite
import com.now.nowbot.entity.MaiSongLite
import com.now.nowbot.mapper.MaiChartLiteRepository
import com.now.nowbot.mapper.MaiFitChartLiteRepository
import com.now.nowbot.mapper.MaiFitDiffLiteRepository
import com.now.nowbot.mapper.MaiSongLiteRepository
import com.now.nowbot.model.json.MaiFit
import com.now.nowbot.model.json.MaiSong
import jakarta.persistence.Transient
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Component
class MaiDao(
    val maiSongLiteRepository: MaiSongLiteRepository,
    val maiChartLiteRepository: MaiChartLiteRepository,
    val maiFitChartLiteRepository: MaiFitChartLiteRepository,
    val maiFitDiffLiteRepository: MaiFitDiffLiteRepository,
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

    @Transient
    fun saveMaiFit(maiFit: MaiFit) {
        val allChart = maiFit
            .charts
            .entries
            .map { (id, charts) ->
                charts.mapIndexed { index, it ->
                    if (it.count == null) return@mapIndexed null
                    MaiFitChartLite.from(id, index, it)
                }.filterNotNull()
            }
            .flatten()

        for (chart in allChart) {
            // 更新策略是 sid 与 level 一致是更新, 否则插入
            if (false/*maiFitChartLiteRepository.existsMaiFitChartLiteBySongIDAndSort(chart.songID, chart.sort)*/) {
                maiFitChartLiteRepository.updateMaiFitChartLiteBySongIDAndSort(chart.songID, chart.sort, chart)
            } else {
                maiFitChartLiteRepository.save(chart)
            }
        }

        val allDiff = maiFit
            .diffData
            .map { (id, diff) -> MaiFitDiffLite.from(id, diff) }
        maiFitDiffLiteRepository.saveAll(allDiff)
    }

    fun getMaiFitChartDataBySongID(songID: Int): List<MaiFit.ChartData> {
        val data = maiFitChartLiteRepository.findMaiFitChartLitesBySongIDOrderBySortAsc(songID)
        var nowIndex = 0
        val result = ArrayList<MaiFit.ChartData>(5)
        for (i in 0..4) {
            if (nowIndex < data.size && data[nowIndex].sort == i) {
                result.add(data[nowIndex].toModel())
                nowIndex++
            } else {
                result.add(MaiFit.ChartData())
            }
        }
        return data.map { it.toModel() }
    }

    fun getMaiFitDiffDataByDifficulty(difficulty: String): MaiFit.DiffData {
        val data = maiFitDiffLiteRepository.findById(difficulty)
        return data.map { it.toModel() }.getOrNull() ?: MaiFit.DiffData()
    }
}
