package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.Replay
import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.CoverType.*
import com.now.nowbot.model.enums.CoverType.Companion.getString
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.BeatmapUserScore
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.JacksonUtil
import okio.IOException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import kotlin.text.HexFormat

@Service
class ScoreApiImpl(
    val base: OsuApiBaseService,
    val scoreDao: ScoreDao,
) : OsuScoreApiService {
    override fun getCovers(
        scores: List<LazerScore>,
        type: CoverType
    ): List<ByteArray?> {
        val async = AsyncMethodExecutor.awaitCallableExecute(
            {
                scores.map { s ->
                    getCover(s, type)
                }
            }
        )

        return async
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getCover(score: LazerScore, type: CoverType): ByteArray? {
        val path = Path.of(IMG_BUFFER_PATH)

        val default by lazy {
            try {
                Files.readAllBytes(
                    Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("Banner").resolve("c8.png")
                )
            } catch (_: IOException) {
                null
            }
        }

        val url = score.beatmapset.covers.getString(type)

        if (url.isBlank()) {
            log.info("获取谱面图片：谱面封面类不完整")
            return default
        }

        val md = MessageDigest.getInstance("MD5")

        try {
            md.update(url.toByteArray(Charsets.UTF_8))
        } catch (_: Exception) {
            log.info("获取谱面图片：计算 MD5 失败")
            return default
        }

        val hex = md.digest().toHexString(HexFormat.Default)

        return if (Files.isRegularFile(path.resolve(hex))) {
            Files.readAllBytes(path.resolve(hex))
        } else {
            try {
                val image = base.osuApiWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ByteArray::class.java)
                    .block()!!

                if (Files.isDirectory(path) && Files.isWritable(path)) {
                    Files.write(path.resolve(hex), image)
                }

                return image
            } catch (_: Exception) {
                default
            }
        }
    }

    override fun getBestScores(
        id: Long,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return if (limit <= 100) {
            getBests(id, mode, offset, limit)
        } else {
            return getBests(id, mode, offset, 100) + getBests(id, mode, offset + 100, limit - 100)
        }
    }

    override fun getPassedScore(
        user: BindUser,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return getRecent(user, mode, false, offset, limit)
    }

    override fun getPassedScore(
        uid: Long,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return getRecent(uid, mode, false, offset, limit)!!
    }

    override fun getRecentScore(
        user: BindUser,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return getRecent(user, mode, true, offset, limit)
    }

    override fun getRecentScore(
        uid: Long,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return getRecent(uid, mode, true, offset, limit)!!
    }

    override fun getScore(scoreID: Long): LazerScore {
        return request { client ->
            client.get().uri {
                it.path("scores/{scoreID}").build(scoreID)
            }.headers(base::insertHeader).retrieve().bodyToMono(LazerScore::class.java)
        }
    }

    override fun getBeatMapScore(bid: Long, uid: Long, mode: OsuMode?): BeatmapUserScore? {
        return retryOn404<BeatmapUserScore>(
            { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 0)
                    .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                    .build(bid, uid)
            },
            { base.insertHeader(it) },
            { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 1)
                    .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                    .build(bid, uid)
            },
        )
    }

    override fun getBeatMapScore(
        bid: Long,
        user: BindUser,
        mode: OsuMode?,
    ): BeatmapUserScore? {
        if (!user.isAuthorized) return getBeatMapScore(bid, user.userID, mode)
        return retryOn404<BeatmapUserScore>(
            {
                it.path("beatmaps/{bid}/scores/users/{uid}").queryParam("legacy_only", 0)

                if (OsuMode.isNotDefaultOrNull(mode)) {
                    it.queryParam("mode", OsuMode.getQueryName(mode))
                }

                it.build(bid, user.userID)
            },
            base.insertHeader(user),
            {
                it.path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 1)

                if (OsuMode.isNotDefaultOrNull(mode)) {
                    it.queryParam("mode", OsuMode.getQueryName(mode))
                }

                it.build(bid, user.userID)
            },
        )
    }

    override fun getBeatMapScore(
        bid: Long,
        uid: Long,
        mode: OsuMode?,
        mods: Iterable<LazerMod?>,
    ): BeatmapUserScore? {

        val uri = Function { n: Int? ->
            Function { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", n)

                if (OsuMode.isNotDefaultOrNull(mode)) {
                    uriBuilder.queryParam("mode", OsuMode.getQueryName(mode))
                }

                LazerMod.setMods(uriBuilder, mods)
                uriBuilder.build(bid, uid)
            }
        }
        return retryOn404<BeatmapUserScore>(
            uri.apply(0),
            { base.insertHeader(it) },
            uri.apply(1),
        )
    }

    override fun getBeatMapScore(
        bid: Long,
        user: BindUser,
        mode: OsuMode,
        mods: Iterable<LazerMod?>,
    ): BeatmapUserScore? {
        if (!user.isAuthorized) {
            return getBeatMapScore(bid, user.userID, mode, mods)
        }
        val uri = Function { n: Int? ->
            Function { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", n)
                    .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                LazerMod.setMods(uriBuilder, mods)
                uriBuilder.build(bid, user.userID)
            }
        }
        return retryOn404<BeatmapUserScore>(
            uri.apply(0),
            base.insertHeader(user),
            uri.apply(1),
        )
    }

    override fun getBeatmapScores(bid: Long, user: BindUser, mode: OsuMode?): List<LazerScore> {
        if (!user.isAuthorized) getBeatmapScores(bid, user.userID, mode)

        return request { client ->
            client.get()
                .uri {
                    it.path("beatmaps/{bid}/scores/users/{uid}/all")
                        .queryParam("legacy_only", 0)
                        .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                        .build(bid, user.userID)
                }
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .map { JacksonUtil.parseObjectList(it["scores"], LazerScore::class.java) }
        }
    }

    override fun getBeatmapScores(bid: Long, uid: Long, mode: OsuMode?): List<LazerScore> {
        return request { client ->
            client.get()
                .uri {
                    it.path("beatmaps/{bid}/scores/users/{uid}/all")
                        .queryParam("legacy_only", 0)
                        .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                        .build(bid, uid)
                }
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .map { JacksonUtil.parseObjectList(it["scores"], LazerScore::class.java) }
        }
    }

    override fun getLeaderBoardScore(
        bindUser: BindUser?,
        bid: Long,
        mode: OsuMode?,
        mods: Iterable<LazerMod>?,
        type: String?,
        legacy: Boolean
    ): List<LazerScore> {
        return request { client ->
            val headersSpec = client.get()
                .uri {
                    it.path("beatmaps/{bid}/scores")
                        .queryParam("legacy_only", if (legacy) 1 else 0)
                        .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                        .queryParamIfPresent("type", Optional.ofNullable(type))

                    LazerMod.setMods(it, mods)

                    it.build(bid)
                }

            if (bindUser != null) {
                headersSpec.headers(base.insertHeader(bindUser))
            } else {
                headersSpec.headers { base.insertHeader(it) }
            }

            headersSpec.retrieve()
                .bodyToMono(JsonNode::class.java)
                .map { JacksonUtil.parseObjectList(it["scores"], LazerScore::class.java) }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun asyncDownloadBackground(covers: Iterable<Covers>, type: CoverType?) {
        val path = Path.of(IMG_BUFFER_PATH)
        if (Files.isDirectory(path).not() || Files.isWritable(path).not()) return

        val actions = covers.map { cover ->
            return@map AsyncMethodExecutor.Runnable {

                val url = when (type) {
                    CARD -> cover.card
                    CARD_2X -> cover.card2x
                    COVER_2X -> cover.cover2x
                    LIST -> cover.list
                    LIST_2X -> cover.list2x
                    SLIM_COVER -> cover.slimcover
                    SLIM_COVER_2X -> cover.slimcover2x
                    else -> cover.cover
                }

                if (url.isBlank()) {
                    log.info("异步下载谱面图片：谱面封面类不完整")
                    return@Runnable
                }

                val md = MessageDigest.getInstance("MD5")

                try {
                    md.update(url.toByteArray(Charsets.UTF_8))
                } catch (_: Exception) {
                    log.info("异步下载谱面图片：计算 MD5 失败")
                    return@Runnable
                }

                val hex = md.digest().toHexString(HexFormat.Default)

                if (Files.isRegularFile(path.resolve(hex))) {
                    return@Runnable
                } else {
                    val split = url.split('?')
                    val query = split.lastOrNull()?.toLongOrNull() ?: 0L
                    val replacePath = split.first().replace("https://assets.ppy.sh/", "")

                    val image = try {
                        request { client ->
                            client.get()
                                .uri {
                                    it.scheme("https").host("assets.ppy.sh").replacePath(replacePath)
                                        .query(query.toString())
                                        .build()
                                }
                                .headers(base::insertHeader)
                                .retrieve()
                                .bodyToMono(ByteArray::class.java)
                        }
                    } catch (e: Exception) {
                        log.error("异步下载谱面图片：任务失败\n", e)
                        return@Runnable
                    }

                    try {
                        Files.write(path.resolve(hex), image)
                    } catch (e: IOException) {
                        log.error("异步下载谱面图片：保存失败\n{}", e.message)
                        return@Runnable
                    }
                }
            }
        }

        AsyncMethodExecutor.asyncRunnableExecute(actions)
    }

    override fun getReplay(score: LazerScore): Replay? {
        if (score.replay && score.scoreID > 0L) {
            return try {
                request { client ->
                    client.get()
                        .uri {
                            it.path("scores/{score}/download")
                                .build(score.scoreID)
                        }
                        .headers(base::insertHeader)
                        .retrieve()
                        .bodyToMono(ByteBuffer::class.java)
                        .map { Replay(it) }
                    /*
                    .bodyToMono(JsonNode::class.java)
                    .map { JacksonUtil.parseObject(it, Replay::class.java) }

                     */
                }
            } catch (_: Exception) {
                return null
            }
        }

        return null
    }

    private fun getBests(
        id: Long,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return request { client ->
            client.get()
                .uri {
                    it.path("users/{uid}/scores/best")
                        .queryParam("legacy_only", 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                        .build(id)
                }
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(LazerScore::class.java)
                .collectList()
        }
    }

    private fun getRecent(
        user: BindUser,
        mode: OsuMode?,
        includeFails: Boolean,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return request { client ->
            client.get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .path("users/{uid}/scores/recent")
                        .queryParam("legacy_only", 0)
                        .queryParam("include_fails", if (includeFails) 1 else 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                        .build(user.userID)
                }
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToFlux(LazerScore::class.java)
                .collectList()
                .doOnNext(scoreDao::saveScoreAsync)
        }
    }

    private fun getRecent(
        uid: Long,
        mode: OsuMode?,
        includeFails: Boolean,
        offset: Int,
        limit: Int,
    ): List<LazerScore>? {
        return request { client ->
            client.get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .path("users/{uid}/scores/recent")
                        .queryParam("legacy_only", 0)
                        .queryParam("include_fails", if (includeFails) 1 else 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                        .build(uid)
                }
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(LazerScore::class.java)
                .collectList()
                .doOnNext(scoreDao::saveScoreAsync)
        }
    }


    /**
     * 错误包装
     */
    private fun <T> request(request: (WebClient) -> Mono<T>): T {
        return try {
            base.request(request)
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<WebClientException>()

            when (ex) {
                is WebClientResponseException.BadRequest -> {
                    throw NetworkException.ScoreException.BadRequest()
                }

                is WebClientResponseException.Unauthorized -> {
                    throw NetworkException.ScoreException.Unauthorized()
                }

                is WebClientResponseException.Forbidden -> {
                    throw NetworkException.ScoreException.Forbidden()
                }

                is WebClientResponseException.NotFound -> {
                    throw NetworkException.ScoreException.NotFound()
                }

                is WebClientResponseException.TooManyRequests -> {
                    throw NetworkException.ScoreException.TooManyRequests()
                }

                is WebClientResponseException.BadGateway -> {
                    throw NetworkException.ScoreException.BadGateway()
                }

                is WebClientResponseException.UnprocessableEntity -> {
                    throw NetworkException.ScoreException.UnprocessableEntity()
                }

                is WebClientResponseException.ServiceUnavailable -> {
                    throw NetworkException.ScoreException.ServiceUnavailable()
                }

                else -> throw NetworkException.ScoreException(e.message)
            }
        }
    }

    private inline fun <reified T> retryOn404(
        uri: Function<UriBuilder, URI>,
        headers: Consumer<HttpHeaders>,
        retry: Function<UriBuilder, URI>,
    ): T {
        return try {
            request { client ->
                client.get()
                    .uri(uri)
                    .headers(headers)
                    .retrieve()
                    .bodyToMono(T::class.java)
            }
        } catch (_: NetworkException.ScoreException.NotFound) {
            request { client ->
                client.get()
                    .uri(retry)
                    .headers(headers)
                    .retrieve()
                    .bodyToMono(T::class.java)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScoreApiImpl::class.java)

        private val IMG_BUFFER_PATH: String = if (System.getenv("BUFFER_PATH").isNullOrBlank().not()) {
            System.getenv("BUFFER_PATH")
        } else {
            System.getProperty("java.io.tmpdir") + "/n-bot/buffer"
        }
    }
}
