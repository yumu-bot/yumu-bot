package com.now.nowbot.dao

import com.now.nowbot.entity.*
import com.now.nowbot.mapper.*
import com.now.nowbot.model.maimai.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.forEach
import kotlin.jvm.optionals.getOrNull

@Component
class MaiDao(
    private val maiSongLiteRepository: MaiSongLiteRepository,
    private val maiChartLiteRepository: MaiChartLiteRepository,
    private val maiFitChartLiteRepository: MaiFitChartLiteRepository,
    private val maiFitDiffLiteRepository: MaiFitDiffLiteRepository,
    private val maiRankLiteRepository: MaiRankLiteRepository,
    private val maiAliasLiteRepository: MaiAliasLiteRepository,
    private val chuSongLiteRepository: ChuSongLiteRepository,
    private val chuChartLiteRepository: ChuChartLiteRepository,
    private val chuAliasLiteRepository: ChuAliasLiteRepository,
    private val lxMaiSongLiteRepository: LxMaiSongLiteRepository,
    private val lxMaiCollectionLiteRepository: LxMaiCollectionLiteRepository,
    private val lxMaiCollectionSongLiteRepository: LxMaiCollectionRequiredSongLiteRepository
) {
    fun saveMaiRanking(ranking: List<MaiRanking>) {
        val ranks = ranking
            .filterNot { it.name.isBlank() }

        ranks.parallelStream().map { maiRankLiteRepository.saveAndUpdate(it.name, it.rating) }

        /*
        val actions = rankingLite.map {
            return@map AsyncMethodExecutor.Runnable {
                maiRankLiteRepository.saveAndUpdate(it.name, it.rating)
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)

         */

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

    fun findMaiSongByTitle(title:String): List<MaiSong> {
        val songs = maiSongLiteRepository.findByQueryTitleLikeIgnoreCase("%$title%")

        if (songs.isNullOrEmpty()) return listOf()

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

    @Transactional
    fun saveLxMaiCollections(collections: List<LxMaiCollection>) {
        // 这是一个局部缓存，用于确保本次 saveAll 过程中，对象在内存中是唯一的
        val songCache = mutableMapOf<LxMaiCollectionRequiredSongID, LxMaiCollectionRequiredSongLite>()

        // 1. 收集本次 API 数据中所有的 songID
        val allSongIDs = collections
            .flatMap { it.required ?: emptyList() }
            .flatMap { it.songs ?: emptyList() }
            .map { LxMaiCollectionRequiredSongID(it.songID, it.type) }
            .distinct()

        // 2. 预先从数据库查出已经存在的歌曲，存入缓存
        lxMaiCollectionSongLiteRepository.findAllById(allSongIDs).forEach {
            songCache[LxMaiCollectionRequiredSongID(it.songID!!, it.type)] = it
        }

        // 3. 正常执行转换（from 方法内部会优先使用 cache 里的 Managed 对象）
        val entities = collections.map {
            LxMaiCollectionLite.from(it, songCache)
        }

        lxMaiCollectionLiteRepository.saveAll(entities)
    }

    @Transactional
    fun findLxMaiCollections(type: String = "plate"): List<LxMaiCollection> {
        return lxMaiCollectionLiteRepository.findAllByType(type).map { it.toModel() }
    }

    @Transactional
    fun saveLxMaiSongs(songs: List<LxMaiSong>) {
        lxMaiSongLiteRepository.saveAll(
            songs.map { LxMaiSongLite.from(it) }
        )
    }

    fun findLxMaiSongByID(songID: Int): LxMaiSong? {
        return lxMaiSongLiteRepository.findBySongID(songID)?.toModel()
    }

    fun findLxMaiSongByTitle(title: String): List<LxMaiSong> {
        val songs = lxMaiSongLiteRepository.findByQueryTitleLikeIgnoreCase(title)

        return songs.map { it.toModel() }
    }

    @Transactional
    fun saveMaiSong(song: MaiSong) {
        val charts = song.getChartLite()
        maiChartLiteRepository.saveAll(charts)
        val songLite = MaiSongLite.from(song)
        maiSongLiteRepository.save(songLite)
    }

    @Transactional
    fun deleteMaiSongByID(id: Int) {
        val songOpt = maiSongLiteRepository.findById(id)
        if (songOpt.isEmpty) return
        val song = songOpt.get()
        deleteMaiChartsByIds(song.chartIDs.toList())
        maiSongLiteRepository.deleteById(id)
    }

    @Transactional
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

    @Transactional
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

    fun getMaiFitDiffDataByLevel(difficulty: String): MaiFit.DiffData {
        val data = maiFitDiffLiteRepository.findById(difficulty)
        return data.map { it.toModel() }.getOrNull() ?: MaiFit.DiffData()
    }

    @Transactional
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

    @Transactional
    fun saveChuAliases(maiAliases: List<ChuAlias>) {
        val lites = maiAliases.map {ChuAliasLite.from(it) }
        chuAliasLiteRepository.saveAll(lites)
    }

    fun getAllChuAliases(): List<ChuAlias> {
        val aliases = chuAliasLiteRepository.findAll()
        return aliases.map { it.toModel() }
    }

    fun getChuAliasByID(id: Int): ChuAlias? {
        val aliasOpt = chuAliasLiteRepository.findById(id)
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

    @Transactional
    fun saveChuSong(song: ChuSong) {
        val charts = song.getChartLite()
        chuChartLiteRepository.saveAll(charts)
        val songLite = ChuSongLite.from(song)
        chuSongLiteRepository.save(songLite)
    }
}
