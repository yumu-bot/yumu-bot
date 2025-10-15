package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.FileConfig
import com.now.nowbot.dao.BeatmapDao
import com.now.nowbot.entity.BeatmapObjectCountLite
import com.now.nowbot.mapper.BeatmapObjectCountMapper
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuBeatmapMirrorApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.JacksonUtil
import okhttp3.internal.toImmutableList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.collections.set

@Service
class BeatmapApiImpl(
    private val base: OsuApiBaseService,
    config: FileConfig,
    private val beatmapDao: BeatmapDao,
    private val beatmapMirrorApiService: OsuBeatmapMirrorApiService,
    private val beatmapObjectCountMapper: BeatmapObjectCountMapper,

    @Qualifier("proxyClient") private val proxyClient: WebClient,
) : OsuBeatmapApiService {

    private val osuDir: Path = Path.of(config.osuFilePath)

    override fun hasBeatmapFileFromDirectory(bid: Long): Boolean {
        val path = osuDir.resolve("$bid.osu")
        return Files.isRegularFile(path)
    }

    private fun getBeatMapFileFromDirectory(bid: Long): String? {
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

    private fun getBeatMapFileFromLocalService(bid: Long): String? {
        return beatmapMirrorApiService.getOsuFile(bid) ?: getBeatMapFileFromOfficialWebsite(bid)

    }

    fun getBeatMapFileFromOfficialWebsite(bid: Long): String? {
        try {
            return request { client ->
                client.get()
                    .uri("https://osu.ppy.sh/osu/{bid}", bid)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .onErrorReturn("")
            }
        } catch (e: WebClientResponseException) {
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

    private fun writeBeatMapFileToDirectory(str: String?, bid: Long): Boolean {
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

    private fun downloadBeatMapFileString(bid: Long): String? {
        var str: String? = try {
            AsyncMethodExecutor.execute(
                { getBeatMapFileFromLocalService(bid) ?: "" },
                "_getBeatMapFileFromLocalService$bid",
                null
            )
        } catch (e: Exception) {
            null
        }

        if (!str.isNullOrBlank()) {
            return str
        }

        str = try {
            AsyncMethodExecutor.execute(
                { getBeatMapFileFromOfficialWebsite(bid) ?: "" },
                "_getBeatMapFileFromWebsite$bid",
                null
            )
        } catch (e: Exception) {
            null
        }

        return str
    }

    // 获取谱面：先获取本地，再获取 bs api，最后获取网页
    override fun getBeatmapFileString(bid: Long): String? {
        var str: String? = null

        if (hasBeatmapFileFromDirectory(bid)) {
            str = getBeatMapFileFromDirectory(bid)
        }

        if (!str.isNullOrBlank()) {
            return str
        } else {
            str = downloadBeatMapFileString(bid)
        }

        if (!str.isNullOrBlank()) {
            writeBeatMapFileToDirectory(str, bid)
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
            return getBeatMapMD5(Files.readString(path)) == checkStr
        }
        return false
    }

    override fun checkBeatmap(beatmap: Beatmap, fileStr: String): Boolean {
        return getBeatMapMD5(fileStr) == beatmap.md5
    }

    private fun getBeatMapMD5(fileStr: String): String {
        return DigestUtils.md5DigestAsHex(fileStr.toByteArray(StandardCharsets.UTF_8))
    }

    override fun getAttributes(id: Long, mode: OsuMode?): BeatmapDifficultyAttributes {
        val body: MutableMap<String, Any> = HashMap()
        if (OsuMode.isNotDefaultOrNull(mode)) {
            body["ruleset_id"] = mode!!.modeValue
        }
        return request { client ->
            client.post().uri("beatmaps/{id}/attributes", id)
                .headers(base::insertHeader)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .mapNotNull {
                    JacksonUtil.parseObject(it["attributes"], BeatmapDifficultyAttributes::class.java)
                }
        }

    }

    override fun getBeatmap(bid: Long): Beatmap {
        return request { client ->
            client.get()
                .uri("beatmaps/{bid}", bid)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Beatmap::class.java)
                .doOnNext(beatmapDao::saveMap)
        }
    }

    override fun getBeatmaps(ids: Iterable<Long>): List<Beatmap> {
        val idChunk = ids.chunked(50)

        val callables = idChunk.map { chunk ->
            return@map AsyncMethodExecutor.Supplier {
                getBeatmapsPrivate(chunk)
            }
        }

        return AsyncMethodExecutor.awaitSupplierExecute(callables).flatten()
    }

    /**
     * @param ids 注意，单次请求量必须小于等于 50
     */
    private fun getBeatmapsPrivate(ids: Iterable<Long>): List<Beatmap> {
        return request { client ->
            client.get()
                .uri {
                    it.path("beatmaps")
                    it.queryParam("ids[]", ids.map { id -> id.toString()})

                    it.build()
                }
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .map { JacksonUtil.parseObjectList(it["beatmaps"], Beatmap::class.java) }
        }
    }

    override fun getUserBeatmapset(id: Long, type: String, offset: Int, limit: Int): List<Beatmapset> {
        return request { client ->
            client.get()
                .uri { it
                    .path("/users/${id}/beatmapsets/${type}")
                    .queryParam("offset", offset)
                    .queryParam("limit", limit)
                    .build()
                }
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(Beatmapset::class.java)
                .collectList()
        }
    }

    override fun getUserMostPlayedBeatmaps(id: Long, offset: Int, limit: Int): Map<Int, Beatmap> {

        data class MostPlayed(
            @JsonProperty("beatmap_id")
            val beatmapID: Long,

            @JsonProperty("count")
            val count: Int,

            @JsonProperty("beatmap")
            val beatmap: Beatmap,

            @JsonProperty("beatmapset")
            val beatmapset: Beatmapset,
        )

        val node = request { client ->
            client.get()
                .uri { it
                    .path("/users/${id}/beatmapsets/most_played")
                    .queryParam("offset", offset)
                    .queryParam("limit", limit)
                    .build()
                }
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
        }

        val most = JacksonUtil.parseObjectList(node, MostPlayed::class.java)

        return most.associate {
            it.beatmap.beatmapset = it.beatmapset

            return@associate it.count to it.beatmap
        }
    }

    override fun getBeatmapset(sids: Iterable<Long>): List<Beatmapset> {
        val callables = sids.map { sid ->
            return@map AsyncMethodExecutor.Supplier {
                getBeatmapset(sid)
            }
        }

        return AsyncMethodExecutor.awaitSupplierExecute(callables)
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
        return request { client ->
            client.get()
                .uri("beatmapsets/{sid}", sid)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Beatmapset::class.java)
                .doOnNext(beatmapDao::saveMapSet)
        }
    }

    override fun getBeatmapFromDatabase(bid: Long): Beatmap {
        try {
            val lite = beatmapDao.getBeatMapLite(bid)
            return BeatmapDao.fromBeatmapLite(lite)
        } catch (e: Exception) {
            return getBeatmap(bid)
        }
    }

    override fun isNotOverRating(bid: Long): Boolean {
        try {
            val map = beatmapDao.getBeatMapLite(bid)
            return map.status.equals("ranked", ignoreCase = true) && map.difficultyRating <= 5.7
        } catch (ignore: Exception) {
        }

        try {
            val map = getBeatmap(bid)
            return map.status.equals("ranked", ignoreCase = true) && map.starRating <= 5.7
        } catch (e: WebClientResponseException.NotFound) {
            return false
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

    private fun getGrouping(x: List<Int>, groups: Int): List<Int> {
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
        val md5 = getBeatMapMD5(file)

        result.check = md5

        var objectList: List<Int>

        try {
            objectList = getMapObjectList(file)
        } catch (e: IndexOutOfBoundsException) {
            refreshBeatmapFileFromDirectory(bid)
            try {
                objectList = getMapObjectList(file)
            } catch (e1: IndexOutOfBoundsException) {
                result.timestamp = intArrayOf()
                result.density = intArrayOf()
                return result
            }
        }

        result.timestamp = objectList.toIntArray()
        val grouping = getGrouping(objectList, 26)
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
        } catch (e: Exception) {
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
            } catch (e: Exception) {
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
        val timeMap = beatmapDao.getBeatMapsHitLength(bid).associate { it.id to it.length }

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

    override fun getAttributes(id: Long, mode: OsuMode?, modsInt: Int): BeatmapDifficultyAttributes {
        val body: MutableMap<String, Any> = HashMap()
        if (OsuMode.isNotDefaultOrNull(mode)) {
            body["ruleset_id"] = mode!!.modeValue
        }

        if (modsInt != 0) {
            body["mods"] = modsInt
        }

        return request { client ->
            client.post()
                .uri("beatmaps/{id}/attributes", id)
                .headers(base::insertHeader)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(AttributesResponse::class.java)
                .onErrorReturn(AttributesResponse())
                .map { it.attributes }

                /*
                .bodyToMono(JsonNode::class.java)
                .mapNotNull {
                    JacksonUtil.parseObject(it["attributes"], BeatmapDifficultyAttributes::class.java)
                }

                 */
        }
    }

    override fun lookupBeatmap(checksum: String?, filename: String?, id: Long?): JsonNode? {
        return request { client ->
            client.get().uri {
                it.path("beatmapsets/lookup")
                    .queryParamIfPresent("checksum", Optional.ofNullable(checksum))
                    .queryParamIfPresent("filename", Optional.ofNullable(filename))
                    .queryParamIfPresent("id", Optional.ofNullable(id)).build()
            }
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
        }
    }

    private fun searchBeatMapSetFromAPI(query: Map<String, Any?>): BeatmapsetSearch {
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
            }
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(BeatmapsetSearch::class.java)
        }
    }

    override fun searchBeatmapset(query: Map<String, Any?>): BeatmapsetSearch {
        val search = searchBeatMapSetFromAPI(query)

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
    override fun parallelSearchBeatmapset(
        query: Map<String, Any?>,
        tries: Int,
        quantity: Int,
        awaitMillis: Long
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
                        searchBeatMapSetFromAPI(q)
                    }
                }

                val searches = AsyncMethodExecutor.awaitCallablesExecute(actions)

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

    /**
     * 依据QualifiedMapService 的逻辑来多次获取
     * tries 一般可以设为 10（500 个结果）
     */
    override fun searchBeatmapset(query: Map<String, Any?>, tries: Int): BeatmapsetSearch {
        val search = BeatmapsetSearch()
        var page = 1
        val queryAlt = query.toMutableMap()

        run {
            do {
                if (search.beatmapsets.isEmpty()) {
                    search.combine(searchBeatMapSetFromAPI(queryAlt))
                } else {
                    page++
                    queryAlt["page"] = page

                    search.combine(searchBeatMapSetFromAPI(queryAlt))
                }
            } while (search.cursorString != null && page < tries)
        }

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

        for (score in scores) {
            applyBeatmapExtend(score, beatmap.copy())
        }
    }

    override fun applyBeatmapExtend(score: LazerScore) {
        applyBeatmapExtend(listOf(score))
    }

    override fun applyBeatmapExtend(scores: List<LazerScore>) {
        val ids = scores.map { it.beatmapID }.toSet()

        val bs = getBeatmaps(ids).associateBy { it.beatmapID }

        scores.forEach { score ->
            bs[score.beatmapID]?.let { applyBeatmapExtend(score, it) }
        }
    }

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

    override fun applyBeatmapExtendFromDatabase(scores: List<LazerScore>) {
        val exists = scores.mapNotNull {
            try {
                BeatmapDao.fromBeatmapLite(beatmapDao.getBeatMapLite(it.beatmapID))
            } catch (e: Exception) {
                null
            }
        }

        val notExistIDs = scores.map { it.beatmapID }.toSet() - exists.map { it.beatmapID }.toSet()

        val notExists = getBeatmaps(notExistIDs)

        val extended = (exists + notExists).associateBy { it.beatmapID }

        scores.forEach { score ->
            extended[score.beatmapID]?.let {
                applyBeatmapExtend(score, it)
            }
        }
    }

    override fun applyBeatmapExtendFromDatabase(score: LazerScore) {
        applyBeatmapExtendFromDatabase(listOf(score))
    }

    override fun getBeatmapsetRankedTime(beatmap: Beatmap): String {
        return if (beatmap.ranked == 3) {
            try {
                val t = getBeatMapSetWithRankedTime(beatmap.beatmapsetID)

                if (t.isEarly) {
                    t.rankDateEarly.replace(".000Z", "Z")
                } else {
                    t.rankDate.replace(".000Z", "Z")
                }
            } catch (e: WebClientResponseException) {
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
        get() = request { client -> client.get()
            .uri {
                it.path("tags").build()
            }.headers(base::insertHeader)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
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

    private fun getBeatmapsetWithRankedTimeLibrary(): List<BeatmapsetWithRankTime> {
        return proxyClient.get()
            .uri("https://mapranktimes.vercel.app/api/beatmapsets")
            .retrieve()
            .bodyToFlux(BeatmapsetWithRankTime::class.java)
            .collectList()
            .block()!!
    }


    fun getBeatMapSetWithRankedTime(beatmapsetID: Long): BeatmapsetWithRankTime {
        return proxyClient.get()
            .uri("https://mapranktimes.vercel.app/api/beatmapsets/{sid}", beatmapsetID)
            .retrieve()
            .bodyToMono(BeatmapsetWithRankTime::class.java)

            /*
            .bodyToMono(String::class.java)
            .map {
                println(it)
                return@map JacksonUtil.parseObject(it, BeatMapSetWithRankTime::class.java)
            }

             */
            .block()!!
    }

    /**
     * 错误包装
     */
    private fun <T> request(request: (WebClient) -> Mono<T>): T {
        return try {
            base.request(request)
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
        private val log: Logger = LoggerFactory.getLogger(BeatmapApiImpl::class.java)

        private fun extend(lite: Beatmap, extended: Beatmap?): Beatmap {
            if (extended == null) {
                return lite
            } else if (lite.beatmapID == 0L) {
                return extended
            }

            // 深拷贝
            return if (lite.CS != null) {
                extended.copy().apply {
                    mode = lite.mode
                    starRating = lite.starRating
                    CS = lite.CS
                    AR = lite.AR
                    OD = lite.OD
                    HP = lite.HP
                    totalLength = lite.totalLength
                    hitLength = lite.hitLength
                    BPM = lite.BPM
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
