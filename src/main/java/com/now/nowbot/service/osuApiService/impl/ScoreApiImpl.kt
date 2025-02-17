package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatmapUserScore
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.util.function.Consumer
import java.util.function.Function

@Service
class ScoreApiImpl(
    val base: OsuApiBaseService,
    val scoreDao: ScoreDao,
) : OsuScoreApiService {

    override fun getBestScores(
        user: BinUser,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        if (!user.isAuthorized) return getBestScores(user.osuID, mode, offset, limit)
        return base.request { client ->
            client.get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .path("users/{uid}/scores/best")
                        .queryParam("legacy_only", 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(user.osuID)
                }
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToFlux(LazerScore::class.java)
                .collectList()
        }
    }

    override fun getBestScores(
        id: Long,
        mode: OsuMode?,
        offset: Int,
        limit: Int,
    ): List<LazerScore> {
        return base.request { client ->
            client.get()
                .uri { uriBuilder: UriBuilder ->
                    uriBuilder
                        .path("users/{uid}/scores/best")
                        .queryParam("legacy_only", 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(id)
                }
                .headers { headers: HttpHeaders? -> base.insertHeader(headers) }
                .retrieve()
                .bodyToFlux(LazerScore::class.java)
                .collectList()
        }
    }

    override fun getPassedScore(
        user: BinUser,
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
        user: BinUser,
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
                    .queryParamIfPresent("mode", OsuMode.getName(mode))
                    .build(bid, uid)
            },
            { headers: HttpHeaders? -> base.insertHeader(headers) },
            { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 1)
                    .queryParamIfPresent("mode", OsuMode.getName(mode))
                    .build(bid, uid)
            },
        )
    }

    override fun getBeatMapScore(
        bid: Long,
        user: BinUser,
        mode: OsuMode?,
    ): BeatmapUserScore? {
        if (!user.isAuthorized) return getBeatMapScore(bid, user.osuID, mode)
        return retryOn404<BeatmapUserScore>(
            {
                it.path("beatmaps/{bid}/scores/users/{uid}").queryParam("legacy_only", 0)

                if (OsuMode.isNotDefaultOrNull(mode)) {
                    it.queryParam("mode", OsuMode.getName(mode))
                }

                it.build(bid, user.osuID)
            },
            base.insertHeader(user),
            {
                it.path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", 1)

                if (OsuMode.isNotDefaultOrNull(mode)) {
                    it.queryParam("mode", OsuMode.getName(mode))
                }

                it.build(bid, user.osuID)
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
                    uriBuilder.queryParam("mode", OsuMode.getName(mode))
                }

                setMods(uriBuilder, mods)
                uriBuilder.build(bid, uid)
            }
        }
        return retryOn404<BeatmapUserScore>(
            uri.apply(0),
            { headers: HttpHeaders? -> base.insertHeader(headers) },
            uri.apply(1),
        )
    }

    override fun getBeatMapScore(
        bid: Long,
        user: BinUser,
        mode: OsuMode,
        mods: Iterable<LazerMod?>,
    ): BeatmapUserScore? {
        if (!user.isAuthorized) {
            return getBeatMapScore(bid, user.osuID, mode, mods)
        }
        val uri = Function { n: Int? ->
            Function { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}")
                    .queryParam("legacy_only", n)
                    .queryParamIfPresent("mode", OsuMode.getName(mode))
                setMods(uriBuilder, mods)
                uriBuilder.build(bid, user.osuID)
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
        mods.forEach(Consumer { mod: LazerMod? -> builder.queryParam("mods[]", mod!!.acronym) })
    }

    override fun getBeatMapScores(bid: Long, user: BinUser, mode: OsuMode?): List<LazerScore> {
        if (!user.isAuthorized) getBeatMapScores(bid, user.osuID, mode)
        return base.request { client ->
            client.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}/all")
                    .queryParam("legacy_only", 0)
                    .queryParamIfPresent("mode", OsuMode.getName(mode))
                    .build(bid, user.osuID)
            }
            .headers(base.insertHeader(user))
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { json: JsonNode ->
                JacksonUtil.parseObjectList(json["scores"], LazerScore::class.java)
            }
        }
    }

    override fun getBeatMapScores(bid: Long, uid: Long, mode: OsuMode?): List<LazerScore> {
        return base.request { client ->
            client.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores/users/{uid}/all")
                    .queryParam("legacy_only", 0)
                    .queryParamIfPresent("mode", OsuMode.getName(mode))
                    .build(bid, uid)
            }
            .headers { headers: HttpHeaders? -> base.insertHeader(headers) }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { json: JsonNode ->
                JacksonUtil.parseObjectList(json["scores"], LazerScore::class.java)
            }
        }
    }

    override fun getLeaderBoardScore(bid: Long, mode: OsuMode?): List<LazerScore> {
        return base.request { client ->
            client.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("beatmaps/{bid}/scores")
                    .queryParam("legacy_only", 0)
                    .queryParamIfPresent("mode", OsuMode.getName(mode))
                    .build(bid)
            }
            .headers { headers: HttpHeaders? -> base.insertHeader(headers) }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { json: JsonNode ->
                JacksonUtil.parseObjectList(json["scores"], LazerScore::class.java)
            }
        }
    }

    private fun getRecent(
        user: BinUser,
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
                    .queryParamIfPresent("mode", OsuMode.getName(mode))
                    .build(user.osuID)
            }
            .headers(base.insertHeader(user))
            .retrieve()
            .bodyToFlux(LazerScore::class.java)
            .collectList()
            .doOnNext(scoreDao::saveScoreAsync)
        }
    }

    fun getRecent(
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
                    .queryParamIfPresent("mode", OsuMode.getName(mode))
                    .build(uid)
            }
            .headers { headers: HttpHeaders? -> base.insertHeader(headers) }
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
    }
}
