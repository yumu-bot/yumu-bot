package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.osu.Covers.Companion.CoverType.*
import com.now.nowbot.model.osu.Covers.Companion.CoverType.Companion.getString
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.findCauseOfType
import io.netty.channel.unix.Errors
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
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import kotlin.text.Charsets
import kotlin.text.HexFormat
import kotlin.text.isBlank
import kotlin.text.isNullOrBlank
import kotlin.text.replace
import kotlin.text.split
import kotlin.text.toByteArray
import kotlin.text.toHexString
import kotlin.text.toLongOrNull

@Service
class ScoreApiImpl(
    private val base: OsuApiBaseService,
    private val scoreDao: ScoreDao,
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
        val step = 100

        if (limit <= step) {
            return getBests(id, mode, offset, limit)
        }

        // 使用你现有的工具方法进行并发
        val result = AsyncMethodExecutor.awaitPairCallableExecute(
            { getBests(id, mode, offset, step) },
            { getBests(id, mode, offset + step, limit - step) }
        )

        // result.first 是前 100 条，result.second 是后 100 条
        return result.first + result.second
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
        isBackground: Boolean
    ): List<LazerScore> {
        return getRecent(uid, mode, true, offset, limit, isBackground)!!
    }

    override fun getScore(scoreID: Long): LazerScore {
        return request { client ->
            client.get().uri {
                it.path("scores/{scoreID}").build(scoreID)
            }.headers(base::insertHeader).retrieve().bodyToMono(LazerScore::class.java)
        }
    }

    override fun getBeatmapScore(bid: Long, uid: Long, mode: OsuMode?): BeatmapUserScore? {
        return retryOn404<BeatmapUserScore>(
            { it
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 0)
                    .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                    .build(bid, uid)
            },
            { base.insertHeader(this) },
            { it
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 1)
                    .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                    .build(bid, uid)
            },
        )
    }

    override fun getBeatmapScore(
        bid: Long,
        user: BindUser,
        mode: OsuMode?,
    ): BeatmapUserScore? {
        if (user.isTokenAvailable == null) return getBeatmapScore(bid, user.userID, mode)
        return retryOn404<BeatmapUserScore>(
            {
                it.path("beatmaps/{bid}/scores/users/{uid}").queryParam("legacy_only", 0)

                if (OsuMode.isNotDefaultOrNull(mode)) {
                    it.queryParam("mode", OsuMode.getQueryName(mode))
                }

                it.build(bid, user.userID)
            },
            {
                base.insertHeader(this, user)
            },
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

    override fun getBeatmapScore(
        bid: Long,
        uid: Long,
        mode: OsuMode?,
        mods: Collection<LazerMod?>,
    ): BeatmapUserScore? {

        fun buildUri(mode: OsuMode? = null) = { builder: UriBuilder ->
            builder.path("beatmaps/{bid}/scores/users/{uid}")
                .queryParam("legacy_only", 0)
                .apply {
                    if (OsuMode.isNotDefaultOrNull(mode)) {
                        queryParam("mode", OsuMode.getQueryName(mode))
                    }
                    LazerMod.setMods(this, mods)
                }
                .build(bid, uid)
        }

        return retryOn404<BeatmapUserScore>(
            uri = buildUri(mode),
            headers = { base.insertHeader(this) },
            retry = buildUri(null)
        )
    }

    override fun getBeatmapScore(
        bid: Long,
        user: BindUser,
        mode: OsuMode,
        mods: Collection<LazerMod>,
    ): BeatmapUserScore? {
        if (user.isTokenAvailable == null) {
            return getBeatmapScore(bid, user.userID, mode, mods)
        }

        fun buildUri(mode: OsuMode? = null) = { builder: UriBuilder ->
            builder.path("beatmaps/{bid}/scores/users/{uid}")
                .queryParam("legacy_only", 0)
                .apply {
                    if (OsuMode.isNotDefaultOrNull(mode)) {
                        queryParam("mode", OsuMode.getQueryName(mode))
                    }
                    LazerMod.setMods(this, mods)
                }
                .build(bid, user.userID)
        }

        return retryOn404<BeatmapUserScore>(
            uri = buildUri(mode),
            headers = { base.insertHeader(this, user) },
            retry = buildUri(null)
        )
    }

    data class BeatmapScoreResponse(val scores: List<LazerScore>)

    override fun getBeatmapScores(bid: Long, user: BindUser, mode: OsuMode?): List<LazerScore> {
        if (user.isTokenAvailable == null) {
            getBeatmapScores(bid, user.userID, mode)
        }

        fun buildUri(mode: OsuMode? = null) = { builder: UriBuilder ->
            builder.path("beatmaps/{bid}/scores/users/{uid}/all")
                .queryParam("legacy_only", 0)
                .apply {
                    if (OsuMode.isNotDefaultOrNull(mode)) {
                        queryParam("mode", OsuMode.getQueryName(mode))
                    }
                }
                .build(bid, user.userID)
        }

        return retryOn404<BeatmapScoreResponse>(
            uri = buildUri(mode),
            headers = { base.insertHeader(this, user) },
            retry = buildUri(null)
        ).scores
    }

    override fun getBeatmapScores(bid: Long, uid: Long, mode: OsuMode?): List<LazerScore> {


        fun buildUri(mode: OsuMode? = null) = { builder: UriBuilder ->
            builder.path("beatmaps/{bid}/scores/users/{uid}/all")
                .queryParam("legacy_only", 0)
                .apply {
                    if (OsuMode.isNotDefaultOrNull(mode)) {
                        queryParam("mode", OsuMode.getQueryName(mode))
                    }
                }
                .build(bid, uid)
        }

        return retryOn404<BeatmapScoreResponse>(
            uri = buildUri(mode),
            headers = { base.insertHeader(this) },
            retry = buildUri(null)
        ).scores
    }

    override fun getLeaderBoardScore(
        bindUser: BindUser?,
        bid: Long,
        mode: OsuMode?,
        mods: Collection<LazerMod>?,
        type: String?,
        legacy: Boolean
    ): List<LazerScore> {
        return request { client -> client.get()
            .uri {
                it.path("beatmaps/{bid}/scores")
                    .queryParam("legacy_only", if (legacy) 1 else 0)
                    .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                    .queryParamIfPresent("type", Optional.ofNullable(type))

                LazerMod.setMods(it, mods)

                it.build(bid)
            }.headers { headers ->
                if (bindUser != null) {
                    base.insertHeader(headers, bindUser)
                } else {
                    base.insertHeader(headers)
                }
            }
            .retrieve()
            .bodyToMono(BeatmapScoreResponse::class.java)
        }.scores
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
        if (!score.replay || score.scoreID <= 0L) {
            return null
        }

        val byte: ByteBuffer? = try {
            request { client ->
                client.get()
                    .uri {
                        it.path("scores/{score}/download")
                            .build(score.scoreID)
                    }
                    .headers(base::insertHeader)
                    .retrieve()
                    .bodyToMono(ByteBuffer::class.java)
            }
        } catch (_: Exception) {
            return null
        }

        return Replay(byte ?: return null)
    }

    private fun getBests(
        id: Long,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        val bests = request { client ->
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
            //.doOnNext(scoreDao::saveScoreAsync)
            // 这里使用的是 netty 的线程，这些线程通常只负责网络并发；
            // 如果将其用于数据库的读写，会直接卡死请求
        }

        scoreDao.saveScoreAsync(bests)

        return bests
    }

    private fun getRecent(
        user: BindUser,
        mode: OsuMode?,
        includeFails: Boolean,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        val recents = request { client ->
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
                .headers { headers ->
                    base.insertHeader(headers, user)
                }
                .retrieve()
                .bodyToFlux(LazerScore::class.java)
                .collectList()
        }

        scoreDao.saveScoreAsync(recents)

        return recents
    }

    private fun getRecent(
        uid: Long,
        mode: OsuMode?,
        includeFails: Boolean,
        offset: Int,
        limit: Int,
        isBackground: Boolean = false
    ): List<LazerScore>? {
        val recents = request(isBackground) { client ->
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
        }

        scoreDao.saveScoreAsync(recents)

        return recents
    }


    /**
     * 错误包装
     */
    private fun <T> request(isBackground: Boolean = false, request: (WebClient) -> Mono<T>): T {
        return try {
            base.request(isBackground, request)
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

                is WebClientResponseException.InternalServerError -> {
                    throw NetworkException.ScoreException.InternalServerError()
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
            }

            if (e.findCauseOfType<RejectedExecutionException>() != null) {
                throw NetworkException.ScoreException.TooManyRequests()
            } else if (e.findCauseOfType<Errors.NativeIoException>() != null) {
                throw NetworkException.ScoreException.GatewayTimeout()
            } else if (e.findCauseOfType<ExecutionException>() != null) {
                throw NetworkException.ScoreException.RequestTimeout()
            } else {
                throw NetworkException.ScoreException.Undefined(e)
            }
        }
    }

    private inline fun <reified T : Any> retryOn404(
        noinline uri: (UriBuilder) -> URI,
        noinline headers: HttpHeaders.() -> Unit,
        noinline retry: (UriBuilder) -> URI,
    ): T {
        return request { client ->
            client.get()
                .uri(uri)
                .headers(headers)
                .exchangeToMono { response ->
                    val contentType = response.headers().contentType().orElse(null)

                    // 1. 如果返回的是 HTML（通常是 200 错误页），直接判定为需要 Retry
                    if (contentType?.includes(org.springframework.http.MediaType.TEXT_HTML) == true) {
                        return@exchangeToMono response.releaseBody().then(doRetry<T>(client, retry, headers))
                    }

                    // 2. 正常读取 Body 并在解析失败时切换
                    response.bodyToMono(T::class.java)
                        .flatMap { result ->
                            // 逻辑判定：例如 result 里的 scores 为空，可能也是一种 404 的表现
                            if (result is BeatmapScoreResponse && result.scores.isEmpty()) {
                                doRetry<T>(client, retry, headers)
                            } else {
                                Mono.just(result)
                            }
                        }
                        .onErrorResume {
                            // 3. 如果反序列化失败（说明 Body 不是预期的 JSON），切换到备用请求
                            log.warn("响应解析失败，尝试备用路径: ${it.message}")
                            doRetry<T>(client, retry, headers)
                        }
                }
        }
    }

    // 提取重试逻辑
    private inline fun <reified T : Any> doRetry(client: WebClient, noinline retry: (UriBuilder) -> URI, noinline headers: HttpHeaders.() -> Unit): Mono<T> {
        return client.get()
            .uri(retry)
            .headers(headers)
            .retrieve()
            .bodyToMono(T::class.java) // 这里不再用 generic，直接按 T 解析
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
