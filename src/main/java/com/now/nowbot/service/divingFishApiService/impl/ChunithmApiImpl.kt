package com.now.nowbot.service.divingFishApiService.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.model.maimai.ChuAlias
import com.now.nowbot.model.maimai.ChuBestScore
import com.now.nowbot.model.maimai.ChuScore
import com.now.nowbot.model.maimai.ChuSong
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.lang.NonNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.file.Files
import kotlin.text.Charsets.UTF_8

@Service
class ChunithmApiImpl(private val base: DivingFishBaseService, private val maiDao: MaiDao) : ChunithmApiService {
    private val path = base.chunithmPath!!

    @JvmRecord private data class ChunithmBestScoreQQBody(val qq: Long, val b50: Boolean)

    @JvmRecord private data class ChunithmBestScoreNameBody(val username: String, val b50: Boolean)

    @JvmRecord
    private data class ChunithmByVersionQQBody(val qq: Long, @NonNull val version: List<String>)

    @JvmRecord
    private data class ChunithmByVersionNameBody(
            val username: String,
            @NonNull val version: List<String>
    )

    @JvmRecord
    private data class ChunithmAliasResponseBody(
        @JsonProperty("aliases") val aliases: List<ChuAlias>
    )

    override fun getChunithmBest30Recent10(qq: Long): ChuBestScore {
        val b = ChunithmBestScoreQQBody(qq, true)

        return base.divingFishApiWebClient.post()
                .uri { it.path("api/chunithmprober/query/player").build() }
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), ChunithmBestScoreQQBody::class.java)
                .headers(base::insertJSONHeader)
                .retrieve()
                .bodyToMono(ChuBestScore::class.java)
                .block() ?: ChuBestScore()
    }

    override fun getChunithmBest30Recent10(probername: String): ChuBestScore {
        val b = ChunithmBestScoreNameBody(probername, true)

        return base.divingFishApiWebClient.post()
                .uri { it.path("api/chunithmprober/query/player").build()}
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), ChunithmBestScoreNameBody::class.java)
                .headers(base::insertJSONHeader)
                .retrieve()
                .bodyToMono(ChuBestScore::class.java)
                .block() ?: ChuBestScore()
    }

    override fun downloadChunithmCover(songID: Long) {
        val path = path.resolve("Cover").resolve("${songID}.png")

        if (! Files.isRegularFile(path)) try {
            val cover = getChunithmCoverFromAPI(songID)

            Files.write(path, cover)
        } catch (e : IOException) {
            log.info("chunithm: 写入封面 $songID 失败")
        } catch (e : Exception) {
            log.info("chunithm: 下载封面 $songID 失败")
        }
    }

    override fun getChunithmCover(songID: Long): ByteArray {
        val song: String = songID.toString()
        val path = path.resolve("Cover").resolve("${song}.png")

        if (Files.isRegularFile(path))
                try {
                    return Files.readAllBytes(path)
                } catch (ignored: IOException) {}

        return getChunithmCoverFromAPI(songID)
    }

    override fun getChunithmCoverFromAPI(songID: Long): ByteArray {
        val song: String = songID.toString()
        val cover = try {
            base.webClient.get()
                .uri { it.scheme("https").host("assets2.lxns.net").path("chunithm/jacket/${song}.png").build()}
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()
        } catch (e: WebClientResponseException.NotFound) {
            val path = path.resolve("Cover").resolve("0.png")

            try {
                return Files.readAllBytes(path)
            } catch (e: IOException) {
                return byteArrayOf()
            }
        }

        return cover ?: byteArrayOf()
    }

    override fun getChunithmSongLibrary(): Map<Int, ChuSong> {
        // return getChunithmSongLibraryFromFile()

        return maiDao.getAllChuSong().associateBy { it.songID }
    }

    override fun getChunithmSong(songID: Long): ChuSong? {
        val o = maiDao.findChuSongByID(songID.toInt())
        insertChunithmAlias(o)
        return o
    }

    @Deprecated("请使用 From Database") private fun getChunithmSongLibraryFromFile(): Map<Int, ChuSong> {
        val song: List<ChuSong>

        if (isRegularFile("data-songs.json")) {
            song = parseFileList("data-songs.json", ChuSong::class.java)
        } else {
            log.info("中二节奏: 本地歌曲库不存在，获取 API 版本")
            song = JacksonUtil.parseObjectList(chunithmSongLibraryFromAPI, ChuSong::class.java)
        }

        return song.associateBy { it.songID }
    }

    override fun getChunithmAlias(songID: Long): ChuAlias? {
        return getChunithmAlias(songID.toInt())
    }

    override fun getChunithmAlias(songID: Int): ChuAlias? {
        return maiDao.getChuAliasByID(songID)
    }

    override fun getChunithmAliasLibrary(): Map<Int, List<String>>? {
        return maiDao.getAllChuAliases().associate { it.songID to it.alias }
    }

    override fun insertChunithmAlias(song: ChuSong?) {
        if (song != null) {
            song.alias = getChunithmAlias(song.songID)?.alias?.firstOrNull()
        }
    }

    override fun insertChunithmAlias(songs: List<ChuSong>?) {
        if (songs.isNullOrEmpty()) return

        val actions = songs.map {
            return@map AsyncMethodExecutor.Runnable {
                it.alias = getChunithmAlias(it.songID)?.alias?.firstOrNull()
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun insertChunithmAliasForScore(scores: List<ChuScore>?) {
        if (scores.isNullOrEmpty()) return

        val actions = scores.map {
            return@map AsyncMethodExecutor.Runnable {
                it.alias = getChunithmAlias(it.songID)?.alias?.firstOrNull()
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun insertChunithmAliasForScore(score: ChuScore?) {
        if (score != null) {
            score.alias = getChunithmAlias(score.songID)?.alias?.firstOrNull()
        }
    }

    override fun insertSongData(scores: List<ChuScore>) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Runnable {
                if (it.songID != 0L) {
                    val o = getChunithmSong(it.songID) ?: ChuSong()
                    insertSongData(it, o)
                }
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun insertSongData(score: ChuScore, song: ChuSong) {
        val chart = song.charts[score.index]

        score.charter = chart.charter
        score.artist = song.info.artist
    }

    override fun insertPosition(scores: List<ChuScore>, isBest30: Boolean) {
        if (scores.isEmpty()) return

        for (i in scores.indices) {
            val s = scores[i]

            if (isBest30) {
                s.position = (i + 1)
            } else {
                s.position = (i + 31)
            }
        }
    }

    override fun updateChunithmSongLibraryDatabase() {
        val songs = JacksonUtil.parseObjectList(chunithmSongLibraryFromAPI, ChuSong::class.java)

        for (s in songs) {
            maiDao.saveChuSong(s)
        }

        log.info("中二节奏: 歌曲数据库已更新")
    }

    override fun updateChunithmAliasLibraryDatabase() {
        val alias = JacksonUtil.parseObject(chunithmAliasLibraryFromAPI, ChunithmAliasResponseBody::class.java).aliases
        maiDao.saveChuAliases(alias)
        log.info("中二节奏: 外号数据库已更新")
    }

    @Deprecated("请使用 From Database")
    override fun updateChunithmSongLibraryFile() {
        saveFile(chunithmSongLibraryFromAPI, "data-fit.json", "统计")
    }

    private val chunithmSongLibraryFromAPI: String
        get() = base.divingFishApiWebClient.get()
            .uri { it.path("api/chunithmprober/music_data").build()}
            .retrieve()
            .bodyToMono(String::class.java)
            .block() ?: ""

    private val chunithmAliasLibraryFromAPI: String
        get() = base.webClient.get()
            .uri { it.scheme("https").host("maimai.lxns.net").path("api/v0/chunithm/alias/list").build()
        }.retrieve().bodyToMono(String::class.java).block()!!

    private fun <T> parseFile(fileName: String, clazz: Class<T>): T? {
        val file = path.resolve(fileName)
        try {
            val s = Files.readString(file)

            if (Files.isRegularFile(file)) {
                return JacksonUtil.parseObject(s, clazz)
            } else {
                log.info("chunithm: 文件{}不存在", fileName)
                return null
            }
        } catch (e: IOException) {
            log.error("chunithm: 获取文件失败", e)
            return null
        }
    }

    private fun <T> parseFileList(fileName: String, clazz: Class<T>): List<T> {
        val file = path.resolve(fileName)
        try {
            val s = Files.readString(file)
            return JacksonUtil.parseObjectList(s, clazz)
        } catch (e: IOException) {
            log.error("chunithm: 获取文件失败", e)
            return listOf()
        }
    }

    private fun saveFile(data: String, fileName: String, dictionaryName: String) {
        val file = path.resolve(fileName)

        try {
            if (isRegularFile(fileName)) {
                Files.writeString(file, data, UTF_8)
                log.info("chunithm: 已更新{}库", dictionaryName)
            } else if (Files.isWritable(path)) {
                Files.writeString(file, data, UTF_8)
                log.info("chunithm: 已保存{}库", dictionaryName)
            } else {
                log.info("chunithm: 未保存{}库", dictionaryName)
            }
        } catch (e: IOException) {
            log.error(String.format("chunithm: %s库保存失败", dictionaryName), e)
        }
    }

    private fun isRegularFile(fileName: String): Boolean {
        return Files.isRegularFile(path.resolve(fileName))
    }


    /**
     * 错误包装
     */

    /*
    private fun <T> request(request: (WebClient) -> Mono<T>): T {
        return try {
            base.request(request)
        } catch (e: ExecutionException) {
            when (e.cause) {
                is WebClientResponseException.BadRequest -> {
                    throw NetworkException.DivingFishException.BadRequest()
                }

                is WebClientResponseException.Unauthorized -> {
                    throw NetworkException.DivingFishException.Unauthorized()
                }

                is WebClientResponseException.NotFound -> {
                    throw NetworkException.DivingFishException.NotFound()
                }

                is ReadTimeoutException -> {
                    throw NetworkException.DivingFishException.RequestTimeout()
                }

                is WebClientResponseException.InternalServerError -> {
                    throw NetworkException.DivingFishException.InternalServerError()
                }

                is WebClientResponseException.BadGateway -> {
                    throw NetworkException.DivingFishException.BadGateway()
                }

                else -> throw NetworkException.DivingFishException(e.message)
            }
        }
    }

     */

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ChunithmApiImpl::class.java)

    }
}
