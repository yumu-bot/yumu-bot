package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.FileConfig
import com.now.nowbot.dao.BeatMapDao
import com.now.nowbot.entity.BeatmapObjectCountLite
import com.now.nowbot.mapper.BeatmapObjectCountMapper
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.*
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuBeatmapMirrorApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min

@Service
class BeatmapApiImpl(
    private val base: OsuApiBaseService,
    config: FileConfig,
    private val beatMapDao: BeatMapDao,
    private val osuBeatmapMirrorApiService: OsuBeatmapMirrorApiService,
    private val beatmapObjectCountMapper: BeatmapObjectCountMapper,

    @Qualifier("proxyClient") private val proxyClient: WebClient,
) : OsuBeatmapApiService {
    private val osuDir: Path = Path.of(config.osuFilePath)

    override fun hasBeatMapFileFromDirectory(bid: Long): Boolean {
        val path = osuDir.resolve("$bid.osu")
        return Files.isRegularFile(path)
    }

    private fun getBeatMapFileFromDirectory(bid: Long): String? {
        val path = osuDir.resolve("$bid.osu")

        if (hasBeatMapFileFromDirectory(bid)) {
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
        return osuBeatmapMirrorApiService.getOsuFile(bid) ?: getBeatMapFileFromOfficialWebsite(bid)

    }

    fun getBeatMapFileFromOfficialWebsite(bid: Long): String? {
        try {
            return base.request { client ->
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

    override fun refreshBeatMapFileFromDirectory(bid: Long): Boolean {
        val path = osuDir.resolve("$bid.osu")

        if (hasBeatMapFileFromDirectory(bid)) {
            try {
                Files.delete(path)
            } catch (e: IOException) {
                log.error("osu 谱面 API：删除本地谱面文件失败: ", e)
                return false
            }

            val str = getBeatMapFileString(bid)

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
    override fun getBeatMapFileByte(bid: Long): ByteArray? {
        return getBeatMapFileString(bid)?.toByteArray(StandardCharsets.UTF_8)
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
    override fun getBeatMapFileString(bid: Long): String? {
        var str: String? = null

        if (hasBeatMapFileFromDirectory(bid)) {
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
    override fun checkBeatMap(beatMap: BeatMap?): Boolean {
        if (beatMap == null) return false
        return checkBeatMap(beatMap.beatMapID, beatMap.md5 ?: return false)
    }

    @Throws(IOException::class)
    override fun checkBeatMap(bid: Long, checkStr: String?): Boolean {
        if (checkStr == null) return false

        val path = osuDir.resolve("$bid.osu")
        if (Files.isRegularFile(path) && checkStr.isNotBlank()) {
            return getBeatMapMD5(Files.readString(path)) == checkStr
        }
        return false
    }

    override fun checkBeatMap(beatMap: BeatMap, fileStr: String): Boolean {
        return getBeatMapMD5(fileStr) == beatMap.md5
    }

    private fun getBeatMapMD5(fileStr: String): String {
        return DigestUtils.md5DigestAsHex(fileStr.toByteArray(StandardCharsets.UTF_8))
    }

    override fun getAttributes(id: Long, mode: OsuMode?): BeatmapDifficultyAttributes {
        val body: MutableMap<String, Any> = HashMap()
        if (OsuMode.isNotDefaultOrNull(mode)) {
            body["ruleset_id"] = mode!!.modeValue
        }
        return base.request { client ->
            client.post().uri("beatmaps/{id}/attributes", id)
                .headers { base.insertHeader(it) }
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .mapNotNull {
                    JacksonUtil.parseObject(it["attributes"], BeatmapDifficultyAttributes::class.java)
                }
        }

    }

    override fun getBeatMap(bid: Long): BeatMap {
        return base.request { client ->
            client.get()
                .uri("beatmaps/{bid}", bid)
                .headers { base.insertHeader(it) }
                .retrieve()
                .bodyToMono(BeatMap::class.java)
                .doOnNext(beatMapDao::saveMap)
        }
    }

    override fun getBeatMapSet(sid: Long): BeatMapSet {
        return base.request { client ->
            client.get()
                .uri("beatmapsets/{sid}", sid)
                .headers { base.insertHeader(it) }
                .retrieve()
                .bodyToMono(BeatMapSet::class.java)
                .doOnNext(beatMapDao::saveMapSet)
        }
    }

    override fun getBeatMapFromDataBase(bid: Long): BeatMap {
        try {
            val lite = beatMapDao.getBeatMapLite(bid)
            return BeatMapDao.fromBeatmapLite(lite)
        } catch (e: Exception) {
            return getBeatMap(bid)
        }
    }

    override fun isNotOverRating(bid: Long): Boolean {
        try {
            val map = beatMapDao.getBeatMapLite(bid)
            return map.status.equals("ranked", ignoreCase = true) && map.difficultyRating <= 5.7
        } catch (ignore: Exception) {
        }

        try {
            val map = getBeatMap(bid)
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
        var file = getBeatMapFileString(bid)
        if (file == null) {
            refreshBeatMapFileFromDirectory(bid)
            file = getBeatMapFileString(bid)
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
            refreshBeatMapFileFromDirectory(bid)
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

    override fun getBeatmapObjectGrouping26(beatMap: BeatMap): IntArray {
        var result: IntArray? = null
        if (!beatMap.md5.isNullOrBlank()) {
            val r = beatmapObjectCountMapper.getDensityByBidAndCheck(
                beatMap.beatMapID, beatMap.md5!!
            )
            if (r.isNotEmpty()) result = r.first()
        } else {
            val r = beatmapObjectCountMapper.getDensityByBid(beatMap.beatMapID)
            if (r.isNotEmpty()) result = r.first()
        }

        if (result == null) {
            var dataObj = getCount(beatMap.beatMapID)
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
            .getTimeStampByBid(bidList)
            .map { it.bid to it.times }
            .toMap()

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
        val timeMap = beatMapDao.getAllBeatmapHitLength(bid)
            .map { it.id to it.length }
            .toMap()

        val result = ArrayList<Pair<Long, Int>>(bid.size)

        for (id in bid) {
            val time = timeMap[id]
            if (time != null) {
                result.add(id to time)
            } else {
                val beatmap = try {
                    getBeatMap(id)
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

        var playPercentage = beatmapObjectCountMapper.getTimeStampPercentageByBidAndIndex(score.beatMap.beatMapID, n)
        if (playPercentage == null) {
            try {
                getBeatmapObjectGrouping26(score.beatMap)
                playPercentage =
                    beatmapObjectCountMapper.getTimeStampPercentageByBidAndIndex(score.beatMap.beatMapID, n)
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

        return base.request { client ->
            client.post()
                .uri("beatmaps/{id}/attributes", id)
                .headers { base.insertHeader(it) }
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
        return base.request { client ->
            client.get().uri {
                it.path("beatmapsets/lookup")
                    .queryParamIfPresent("checksum", Optional.ofNullable(checksum))
                    .queryParamIfPresent("filename", Optional.ofNullable(filename))
                    .queryParamIfPresent("id", Optional.ofNullable(id)).build()
            }
                .headers { base.insertHeader(it) }
                .retrieve()
                .bodyToMono(JsonNode::class.java)
        }
    }

    private fun searchBeatMapSetFromAPI(query: Map<String, Any?>): BeatMapSetSearch {
        return base.request { client ->
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
                .headers { base.insertHeader(it) }
                .retrieve()
                .bodyToMono(BeatMapSetSearch::class.java)
        }
    }

    override fun searchBeatMapSet(query: Map<String, Any?>): BeatMapSetSearch {
        val search = searchBeatMapSetFromAPI(query)

        // 后处理
        search.resultCount = min(50, search.beatmapSets.size)
        if (query["s"] !== null || query["s"] !== "any") {
            search.rule = query["s"].toString()
        }
        search.sortBeatmapDiff()

        return search
    }

    /**
     * 依据QualifiedMapService 的逻辑来多次获取
     * tries 一般可以设为 10（500 个结果）
     */
    @Throws(GeneralTipsException::class)
    override fun searchBeatMapSet(query: Map<String, Any?>, tries: Int): BeatMapSetSearch {
        var search = BeatMapSetSearch()
        var page = 1
        val queryAlt = query.toMutableMap()

        run {
            var resultCount = 0
            do {
                if (search.beatmapSets.isEmpty()) {
                    search = this.searchBeatMapSetFromAPI(queryAlt)
                    resultCount += search.beatmapSets.size
                } else {
                    page++
                    queryAlt["page"] = page
                    val result = this.searchBeatMapSetFromAPI(queryAlt)
                    resultCount += result.beatmapSets.size
                    search.beatmapSets += result.beatmapSets
                }
            } while (resultCount < search.total && page < tries)
        }

        // 后处理
        search.resultCount = min(search.total, search.beatmapSets.size)
        if (query["s"] !== null || query["s"] !== "any") {
            search.rule = query["s"].toString()
        }
        search.sortBeatmapDiff()

        return search
    }

    // 给同一张图的成绩添加完整的谱面
    override fun applyBeatMapExtendForSameScore(scores: List<LazerScore>, beatMap: BeatMap) {
        if (scores.isEmpty()) return

        for (score in scores) {
            val lite = score.beatMap

            score.beatMap = extend(lite, beatMap)!!
            if (beatMap.beatMapSet != null) {
                score.beatMapSet = beatMap.beatMapSet!!
            }
        }
    }

    override fun applyBeatMapExtend(score: LazerScore) {
        val extended = getBeatMap(score.beatMapID)

        applyBeatMapExtend(score, extended)
    }

    override fun applyBeatMapExtend(scores: List<LazerScore>) {
        val actions = scores.map {
            return@map AsyncMethodExecutor.Runnable { applyBeatMapExtend(it) }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions)
    }

    override fun applyBeatMapExtend(score: LazerScore, extended: BeatMap) {
        val lite = score.beatMap

        score.beatMap = extend(lite, extended)!!
        if (extended.beatMapSet != null) {
            score.beatMapSet = extended.beatMapSet!!
        }
    }

    override fun applyBeatMapExtendFromDataBase(score: LazerScore) {
        val extended = getBeatMapFromDataBase(score.beatMapID)
        val lite = score.beatMap

        score.beatMap = extend(lite, extended)!!
        if (extended.beatMapSet != null) {
            score.beatMapSet = extended.beatMapSet!!
        }
    }

    override fun getBeatMapSetRankedTime(beatMap: BeatMap): String {
        return if (beatMap.ranked == 3) {
            try {
                val t = getBeatMapSetWithRankedTime(beatMap.beatMapSetID)

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

    override fun getBeatMapSetRankedTimeMap(): Map<Long, String> {
        return getBeatMapSetWithRankedTimeLibrary()
            .associate {
                it.beatMapID to (
                    if (it.isEarly) {
                        it.rankDateEarly.replace(".000Z", "Z")
                    } else {
                        it.rankDate.replace(".000Z", "Z")
                    }
            )
            }
    }

    override fun applyBeatMapSetRankedTime(beatMapSets: List<BeatMapSet>) {
        val l = getBeatMapSetRankedTimeMap()

        beatMapSets.forEach {
            val t = l[it.beatMapSetID]

            if (t != null) {
                it.rankedDate = OffsetDateTime.parse(t)
            }
        }
    }

    private val beatMapTagLibraryFromAPI: JsonNode
        get() = base.osuApiWebClient.get()
            .uri {
                it.path("tags").build()
            }.headers {
                base.insertHeader(it)
            }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block()!!

                /*
            .map { JacksonUtil.parseObjectList(it["tags"], Tag::class.java) }
            .flatten()

                 */

    override fun updateBeatMapTagLibraryDatabase() {
        val tags = JacksonUtil.parseObjectList(beatMapTagLibraryFromAPI["tags"], Tag::class.java)

        beatMapDao.saveTag(tags)

        log.info("谱面: 玩家标签已更新")
    }

    override fun extendBeatMapTag(beatMap: BeatMap) {
        val ids = beatMap.tagIDs

        if (ids.isNullOrEmpty()) return

        beatMap.tags = ids.mapNotNull { beatMapDao.getTag(it.id) }
    }

    private fun getBeatMapSetWithRankedTimeLibrary(): List<BeatMapSetWithRankTime> {
        return proxyClient.get()
            .uri("https://mapranktimes.vercel.app/api/beatmapsets")
            .retrieve()
            .bodyToFlux(BeatMapSetWithRankTime::class.java)
            .collectList()
            .block()!!
    }


    fun getBeatMapSetWithRankedTime(beatMapSetID: Long): BeatMapSetWithRankTime {
        return proxyClient.get()
            .uri("https://mapranktimes.vercel.app/api/beatmapsets/{sid}", beatMapSetID)
            .retrieve()
            .bodyToMono(BeatMapSetWithRankTime::class.java)

            /*
            .bodyToMono(String::class.java)
            .map {
                println(it)
                return@map JacksonUtil.parseObject(it, BeatMapSetWithRankTime::class.java)
            }

             */
            .block()!!
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BeatmapApiImpl::class.java)

        private fun extend(lite: BeatMap?, extended: BeatMap?): BeatMap? {
            if (extended == null) {
                return lite
            } else if (lite?.CS == null) {
                return extended
            }

            // 深拷贝
            val deepL = BeatMap().apply {
                beatMapSetID = extended.beatMapSetID
                beatMapID = extended.beatMapID
                modeStr = extended.modeStr
                status = extended.status
                totalLength = extended.totalLength
                mapperID = extended.mapperID
                difficultyName = extended.difficultyName
                beatMapSet = extended.beatMapSet
                md5 = extended.md5
                failTimes = extended.failTimes
                maxCombo = extended.maxCombo
                tagIDs = extended.tagIDs
                tags = extended.tags

                starRating = lite.starRating
                CS = lite.CS
                AR = lite.AR
                OD = lite.OD
                HP = lite.HP
                totalLength = lite.totalLength
                hitLength = lite.hitLength
                BPM = lite.BPM
                convert = lite.convert

                circles = extended.circles
                sliders = extended.sliders
                spinners = extended.spinners
                deletedAt = extended.deletedAt
                scoreAble = extended.scoreAble
                lastUpdated = extended.lastUpdated
                owners = extended.owners
                modeInt = extended.modeInt
                passCount = extended.passCount
                playCount = extended.playCount
                ranked = extended.ranked
                url = extended.url
            }

            return deepL
        }

        /*
        private fun getScoreJudgeCount(score: Score): Int {
            val mode = score.mode

            val s = score.statistics
            val n320 = s.countGeki
            val n300 = s.count300
            val n200 = s.countKatu
            val n100 = s.count100
            val n50 = s.count50
            val n0 = s.countMiss

            return when (mode) {
                OsuMode.OSU -> n300 + n100 + n50 + n0
                OsuMode.TAIKO -> n300 + n100 + n0
                OsuMode.CATCH -> n300 + n0
                else -> n320 + n300 + n200 + n100 + n50 + n0
            }
        }

         */
    }
}
