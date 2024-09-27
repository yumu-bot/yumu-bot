package com.now.nowbot.service.divingFishApiService.impl

import com.now.nowbot.model.json.ChuBestScore
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.lang.NonNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@Service
class ChunithmApiImpl(private val base: DivingFishBaseService) : ChunithmApiService {
    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Chunithm
    // /home/spring/work/img/ExportFileV3/Chunithm

    private val path: Path = Path.of("/home/spring/work/img/ExportFileV3/Chunithm")

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

        return base.divingFishApiWebClient
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

        return base.divingFishApiWebClient
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
            base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                uriBuilder.path(
                    "covers/$song.png"
                ).build()
            }.retrieve().bodyToMono(
                ByteArray::class.java
            ).block()
        } catch (e: WebClientResponseException.NotFound) {
            base.divingFishApiWebClient.get().uri { uriBuilder: UriBuilder ->
                uriBuilder.path("covers/00000.png").build()
            }.retrieve().bodyToMono(
                ByteArray::class.java
            ).block()
        }

        return cover ?: byteArrayOf()
    }
}
