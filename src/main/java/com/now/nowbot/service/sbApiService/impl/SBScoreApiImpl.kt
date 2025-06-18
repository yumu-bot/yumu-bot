package com.now.nowbot.service.sbApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.ppysb.SBScore
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.*

@Service
class SBScoreApiImpl(private val base: SBBaseService): SBScoreApiService {
    override fun getScore(
        id: Long?,
        username: String?,
        mods: List<LazerMod>?,
        mode: OsuMode?,
        offset: Int?,
        limit: Int?,
        includeLoved: Boolean,
        includeFailed: Boolean,
        scope: String
    ): List<SBScore> {
        val modeValue = if (mode?.modeValue == (-1).toByte()) {
            null
        } else {
            mode?.modeValue?.toInt()
        }

        val off = (offset ?: 0)
        val lim = (limit ?: 100)

        val size = off + lim

        return request { client ->
            client.get().uri {

                if (!mods.isNullOrEmpty()) {
                    it.queryParam("mods", LazerMod.getModsValue(mods))
                }

                it.path("/v1/get_player_scores")
                    .queryParam("scope", scope) // recent, best
                    .queryParamIfPresent("id", Optional.ofNullable(id))
                    .queryParamIfPresent("name", Optional.ofNullable(username))
                    .queryParamIfPresent("mode", Optional.ofNullable(modeValue))
                    .queryParam("limit", size)
                    .queryParamIfPresent("include_loved", Optional.ofNullable(includeLoved))
                    .queryParamIfPresent("include_failed", Optional.ofNullable(includeFailed))
                    .build()
            }.retrieve().bodyToMono(JsonNode::class.java).map {
                parseList<SBScore>(it, "scores", "玩家成绩")
            }
        }
            .drop(off)
    }

    override fun getBeatmapScore(
        id: Long?,
        md5: String?,
        mods: List<LazerMod>?,
        mode: OsuMode?,
        offset: Int?,
        limit: Int?,
        scope: String
    ): List<SBScore> {
        val modeValue = if (mode?.modeValue == (-1).toByte()) {
            null
        } else {
            mode?.modeValue?.toInt()
        }

        val off = (offset ?: 0)
        val lim = (limit ?: 100)

        val size = off + lim

        return request { client ->
            client.get().uri {

                if (!mods.isNullOrEmpty()) {
                    it.queryParam("mods", LazerMod.getModsValue(mods))
                }

                it.path("/v1/get_map_scores")
                    .queryParam("scope", scope) // recent, best
                    .queryParamIfPresent("id", Optional.ofNullable(id))
                    .queryParamIfPresent("md5", Optional.ofNullable(md5))
                    .queryParamIfPresent("mode", Optional.ofNullable(modeValue))
                    .queryParam("limit", size)
                    .build()
            }.retrieve().bodyToMono(JsonNode::class.java).map {
                parseList<SBScore>(it, "scores", "谱面成绩")
            }
        }.drop(off)
    }

    /**
     * 错误包装
     */
    private fun <T> request(request: (WebClient) -> Mono<T>): T {
        return try {
            request(base.sbApiWebClient).block()!!
        } catch (e: Throwable) {
            when (e.cause) {
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

                is WebClientResponseException.ServiceUnavailable -> {
                    throw NetworkException.ScoreException.ServiceUnavailable()
                }

                else -> throw NetworkException.ScoreException(e.message)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBScoreApiService::class.java)

        private inline fun <reified T> parse(node: JsonNode, field: String, name: String): T {
            val status = node.get("status").asText("未知")

            if (status != "success") {
                throw TipsException("获取${name}失败。失败提示：${status}")
            } else try {
                return JacksonUtil.parseObject(node[field]!!, T::class.java)
            } catch (e : Exception) {
                log.error("生成${name}失败。", e)
                return T::class.objectInstance!!
            }
        }

        private inline fun <reified T> parseList(node: JsonNode, field: String, name: String): List<T> {
            val status = node.get("status").asText("未知")

            if (status != "success") {
                throw TipsException("获取${name}失败。失败提示：${status}")
            } else try {
                return JacksonUtil.parseObjectList(node[field], T::class.java)
            } catch (e : Exception) {
                log.error("生成${name}失败。", e)
                return listOf()
            }
        }
    }
}