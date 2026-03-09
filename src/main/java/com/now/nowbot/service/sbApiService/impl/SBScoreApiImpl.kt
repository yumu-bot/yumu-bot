package com.now.nowbot.service.sbApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.ppysb.SBScore
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.toBody
import io.netty.channel.unix.Errors
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.*
import kotlin.math.min

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

        val size = min(off + lim, 100)

        return request { client ->
            client.get().uri {
                it.path("/v1/get_player_scores")
                    .queryParam("scope", scope) // recent, best
                    .queryParamIfPresent("id", Optional.ofNullable(id))
                    .queryParamIfPresent("name", Optional.ofNullable(username))
                    .queryParamIfPresent("mode", Optional.ofNullable(modeValue))
                    .queryParam("limit", size)
                    .queryParamIfPresent("include_loved", Optional.ofNullable(includeLoved))
                    .queryParamIfPresent("include_failed", Optional.ofNullable(includeFailed))

                if (!mods.isNullOrEmpty()) {
                    it.queryParam("mods", LazerMod.getModsValue(mods))
                }

                it.build()
            }.toBody<JsonNode>().let {
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

        val size = min(off + lim, 100)

        return request { client ->
            client.get().uri {
                it.path("/v1/get_map_scores")
                    .queryParam("scope", scope) // recent, best
                    .queryParamIfPresent("id", Optional.ofNullable(id))
                    .queryParamIfPresent("md5", Optional.ofNullable(md5))
                    .queryParamIfPresent("mode", Optional.ofNullable(modeValue))
                    .queryParam("limit", size)

                if (!mods.isNullOrEmpty()) {
                    it.queryParam("mods", LazerMod.getModsValue(mods))
                }

                it.build()
            }.toBody<JsonNode>().let {
                parseList<SBScore>(it, "scores", "谱面成绩")
            }
        }.drop(off)
    }

    /**
     * 错误包装
     */
    private fun <T> request(request: (RestClient) -> T): T {
        return try {
            request(base.sbApiRestClient)
        } catch (e: Exception) {
            val cause = e as? RestClientResponseException ?: e.cause
            if (cause is RestClientResponseException) {
                when (cause.statusCode.value()) {
                    400 -> throw NetworkException.ScoreException.BadRequest()
                    401 -> throw NetworkException.ScoreException.Unauthorized()
                    403 -> throw NetworkException.ScoreException.Forbidden()
                    404 -> throw NetworkException.ScoreException.NotFound()
                    422 -> throw NetworkException.UserException.UnprocessableEntity()
                    503 -> throw NetworkException.ScoreException.ServiceUnavailable()
                }
            }

            if (e.findCauseOfType<Errors.NativeIoException>() != null) {
                throw NetworkException.ScoreException.GatewayTimeout()
            } else if (e.findCauseOfType<ReadTimeoutException>() != null) {
                throw NetworkException.ScoreException.RequestTimeout()
            } else {
                throw NetworkException.ScoreException.Undefined(e)
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