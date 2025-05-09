package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.Replay
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatmapUserScore
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.service.osuApiService.impl.ScoreApiImpl.CoverType.*
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.JacksonUtil
import okio.IOException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.function.Consumer
import java.util.function.Function

@Service
class ScoreApiImpl(
    val base: OsuApiBaseService,
    val scoreDao: ScoreDao,
) : OsuScoreApiService {

    override fun getBestScores(
        user: BindUser,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        if (!user.isAuthorized) return getBestScores(user.userID, mode, offset, limit)
        return base.osuApiWebClient.get()
            .uri { it.path("users/{uid}/scores/best")
                .queryParam("legacy_only", 0)
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                .build(user.userID)
            }
            .headers(base.insertHeader(user))
            .retrieve()
            .bodyToFlux(LazerScore::class.java)
            .collectList()
            .block()!!
    }

    override fun getBestScores(
        id: Long,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return base.osuApiWebClient.get()
            .uri { it.path("users/{uid}/scores/best")
                .queryParam("legacy_only", 0)
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                .build(id)
            }
            .headers { base.insertHeader(it) }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { JacksonUtil.parseObjectList(it, LazerScore::class.java) }.block()!!

            /*
            .bodyToFlux(LazerScore::class.java)
            .collectList()
            .block()!!

             */
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

                setMods(uriBuilder, mods)
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
                setMods(uriBuilder, mods)
                uriBuilder.build(bid, user.userID)
            }
        }
        return retryOn404<BeatmapUserScore>(
            uri.apply(0),
            base.insertHeader(user),
            uri.apply(1),
        )
    }

    private fun setMods(builder: UriBuilder, mods: Iterable<LazerMod?>) {
        for (mod in mods) {
            if (mod is LazerMod.NoMod) {
                builder.queryParam("mods[]", "NM")
                return
            }
        }

        mods.filterNotNull().forEach { builder.queryParam("mods[]", it.acronym) }
    }

    override fun getBeatMapScores(bid: Long, user: BindUser, mode: OsuMode?): List<LazerScore> {
        if (!user.isAuthorized) getBeatMapScores(bid, user.userID, mode)

        return base.osuApiWebClient.get()
            .uri { it.path("beatmaps/{bid}/scores/users/{uid}/all")
                .queryParam("legacy_only", 0)
                .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                .build(bid, user.userID)
            }
            .headers(base.insertHeader(user))
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { JacksonUtil.parseObjectList(it["scores"], LazerScore::class.java) }
            .block()!!
    }

    override fun getBeatMapScores(bid: Long, uid: Long, mode: OsuMode?): List<LazerScore> {
        return base.osuApiWebClient.get()
            .uri { it.path("beatmaps/{bid}/scores/users/{uid}/all")
                .queryParam("legacy_only", 0)
                .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                .build(bid, uid)
            }
            .headers { base.insertHeader(it) }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { JacksonUtil.parseObjectList(it["scores"], LazerScore::class.java) }
            .block()!!
    }

    override fun getLeaderBoardScore(bid: Long, mode: OsuMode?, legacy: Boolean): List<LazerScore> {
        return base.osuApiWebClient.get()
            .uri { it.path("beatmaps/{bid}/scores")
                .queryParam("legacy_only", if (legacy) 1 else 0)
                .queryParamIfPresent("mode", OsuMode.getQueryName(mode))
                .build(bid)
            }
            .headers { base.insertHeader(it) }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { JacksonUtil.parseObjectList(it["scores"], LazerScore::class.java) }
            .block()!!
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun asyncDownloadBackground(scores: Iterable<LazerScore>, type: CoverType?) {
        val path = Path.of(IMG_BUFFER_PATH)
        if (Files.isDirectory(path).not() || Files.isWritable(path).not() ) return

        val actions = scores.map { score ->
            return@map AsyncMethodExecutor.Runnable {
                val covers = score.beatMapSet.covers

                val url = when(type) {
                    CARD -> covers.card
                    CARD_2X -> covers.card2x
                    COVER_2X -> covers.cover2x
                    LIST -> covers.list
                    LIST_2X -> covers.list2x
                    SLIM_COVER -> covers.slimcover
                    SLIM_COVER_2X -> covers.slimcover2x
                    else -> covers.cover
                }

                if (url.isNullOrBlank()) {
                    log.info("异步下载谱面图片：成绩的谱面不完整")
                    return@Runnable
                }

                val md = MessageDigest.getInstance("MD5")

                try {
                    md.update(url.toByteArray(Charsets.UTF_8))
                } catch (e: Exception) {
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
                        base.osuApiWebClient.get()
                            .uri { it.scheme("https").host("assets.ppy.sh").replacePath(replacePath)
                                .query(query.toString())
                                .build() }
                            .headers { base.insertHeader(it) }
                            .retrieve()
                            .bodyToMono(ByteArray::class.java)
                            .block()!!
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
            try {
                return base.osuApiWebClient.get()
                    .uri { it.path("scores/{score}/download")
                        .build(score.scoreID)
                    }
                    .headers { base.insertHeader(it) }
                    .retrieve()
                    .bodyToMono(ByteBuffer::class.java)
                    .map { Replay(it) }
                    /*
                    .bodyToMono(JsonNode::class.java)
                    .map { JacksonUtil.parseObject(it, Replay::class.java) }

                     */
                    .block()!!
            } catch (e: Exception) {
                throw e
            }


        } else return null
    }

    private fun getRecent(
        user: BindUser,
        mode: OsuMode?,
        includeFails: Boolean,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return base.request { client ->
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
        return base.request { client ->
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
            .headers { base.insertHeader(it) }
            .retrieve()
            .bodyToFlux(LazerScore::class.java)
            .collectList()
            .doOnNext(scoreDao::saveScoreAsync)
        }

    }

    private inline fun <reified T> retryOn404(
        uri: Function<UriBuilder, URI>,
        headers: Consumer<HttpHeaders>,
        retry: Function<UriBuilder, URI>,
    ): T {
        return try {
            base.request {
                it.get()
                .uri(uri)
                .headers(headers)
                .retrieve()
                .bodyToMono(T::class.java)
            }
        } catch (e: WebClientResponseException.NotFound) {
            base.request {
                it.get()
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

    enum class CoverType {
        RAW, CARD, CARD_2X, COVER, COVER_2X, LIST, LIST_2X, SLIM_COVER, SLIM_COVER_2X
    }
}
