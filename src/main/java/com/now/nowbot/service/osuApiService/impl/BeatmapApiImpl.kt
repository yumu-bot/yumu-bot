package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.FileConfig
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.dao.BeatmapDao
import com.now.nowbot.entity.BeatmapObjectCountLite
import com.now.nowbot.mapper.BeatmapObjectCountMapper
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.osu.Covers.Companion.CoverType.Companion.getString
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuBeatmapMirrorApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.JacksonUtil
import okhttp3.internal.toImmutableList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.math.min

@Service
class BeatmapApiImpl(
    private val base: OsuApiBaseService,
    config: FileConfig,
    private val beatmapDao: BeatmapDao,
    private val beatmapMirrorApiService: OsuBeatmapMirrorApiService,
    private val beatmapObjectCountMapper: BeatmapObjectCountMapper,

    @param:Qualifier("proxyRestClient") private val proxyClient: RestClient,
) : OsuBeatmapApiService {

    private val osuDir: Path = Path.of(config.osuFilePath)

    @OptIn(ExperimentalStdlibApi::class)
    override fun asyncDownloadCover(
        covers: List<Covers>,
        type: CoverType
    ) {
        AsyncMethodExecutor.asyncRunnableExecute {
            covers.forEach { c ->
                getCover(c, type)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getCover(covers: Covers, type: CoverType): ByteArray? {
        val path = Path.of(IMG_BUFFER_PATH)

        val default by lazy {
            try {
                Files.readAllBytes(
                    Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("Banner").resolve("c8.png")
                )
            } catch (_: okio.IOException) {
                null
            }
        }

        val url = covers.getString(type)

        if (url.isBlank()) {
            log.info("获取谱面图片：谱面封面类不完整")
            return default
        }

        val md = MessageDigest.getInstance("MD5")

        try {
            md.update(url.toByteArray(Charsets.UTF_8))
        } catch (_: Exception) {
            log.info("获取谱面图片：计算 MD5 失败")
            return default
        }

        val hex = md.digest().toHexString()

        return if (Files.isRegularFile(path.resolve(hex))) {
            Files.readAllBytes(path.resolve(hex))
        } else {
            try {
                val image = base.osuApiRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body<ByteArray>()!!

                if (Files.isDirectory(path) && Files.isWritable(path)) {
                    Files.write(path.resolve(hex), image)
                }

                return image
            } catch (_: Exception) {
                default
            }
        }
    }

    override fun hasBeatmapFileFromDirectory(bid: Long): Boolean {
        val path = osuDir.resolve("$bid.osu")
        return Files.isRegularFile(path)
    }

    private fun getBeatmapFileFromDirectory(bid: Long): String? {
        val path = osuDir.resolve("$bid.osu")

        if (hasBeatmapFileFromDirectory(bid)) {
            try {
                return Files.readString(path, StandardCharsets.UTF_8)
            } catch (e: IOException) {
                log.error("osu 谱面 API：读取本地谱面文件失败: ", e)
                return null
            }
        } else {
            return null
        }
    }

    private fun getBeatmapFileFromLocalService(bid: Long): String? {
        return beatmapMirrorApiService.getOsuFile(bid)

    }

    fun getBeatmapFileFromOfficialWebsite(bid: Long): String? {
        try {
            return request { client ->
                client.get()
                    .uri("https://osu.ppy.sh/osu/{bid}", bid)
                    .retrieve()
                    .body<String>() ?: ""
            }
        } catch (e: RestClientResponseException) {
            log.error("osu 谱面 API：请求官网谱面失败: ", e)
            return null
        }
    }

    override fun refreshBeatmapFileFromDirectory(bid: Long): Boolean {
        val path = osuDir.resolve("$bid.osu")

        if (hasBeatmapFileFromDirectory(bid)) {
            try {
                Files.delete(path)
            } catch (e: IOException) {
                log.error("osu 谱面 API：删除本地谱面文件失败: ", e)
                return false
            }

            val str = getBeatmapFileString(bid)

            return !str.isNullOrBlank()
        }

        return false
    }

    private fun writeBeatmapFileToDirectory(str: String?, bid: Long): Boolean {
        val path = osuDir.resolve("$bid.osu")

        if (!str.isNullOrBlank()) {
            try {
                Files.writeString(path, str, StandardCharsets.UTF_8)
                return true
            } catch (e: IOException) {
                log.error("osu 谱面 API：写入本地谱面文件失败: ", e)
                return false
            }
        } else {
            return false
        }
    }

    @Cacheable(value = ["beatmap file"], key = "#bid")
    override fun getBeatmapFileByte(bid: Long): ByteArray? {
        return getBeatmapFileString(bid)?.toByteArray(StandardCharsets.UTF_8)
    }

    private fun downloadBeatmapFileString(bid: Long): String? {
        var str: String? = try {
            getBeatmapFileFromLocalService(bid)
        } catch (_: Exception) {
            null
        }

        if (!str.isNullOrBlank()) {
            return str
        }

        str = try {
            getBeatmapFileFromOfficialWebsite(bid)
        } catch (_: Exception) {
            null
        }

        return str
    }

    // 获取谱面：先获取本地，再获取 bs api，最后获取网页
    override fun getBeatmapFileString(bid: Long): String? {
        var str: String? = null

        if (hasBeatmapFileFromDirectory(bid)) {
            str = getBeatmapFileFromDirectory(bid)
        }

        if (!str.isNullOrBlank()) {
            return str
        } else {
            str = downloadBeatmapFileString(bid)
        }

        if (!str.isNullOrBlank()) {
            writeBeatmapFileToDirectory(str, bid)
        }

        return str
    }

    // 查一下文件是否跟 checksum 是否对得上
    @Throws(IOException::class)
    override fun checkBeatmap(beatmap: Beatmap?): Boolean {
        if (beatmap == null) return false
        return checkBeatmap(beatmap.beatmapID, beatmap.md5 ?: return false)
    }

    @Throws(IOException::class)
    override fun checkBeatmap(bid: Long, checkStr: String?): Boolean {
        if (checkStr == null) return false

        val path = osuDir.resolve("$bid.osu")
        if (Files.isRegularFile(path) && checkStr.isNotBlank()) {
            return getBeatmapMD5(Files.readString(path)) == checkStr
        }
        return false
    }

    override fun checkBeatmap(beatmap: Beatmap, fileStr: String): Boolean {
        return getBeatmapMD5(fileStr) == beatmap.md5
    }

    private fun getBeatmapMD5(fileStr: String): String {
        return DigestUtils.md5DigestAsHex(fileStr.toByteArray(StandardCharsets.UTF_8))
    }

    override fun getBeatmap(bid: Long): Beatmap {
        val beatmap = request { client ->
            client.get()
                .uri("beatmaps/{bid}", bid)
                .headers(base::insertHeader)
                .retrieve()
                .body<Beatmap>()!!
        }

        beatmapDao.saveBeatmapAndSaveExtendAsync(beatmap)

        return beatmap
    }

    override fun getBeatmaps(ids: Iterable<Long>): List<Beatmap> {
        val idChunk = ids.chunked(50)

        val callables = idChunk.map { chunk ->
            Callable {
                getBeatmapsPrivate(chunk)
            }
        }

        val beatmaps = AsyncMethodExecutor.awaitCallableExecute(callables).flatten()

        beatmapDao.saveExtendedBeatmap(beatmaps)

        return beatmaps
    }

    /**
     * @param ids 注意，单次请求量必须小于等于 50
     */
    private fun getBeatmapsPrivate(ids: Iterable<Long>): List<Beatmap> {
        val json = request { client ->
            val idsStr = ids.map { id -> id.toString() }
            client.get()
                .uri {
                    it.path("beatmaps")
                    it.queryParam("ids[]", *idsStr.toTypedArray())
                    it.build()
                }
                .headers(base::insertHeader)
                .retrieve()
                .body<JsonNode>()!!
        }
        return JacksonUtil.parseObjectList(json["beatmaps"], Beatmap::class.java)
    }

    override fun getUserBeatmapset(id: Long, type: String, offset: Int, limit: Int): List<Beatmapset> {

        if (limit <= 100) {
            return getUserBeatmapsetPrivate(id, type, offset, limit)
        } else {
            val times = (limit / 100).coerceAtLeast(1)

            val works = (0..<times).map { i ->
                val of = offset + (i * 100)
                val li = min(100, limit - (i * 100))

                Callable {
                    getUserBeatmapsetPrivate(id, type, of, li)
                }
            }

            val async = AsyncMethodExecutor.awaitCallableExecute(works)

            return async.flatten()
        }
    }


    private fun getUserBeatmapsetPrivate(id: Long, type: String, offset: Int, limit: Int): List<Beatmapset> {
        val json = request { client ->
            client.get()
                .uri {
                    it
                        .path("/users/${id}/beatmapsets/${type}")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build()
                }
                .headers(base::insertHeader)
                .retrieve()
                .body<JsonNode>()!!
        }
        return JacksonUtil.parseObjectList(json, Beatmapset::class.java)
    }

    override fun getUserMostPlayedBeatmaps(id: Long, offset: Int, limit: Int): List<Beatmap> {
        if (limit <= 100) {
            return getUserMostPlayedBeatmapsPrivate(id, offset, limit)
        } else {
            val times = (limit / 100).coerceAtLeast(1)

            val works = (0..<times).map { i ->
                val of = offset + (i * 100)
                val li = min(100, limit - (i * 100))

                Callable {
                    getUserMostPlayedBeatmapsPrivate(id, of, li)
                }
            }

            val async = AsyncMethodExecutor.awaitCallableExecute(works)

            return async.flatten()
        }
    }

    private fun getUserMostPlayedBeatmapsPrivate(id: Long, offset: Int, limit: Int): List<Beatmap> {
        data class MostPlayed(
            @field:JsonProperty("beatmap_id")
            val beatmapID: Long,

            @field:JsonProperty("count")
            val count: Int,

            @field:JsonProperty("beatmap")
            val beatmap: Beatmap,

            @field:JsonProperty("beatmapset")
            val beatmapset: Beatmapset,
        )

        val node = request { client ->
            client.get()
                .uri {
                    it
                        .path("/users/${id}/beatmapsets/most_played")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build()
                }
                .headers(base::insertHeader)
                .retrieve()
                .body<JsonNode>()!!
        }

        val most = JacksonUtil.parseObjectList(node, MostPlayed::class.java)

        return most.map { mp ->
            mp.beatmap.copy().apply {
                this.beatmapset = mp.beatmapset
                this.beatmapID = mp.beatmapID
                this.currentPlayCount = mp.count
            }
        }
    }

    override fun getBeatmapset(sids: Iterable<Long>): List<Beatmapset> {
        val callables = sids.map { sid ->
            Callable {
                getBeatmapset(sid)
            }
        }

        return AsyncMethodExecutor.awaitCallableExecute(callables)
    }

    override fun extendBeatmapInSet(sets: Iterable<Beatmapset>): List<Beatmapset> {
        val map = sets.associate { set ->
            set.beatmapsetID to (set.beatmaps ?: listOf()).map { b -> b.beatmapID }
        }

        val bids = map.flatMap { it.value }

        val beatmaps = getBeatmaps(bids)

        val group = beatmaps.groupBy { it.beatmapsetID }

        sets.forEach { set ->
            group[set.beatmapsetID]?.let { set.beatmaps = it }
        }

        return sets.toList()
    }

    override fun extendBeatmapInScore(scores: Iterable<LazerScore>): List<LazerScore> {

        val bids = scores.map { it.beatmapID }.toHashSet()

        val beatmaps = getBeatmaps(bids).associateBy { it.beatmapID }

        scores.forEach { s ->
            beatmaps[s.beatmapID]?.let { s.beatmap = it }
        }

        return scores.toList()
    }

    override fun getBeatmapset(sid: Long): Beatmapset {
        val beatmapset = request { client ->
            client.get()
                .uri("beatmapsets/{sid}", sid)
                .headers(base::insertHeader)
                .retrieve()
                .body<Beatmapset>()!!
        }

        beatmapDao.saveBeatmapsetAsync(beatmapset)

        return beatmapset
    }

    override fun getBeatmapFromDatabase(bid: Long): Beatmap {
        try {
            val lite = beatmapDao.getBeatmapLite(bid)
            return BeatmapDao.fromBeatmapLite(lite!!)
        } catch (_: Exception) {
            return getBeatmap(bid)
        }
    }

    override fun applyVersion(scores: List<LazerScore>) {
        val existSet = scores.mapNotNull { s ->
            val b = beatmapDao.getBeatmapLite(s.beatmapID)

            b?.let {
                s.beatmap.difficultyName = b.version
                s.beatmap.starRating = b.difficultyRating.toDouble()
                s.beatmap.beatmapID = b.id
            }

            b?.id
        }.toSet()

        val notExistScores = scores.filterNot { it.beatmapID in existSet }

        if (notExistScores.isNotEmpty()) {
            applyBeatmapExtendFromAPI(notExistScores)
        }
    }


    override fun isNotOverRating(bid: Long): Boolean {
        try {
            val map = beatmapDao.getBeatmapLite(bid)!!
            return map.status.equals("ranked", ignoreCase = true) && map.difficultyRating <= 5.7
        } catch (_: Exception) {
        }

        try {
            val map = getBeatmap(bid)
            return map.status.equals("ranked", ignoreCase = true) && map.starRating <= 5.7
        } catch (e: Exception) {
            val ex = e.findCauseOfType<RestClientResponseException>()
            if (ex?.statusCode == org.springframework.http.HttpStatus.NOT_FOUND) {
                return false
            }
            throw e
        }
    }

    /**
     * @throws IndexOutOfBoundsException 谱面不完整的时候会丢这个
     */
    @Throws(IndexOutOfBoundsException::class)
    private fun getMapObjectList(mapStr: String): List<Int> {
        val start = mapStr.indexOf("[HitObjects]") + 12
        val end = mapStr.indexOf("[", start)
        val hit = if (end > start) {
            mapStr.substring(start, end)
        } else {
            mapStr.substring(start)
        }

        val hitObjects = hit.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val hitObjectStr = hitObjects.filterNot { it.isBlank() }

        val p = Pattern.compile("^\\d+,\\d+,(\\d+)")

        return hitObjectStr.map {
            val m2 = p.matcher(it)
            if (m2.find()) {
                return@map m2.group(1).toInt()
            } else {
                return@map 0
            }
        }
    }

    private fun getGrouping(x: List<Int>, groups: Int = 26): List<Int> {
        require(groups >= 1)
        if (x.isEmpty()) return emptyList()

        val steps: Int = (x.last() - x.first()) / (groups + 1)
        val out = LinkedList<Int>()
        var m: Int = x.first() + steps
        var sum: Short = 0
        for (i in x) {
            if (i < m) {
                sum++
            } else {
                out.add(sum.toInt())
                sum = 0
                m += steps
            }
        }

        return out
    }

    private fun getCount(bid: Long): BeatmapObjectCountLite? {
        val result = BeatmapObjectCountLite()
        result.bid = bid
        var file = getBeatmapFileString(bid)
        if (file == null) {
            refreshBeatmapFileFromDirectory(bid)
            file = getBeatmapFileString(bid)
            if (file == null) {
                return null
            }
        }
        val md5 = getBeatmapMD5(file)

        result.check = md5

        var objectList: List<Int>

        try {
            objectList = getMapObjectList(file)
        } catch (_: IndexOutOfBoundsException) {
            refreshBeatmapFileFromDirectory(bid)
            try {
                objectList = getMapObjectList(file)
            } catch (_: IndexOutOfBoundsException) {
                result.timestamp = intArrayOf()
                result.density = intArrayOf()
                return result
            }
        }

        result.timestamp = objectList.toIntArray()
        val grouping = getGrouping(objectList)
        result.density = grouping.toIntArray()

        return result
    }

    override fun getBeatmapObjectGrouping26(beatmap: Beatmap): IntArray {
        var result: IntArray? = null
        if (!beatmap.md5.isNullOrBlank()) {
            val r = beatmapObjectCountMapper.getDensityByBidAndCheck(
                beatmap.beatmapID, beatmap.md5!!
            )
            if (r.isNotEmpty()) result = r.first()
        } else {
            val r = beatmapObjectCountMapper.getDensityByBid(beatmap.beatmapID)
            if (r.isNotEmpty()) result = r.first()
        }

        if (result == null) {
            var dataObj = getCount(beatmap.beatmapID)
            dataObj = beatmapObjectCountMapper.saveAndFlush(dataObj!!)

            result = dataObj.density
        }

        return result ?: intArrayOf(0)
    }

    override fun getFailTime(bid: Long, passObj: Int): Int {
        if (passObj <= 0) return 0
        val time = beatmapObjectCountMapper.getTimeStampByBidAndIndex(bid, passObj) ?: try {
            var dataObj = getCount(bid) ?: return 0
            if (dataObj.timestamp!!.size < passObj) return 0

            dataObj = beatmapObjectCountMapper.saveAndFlush(dataObj)
            val start = dataObj.timestamp!![0]
            return dataObj.timestamp!![passObj] - start
        } catch (_: Exception) {
            return 0
        }
        return (time / 1000)
    }

    override fun getAllFailTime(all: List<Pair<Long, Int>>): List<Int> {
        if (all.isEmpty()) return emptyList()
        val bidList = all.map { it.first }
        val timeMap: Map<Long, IntArray> = beatmapObjectCountMapper
            .getTimeStampByBid(bidList).associate { it.bid to it.times }

        val result = ArrayList<Int>(all.size)

        for ((bid, passObj) in all) {
            val time = timeMap[bid] ?: try {
                val dataObj = getCount(bid)
                if (dataObj?.timestamp == null || dataObj.timestamp!!.size < passObj) {
                    result.add(0)
                    continue
                }
                beatmapObjectCountMapper.saveAndFlush(dataObj)
                timeMap.plus(bid to dataObj.timestamp)
                dataObj.timestamp!!
            } catch (_: Exception) {
                result.add(0)
                continue
            }
            if (time.size <= passObj) {
                result.add(0)
                continue
            }

            result.add((time[passObj] - time[0]) / 1000)
        }
        return result
    }

    override fun getAllBeatmapHitLength(bid: Collection<Long>): List<Pair<Long, Int>> {
        if (bid.isEmpty()) return emptyList()
        val timeMap = beatmapDao.getBeatmapHitLength(bid).associate { it.id to it.length }

        val result = ArrayList<Pair<Long, Int>>(bid.size)

        for (id in bid) {
            val time = timeMap[id]
            if (time != null) {
                result.add(id to time)
            } else {
                val beatmap = try {
                    getBeatmap(id)
                } catch (e: Exception) {
                    log.error("获取谱面失败", e)
                    result.add(id to 0)
                    continue
                }
                result.add(id to beatmap.hitLength!!)
            }
        }

        return result
    }

    /**
     * 计算成绩f时, 打到的进度
     *
     * @param score 成绩
     * @return double 0-1
     */
    override fun getPlayPercentage(score: LazerScore): Double {
        if (score.passed) return 1.0
        val n = score.scoreHit

        var playPercentage = beatmapObjectCountMapper.getTimeStampPercentageByBidAndIndex(score.beatmap.beatmapID, n)

        if (playPercentage == null) {
            try {
                getBeatmapObjectGrouping26(score.beatmap)
                playPercentage =
                    beatmapObjectCountMapper.getTimeStampPercentageByBidAndIndex(score.beatmap.beatmapID, n)
            } catch (e: Exception) {
                log.error("计算或存储物件数据失败", e)
                playPercentage = 1.0
            }
        }

        // 仍然失败, 取消计算
        return playPercentage ?: 1.0
    }

    data class AttributesResponse(val attributes: BeatmapDifficultyAttributes = BeatmapDifficultyAttributes())

    override fun getAttributes(id: Long, mode: OsuMode?, mods: List<LazerMod>?): BeatmapDifficultyAttributes {
        val body: MutableMap<String, Any> = HashMap()

        if (OsuMode.isNotDefaultOrNull(mode)) {
            body["ruleset_id"] = mode!!.modeValue
        }

        val modsInt = LazerMod.getModsValue(mods)

        if (!mods.isNullOrEmpty() && modsInt > 0) {
            body["mods"] = modsInt
        }

        return try {
            request { client ->
                client.post()
                    .uri("beatmaps/{id}/attributes", id)
                    .headers(base::insertHeader)
                    .body(body)
                    .retrieve()
                    .body<AttributesResponse>()!!.attributes
            }
        } catch (e: Exception) {
            BeatmapDifficultyAttributes()
        }
    }

    override fun lookupBeatmap(checksum: String?, filename: String?, id: Long?): JsonNode? {
        return try {
            request { client ->
                client.get().uri {
                    it.path("beatmapsets/lookup")
                        .queryParamIfPresent("checksum", Optional.ofNullable(checksum))
                        .queryParamIfPresent("filename", Optional.ofNullable(filename))
                        .queryParamIfPresent("id", Optional.ofNullable(id)).build()
                }
                    .headers(base::insertHeader)
                    .retrieve()
                    .body<JsonNode>()!!
            }
        } catch (e: Exception) {
            val ex = e.findCauseOfType<RestClientResponseException>()
            if (ex?.statusCode == org.springframework.http.HttpStatus.NOT_FOUND) {
                return null
            }
            throw e
        }
    }

    private fun searchBeatmapsetFromAPI(query: Map<String, Any?>, user: BindUser? = null): BeatmapsetSearch {
        return request { client ->
            client.get().uri {
                it.path("beatmapsets/search")
                query.forEach { (k: String, v: Any?) ->
                    if (v != null) {
                        it.queryParam(k, v)
                    } else {
                        it.queryParam(k)
                    }
                }
                it.build()
            }.headers { header ->
                if (user != null) {
                    base.insertHeader(header, user)
                } else {
                    base.insertHeader(header)
                }
            }
                .retrieve()
                .body<BeatmapsetSearch>()!!
        }
    }

    override fun searchBeatmapset(query: Map<String, Any?>, user: BindUser?): BeatmapsetSearch {
        val search = searchBeatmapsetFromAPI(query, user)

        // 后处理
        if (query["s"] !== null || query["s"] !== "any") {
            search.rule = query["s"].toString()
        }
        search.sortBeatmapDiff()

        return search
    }

    /**
     * 更加高效的搜索谱面集合方式
     * @param query
     * @param tries 尝试次数
     * @param quantity 同时获取多少页，这个太大不好，总可获取的页数是它乘以 tries
     * @param awaitMillis 每次并行获取后的冷却时间（毫秒），避免被判定为恶意获取
     */
    override fun searchBeatmapsetParallel(
        query: Map<String, Any?>,
        tries: Int,
        quantity: Int,
        awaitMillis: Long,
        user: BindUser?
    ): BeatmapsetSearch {
        val size = (tries * quantity).coerceAtLeast(1)

        val queries = List(size) { index ->
            val q = query.toMutableMap()
            q["page"] = index + 1

            q
        }.toImmutableList()

        val map = ConcurrentHashMap<Int, BeatmapsetSearch>()

        Thread.startVirtualThread {
            val isEnd = AtomicBoolean(false)
            val tried = AtomicInteger(0)

            do {
                val qs = queries.drop(tried.get() * quantity).take(quantity)

                val actions = qs.map { q ->
                    Callable {
                        searchBeatmapsetFromAPI(q, user)
                    }
                }

                val searches = AsyncMethodExecutor.awaitCallableExecute(actions)

                if (searches.map { it.cursorString }.any { it == null }) {
                    isEnd.set(true)
                }

                searches.filter { it.beatmapsets.isNotEmpty() }
                    .forEachIndexed { i, search ->
                        val index = tried.get() * quantity + i

                        map[index] = search
                    }

                tried.getAndIncrement()

                if (!isEnd.get() && tried.get() < tries) {
                    Thread.sleep(awaitMillis.coerceIn(0L, 30000L))
                }
            } while (!isEnd.get() && tried.get() < tries)
        }.join((5L + 5L * tries).coerceIn(10L, 30L) * 1000L)

        val search = BeatmapsetSearch()

        map.toList()
            .sortedBy { it.first }
            .forEach { search.combine(it.second) }

        // 后处理
        if (query["s"] !== null && query["s"] !== "any") {
            search.rule = query["s"].toString()
        }

        search.sortBeatmapDiff()

        return search
    }

    // 给同一张图的成绩添加完整的谱面
    override fun applyBeatmapExtendForSameScore(scores: List<LazerScore>, beatmap: Beatmap) {
        if (scores.isEmpty()) return

        beatmapDao.saveExtendedBeatmap(beatmap)

        for (score in scores) {
            applyBeatmapExtend(score, beatmap.copy())
        }
    }

    override fun applyBeatmapExtendFromAPI(score: LazerScore) {
        applyBeatmapExtendFromAPI(listOf(score))
    }

    override fun applyBeatmapExtendFromAPI(scores: List<LazerScore>) {
        val ids = scores.map { it.beatmapID }.toSet()

        val extends = getBeatmaps(ids)

        beatmapDao.saveExtendedBeatmap(extends)

        val map = extends.associateBy { it.beatmapID }

        scores.forEach { score ->
            map[score.beatmapID]?.let { applyBeatmapExtend(score, it) }
        }
    }

    /**
     * 这个方法本身不存 extended Beatmap
     */
    override fun applyBeatmapExtend(score: LazerScore, extended: Beatmap) {
        val lite = if (score.beatmap.beatmapID > 0L) {
            score.beatmap
        } else {
            // 部分成绩连谱面都没有，比如多成绩模式
            score.beatmap.apply {
                beatmapID = score.beatmapID
                mode = score.mode
                modeInt = score.ruleset.toInt()
            }
        }

        score.beatmapID = extended.beatmapID

        score.beatmap = extend(lite, extended)

        extended.beatmapset?.let { score.beatmapset = it }
    }

    override fun applyBeatmapExtend(scores: List<LazerScore>) {
        val existSet = scores.mapNotNull { s ->
            beatmapDao.extendBeatmap(s)
        }.toSet()

        val notExistScores = scores.filterNot { it.beatmapID in existSet }

        if (notExistScores.isNotEmpty()) {
            applyBeatmapExtendFromAPI(notExistScores)
        }
    }

    override fun applyBeatmapExtend(score: LazerScore) {
        applyBeatmapExtend(listOf(score))
    }

    override fun getBeatmapsetRankedTime(beatmap: Beatmap): String {
        return if (beatmap.ranked == 3) {
            try {
                val t = getBeatmapSetWithRankedTime(beatmap.beatmapsetID)

                if (t.isEarly) {
                    t.rankDateEarly.replace(".000Z", "Z")
                } else {
                    t.rankDate.replace(".000Z", "Z")
                }
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    override fun getBeatmapsetRankedTimeMap(): Map<Long, String> {
        return getBeatmapsetWithRankedTimeLibrary().associate {
            it.beatmapID to if (it.isEarly) {
                it.rankDateEarly.replace(".000Z", "Z")
            } else {
                it.rankDate.replace(".000Z", "Z")
            }
        }
    }

    override fun applyBeatmapsetRankedTime(beatmapsets: List<Beatmapset>) {
        val l = getBeatmapsetRankedTimeMap()

        beatmapsets.forEach {
            val t = l[it.beatmapsetID]

            if (t != null) {
                it.rankedDate = OffsetDateTime.parse(t)
            }
        }
    }

    private val beatmapTagLibraryFromAPI: JsonNode
        get() = request { client ->
            client.get()
                .uri {
                    it.path("tags").build()
                }.headers(base::insertHeader)
                .retrieve()
                .body<JsonNode>()!!
        }

    /*
.map { JacksonUtil.parseObjectList(it["tags"], Tag::class.java) }
.flatten()

     */

    override fun updateBeatmapTagLibraryDatabase() {
        val tags = JacksonUtil.parseObjectList(beatmapTagLibraryFromAPI["tags"], Tag::class.java)

        beatmapDao.saveTag(tags)

        log.info("谱面: 玩家标签已更新")
    }

    override fun extendBeatmapTag(beatmap: Beatmap) {
        val ids = beatmap.tagIDs

        if (ids.isNullOrEmpty()) return

        beatmap.tags = ids.mapNotNull { beatmapDao.getTag(it.id) }
    }

    /**
     * 定时任务，更新谱面的数据
     */
    @Transactional
    override fun updateExtendedBeatmapFailTimes(): Int {
        log.info("自动更新扩展谱面：正在启动")

        val ids = beatmapDao.findMapByUpdateAtAscend(LocalDateTime.now().minusDays(3)).map { it.beatmapID }

        if (ids.isEmpty()) {
            log.info("自动更新扩展谱面：没有可更新的谱面。")
            return 0
        }

        val news = try {
            getBeatmaps(ids)
        } catch (e: Exception) {
            log.error("自动更新扩展谱面：出现了问题", e)
            return -1
        }

        // 有图被 nuke 了
        if (news.size != ids.size && news.isNotEmpty()) {
            val nuked = ids.toSet().subtract(news.map { it.beatmapID }.toSet())

            beatmapDao.deleteExtendedBeatmapAndSet(nuked)
            log.warn("自动更新扩展谱面：删除了无法从官网获取的疑似下架谱面：${nuked.joinToString(", ")}")
        }

        val result = news.sumOf {
            beatmapDao.updateFailTimeByBeatmapID(it.beatmapID, it.failTimes?.toString())
        }

        val result2 = news
            .distinctBy { it.beatmapsetID }
            .sumOf {
                val set = it.beatmapset!!

                beatmapDao.updateFailTimeByBeatmapsetID(
                    it.beatmapsetID,
                    set.animeCover,
                    set.favouriteCount,
                    set.offset,
                    set.playCount,
                    set.spotlight,
                    set.trackID,
                    set.discussionLocked,
                    set.rating,
                    set.ratings.toTypedArray()
                )
            }

        log.info("自动更新扩展谱面：已更新 $result($result2) 张谱面。")

        return result + result2
    }

    private fun getBeatmapsetWithRankedTimeLibrary(): List<BeatmapsetWithRankTime> {
        val json = proxyClient.get()
            .uri("https://mapranktimes.vercel.app/api/beatmapsets")
            .retrieve()
            .body<JsonNode>()!!
        return JacksonUtil.parseObjectList(json, BeatmapsetWithRankTime::class.java)
    }


    fun getBeatmapSetWithRankedTime(beatmapsetID: Long): BeatmapsetWithRankTime {
        return proxyClient.get()
            .uri("https://mapranktimes.vercel.app/api/beatmapsets/{sid}", beatmapsetID)
            .retrieve()
            .body<BeatmapsetWithRankTime>()!!
    }

    /**
     * 错误包装
     */
    private fun <T : Any> request(isBackground: Boolean = false, request: (RestClient) -> T): T {
        return try {
            base.request(isBackground, request)
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<RestClientResponseException>()

            when {
                ex == null -> {
                    throw NetworkException.BeatmapException.Undefined(e)
                }

                ex.statusCode == org.springframework.http.HttpStatus.BAD_REQUEST -> {
                    throw NetworkException.BeatmapException.BadRequest()
                }

                ex.statusCode == org.springframework.http.HttpStatus.UNAUTHORIZED -> {
                    throw NetworkException.BeatmapException.Unauthorized()
                }

                ex.statusCode == org.springframework.http.HttpStatus.FORBIDDEN -> {
                    throw NetworkException.BeatmapException.Forbidden()
                }

                ex.statusCode == org.springframework.http.HttpStatus.NOT_FOUND -> {
                    throw NetworkException.BeatmapException.NotFound()
                }

                ex.statusCode == org.springframework.http.HttpStatus.TOO_MANY_REQUESTS -> {
                    throw NetworkException.BeatmapException.TooManyRequests()
                }

                ex.statusCode == org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR -> {
                    throw NetworkException.BeatmapException.InternalServerError()
                }

                ex.statusCode == org.springframework.http.HttpStatus.BAD_GATEWAY -> {
                    throw NetworkException.BeatmapException.BadGateWay()
                }

                ex.statusCode == org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE -> {
                    throw NetworkException.BeatmapException.ServiceUnavailable()
                }

                e.findCauseOfType<java.net.SocketException>() != null -> {
                    throw NetworkException.BeatmapException.GatewayTimeout()
                }

                else -> {
                    throw NetworkException.BeatmapException.Undefined(e)
                }
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BeatmapApiImpl::class.java)

        private val IMG_BUFFER_PATH: String = if (System.getenv("BUFFER_PATH").isNullOrBlank().not()) {
            System.getenv("BUFFER_PATH")
        } else {
            System.getProperty("java.io.tmpdir") + "/n-bot/buffer"
        }

        private fun extend(lite: Beatmap, extended: Beatmap?): Beatmap {
            if (extended == null) {
                return lite
            } else if (lite.beatmapID == 0L) {
                return extended
            }

            // 深拷贝
            return if (lite.cs != null) {
                extended.copy().apply {
                    mode = lite.mode
                    starRating = lite.starRating
                    cs = lite.cs
                    ar = lite.ar
                    od = lite.od
                    hp = lite.hp
                    totalLength = lite.totalLength
                    hitLength = lite.hitLength
                    bpm = lite.bpm
                    convert = lite.convert
                }
            } else {
                extended.copy().apply {
                    mode = lite.mode
                    convert = lite.modeInt != 0 && modeInt == 0
                    modeInt = lite.modeInt
                }
            }
        }
    }
}
