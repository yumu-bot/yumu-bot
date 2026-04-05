package com.now.nowbot.service.lxnsApiService.impl

import com.now.nowbot.dao.MaiDao
import com.now.nowbot.model.maimai.LxMaiCollection
import com.now.nowbot.model.maimai.LxMaiSong
import com.now.nowbot.model.maimai.MaiAlias
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.service.lxnsApiService.LxMaiApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.toBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

@Service
class LxMaiApiImpl(
    private val base: LxnsBaseService,
    private val maiDao: MaiDao
) : LxMaiApiService {

    override fun getAudio(songID: Int): ByteArray {
        return request { client ->
            client.get()
                .uri {
                    it.host(base.assetHost)
                        .path("maimai/music/${songID % 10000}.mp3")
                        .build()
                }
                .headers(base::insertDeveloperHeader)
                .toBody<ByteArray>()
        }
    }

    override fun getLxMaiSongs(): List<LxMaiSong> {
        val jsonString = request { client ->
            client.get()
                .uri {
                    it.path("api/v0/maimai/song/list")
                        .queryParam("notes", true)
                        .build()
                }
                .headers(base::insertDeveloperHeader)
                .toBody<String>()
        }
        val node = JacksonUtil.toNode(jsonString)

        return JacksonUtil.parseObjectList(node.get("songs"), LxMaiSong::class.java)
    }

    override fun getMaiSong(songID: Int): MaiSong? {
        val convertedID = LxMaiApiService.convertToLxMaiSongID(songID)

        return getLxMaiSong(convertedID)?.toMaiSong()
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

    override fun saveLxMaiCollections() {
        val plates = getLxMaiCollection("plate", "plates")

        maiDao.saveLxMaiCollections(plates)

        Thread.startVirtualThread {
            Thread.sleep(1000)
        }.join()

        val trophies = getLxMaiCollection("trophy", "trophies")

        maiDao.saveLxMaiCollections(trophies)

        Thread.startVirtualThread {
            Thread.sleep(1000)
        }.join()

        val icons = getLxMaiCollection("icon", "icons")

        maiDao.saveLxMaiCollections(icons)

        Thread.startVirtualThread {
            Thread.sleep(1000)
        }.join()

        val frames = getLxMaiCollection("frame", "frames")

        maiDao.saveLxMaiCollections(frames)

        log.info("舞萌: 落雪收藏数据库已更新")
    }

    override fun getLxMaiSong(songID: Int): LxMaiSong? {
        val convertedID = LxMaiApiService.convertToLxMaiSongID(songID)

        val o = maiDao.findLxMaiSongByID(convertedID)
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
                it.alias = it.aliases?.minByOrNull { alias -> alias.length }
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun insertMaimaiAliasForMaiSong(songs: List<MaiSong>?) {
        if (songs.isNullOrEmpty()) return

        val actions = songs.map {
            return@map AsyncMethodExecutor.Runnable {
                it.aliases = getMaimaiAlias(it.songID)?.alias
                it.alias = it.aliases?.minByOrNull { alias -> alias.length }
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

    override fun getLxMaiCollection(type: String, types: String): List<LxMaiCollection> {

        val jsonString = request { client ->
            client.get()
                .uri {
                    it.path("api/v0/maimai/${type}/list")
                        .queryParam("required", true)
                        .build()
                }
                .headers(base::insertDeveloperHeader)
                .toBody<String>()
        }
        val node = JacksonUtil.toNode(jsonString)

        val body = if (node.has(types)) {
            node[types]
        } else {
            throw NoSuchElementException.Data()
        }

        return JacksonUtil.parseObjectList(body, LxMaiCollection::class.java).onEach { it.type = type }
    }

    /**
     * 错误包装
     */
    @Throws(NetworkException::class)
    private fun <T : Any> request(request: (RestClient) -> T): T {
        return try {
            request(base.lxnsApiRestClient)
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<HttpClientErrorException>()

            when (ex?.statusCode?.value()) {
                502 -> throw NetworkException.LxnsException.BadGateway()
                500 -> throw NetworkException.LxnsException.InternalServerError()
                401 -> throw NetworkException.LxnsException.Unauthorized()
                403 -> throw NetworkException.LxnsException.Forbidden()
                404 -> throw NetworkException.LxnsException.NotFound()
                408 -> throw NetworkException.LxnsException.RequestTimeout()
                429 -> throw NetworkException.LxnsException.TooManyRequests()
                503 -> throw NetworkException.LxnsException.ServiceUnavailable()

                504 -> throw NetworkException.LxnsException.GatewayTimeout()
                else -> {
                    log.error("落雪咖啡屋：获取失败", e)
                    throw NetworkException.LxnsException.Undefined(e)
                }
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LxMaiApiService::class.java)


    }
}