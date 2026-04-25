package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.dao.UserSnapShotDao
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
import com.now.nowbot.util.toBody
import com.now.nowbot.util.toBodyList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriBuilder
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.RejectedExecutionException
import java.util.function.Function
import kotlin.text.HexFormat
import kotlin.text.toHexString

@Service
class ScoreApiImpl(
    private val base: OsuApiBaseService,
    private val scoreDao: ScoreDao,
    private val userSnapShotDao: UserSnapShotDao,
) : OsuScoreApiService {
    override fun getCovers(
        scores: List<LazerScore>,
        type: CoverType
    ): List<ByteArray?> {
        val async = AsyncMethodExecutor.await(
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
                val image = base.osuApiRestClient.get()
                    .uri(url)
                    .toBody<ByteArray>()

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

//        val firstBatch = getBests(id, mode, offset, step)
//        val secondBatch = getBests(id, mode, offset + step, limit - step)
//
//        return firstBatch + secondBatch

        val scores = AsyncMethodExecutor.awaitList(
            listOf(
                Callable { getBests(id, mode, offset, step) },
                Callable { getBests(id, mode, offset + step, limit - step) }
            )
        ).flatten()

        Thread.startVirtualThread {
            if (offset == 0 && limit == 200) {
                userSnapShotDao.upsertSnapshot(scores)
            }
        }

        return scores

//        // 使用你现有的工具方法进行并发
//        val result = AsyncMethodExecutor.awaitPairCallableExecute(
//            { getBests(id, mode, offset, step) },
//            { getBests(id, mode, offset + step, limit - step) }
//        )
//
//        // result.first 是前 100 条，result.second 是后 100 条
//        return result.first + result.second
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
        return getRecent(uid, mode, false, offset, limit)
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
        return getRecent(uid, mode, true, offset, limit, isBackground)
    }

    override fun getScore(scoreID: Long): LazerScore {
        val score = request { client ->
            client.get().uri {
                it.path("scores/{scoreID}").build(scoreID)
            }.headers(base::insertHeader).toBody<LazerScore>()
        }

        scoreDao.saveScoreAsync(listOfNotNull(score))

        return score
    }

    override fun getBeatmapScore(bid: Long, uid: Long, mode: OsuMode?): BeatmapUserScore? {
        val score = retryOn404<BeatmapUserScore>(
            { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 0)
                    .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                    .build(bid, uid)
            },
            { base.insertHeader(this) },
            { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 1)
                    .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                    .build(bid, uid)
            },
        )

        scoreDao.saveScoreAsync(listOfNotNull(score.score))

        return score
    }

    override fun getBeatmapScore(
        bid: Long,
        user: BindUser,
        mode: OsuMode?,
    ): BeatmapUserScore? {
        if (user.isTokenAvailable == null) return getBeatmapScore(bid, user.userID, mode)
        val score = retryOn404<BeatmapUserScore>(
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

        scoreDao.saveScoreAsync(listOfNotNull(score.score))

        return score
    }

    override fun getBeatmapScore(
        bid: Long,
        uid: Long,
        mode: OsuMode?,
        mods: Collection<LazerMod?>?,
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

        val score = retryOn404<BeatmapUserScore>(
            uri = buildUri(mode),
            headers = { base.insertHeader(this) },
            retry = buildUri(null)
        )

        scoreDao.saveScoreAsync(listOfNotNull(score.score))

        return score
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

        val score = retryOn404<BeatmapUserScore>(
            uri = buildUri(mode),
            headers = { base.insertHeader(this, user) },
            retry = buildUri(null)
        )

        scoreDao.saveScoreAsync(listOfNotNull(score.score))

        return score
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

        val scores = retryOn404<BeatmapScoreResponse>(
            uri = buildUri(mode),
            headers = { base.insertHeader(this, user) },
            retry = buildUri(null)
        ).scores

        scoreDao.saveScoreAsync(scores)

        return scores
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

        val scores = retryOn404<BeatmapScoreResponse>(
            uri = buildUri(mode),
            headers = { base.insertHeader(this) },
            retry = buildUri(null)
        ).scores

        scoreDao.saveScoreAsync(scores.take(50))

        return scores
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
            .toBody<BeatmapScoreResponse>()
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
                                .toBody<ByteArray>()
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
                    val buf = client.get()
                        .uri {
                            it.path("scores/{score}/download")
                                .build(score.scoreID)
                        }
                        .headers(base::insertHeader)
                        .toBody<ByteBuffer>()
                    Replay(buf)
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
                .toBodyList<LazerScore>()
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
                .toBodyList<LazerScore>()
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
    ): List<LazerScore> {
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
                .toBodyList<LazerScore>()
        }

        scoreDao.saveScoreAsync(recents)

        return recents
    }


    /**
     * 错误包装
     * @param isBackground 如果为真，则使用限流策略，如果为 null，则使用不重试的策略
     */
    private fun <T: Any> request(isBackground: Boolean? = false, request: (RestClient) -> T): T {
        return try {
            if (isBackground == null) {
                return request(base.noRetryRestClient)
            }

            if (isBackground) {
                base.request(isBackground = true, request)
            } else {
                request(base.osuApiRestClient)
            }
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<HttpClientErrorException>()

            when(ex?.statusCode?.value()) {
                400 -> throw NetworkException.ScoreException.BadRequest()
                401 -> throw NetworkException.ScoreException.Unauthorized()
                403 -> throw NetworkException.ScoreException.Forbidden()
                404 -> throw NetworkException.ScoreException.NotFound()
                422 -> throw NetworkException.ScoreException.UnprocessableEntity()
                429 -> throw NetworkException.ScoreException.TooManyRequests()
                500 -> throw NetworkException.ScoreException.InternalServerError()
                502 -> throw NetworkException.ScoreException.BadGateway()
                503 -> throw NetworkException.ScoreException.ServiceUnavailable()

                else -> when {
                    e.findCauseOfType<RejectedExecutionException>() != null -> {
                        throw NetworkException.ScoreException.TooManyRequests()
                    }

                    e.findCauseOfType<java.net.SocketException>() != null -> {
                        throw NetworkException.ScoreException.GatewayTimeout()
                    }

                    else -> {
                        if (e !is CancellationException) {
                            log.error("成绩请求：未定义的错误：", e)
                            throw NetworkException.ScoreException.Undefined(e)
                        } else {
                            throw e
                        }
                    }
                }
            }
        }
    }

    private inline fun <reified T : Any> retryOn404(
        uri: Function<UriBuilder, URI>,
        crossinline headers: HttpHeaders.() -> Unit,
        retry: Function<UriBuilder, URI>,
    ): T {
        val call = { target: Function<UriBuilder, URI>, isBackground: Boolean? ->
            request(isBackground) { client ->
                client.get()
                    .uri(target)
                    .headers { h -> headers(h) }
                    .toBody<T>()
            }
        }

        return try {
            call(uri, false)
        } catch (e: Throwable) {
            // 解包虚拟线程或包装器可能产生的异常
            val actual = e as? NetworkException.ScoreException.NotFound ?: e.cause

            if (actual is NetworkException.ScoreException.NotFound) {
                log.warn("检测到 404，正在尝试备用请求...")
                call(retry, null)
            } else {
                throw e
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
