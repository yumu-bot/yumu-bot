package com.now.nowbot.service.lxnsApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.model.maimai.LxMaiSong
import com.now.nowbot.model.maimai.MaiAlias
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.service.lxnsApiService.LxMaiApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.JacksonUtil
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class LxMaiApiImpl(private val base: LxnsBaseService, private val maiDao: MaiDao): LxMaiApiService {

    override fun getLxMaiSongs(): List<LxMaiSong> {
        val node = request { client -> client.get()
            .uri {
                it.path("api/v0/maimai/song/list")
                    .queryParam("notes", true)
                    .build() }
            .headers(base::insertDeveloperHeader)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
        }

        return JacksonUtil.parseObjectList(node["songs"]!!, LxMaiSong::class.java)
    }

    override fun getMaiSongs(): List<MaiSong> {
        val list = getLxMaiSongs()

        val standard = list
            .filter { it.difficulties.standard.isNotEmpty() }
            .map { it.toMaiSong(type = false) }

        val deluxe = list
            .filter { it.difficulties.deluxe.isNotEmpty() }
            .map { it.toMaiSong(type = true) }

        val utage = list
            .filter { it.difficulties.utage.isNotEmpty() }
            .map { it.toMaiSong(type = null) }

        return (deluxe + standard + utage)
    }

    override fun saveLxMaiSongs() {
        val songs = getLxMaiSongs()

        maiDao.saveLxMaiSongs(songs)
        log.info("舞萌: 落雪歌曲数据库已更新")
    }

    override fun getLxMaiSong(songID: Long): LxMaiSong? {
        val o = maiDao.findLxMaiSongByID(songID.toInt())
        insertMaimaiAlias(o)
        return o
    }

    override fun getMaimaiAlias(songID: Int): MaiAlias? {
        return maiDao.getMaiAliasByID((songID % 10000))
    }


    override fun insertMaimaiAlias(song: LxMaiSong?) {
        if (song != null) {
            song.aliases = getMaimaiAlias(song.songID)?.alias
            song.alias = song.aliases?.minByOrNull { it.length }
        }
    }

    override fun insertMaimaiAlias(songs: List<LxMaiSong>?) {
        if (songs.isNullOrEmpty()) return

        val actions = songs.map {
            return@map AsyncMethodExecutor.Runnable {
                it.aliases = getMaimaiAlias(it.songID)?.alias
                it.alias = it.aliases?.minByOrNull { it.length }
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun getPossibleMaiSongs(text: String): List<MaiSong> {
        return maiDao.findLxMaiSongByTitle(text).map { it.toMaiSong() }
    }

    override fun getMaiAliasLibrary(): Map<Int, List<String>> {
        return maiDao.getAllMaiAliases().associate { it.songID to it.alias }
    }

    override fun getMaiAliasSongs(text: String): List<MaiSong> {
        val aliases = getMaiAliasLibrary()
        val result = mutableListOf<Triple<LxMaiSong, Int, Double>>()

        search@ for (e in aliases.entries) {
            for (alias in e.value) {

                val y = DataUtil.getStringSimilarity(text, alias)

                if (y >= 0.5) {
                    val s = maiDao.findLxMaiSongByID(e.key)
                        ?: maiDao.findLxMaiSongByID(e.key + 10000) //getMaimaiSong(e.key.toLong()) ?: getMaimaiSong(e.key + 10000L) 避免循环引用

                    if (s != null) {
                        s.aliases = e.value
                        s.alias = e.value.minByOrNull { it.length }

                        result.add(Triple(s, s.songID, y))
                        continue@search
                    }
                }
            }
        }

        return if (result.isEmpty()) {
            listOf()
        } else {
            result.sortBy { it.second }
            result.sortByDescending { it.third }

            result.map { it.first.toMaiSong() }
        }
    }

    /**
     * 错误包装
     */
    @Throws(NetworkException::class)
    private fun <T> request(request: (WebClient) -> Mono<T>): T {
        return try {
            request(base.lxnsApiWebClient).block()!!
        } catch (_: WebClientResponseException.BadGateway) {
            throw NetworkException.LxnsException.BadGateway()
        } catch (_: WebClientResponseException.Unauthorized) {
            throw NetworkException.LxnsException.Unauthorized()
        } catch (_: WebClientResponseException.Forbidden) {
            throw NetworkException.LxnsException.Forbidden()
        } catch (_: ReadTimeoutException) {
            throw NetworkException.LxnsException.RequestTimeout()
        } catch (_: WebClientResponseException.InternalServerError) {
            throw NetworkException.LxnsException.InternalServerError()
        } catch (e: Exception) {
            log.error("落雪咖啡屋：获取失败", e)
            throw NetworkException.LxnsException.Undefined(e)
        }
    }

    companion object {

        private inline fun <reified T> parse(node: JsonNode, field: String, name: String): T {
            val success = node.get("success").asText("未知")

            if (success != "true") {
                throw TipsException("""
                    获取${name}失败。
                    失败代码：${node.get("code").asInt(-1)}
                    失败原因：${node.get("message").asText("未知")}
                    """.trimIndent()
                )
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

        private val log: Logger = LoggerFactory.getLogger(LxMaiApiService::class.java)

    }

}