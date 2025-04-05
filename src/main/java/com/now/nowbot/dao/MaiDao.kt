package com.now.nowbot.dao

import com.now.nowbot.entity.*
import com.now.nowbot.mapper.*
import com.now.nowbot.model.json.*
import com.now.nowbot.util.AsyncMethodExecutor
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
    val maiAliasLiteRepository: MaiAliasLiteRepository,
    val chuSongLiteRepository: ChuSongLiteRepository,
    val chuChartLiteRepository: ChuChartLiteRepository,
) {
    fun saveMaiRanking(ranking: List<MaiRanking>) {
        val rankingLite = ranking.mapNotNull {
            if (it.name.isBlank()) null
            else MaiRankingLite.from(it)
        }

        val actions = rankingLite.map {
            return@map AsyncMethodExecutor.Supplier<Unit> {
                maiRankLiteRepository.saveAndUpdate(it.name, it.rating)
            }
        }

        AsyncMethodExecutor.AsyncSupplier(actions)
    }

    fun getMaiRanking(name: String): MaiRanking? {
        val ranking = maiRankLiteRepository.findById(name)
        return ranking.map { it.toModel() }.getOrNull()
    }

    fun getAllMaiRanking(): List<MaiRanking> {
        return maiRankLiteRepository.findAll().map { it.toModel() }
    }

    fun getSurroundingMaiRanking(rating: Int = 15000): List<MaiRanking> {
        return maiRankLiteRepository.findSurrounding(rating - 500, rating + 500).map { it.toModel() }
    }

    fun findMaiSongByID(id: Int): MaiSong? {
        val songOpt = maiSongLiteRepository.findById(id)
        if (songOpt.isEmpty) {
            return null
        }
        val song = songOpt.get()
        val charts = maiChartLiteRepository
            .findAllById(song.chartIDs.asList())
            .toCollection(ArrayList())
        song.charts = charts
        return song.toModel()
    }

    fun findMaiSongByTitle(title:String): List<MaiSong>? {
        val songs = maiSongLiteRepository.findByQueryTitleLikeIgnoreCase("%$title%")

        if (songs.isNullOrEmpty()) return null

        songs.forEach {
            val charts = maiChartLiteRepository
                .findAllById(it.chartIDs.asList())
                .toCollection(ArrayList())
            it.charts = charts
        }
        return songs.map { it.toModel() }
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
    fun deleteMaiSongByID(id: Int) {
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
                    MaiFitChartLite.from(id, index, it)
                }
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

    fun saveMaiAliases(maiAliases: List<MaiAlias>) {
        val lites = maiAliases.map { MaiAliasLite.from(it) }

        maiAliasLiteRepository.saveAll(lites)
    }

    fun getAllMaiAliases(): List<MaiAlias> {
        val aliases = maiAliasLiteRepository.findAll()
        return aliases.map { it.toModel() }
    }

    fun getMaiAliasByID(id: Int): MaiAlias? {
        // 他只存 10000 以下的 id，除了两个协作宴谱
        val i = if (id >= 10000) {
            id % 10000
        } else {
            id
        }

        val aliasOpt = maiAliasLiteRepository.findById(i)
        if (aliasOpt.isEmpty) {
            return null
        }
        val alias = aliasOpt.get()
        return alias.toModel()
    }

    fun findChuSongByID(id: Int): ChuSong? {
        val songOpt = chuSongLiteRepository.findById(id)
        if (songOpt.isEmpty) {
            return null
        }
        val song = songOpt.get()
        val charts = chuChartLiteRepository
            .findAllById(song.chartIDs.asList())
            .toCollection(ArrayList())
        song.charts = charts
        return song.toModel()
    }

    fun getAllChuSong(): List<ChuSong> {
        val songs = chuSongLiteRepository.findAll()
        songs.forEach {
            val charts = chuChartLiteRepository
                .findAllById(it.chartIDs.asList())
                .toCollection(ArrayList())
            it.charts = charts
        }
        return songs.map { it.toModel() }
    }

    fun ChuSong.getChartLite(): List<ChuChartLite> {
        if (this.charts.size != this.chartIDs.size) {
            throw IllegalArgumentException("Chart size not match")
        }

        return charts.mapIndexed { i, chart ->
            ChuChartLite.from(chartIDs[i], chart)
        }
    }

    @Transient
    fun saveChuSong(song: ChuSong) {
        val charts = song.getChartLite()
        chuChartLiteRepository.saveAll(charts)
        val songLite = ChuSongLite.from(song)
        chuSongLiteRepository.save(songLite)
    }
}
