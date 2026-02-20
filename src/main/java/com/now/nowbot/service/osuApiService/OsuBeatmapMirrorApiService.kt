package com.now.nowbot.service.osuApiService

import com.now.nowbot.config.BeatmapMirrorConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Service("OsuBeatmapMirrorApiService")
class OsuBeatmapMirrorApiService(
    private val webClient: WebClient,
    beatmapMirrorConfig: BeatmapMirrorConfig
) {
    private val url = beatmapMirrorConfig.url
    private val token = beatmapMirrorConfig.token ?: "unknown"

    fun getOsuFile(bid: Long): String? {

        if (url.isNullOrEmpty()) return null

        return try {
            val str = webClient.get()
                .uri(url) { it.path("/api/mirror/beatmap/osufile/{bid}").build(bid) }
                .header("X-TOKEN", token)
                .retrieve()
                .bodyToMono(String::class.java).block()!!

            val sub = str.replaceBefore("osu", "")

            if (sub.startsWith("osu file format")) {
                sub
            } else {
                log.error("谱面镜像站：谱面 $bid 文件损坏！\n谱面前 100 位是：${sub.take(100)}")
                null
            }
        } catch (e: Exception) {
            log.error("谱面镜像站：请求谱面 $bid 失败：${e.message}")

            null
        }
    }

    fun getFullBackgroundPath(bid: Long): Path? {
        if (url.isNullOrEmpty()) return null

        try {
            val localPath = webClient.get()
                .uri(url) { it.path("/api/mirror/fileName/bg/{bid}").build(bid) }
                .header("X-TOKEN", token)
                .retrieve()
                .bodyToMono(String::class.java).block()!!
            val path = Path.of(localPath)

            var ready = false

            @Suppress("UNUSED")
            for (i in 1..10) {
                if (Files.exists(path) && !isFileLocked(path) && Files.size(path) > 0) {
                    ready = true
                    break
                }
                Thread.sleep(200)
            }

            if (!ready) {
                log.error("获取谱面 $bid 背景失败: 文件可能损坏或下载超时")
                return null
            }

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


        private fun isFileLocked(path: Path): Boolean {
            return try {
                // 尝试以写模式打开，如果被其他进程占用会抛出异常
                FileChannel.open(path, StandardOpenOption.WRITE).use {
                    false // 没锁住，可以访问
                }
            } catch (_: IOException) {
                true // 文件正在被另一个进程使用（下载中）
            }
        }
    }
}
