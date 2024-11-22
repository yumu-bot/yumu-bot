package com.now.nowbot.service.divingFishApiService.impl

import com.now.nowbot.model.json.ChuBestScore
import com.now.nowbot.model.json.ChuSong
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
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
import java.util.stream.Collectors
import kotlin.text.Charsets.UTF_8

@Service
class ChunithmApiImpl(private val base: DivingFishBaseService) : ChunithmApiService {
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

    override fun getChunithmBest30Recent10(qq: Long): ChuBestScore {
        val b = ChunithmBestScoreQQBody(qq, true)

        return base.divingFishApiWebClient!!
                .post()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/chunithmprober/query/player").build()
                }
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), ChunithmBestScoreQQBody::class.java)
                .headers { headers: HttpHeaders -> base.insertJSONHeader(headers) }
                .retrieve()
                .bodyToMono(ChuBestScore::class.java)
                .block() ?: ChuBestScore()
    }

    override fun getChunithmBest30Recent10(probername: String): ChuBestScore {
        val b = ChunithmBestScoreNameBody(probername, true)

        return base.divingFishApiWebClient!!
                .post()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder.path("api/chunithmprober/query/player").build()
                }
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), ChunithmBestScoreNameBody::class.java)
                .headers { headers: HttpHeaders -> base.insertJSONHeader(headers) }
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
        val cover =
                try {
                    base.webClient!!
                            .get()
                            .uri { uriBuilder: UriBuilder ->
                                uriBuilder
                                    .scheme("https")
                                    .host("assets2.lxns.net")
                                    .path("chunithm/jacket/${song}.png").build()
                            }
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
        return getChunithmSongLibraryFromFile()
    }

    private fun getChunithmSongLibraryFromFile(): Map<Int, ChuSong> {
        val song: List<ChuSong>

        if (isRegularFile("data-songs.json")) {
            song = parseFileList("data-songs.json", ChuSong::class.java)
        } else {
            log.info("chunithm: 本地歌曲库不存在，获取 API 版本")
            song = JacksonUtil.parseObjectList(chunithmSongLibraryFromAPI, ChuSong::class.java)
        }

        return song.stream().collect(Collectors.toMap(ChuSong::songID) { s: ChuSong -> s })
    }

    override fun updateChunithmSongLibraryFile() {
        saveFile(chunithmSongLibraryFromAPI, "data-fit.json", "统计")
        log.info("chunithm: 歌曲数据库已更新")
    }

    private val chunithmSongLibraryFromAPI: String
        get() =
                base.divingFishApiWebClient!!
                        .get()
                        .uri { uriBuilder: UriBuilder ->
                            uriBuilder.path("api/chunithmprober/music_data").build()
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

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ChunithmApiImpl::class.java)

    }
}
