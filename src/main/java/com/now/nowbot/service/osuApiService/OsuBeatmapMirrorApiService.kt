package com.now.nowbot.service.osuApiService

import com.now.nowbot.config.BeatmapMirrorConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.file.Files
import java.nio.file.Path

@Service("OsuBeatmapMirrorApiService") class OsuBeatmapMirrorApiService(
    private val webClient: WebClient,
    beatmapMirrorConfig: BeatmapMirrorConfig
) {
    private val url = beatmapMirrorConfig.url

    fun getOsuFile(bid: Long): String? {
        if (url.isNullOrEmpty()) return null

        try {
            val str = webClient.get().uri(url) {
                it.path("/api/file/map/osufile/{bid}").build(bid)}
                .retrieve()
                .bodyToMono(String::class.java).block()!!

            val first20 = str.trim().take(20)

            return if (first20.startsWith("osu file format")) {
                str
            } else {
                log.error("谱面镜像站：谱面 $bid 文件损坏！\n谱面前 20 位是：$first20")
                null
            }
        } catch (e: Exception) {
            // TODO 谱面镜像站修好之前，这个都要忽视掉
            // log.error("谱面镜像站：请求谱面 $bid 失败：${e.message}")

            return null
        }
    }

    fun getFullBackgroundPath(bid: Long): Path? {
        if (url.isNullOrEmpty()) return null

        try {
            val localPath = webClient.get().uri(url) { it.path("/api/file/local/bg/{bid}").build(bid) }.retrieve()
                .bodyToMono(String::class.java).block()!!
            val path = Path.of(localPath)

            if (Files.isRegularFile(path)) {
                return path
            }

            log.error("获取谱面 $bid 背景失败: 文件 [$localPath] 不存在")
            return null
        } catch (e: WebClientResponseException) {
            log.error("获取谱面 $bid 背景失败：${e.statusCode}")
            return null
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OsuBeatmapMirrorApiService::class.java)
    }
}
