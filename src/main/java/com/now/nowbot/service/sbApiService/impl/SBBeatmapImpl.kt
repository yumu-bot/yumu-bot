package com.now.nowbot.service.sbApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.ppysb.SBBeatmap
import com.now.nowbot.service.sbApiService.SBBeatmapApiService
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
class SBBeatmapImpl(private val base: SBBaseService): SBBeatmapApiService {

    override fun getBeatmap(id: Long?, md5: String?): SBBeatmap? {
        return request { client ->
            client.get().uri { it
                .path("/v1/get_map_info")
                .queryParamIfPresent("id", Optional.ofNullable(id))
                .queryParamIfPresent("md5", Optional.ofNullable(md5))
                .build()
            }.retrieve()
                .bodyToMono(JsonNode::class.java)
                .map { parse<SBBeatmap>(it, "map", "谱面信息") }
        }
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
                    throw NetworkException.BeatmapException.BadRequest()
                }

                is WebClientResponseException.Unauthorized -> {
                    throw NetworkException.BeatmapException.Unauthorized()
                }

                is WebClientResponseException.Forbidden -> {
                    throw NetworkException.BeatmapException.Forbidden()
                }

                is WebClientResponseException.NotFound -> {
                    throw NetworkException.BeatmapException.NotFound()
                }

                is WebClientResponseException.TooManyRequests -> {
                    throw NetworkException.BeatmapException.TooManyRequests()
                }

                is WebClientResponseException.BadGateway -> {
                    throw NetworkException.BeatmapException.BadGateWay()
                }

                is WebClientResponseException.ServiceUnavailable -> {
                    throw NetworkException.BeatmapException.ServiceUnavailable()
                }

                else -> throw NetworkException.BeatmapException(e.message)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBBeatmapApiService::class.java)

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