package com.now.nowbot.service.osuApiService

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.BeatmapMirrorConfig
import jakarta.annotation.Nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.file.Files
import java.nio.file.Path

@Service("OsuBeatmapMirrorApiService") class OsuBeatmapMirrorApiService(
    private val webClient: WebClient,
    beatmapMirrorConfig: BeatmapMirrorConfig
) {
    private val url = beatmapMirrorConfig.url

    @Nullable fun getOsuFile(bid: Long): String? {
        if (url == null) return null

        try {
            return webClient.get().uri(url) { it.path("/api/file/map/osufile/{bid}").build(bid) }.retrieve()
                .bodyToMono(String::class.java).map { s: String ->
                    if (s.trim().startsWith("osu file format")) {
                        return@map s
                    }
                    throw IllegalStateException("not beatmap file")
                }.block()
        } catch (e: Exception) {
            if (e is WebClientResponseException) {
                log.error("谱面镜像站：返回谱面 {} 失败：{}", bid, e.statusCode)
                log.error(e.responseBodyAsString)
            }
            if (e is WebClientRequestException) {
                log.error("谱面镜像站：请求谱面 {} 失败!", bid)
                log.error(e.message)
            }
            throw e
        }
    }

    @Nullable fun getFullBackgroundPath(bid: Long): Path? {
        if (url == null) return null

        try {
            val localPath = webClient.get().uri(url) { it.path("/api/file/local/bg/{bid}").build(bid) }.retrieve()
                .bodyToMono(String::class.java).block()!!
            val path = Path.of(localPath)
            if (Files.isRegularFile(path)) {
                return path
            }

            log.error("获取谱面 {} 背景失败: 文件 [{}] 不存在, 大概率被版权然后移出了资源", bid, localPath)
            return null
        } catch (e: WebClientResponseException) {
            val json = e.getResponseBodyAs(
                JsonNode::class.java
            )
            if (json != null) log.error(
                "获取谱面 {} 背景失败: {}", bid, json["message"]
            )
            return null
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OsuBeatmapMirrorApiService::class.java)
    }
}
