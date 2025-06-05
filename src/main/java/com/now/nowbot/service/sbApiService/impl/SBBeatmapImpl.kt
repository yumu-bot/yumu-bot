package com.now.nowbot.service.sbApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.ppysb.SBBeatmap
import com.now.nowbot.service.sbApiService.SBBeatmapApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class SBBeatmapImpl(private val base: SBBaseService): SBBeatmapApiService {
    override fun getBeatmap(id: Long?, md5: String?): SBBeatmap? {
        return base.sbApiWebClient.get().uri { it
            .path("/v1/get_map_info")
            .queryParamIfPresent("id", Optional.ofNullable(id))
            .queryParamIfPresent("md5", Optional.ofNullable(md5))
            .build()
        }.retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { parse<SBBeatmap>(it, "map", "谱面信息") }
            .block()!!
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