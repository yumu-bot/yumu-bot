package com.now.nowbot.dao

import com.now.nowbot.entity.*
import com.now.nowbot.mapper.*
import com.now.nowbot.model.json.MaiFit
import com.now.nowbot.model.json.MaiRanking
import com.now.nowbot.model.json.MaiSong
import jakarta.persistence.Transient
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class MaiDao(
    val maiSongLiteRepository: MaiSongLiteRepository,
    val maiChartLiteRepository: MaiChartLiteRepository,
    val maiFitChartLiteRepository: MaiFitChartLiteRepository,
    val maiFitDiffLiteRepository: MaiFitDiffLiteRepository,
    val maiRankLiteRepository: MaiRankLiteRepository,
) {
    fun saveMaiRanking(ranking: List<MaiRanking>) {
        val rankingLite = ranking.mapNotNull {
            if (it.name.isBlank()) null
            else MaiRankingLite.from(it)
        }
        maiRankLiteRepository.saveAll(rankingLite)
    }

    fun getMaiRanking(name: String): MaiRanking? {
        val ranking = maiRankLiteRepository.findById(name)
        return ranking.map { it.toModel() }.getOrNull()
    }

    fun getAllMaiRanking(): List<MaiRanking> {
        return maiRankLiteRepository.findAll().map { it.toModel() }
    }

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

    fun getAllMaiSong(): List<MaiSong> {
        val songs = maiSongLiteRepository.findAll()
        songs.forEach {
            val charts = maiChartLiteRepository
                .findAllById(it.chartIDs.asList())
                .toCollection(ArrayList())
            it.charts = charts
        }
        return songs.map { it.toModel() }
    }

    @Transient
    fun saveMaiSong(song: MaiSong) {
        val charts = song.getChartLite()
        maiChartLiteRepository.saveAll(charts)
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
            maiFitChartLiteRepository.findBySongIDAndSort(chart.songID, chart.sort)?.let {
                chart.id = it
            }
        }

        maiFitChartLiteRepository.saveAll(allChart)

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
