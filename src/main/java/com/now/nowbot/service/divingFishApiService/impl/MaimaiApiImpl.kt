package com.now.nowbot.service.divingFishApiService.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.enums.MaiVersion.Companion.getNameList
import com.now.nowbot.model.maimai.*
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.file.Files
import kotlin.math.abs
import kotlin.text.Charsets.UTF_8

@Service class MaimaiApiImpl(
    private val base: DivingFishBaseService,
    private val maiDao: MaiDao,
) : MaimaiApiService {
    private val path = base.maimaiPath!!

    @JvmRecord private data class MaimaiBestQQRequestBody(val qq: Long, val b50: Boolean)

    @JvmRecord private data class MaimaiBestNameRequestBody(val username: String, val b50: Boolean)

    @JvmRecord private data class MaimaiVersionQQRequestBody(
        val qq: Long, val version: List<String>
    )

    @JvmRecord private data class MaimaiVersionNameRequestBody(
        val username: String, val version: List<String>
    )

    @JvmRecord //傻逼吧外面怎么还有一层
    private data class MaimaiAliasResponseBody(
        @JsonProperty("aliases") val aliases: List<MaiAlias>
    )

    override fun getMaimaiBest50(qq: Long): MaiBestScore {
        val b = MaimaiBestQQRequestBody(qq, true)

        return try {
            base.divingFishApiWebClient.post().uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/query/player").build()
                }.contentType(MediaType.APPLICATION_JSON).body(Mono.just(b), MaimaiBestQQRequestBody::class.java)
                .headers { headers: HttpHeaders? -> base.insertJSONHeader(headers) }.retrieve()
                .bodyToMono(MaiBestScore::class.java).block()!!
        } catch (e: WebClientRequestException) {
            log.error("水鱼查分器：获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "水鱼查分器")
        }
    }

    override fun getMaimaiBest50(username: String): MaiBestScore {
        val b = MaimaiBestNameRequestBody(username, true)

        return try {
            base.divingFishApiWebClient.post().uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/query/player").build()
                }.contentType(MediaType.APPLICATION_JSON).body(Mono.just(b), MaimaiBestNameRequestBody::class.java)
                .retrieve().bodyToMono(MaiBestScore::class.java).block()!!
        } catch (e: WebClientRequestException) {
            log.error("水鱼查分器：获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "水鱼查分器")
        }
    }

    override fun getMaimaiScoreByVersion(
        username: String, versions: List<MaiVersion>
    ): MaiVersionScore {
        val b = MaimaiVersionNameRequestBody(username, getNameList(versions))

        return try {
            base.divingFishApiWebClient.post().uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/query/plate").build()
                }.contentType(MediaType.APPLICATION_JSON).body(Mono.just(b), MaimaiVersionNameRequestBody::class.java)
                .headers { headers: HttpHeaders? -> base.insertJSONHeader(headers) }.retrieve()
                .bodyToMono(MaiVersionScore::class.java).block()!!
        } catch (e: WebClientRequestException) {
            log.error("水鱼查分器：获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "水鱼查分器")
        }
    }

    override fun getMaimaiScoreByVersion(
        qq: Long, versions: List<MaiVersion>
    ): MaiVersionScore {
        val b = MaimaiVersionQQRequestBody(qq, getNameList(versions))

        return try {
            base.divingFishApiWebClient.post().uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/query/plate").build()
                }.contentType(MediaType.APPLICATION_JSON).body(Mono.just(b), MaimaiVersionQQRequestBody::class.java)
                .headers { headers: HttpHeaders? -> base.insertJSONHeader(headers) }.retrieve()
                .bodyToMono(MaiVersionScore::class.java).block()!!
        } catch (e: WebClientRequestException) {
            log.error("水鱼查分器：获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "水鱼查分器")
        }
    }

    override fun getMaimaiCover(songID: Long): ByteArray {
        val song = getStandardisedSongID(songID)
        val f = path.resolve("Cover").resolve("$song.png")

        if (Files.isRegularFile(f)) try {
            return Files.readAllBytes(f)
        } catch (ignored: IOException) {
        }

        return getMaimaiCoverFromAPI(songID)
    }

    private fun getStandardisedSongID(songID: Long?): String {
        val id = if (songID == null) {
            0L
        } else if (songID == 1235L) {
            11235L // 这是水鱼的 bug，不关我们的事
        } else if (songID in 10001..10999) {
            songID - 10000L
        } else if (songID >= 100000L) {
            songID - 100000L
        } else {
            songID
        }

        return String.format("%05d", id)
    }

    override fun getMaimaiCoverFromAPI(songID: Long): ByteArray {
        val song = getStandardisedSongID(songID)
        val cover = try {
            base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("covers/$song.png").build()
                }.retrieve().bodyToMono(ByteArray::class.java).block()!!
        } catch (e: WebClientResponseException.NotFound) {
            base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("covers/00000.png").build()
                }.retrieve().bodyToMono(ByteArray::class.java).block()!!
        }

        return cover
    }

    override fun getMaimaiSongLibrary(): Map<Int, MaiSong> { //return getMaimaiSongLibraryFromFile()
        return maiDao.getAllMaiSong().map {
            insertMaimaiAlias(it)
            it
        }.associateBy { it.songID }
    }

    override fun getMaimaiSong(songID: Long): MaiSong? { //return getMaimaiSongLibraryFromFile()[songID.toInt()] ?: MaiSong()
        val o = maiDao.findMaiSongByID(songID.toInt())
        insertMaimaiAlias(o)
        return o
    }

    override fun getMaimaiRank(): Map<String, Int> { //return getMaimaiRankLibraryFromFile()
        return maiDao.getAllMaiRanking().associate { it.name to it.rating }
    }


    override fun getMaimaiSurroundingRank(rating: Int): Map<String, Int> {
        return maiDao.getSurroundingMaiRanking(rating).sortedBy { abs(rating - it.rating) }.associate { it.name to it.rating }
    }

    override fun getMaimaiChartData(songID: Long): List<MaiFit.ChartData> { //return getMaimaiFitLibraryFromFile().charts[songID.toString()] ?: listOf()
        return maiDao.getMaiFitChartDataBySongID(songID.toInt())
    }

    override fun getMaimaiDiffData(difficulty: String): MaiFit.DiffData { //return getMaimaiFitLibraryFromFile().diffData[difficulty] ?: MaiFit.DiffData()
        return maiDao.getMaiFitDiffDataByDifficulty(difficulty)
    }

    override fun getMaimaiAlias(songID: Long): MaiAlias? {
        return getMaimaiAlias(songID.toInt())
    }

    override fun getMaimaiAlias(songID: Int): MaiAlias? {
        return maiDao.getMaiAliasByID((songID % 10000))
    }

    override fun getMaimaiAliasLibrary(): Map<Int, List<String>> {
        return maiDao.getAllMaiAliases().associate { it.songID to it.alias }
    }

    override fun insertMaimaiAlias(songs: List<MaiSong>?) {
        if (songs.isNullOrEmpty()) return

        val actions = songs.map {
            return@map AsyncMethodExecutor.Runnable {
                it.alias = getMaimaiAlias(it.songID)?.alias?.firstOrNull()
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun insertMaimaiAlias(song: MaiSong?) {
        if (song != null) {
            song.alias = getMaimaiAlias(song.songID)?.alias?.firstOrNull()
        }
    }

    override fun insertMaimaiAliasForScore(scores: List<MaiScore>?) {
        if (scores.isNullOrEmpty()) return

        val actions = scores.map {
            return@map AsyncMethodExecutor.Runnable {
                it.alias = getMaimaiAlias(it.songID)?.alias?.firstOrNull()
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun insertMaimaiAliasForScore(score: MaiScore?) {
        if (score != null) {
            score.alias = getMaimaiAlias(score.songID)?.alias?.firstOrNull()
        }
    }

    override fun insertSongData(scores: List<MaiScore>) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Runnable {
                if (it.songID != 0L) {
                    val o = getMaimaiSong(it.songID) ?: MaiSong()
                    insertSongData(it, o)
                }
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun insertSongData(score: MaiScore, song: MaiSong) {
        score.artist = song.info.artist

        if (song.charts.isEmpty() || score.index >= song.charts.size) return

        val chart = song.charts[score.index]
        val notes = chart.notes

        score.charter = chart.charter
        score.max = 3 * (notes.tap + notes.touch + notes.hold + notes.slide + notes.break_)
    }

    override fun insertPosition(scores: List<MaiScore>, isBest35: Boolean) {
        if (scores.isEmpty()) return

        for (i in scores.indices) {
            val s = scores[i]

            if (isBest35) {
                s.position = (i + 1)
            } else {
                s.position = (i + 36)
            }
        }
    }

    @Deprecated("请使用 From Database") private fun getMaimaiSongLibraryFromFile(): Map<Int, MaiSong> {
        val song: List<MaiSong>

        if (isRegularFile("data-songs.json")) {
            song = parseFileList("data-songs.json", MaiSong::class.java)
        } else {
            log.info("舞萌: 本地歌曲库不存在，获取 API 版本")
            song = JacksonUtil.parseObjectList(maimaiSongLibraryFromAPI, MaiSong::class.java)
        }

        return song.associateBy { it.songID }
    }

    @Deprecated("请使用 From Database") private fun getMaimaiRankLibraryFromFile(): Map<String, Int> {
        val ranking: List<MaiRanking>

        if (isRegularFile("data-songs.json")) {
            ranking = parseFileList("data-ranking.json", MaiRanking::class.java)
        } else {
            log.info("舞萌: 本地排名库不存在，获取 API 版本")
            ranking = JacksonUtil.parseObjectList(maimaiRankLibraryFromAPI, MaiRanking::class.java)
        }

        return ranking.associate { it.name to it.rating }
    }

    @Deprecated("请使用 From Database") private fun getMaimaiFitLibraryFromFile(): MaiFit {
        if (isRegularFile("data-fit.json")) {
            return parseFile("data-fit.json", MaiFit::class.java) ?: return JacksonUtil.parseObject(
                maimaiFitLibraryFromAPI,
                MaiFit::class.java
            )
        } else {
            log.info("舞萌: 本地统计库不存在，获取 API 版本")
            return JacksonUtil.parseObject(maimaiFitLibraryFromAPI, MaiFit::class.java)
        }
    }

    @Deprecated("请使用 From Database") private fun getMaimaiAliasLibraryFromFile(): List<MaiAlias> {
        if (isRegularFile("data-aliases.json")) {
            return parseFile("data-aliases.json", MaimaiAliasResponseBody::class.java)?.aliases
                ?: return JacksonUtil.parseObject(
                    maimaiAliasLibraryFromAPI,
                    MaimaiAliasResponseBody::class.java
                ).aliases
        } else {
            log.info("舞萌: 本地外号库不存在，获取 API 版本")
            return JacksonUtil.parseObject(maimaiAliasLibraryFromAPI, MaimaiAliasResponseBody::class.java).aliases
        }
    }

    override fun updateMaimaiSongLibraryFile() {
        saveFile(maimaiSongLibraryFromAPI, "data-songs.json", "歌曲")
    }

    override fun updateMaimaiRankLibraryFile() {
        saveFile(maimaiRankLibraryFromAPI, "data-ranking.json", "排名")
    }

    override fun updateMaimaiFitLibraryFile() {
        saveFile(maimaiFitLibraryFromAPI, "data-fit.json", "统计")
    }

    override fun updateMaimaiSongLibraryDatabase() {
        val songs = JacksonUtil.parseObjectList(maimaiSongLibraryFromAPI, MaiSong::class.java)

        for (s in songs) {
            maiDao.saveMaiSong(s)
        }
        log.info("舞萌: 歌曲数据库已更新")
    }

    override fun updateMaimaiRankLibraryDatabase() {
        val rank = JacksonUtil.parseObjectList(maimaiRankLibraryFromAPI, MaiRanking::class.java)
        maiDao.saveMaiRanking(rank)
        log.info("舞萌: 排名数据库已更新")
    }

    override fun updateMaimaiFitLibraryDatabase() {
        val fit = JacksonUtil.parseObject(maimaiFitLibraryFromAPI, MaiFit::class.java)
        maiDao.saveMaiFit(fit)
        log.info("舞萌: 统计数据库已更新")
    }

    override fun updateMaimaiAliasLibraryDatabase() {
        val alias = JacksonUtil.parseObject(maimaiAliasLibraryFromAPI, MaimaiAliasResponseBody::class.java).aliases
        maiDao.saveMaiAliases(alias)
        log.info("舞萌: 外号数据库已更新")
    }

    @Throws(
        WebClientResponseException.Forbidden::class, WebClientResponseException.BadGateway::class
    ) override fun getMaimaiSongScore(qq: Long, songID: Int): MaiScore {
        return MaiScore()
    }

    @Throws(
        WebClientResponseException.Forbidden::class, WebClientResponseException.BadGateway::class
    ) override fun getMaimaiSongsScore(qq: Long, songIDs: List<Int>): List<MaiScore> {
        return listOf()
    }

    @Throws(
        WebClientResponseException.Forbidden::class, WebClientResponseException.BadGateway::class
    ) override fun getMaimaiSongScore(username: String, songID: Int): MaiScore {
        return MaiScore()
    }

    @Throws(
        WebClientResponseException.Forbidden::class, WebClientResponseException.BadGateway::class
    ) override fun getMaimaiSongsScore(username: String, songIDs: List<Int>): List<MaiScore> {
        return listOf()
    }

    @Throws(
        WebClientResponseException.Forbidden::class, WebClientResponseException.BadGateway::class
    ) override fun getMaimaiFullScores(qq: Long): MaiBestScore {
        return try {
            base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/dev/player/records").queryParam("qq", qq).build()
                }.headers { headers: HttpHeaders? -> base.insertDeveloperHeader(headers) }.retrieve()
                .bodyToMono(MaiBestScore::class.java).block()!!
        } catch (e: WebClientResponseException.Forbidden) {
            throw e
        } catch (e: WebClientResponseException.BadRequest) {
            throw e
        } catch (e: WebClientRequestException) {
            log.error("水鱼查分器：获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "水鱼查分器")
        }
    }

    @Throws(
        WebClientResponseException.Forbidden::class, WebClientResponseException.BadGateway::class
    ) override fun getMaimaiFullScores(username: String): MaiBestScore {
        return try {
            base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/dev/player/records").queryParam("username", username).build()
                }.headers { headers: HttpHeaders? -> base.insertDeveloperHeader(headers) }.retrieve()
                .bodyToMono(MaiBestScore::class.java).block()!!
        } catch (e: WebClientResponseException.Forbidden) {
            throw e
        } catch (e: WebClientResponseException.BadRequest) {
            throw e
        } catch (e: WebClientRequestException) {
            log.error("水鱼查分器：获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "水鱼查分器")
        }
    }

    override fun getMaimaiPossibleSong(text: String): MaiSong? {
        return maiDao.findMaiSongByTitle(text)?.first()/*
        val s = getMaimaiPossibleSongs(text) ?: return MaiSong()

        return s.entries.first().value

         */
    }

    override fun getMaimaiPossibleSongs(text: String): List<MaiSong>? {
        val o = maiDao.findMaiSongByTitle(text)
        insertMaimaiAlias(o)
        return o

        /*
        val songs = getMaimaiSongLibrary()

        val result = mutableMapOf<Double, MaiSong>()

        for (s in songs) {
            val similarity = DataUtil.getStringSimilarity(text, s.value.title)

            if (similarity >= 0.5) {
                result[similarity] = s.value
            }
        }

        if (result.isEmpty()) {
            return null
        }

        return result.toSortedMap().reversed()

         */
    }

    override fun getMaimaiAliasSong(text: String): MaiSong? {
        return getMaimaiAliasSongs(text)?.firstOrNull()
    }

    override fun getMaimaiAliasSongs(text: String): List<MaiSong>? {
        val aliases = getMaimaiAliasLibrary()
        val result = mutableListOf<Triple<MaiSong, Int, Double>>()

        search@ for (e in aliases.entries) {
            for (alias in e.value) {

                val y = DataUtil.getStringSimilarity(text, alias)

                if (y >= 0.5) {
                    val s = maiDao.findMaiSongByID(e.key)
                        ?: maiDao.findMaiSongByID(e.key + 10000) //getMaimaiSong(e.key.toLong()) ?: getMaimaiSong(e.key + 10000L) 避免循环引用

                    if (s != null) {
                        s.alias = e.value.firstOrNull()

                        result.add(Triple(s, s.songID, y))
                        continue@search
                    }
                }
            }
        }

        return if (result.isEmpty()) {
            null
        } else {
            result
                .sortedBy { it.second }
                .sortedByDescending { it.third }
                .map { it.first }
        }
    }

    private val maimaiSongLibraryFromAPI: String
        get() = base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                uriBuilder.path("api/maimaidxprober/music_data").build()
            }.retrieve().bodyToMono(String::class.java).block()!!

    private val maimaiRankLibraryFromAPI: String
        get() = base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                uriBuilder.path("api/maimaidxprober/rating_ranking").build()
            }.retrieve().bodyToMono(String::class.java).block()!!

    private val maimaiFitLibraryFromAPI: String
        get() = base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                uriBuilder.path("api/maimaidxprober/chart_stats").build()
            }.retrieve().bodyToMono(String::class.java).block()!!

    private val maimaiAliasLibraryFromAPI: String
        get() = base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                uriBuilder.scheme("https").host("maimai.lxns.net").path("api/v0/maimai/alias/list").build()
            }.retrieve().bodyToMono(String::class.java).block()!!

    private fun <T> parseFile(fileName: String, clazz: Class<T>): T? {
        val file = path.resolve(fileName)
        try {
            val s = Files.readString(file)

            if (Files.isRegularFile(file)) {
                return JacksonUtil.parseObject(s, clazz)
            } else {
                log.info("舞萌: 文件{}不存在", fileName)
                return null
            }

        } catch (e: IOException) {
            log.error("舞萌: 获取文件失败", e)
            return null
        }
    }

    private fun <T> parseFileList(fileName: String, clazz: Class<T>): List<T> {
        val file = path.resolve(fileName)
        try {
            val s = Files.readString(file)
            return JacksonUtil.parseObjectList(s, clazz)
        } catch (e: IOException) {
            log.error("舞萌: 获取文件失败", e)
            return listOf()
        }
    }

    private fun saveFile(data: String, fileName: String, dictionaryName: String) {
        val file = path.resolve(fileName)

        try {
            if (isRegularFile(fileName)) {
                Files.writeString(file, data, UTF_8)
                log.info("舞萌: 已更新{}库", dictionaryName)
            } else if (Files.isWritable(path)) {
                Files.writeString(file, data, UTF_8)
                log.info("舞萌: 已保存{}库", dictionaryName)
            } else {
                log.info("舞萌: 未保存{}库", dictionaryName)
            }
        } catch (e: IOException) {
            log.error(String.format("舞萌: %s库保存失败", dictionaryName), e)
        }
    }

    private fun isRegularFile(fileName: String): Boolean {
        return Files.isRegularFile(path.resolve(fileName))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MaimaiApiImpl::class.java)
    }
}
