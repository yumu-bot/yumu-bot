package com.now.nowbot.service.divingFishApiService.impl

import com.now.nowbot.dao.MaiDao
import com.now.nowbot.entity.MaiSongLite
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.enums.MaiVersion.Companion.getNameList
import com.now.nowbot.model.json.*
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.lang.NonNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.stream.Collectors
import kotlin.text.Charsets.UTF_8

@Service
class MaimaiApiImpl(
    private val base: DivingFishBaseService,
    private val maiDao: MaiDao,
) : MaimaiApiService {
    private val path = base.maimaiPath!!

    @JvmRecord
    private data class MaimaiBestScoreQQBody(val qq: Long, val b50: Boolean)

    @JvmRecord
    private data class MaimaiBestScoreNameBody(val username: String, val b50: Boolean)

    @JvmRecord
    private data class MaimaiByVersionQQBody(
        val qq: Long,
        @field:NonNull @param:NonNull val version: List<String>
    )

    @JvmRecord
    private data class MaimaiByVersionNameBody(
        val username: String,
        @field:NonNull @param:NonNull val version: List<String>
    )

    override fun getMaimaiBest50(qq: Long): MaiBestScore {
        val b = MaimaiBestScoreQQBody(qq, true)

        return base.divingFishApiWebClient!!
            .post()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder.path("api/maimaidxprober/query/player").build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(b), MaimaiBestScoreQQBody::class.java)
            .headers { headers: HttpHeaders? -> base.insertJSONHeader(headers) }
            .retrieve()
            .bodyToMono(MaiBestScore::class.java)
            .block() ?: MaiBestScore()
    }

    override fun getMaimaiBest50(username: String): MaiBestScore {
        val b = MaimaiBestScoreNameBody(username, true)

        return base.divingFishApiWebClient!!
            .post()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder.path("api/maimaidxprober/query/player").build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(b), MaimaiBestScoreNameBody::class.java)
            .retrieve()
            .bodyToMono(MaiBestScore::class.java)
            .block() ?: MaiBestScore()
    }

    override fun getMaimaiScoreByVersion(
        username: String,
        versions: MutableList<MaiVersion>
    ): MaiVersionScore {
        val b = MaimaiByVersionNameBody(username, getNameList(versions))

        return base.divingFishApiWebClient!!
            .post()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder.path("api/maimaidxprober/query/plate").build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(b), MaimaiByVersionNameBody::class.java)
            .headers { headers: HttpHeaders? -> base.insertJSONHeader(headers) }
            .retrieve()
            .bodyToMono(MaiVersionScore::class.java)
            .block() ?: MaiVersionScore()
    }

    override fun getMaimaiScoreByVersion(
        qq: Long,
        versions: MutableList<MaiVersion>
    ): MaiVersionScore {
        val b = MaimaiByVersionQQBody(qq, getNameList(versions))

        return base.divingFishApiWebClient!!
            .post()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder.path("api/maimaidxprober/query/plate").build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(b), MaimaiByVersionQQBody::class.java)
            .headers { headers: HttpHeaders? -> base.insertJSONHeader(headers) }
            .retrieve()
            .bodyToMono(MaiVersionScore::class.java)
            .block() ?: MaiVersionScore()
    }

    override fun getMaimaiCover(songID: Long): ByteArray {
        val song = getStandardisedSongID(songID)
        val f = path.resolve("Cover").resolve("$song.png")

        if (Files.isRegularFile(f))
            try {
                return Files.readAllBytes(f)
            } catch (ignored: IOException) {
            }

        return getMaimaiCoverFromAPI(songID)
    }

    private fun getStandardisedSongID(songID: Long?): String {
        val id =
            if (songID == null) {
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
        val cover =
            try {
                base.divingFishApiWebClient!!
                    .get()
                    .uri { uriBuilder: UriBuilder ->
                        uriBuilder.path("covers/$song.png").build()
                    }
                    .retrieve()
                    .bodyToMono(ByteArray::class.java)
                    .block()
            } catch (e: WebClientResponseException.NotFound) {
                base.divingFishApiWebClient!!
                    .get()
                    .uri { uriBuilder: UriBuilder ->
                        uriBuilder.path("covers/00000.png").build()
                    }
                    .retrieve()
                    .bodyToMono(ByteArray::class.java)
                    .block()
            }

        return cover!!
    }

    override fun getMaimaiSongLibrary(): Map<Int, MaiSong> {
        return getMaimaiSongLibraryFromFile()
        // TODO 这个有问题
        // return maiDao.maiSongLiteRepository.findAll().filter(Objects::nonNull).stream().map((MaiSongLite::toModel))
        //    .collect(Collectors.toMap(MaiSong::songID) { it })
    }

    override fun getMaimaiSong(songID: Long): MaiSong {
        return getMaimaiSongLibraryFromFile().get(songID.toInt()) ?: MaiSong()
        //return maiDao.maiSongLiteRepository.findById(songID.toInt()).filter(Objects::nonNull).stream().map(MaiSongLite::toModel).toList().first()
    }

    override fun getMaimaiRank(): Map<String, Int> {
        return getMaimaiRankLibraryFromFile()
    }

    override fun getMaimaiChartData(songID: Long): List<MaiFit.ChartData> {
        return getMaimaiFitLibraryFromFile().charts.get(songID.toString()) ?: listOf()
        //return maiDao.getMaiFitChartDataBySongID(songID.toInt())
    }

    override fun getMaimaiDiffData(difficulty: String): MaiFit.DiffData {
        return getMaimaiFitLibraryFromFile().diffData.get(difficulty) ?: MaiFit.DiffData()
        //return maiDao.getMaiFitDiffDataByDifficulty(difficulty)
    }

    // @Deprecated("请使用 From Database")
    private fun getMaimaiSongLibraryFromFile(): Map<Int, MaiSong> {
        val song: List<MaiSong>

        if (isRegularFile("data-songs.json")) {
            song = parseFileList("data-songs.json", MaiSong::class.java)
        } else {
            log.info("maimai: 本地歌曲库不存在，获取 API 版本")
            song = JacksonUtil.parseObjectList(maimaiSongLibraryFromAPI, MaiSong::class.java)
        }

        return song.stream().collect(Collectors.toMap(MaiSong::songID) { s: MaiSong -> s })
    }

    // @Deprecated("请使用 From Database")
    private fun getMaimaiRankLibraryFromFile(): Map<String, Int> {
        val ranking: List<MaiRanking>

        if (isRegularFile("data-songs.json")) {
            ranking = parseFileList("data-ranking.json", MaiRanking::class.java)
        } else {
            log.info("maimai: 本地排名库不存在，获取 API 版本")
            ranking = JacksonUtil.parseObjectList(maimaiRankLibraryFromAPI, MaiRanking::class.java)
        }

        return ranking.stream().collect(Collectors.toMap(MaiRanking::name, MaiRanking::rating))
    }

    // @Deprecated("请使用 From Database")
    private fun getMaimaiFitLibraryFromFile(): MaiFit {
        if (isRegularFile("data-fit.json")) {
            return parseFile("data-fit.json", MaiFit::class.java)
                ?: return JacksonUtil.parseObject(maimaiFitLibraryFromAPI, MaiFit::class.java)
        } else {
            log.info("maimai: 本地统计库不存在，获取 API 版本")
            return JacksonUtil.parseObject(maimaiFitLibraryFromAPI, MaiFit::class.java)
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

        for(s in songs) {
            maiDao.saveMaiSong(s)
        }
        log.info("maimai: 歌曲数据库已更新")
    }

    override fun updateMaimaiRankLibraryDatabase() {

    }

    override fun updateMaimaiFitLibraryDatabase() {
        val fit = JacksonUtil.parseObject(maimaiFitLibraryFromAPI, MaiFit::class.java)
        maiDao.saveMaiFit(fit)
        log.info("maimai: 统计数据库已更新")
    }

    @Throws(
        WebClientResponseException.Forbidden::class,
        WebClientResponseException.BadGateway::class
    )
    override fun getMaimaiSongScore(qq: Long, songID: Int): MaiScore {
        return MaiScore()
    }

    @Throws(
        WebClientResponseException.Forbidden::class,
        WebClientResponseException.BadGateway::class
    )
    override fun getMaimaiSongsScore(qq: Long, songIDs: List<Int>): List<MaiScore> {
        return listOf()
    }

    @Throws(
        WebClientResponseException.Forbidden::class,
        WebClientResponseException.BadGateway::class
    )
    override fun getMaimaiSongScore(username: String, songID: Int): MaiScore {
        return MaiScore()
    }

    @Throws(
        WebClientResponseException.Forbidden::class,
        WebClientResponseException.BadGateway::class
    )
    override fun getMaimaiSongsScore(username: String, songIDs: List<Int>): List<MaiScore> {
        return listOf()
    }

    @Throws(
        WebClientResponseException.Forbidden::class,
        WebClientResponseException.BadGateway::class
    )
    override fun getMaimaiFullScores(qq: Long): MaiBestScore {
        return base.divingFishApiWebClient!!
            .get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("api/maimaidxprober/dev/player/records")
                    .queryParam("qq", qq)
                    .build()
            }
            .headers { headers: HttpHeaders? -> base.insertDeveloperHeader(headers) }
            .retrieve()
            .bodyToMono(MaiBestScore::class.java)
            .block() ?: MaiBestScore()
    }

    @Throws(
        WebClientResponseException.Forbidden::class,
        WebClientResponseException.BadGateway::class
    )
    override fun getMaimaiFullScores(username: String): MaiBestScore {
        return base.divingFishApiWebClient!!
            .get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("api/maimaidxprober/dev/player/records")
                    .queryParam("username", username)
                    .build()
            }
            .headers { headers: HttpHeaders? -> base.insertDeveloperHeader(headers) }
            .retrieve()
            .bodyToMono(MaiBestScore::class.java)
            .block() ?: MaiBestScore()
    }

    override fun getMaimaiPossibleSong(text: String): MaiSong {
        val s = getMaimaiPossibleSongs(text) ?: return MaiSong()

        return s.entries.first().value
    }

    override fun getMaimaiPossibleSongs(text: String): Map<Double, MaiSong>? {
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
    }

    private val maimaiSongLibraryFromAPI: String
        get() =
            base.divingFishApiWebClient!!
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/music_data").build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: ""

    private val maimaiRankLibraryFromAPI: String
        get() =
            base.divingFishApiWebClient!!
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/rating_ranking").build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: ""

    private val maimaiFitLibraryFromAPI: String
        get() =
            base.divingFishApiWebClient!!
                .get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/maimaidxprober/chart_stats").build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: ""

    private fun <T> parseFile(fileName: String, clazz: Class<T>): T? {
        val file = path.resolve(fileName)
        try {
            val s = Files.readString(file)

            if (Files.isRegularFile(file)) {
                return JacksonUtil.parseObject(s, clazz)
            } else {
                log.info("maimai: 文件{}不存在", fileName)
                return null
            }

        } catch (e: IOException) {
            log.error("maimai: 获取文件失败", e)
            return null
        }
    }

    private fun <T> parseFileList(fileName: String, clazz: Class<T>): List<T> {
        val file = path.resolve(fileName)
        try {
            val s = Files.readString(file)
            return JacksonUtil.parseObjectList(s, clazz)
        } catch (e: IOException) {
            log.error("maimai: 获取文件失败", e)
            return listOf()
        }
    }

    private fun saveFile(data: String, fileName: String, dictionaryName: String) {
        val file = path.resolve(fileName)

        try {
            if (isRegularFile(fileName)) {
                Files.writeString(file, data, UTF_8)
                log.info("maimai: 已更新{}库", dictionaryName)
            } else if (Files.isWritable(path)) {
                Files.writeString(file, data, UTF_8)
                log.info("maimai: 已保存{}库", dictionaryName)
            } else {
                log.info("maimai: 未保存{}库", dictionaryName)
            }
        } catch (e: IOException) {
            log.error(String.format("maimai: %s库保存失败", dictionaryName), e)
        }
    }

    private fun isRegularFile(fileName: String): Boolean {
        return Files.isRegularFile(path.resolve(fileName))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MaimaiApiImpl::class.java)
    }
}
