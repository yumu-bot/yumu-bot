package com.now.nowbot.service.sbApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.ppysb.SBScore
import com.now.nowbot.service.sbApiService.SBScoreApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class SBScoreApiImpl(private val base: SBBaseService): SBScoreApiService {
    override fun getScore(
        id: Long?,
        username: String?,
        mods: List<LazerMod>?,
        mode: OsuMode?,
        limit: Int?,
        includedLoved: Boolean,
        includeFailed: Boolean,
        scope: String
    ): List<SBScore> {
        val modeValue = if (mode?.modeValue == (-1).toByte()) null else mode?.modeValue?.toInt()

        return base.sbApiWebClient.get().uri { it
            .path("/v1/get_player_scores")
            .queryParam("scope", scope) // recent, best
            .queryParamIfPresent("id", Optional.ofNullable(id))
            .queryParamIfPresent("name", Optional.ofNullable(username))
            .queryParamIfPresent("mods[]", Optional.ofNullable(mods?.map { m -> m.acronym }?.toTypedArray()))
            .queryParamIfPresent("mode", Optional.ofNullable(modeValue))
            .queryParamIfPresent("limit", Optional.ofNullable(limit))
            .queryParamIfPresent("included_loved", Optional.ofNullable(includedLoved))
            .queryParamIfPresent("included_failed", Optional.ofNullable(includeFailed))
            .build()
        }.retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { parseList<SBScore>(it, "scores", "玩家成绩") }
            .block()!!
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