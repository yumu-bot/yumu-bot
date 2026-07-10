package com.now.nowbot.service.osuApiService.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.config.FileConfig
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.config.OsuLocalCalculateConfig
import com.now.nowbot.dao.BeatmapDao
import com.now.nowbot.entity.BeatmapCountLite
import com.now.nowbot.mapper.BeatmapCountMapper
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.calculate.CosuRequest
import com.now.nowbot.model.calculate.CosuResponse
import com.now.nowbot.model.calculate.CosuScore
import com.now.nowbot.model.enums.IDType
import com.now.nowbot.model.enums.IDType.BeatmapID
import com.now.nowbot.model.enums.IDType.BeatmapsetID
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.osu.Covers.Companion.CoverType.Companion.getString
import com.now.nowbot.service.NewbieRestrictService.Companion.STAR_BOUNDARY
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuBeatmapMirrorApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.IntArrayCompressor
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.UUIDConverter
import com.now.nowbot.util.toBody
import com.now.nowbot.util.toBodyList
import io.ktor.util.collections.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.DigestUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@Service
class BeatmapApiImpl(
    private val base: OsuApiBaseService,
    private val beatmapDao: BeatmapDao,
    private val beatmapMirrorApiService: OsuBeatmapMirrorApiService,
    private val beatmapCountMapper: BeatmapCountMapper,

    @param:Qualifier("proxyRestClient") private val proxyClient: RestClient,

    fileConfig: FileConfig,
    private val calculateConfig: OsuLocalCalculateConfig
) : OsuBeatmapApiService {

    private val osuDir: Path = Path.of(fileConfig.osuFilePath)

    override fun getVoice(beatmapsetID: Number): ByteArray? {
        val url = "https://b.ppy.sh/preview/${beatmapsetID}.mp3"

        return runCatching {
            base.osuApiRestClient.get().uri(url).toBody<ByteArray>()
        }.getOrNull()
    }

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
            } catch (_: IOException) {
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
                    .toBody<ByteArray>()

                if (Files.isDirectory(path) && Files.isWritable(path)) {
                    Files.write(path.resolve(hex), image)
                }

                return image
            } catch (_: Exception) {
                default
            }
        }
    }


    override fun deleteBeatmapFileFromDirectory(bid: Long): Boolean  {
        val path = osuDir.resolve("$bid.osu")

        return runCatching {
            Files.deleteIfExists(path)
        }.onFailure {
            log.warn("谱面实现：无法删除谱面 $bid，原因：${it.message}")
        }.isSuccess
    }

    override fun hasBeatmapFileFromDirectory(bid: Long): Boolean {
        val path = osuDir.resolve("$bid.osu")

        if (!Files.isRegularFile(path)) return false

        return try {
            if (Files.size(path) < 100) {
                log.info("谱面实现：检查到僵尸文件 $bid.osu，删除中...")
                deleteBeatmapFileFromDirectory(bid)
                false
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun getBeatmapFileFromDirectory(bid: Long): String? {
        if (hasBeatmapFileFromDirectory(bid)) {
            try {
                val path = osuDir.resolve("$bid.osu")
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
                    .toBody<String>()
            }
        } catch (e: HttpClientErrorException) {
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

    @Cacheable(value = ["beatmap file_path"], key = "#bid")
    override fun getBeatmapFilePath(bid: Long): String {
        var str: String? = null

        if (hasBeatmapFileFromDirectory(bid)) {
            str = osuDir.resolve("$bid.osu").absolutePathString()
        }

        if (!str.isNullOrBlank()) {
            writeBeatmapFileToDirectory(getBeatmapFileStringFromOutside(bid), bid)
        }

        return str ?: osuDir.resolve("$bid.osu").absolutePathString()
    }


    override fun downloadBeatmapFile(bids: Collection<Long>): List<Long> {
        // 1. 区分本地和在线
        val exists = bids.filter { hasBeatmapFileFromDirectory(it) }.toSet()
        val notExists = bids.minus(exists)

        val downloaded = ConcurrentSet<Long>()

        // 2. 分批下载缺失的谱面
        notExists.chunked(15).forEach { ids ->
            val actions = ids.map { id ->
                Callable {
                    // 返回 Pair，方便后面 toMap
                    id to getBeatmapFileStringFromOutside(id)
                }
            }

            // 每一组给 30 秒，防止网络波动
            val result = AsyncMethodExecutor.awaitList(actions, 30.seconds)

            result.forEach { (id, fileString) ->
                if (!fileString.isNullOrEmpty()) {
                    val path = osuDir.resolve("$id.osu")

                    runCatching {
                        Files.writeString(path, fileString)
                        downloaded.add(id)
                    }.onFailure {
                        log.warn("谱面实现：写谱面 $id 时失败。")
                    }
                } else {
                    log.warn("谱面实现：谱面 $id 下载返回内容为空")
                }
            }
        }

        val all = exists + downloaded

        return bids.filter {
            it in all
        }
    }

    private fun getBeatmapFileStringFromOutside(bid: Long): String? {
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
            str = getBeatmapFileStringFromOutside(bid)
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
                .toBody<Beatmap>()
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

        val beatmaps = AsyncMethodExecutor.awaitList(callables).flatten()

        beatmapDao.saveBeatmapsAndSaveExtendAsync(beatmaps)

        return beatmaps
    }

    /**
     * @param ids 注意，单次请求量必须小于等于 50
     */
    private fun getBeatmapsPrivate(ids: Iterable<Long>): List<Beatmap> {
        val jsonString = request { client ->
            val idsStr = ids.map { id -> id.toString() }
            client.get()
                .uri {
                    it.path("beatmaps")
                    it.queryParam("ids[]", *idsStr.toTypedArray())
                    it.build()
                }
                .headers(base::insertHeader)
                .toBody<String>()
        }
        val json = JacksonUtil.toNode(jsonString)
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

            val async = AsyncMethodExecutor.awaitList(works)

            return async.flatten()
        }
    }


    private fun getUserBeatmapsetPrivate(id: Long, type: String, offset: Int, limit: Int): List<Beatmapset> {
        val sets = request { client ->
            client.get()
                .uri {
                    it
                        .path("/users/${id}/beatmapsets/${type}")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build()
                }
                .headers(base::insertHeader)
                .toBodyList<Beatmapset>()
        }

        beatmapDao.saveBeatmapsetsAsync(sets)

        return sets
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

            val async = AsyncMethodExecutor.awaitList(works)

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

        val most = request { client ->
            client.get()
                .uri {
                    it
                        .path("/users/${id}/beatmapsets/most_played")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build()
                }
                .headers(base::insertHeader)
                .toBodyList<MostPlayed>()
        }

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

        return AsyncMethodExecutor.awaitList(callables)
    }

    override fun extendBeatmapInSetFromAPI(sets: Iterable<Beatmapset>): List<Beatmapset> {
        val map = sets.associate { set ->
            set.beatmapsetID to (set.beatmaps.orEmpty()).map { b -> b.beatmapID }
        }

        val bids = map.flatMap { it.value }

        val beatmaps = getBeatmaps(bids)

        val group = beatmaps.groupBy { it.beatmapsetID }

        sets.forEach { set ->
            group[set.beatmapsetID]?.let { set.beatmaps = it }
        }

        return sets.toList()
    }

    override fun extendBeatmapInScoreFromAPI(scores: Iterable<LazerScore>): List<LazerScore> {

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
                .toBody<Beatmapset>()
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

    override fun getBeatmapsetFromDatabase(sid: Long): Beatmapset {
        try {
            val lite = beatmapDao.getBeatmapsetLite(sid)
            return BeatmapDao.fromBeatmapsetLite(lite!!)
        } catch (_: Exception) {
            return getBeatmapset(sid)
        }
    }

    override fun applyVersion(scores: Collection<LazerScore>) {
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
            return map.status.equals("ranked", ignoreCase = true) && map.difficultyRating <= STAR_BOUNDARY
        } catch (_: Exception) {
        }

        try {
            val map = getBeatmap(bid)
            return map.status.equals("ranked", ignoreCase = true) && map.starRating <= STAR_BOUNDARY
        } catch (_: NetworkException.BeatmapException.NotFound) {
            return false
        }
    }

    private fun getTimestamp(str: String): IntArray {
        val hitObjectsIdx = str.indexOf("[HitObjects]")
        if (hitObjectsIdx == -1) {
            return intArrayOf()
        }

        // 截取 [HitObjects] 之后的内容，减少后续处理的文本量
        val hitSection = str.substring(hitObjectsIdx + 12)

        // 使用动态扩容的 IntArray 避免 List<Int> 的自动装箱操作
        var capacity = 512
        var result = IntArray(capacity)
        var size = 0

        // 使用 BufferedReader 逐行读取，内存极度友好
        BufferedReader(StringReader(hitSection)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) continue

                // 如果遇到了下一个配置组（例如 [TimingPoints]），直接提前结束读取
                if (trimmed.startsWith("[")) {
                    break
                }

                // 高效的手动字符串切分，替代复杂的正则表达式
                // osu 格式: x,y,time,type,hitSound,objectParams...
                val firstComma = trimmed.indexOf(',')
                if (firstComma == -1) continue

                val secondComma = trimmed.indexOf(',', firstComma + 1)
                if (secondComma == -1) continue

                val thirdComma = trimmed.indexOf(',', secondComma + 1)
                // 考虑有些滑条或转盘参数较少，如果没有第三个逗号，说明时间戳是最后的整段
                val timeStr = if (thirdComma == -1) {
                    trimmed.substring(secondComma + 1)
                } else {
                    trimmed.substring(secondComma + 1, thirdComma)
                }

                // 安全转换为 Int
                val time = timeStr.toIntOrNull() ?: 0

                // 动态扩容 IntArray
                if (size == capacity) {
                    capacity *= 2
                    result = result.copyOf(capacity)
                }
                result[size++] = time
            }
        }

        // 裁剪到实际大小并返回
        return if (size == result.size) {
            result
        } else {
            result.copyOf(size)
        }
    }

    private fun getGrouping(x: IntArray, groups: Int = 26): IntArray {
        require(groups >= 1)

        if (x.isEmpty()) return IntArray(groups)

        val firstTime = x.first()
        val lastTime = x.last()
        val totalDuration = lastTime - firstTime

        if (totalDuration == 0) {
            val out = IntArray(groups)
            out[0] = x.size
            return out
        }

        val out = IntArray(groups)

        for (time in x) {
            var bucketIndex = ((time - firstTime).toLong() * groups / totalDuration).toInt()

            if (bucketIndex >= groups) {
                bucketIndex = groups - 1
            }

            out[bucketIndex]++
        }

        return out
    }

    private inline fun <T> tryWithRetry(
        fn: () -> T?,
        refresh: () -> Unit
    ): T? {
        // 1. 尝试第一次获取
        return fn() ?: run {
            refresh()
            fn()
        }
    }

    /**
     * 通用的异常重试高阶函数
     * @param E 期望捕获的异常类型
     * @param T 返回的数据类型
     * @param block 核心执行逻辑
     * @param onException 发生异常时的刷新/补偿逻辑
     * @param onFailure 彻底失败（第二次也崩溃）时的兜底返回值
     */
    private inline fun <reified E : Throwable, T> tryWithExceptionRetry(
        block: () -> T,
        onException: () -> Unit,
        onFailure: () -> T
    ): T {
        return try {
            block()
        } catch (e: Throwable) {
            if (e is E) {
                onException() // 触发刷新
                try {
                    block() // 再次尝试
                } catch (second: Throwable) {
                    if (second is E) onFailure() else throw second
                }
            } else {
                throw e
            }
        }
    }
    
    private fun getTimeStampByBeatmapIDAndIndex(beatmapID: Long, index: Int): Int? {
        val entity = beatmapCountMapper.findById(beatmapID).orElse(null) ?: return null
        val times = entity.readTimestamps() ?: return null

        val ktIndex = index - 1
        if (ktIndex !in times.indices) return null

        return times[ktIndex] - times[0]
    }

    private fun getTimeStampPercentageByBeatmapIDAndIndex(beatmapID: Long, index: Int): Double? {
        val entity = beatmapCountMapper.findById(beatmapID).orElse(null) ?: return null

        return getPercentageByEntity(entity.readTimestamps(), index)
    }

    private fun getPercentageByEntity(times: IntArray?, index: Int): Double? {
        if (times == null) return null

        val ktIndex = index - 1
        if (ktIndex !in times.indices || times.size < 2) return null

        val totalDuration = times.last() - times[0]
        if (totalDuration == 0) return 0.0

        val currentOffset = times[ktIndex] - times[0]
        return currentOffset.toDouble() / totalDuration
    }

    private fun getBeatmapCountLite(beatmapID: Long): BeatmapCountLite? {
        val file: String = tryWithRetry(
            fn = { getBeatmapFileString(beatmapID) },
            refresh = { refreshBeatmapFileFromDirectory(beatmapID) }
        ) ?: return null

        val md5 = getBeatmapMD5(file)

        val times: IntArray = tryWithExceptionRetry<IndexOutOfBoundsException, IntArray>(
            block = { getTimestamp(file) },
            onException = { refreshBeatmapFileFromDirectory(beatmapID) },
            onFailure = { return BeatmapCountLite(beatmapID) }
        )

        val grouping = getGrouping(times)

        val result = BeatmapCountLite(beatmapID).apply {
            this.writeTimestamps(times)
            this.writeHash(md5)

            this.density = grouping
        }

        return result
    }

    override fun getBeatmapObjectGrouping26(beatmap: Beatmap): IntArray {

        val md5 = beatmap.md5

        var result: IntArray? = null

        if (!md5.isNullOrBlank()) {
            val entity = beatmapCountMapper.getDensityResultByBeatmapID(beatmap.beatmapID)

            result = entity?.takeIf { it.hash == UUIDConverter.md5ToUUID(md5) }?.density
        }

        if (result == null) {
            result = beatmapCountMapper.getDensityByBeatmapID(beatmap.beatmapID)
        }


        if (result == null) {
            val entity = beatmapCountMapper.saveAndFlush(getBeatmapCountLite(beatmap.beatmapID) ?: return intArrayOf(0))

            result = entity.density
        }

        return result ?: intArrayOf(0)
    }

    override fun getFailTime(bid: Long, index: Int): Int {
        if (index <= 0) return 0
        val time = getTimeStampByBeatmapIDAndIndex(bid, index)
            ?: runCatching {
                val count = getBeatmapCountLite(bid) ?: return 0

                if ((count.readTimestamps()?.size ?: 0) < index) return 0

                val timestamps = beatmapCountMapper.saveAndFlush(count).readTimestamps() ?: return 0
                return timestamps[index] - timestamps[0]
            }.getOrElse { return 0 }

        return (time / 1000)
    }

    override fun getAllFailTime(all: List<Pair<Long, Int>>): List<Int> {
        if (all.isEmpty()) return emptyList()

        val bids = all.map { it.first }.toSet()

        // 1. 初始化非空 Value 的可变 Map
        val timeMap: MutableMap<Long, IntArray> = beatmapCountMapper
            .getTimeStampByBeatmapIDs(bids)
            .associateTo(HashMap()) { it.beatmapID to IntArrayCompressor.byteArrayToIntArray(it.delta) }

        return all.map { (bid, index) ->
            val time: IntArray = timeMap.computeIfAbsent(bid) { _ ->
                try {
                    // 如果找不到谱面，返回空数组，避免下次循环重复去数据库捞文件
                    val count = getBeatmapCountLite(bid) ?: return@computeIfAbsent intArrayOf()
                    val timestamps = count.readTimestamps() ?: return@computeIfAbsent intArrayOf()

                    if (timestamps.size <= index) return@computeIfAbsent intArrayOf()

                    beatmapCountMapper.saveAndFlush(count)
                    timestamps
                } catch (_: Exception) {
                    intArrayOf()
                }
            }

            if (index !in time.indices) {
                0
            } else {
                (time[index] - time[0]) / 1000
            }
        }
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
        val index = score.scoreHit
        val id = score.beatmap.beatmapID.takeIf { it > 0 } ?: return 1.0

        var playPercentage = getTimeStampPercentageByBeatmapIDAndIndex(id, index)

        if (playPercentage == null) {
            val lite = getBeatmapCountLite(id) ?: return 1.0
            playPercentage = getPercentageByEntity(lite.readTimestamps(), index)
        }

        // 仍然失败, 取消计算
        return playPercentage ?: 1.0
    }

    data class AttributesResponse(val attributes: BeatmapDifficultyAttributes = BeatmapDifficultyAttributes())

    override fun getAttributes(id: Long, mode: OsuMode?, mods: List<LazerMod>?): BeatmapDifficultyAttributes {
        val body: MutableMap<String, Any> = HashMap()

        if (OsuMode.isNotDefaultOrNull(mode)) {
            body["ruleset_id"] = mode.toSafeModeValue()
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
                    .toBody<AttributesResponse>().attributes
            }
        } catch (_: NetworkException.BeatmapException.UnprocessableEntity) {
            log.error("谱面请求：遇到了无法处理的属性：${id}: ${JacksonUtil.objectToJsonPretty(body)}}")

            body.remove("ruleset_id")

            return request { client ->
                client.post()
                    .uri("beatmaps/{id}/attributes", id)
                    .headers(base::insertHeader)
                    .body(body)
                    .toBody<AttributesResponse>().attributes
            }
        }
    }

    @Throws(NoSuchElementException.Beatmap::class)
    override fun getAttributesFromLocal(beatmapID: Long, mode: OsuMode, score: CosuScore?, isRetry: Boolean): CosuResponse {
        val path = osuDir.resolve("${beatmapID}.osu")

        // 1. 检查本地文件是否存在
        if (!path.exists()) {
            // 2. 触发同步下载（确保下载完成后再往下走）
            val downloadSuccess = downloadBeatmapFile(listOf(beatmapID))

            // 3. 如果下载失败，直接返回错误状态或抛出异常
            if (downloadSuccess.isEmpty()) {
                throw NoSuchElementException.Beatmap(beatmapID)
            }
        }

        return try {
            getAttributesFromLocal(CosuRequest(path.absolutePathString(), mode.shortName, score))

        } catch (e: Exception) {
            val is404 = e.message?.contains("404") == true || e is HttpClientErrorException.NotFound

            if (is404 && !isRetry) {
                path.deleteIfExists()

                return getAttributesFromLocal(beatmapID, mode, score, true)
            } else {
                throw NoSuchElementException.Beatmap(beatmapID)
            }
        }
    }

    private fun getAttributesFromLocal(request: CosuRequest): CosuResponse {
        return base.noRetryRestClient.post().uri {
                it.scheme("http")
                    .host(calculateConfig.host)
                    .port(calculateConfig.port)
                    .replacePath("calculate")
                    .build()
            }
            .body(JacksonUtil.toJson(request))
            .toBody<CosuResponse>()
    }

    override fun lookupBeatmap(checksum: String?, filename: String?, id: Long?): JsonNode? {
        return try {
            val jsonString = request { client ->
                client.get().uri {
                    it.path("beatmapsets/lookup")
                        .queryParamIfPresent("checksum", Optional.ofNullable(checksum))
                        .queryParamIfPresent("filename", Optional.ofNullable(filename))
                        .queryParamIfPresent("id", Optional.ofNullable(id)).build()
                }
                    .headers(base::insertHeader)
                    .toBody<String>()
            }
            JacksonUtil.toNode(jsonString)
        } catch (e: Exception) {
            val ex = e.findCauseOfType<HttpClientErrorException>()
            if (ex?.statusCode?.value() == 404) {
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
                .toBody<BeatmapsetSearch>()
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
        }

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

                val searches = AsyncMethodExecutor.awaitList(actions)

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

    private data class BeatmapPassedResponse(
        @field:JsonProperty("beatmaps_passed")
        val beatmapsPassed: List<Beatmap>
    )

    override fun getBeatmapPassed(userID: Long, beatmapsetIDs: List<Long>, mode: OsuMode?, excludeConverts: Boolean?, isLegacy: Boolean?, noDiffReductionMods: Boolean?): List<Beatmap> {

        val resp = request { client ->
            client.get().uri { uri -> uri.path("users/${userID}/beatmaps-passed")
                .queryParam("beatmapset_ids[]", *beatmapsetIDs.toTypedArray())

                excludeConverts?.let { ex ->
                    uri.queryParam("exclude_converts", ex)
                }

                isLegacy?.let { l ->
                    uri.queryParam("is_legacy", l)
                }

                noDiffReductionMods?.let { no ->
                    uri.queryParam("no_diff_reduction", no)
                }

                if (OsuMode.isNotDefaultOrNull(mode)) {
                    uri.queryParam("ruleset_id", mode.shortName)
                }

                uri.build()
            }
                .headers(base::insertHeader)
                .toBody<BeatmapPassedResponse>()
        }

        return resp.beatmapsPassed
    }

    // 给同一张图的成绩添加完整的谱面
    override fun applyBeatmapExtendForSameScore(scores: Collection<LazerScore>, beatmap: Beatmap) {
        if (scores.isEmpty()) return

        beatmapDao.saveExtendedBeatmapAsync(beatmap)

        for (score in scores) {
            applyBeatmapExtend(score, beatmap.copy())
        }
    }

    override fun applyBeatmapExtendFromAPI(score: LazerScore) {
        applyBeatmapExtendFromAPI(listOf(score))
    }

    override fun applyBeatmapExtendFromAPI(scores: Collection<LazerScore>) {
        val ids = scores.map { it.beatmapID }.toSet()

        val extends = getBeatmaps(ids)

        beatmapDao.saveExtendedBeatmapsAsync(extends)

        val map = extends.associateBy { it.beatmapID }

        scores.forEach { score ->
            map[score.beatmapID]?.let { applyBeatmapExtend(score, it) }
        }
    }

    override fun applyExtendFromAPI(beatmaps: Collection<Beatmap>) {
        val ids = beatmaps.map { it.beatmapID }.toSet()

        val extends = getBeatmaps(ids)

        beatmapDao.saveExtendedBeatmapsAsync(extends)

        val map = extends.associateBy { it.beatmapID }

        beatmaps.forEach { b ->
            map[b.beatmapID]?.let { extend(b, it) }
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

    override fun applyBeatmapExtend(scores: Collection<LazerScore>) {
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

    override fun applyExtend(beatmaps: Collection<Beatmap>) {
        val existSet = beatmaps.mapNotNull { b ->
            beatmapDao.extendBeatmap(b)
        }.toSet()

        val notExistBeatmaps = beatmaps.filterNot { it.beatmapID in existSet }

        if (notExistBeatmaps.isNotEmpty()) {
            applyExtendFromAPI(notExistBeatmaps)
        }
    }

    override fun applyBeatmapsetExtend(sets: Iterable<Beatmapset>) {
        val existSet = sets.mapNotNull { s ->
            beatmapDao.extendBeatmapset(s)
        }.toSet()

        val notExistBeatmapsets = sets.filterNot { it.beatmapsetID in existSet }

        if (notExistBeatmapsets.isNotEmpty()) {
            extendBeatmapInSetFromAPI(notExistBeatmapsets)
        }
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
            } catch (_: Exception) {
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

    override fun applyBeatmapsetRankedTime(beatmapsets: Collection<Beatmapset>) {
        val l = getBeatmapsetRankedTimeMap()

        beatmapsets.forEach {
            val t = l[it.beatmapsetID]

            if (t != null) {
                it.rankedDate = OffsetDateTime.parse(t)
            }
        }
    }

    private val beatmapTagLibraryFromAPI: JsonNode
        get() {
            val jsonString = request { client ->
                client.get()
                    .uri {
                        it.path("tags").build()
                    }.headers(base::insertHeader)
                    .toBody<String>()
            }
            return JacksonUtil.toNode(jsonString)
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
        // log.info("自动更新扩展谱面：正在启动")

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
            beatmapDao.updateFailTimeByBeatmapID(it)
        }

        val result2 = news
            .distinctBy { it.beatmapsetID }
            .sumOf {
                beatmapDao.updateFailTimeByBeatmapsetID(it.beatmapset!!)
            }

        log.info("自动更新扩展谱面：已更新 $result($result2) 张谱面。")

        return result + result2
    }

    private fun getBeatmapsetWithRankedTimeLibrary(): List<BeatmapsetWithRankTime> {
        val jsonString = proxyClient.get()
            .uri("https://mapranktimes.vercel.app/api/beatmapsets")
            .toBody<String>()
        val json = JacksonUtil.toNode(jsonString)
        return JacksonUtil.parseObjectList(json, BeatmapsetWithRankTime::class.java)
    }


    fun getBeatmapSetWithRankedTime(beatmapsetID: Long): BeatmapsetWithRankTime {
        return proxyClient.get()
            .uri("https://mapranktimes.vercel.app/api/beatmapsets/{sid}", beatmapsetID)
            .toBody<BeatmapsetWithRankTime>()
    }

    override fun getBeatmapsetIDFromBeatmapIDByExtend(beatmapID: Long): Long? {
        return beatmapDao.getBeatmapsetIDFromExtend(beatmapID)
    }

    override fun getBeatmapFromAnyID(inputType: IDType, inputID: Long?, groupIDFn: () -> Long?): Beatmap {

        val beatmap: Beatmap = if (inputID != null) {
            when (inputType) {
                BeatmapID -> runCatching {
                    getBeatmap(inputID)
                }.recoverCatching {
                    getBeatmapset(inputID).getTopDiff()!!
                }.getOrThrow()

                BeatmapsetID -> runCatching {
                    getBeatmapset(inputID).getTopDiff()!!
                }.recoverCatching {
                    getBeatmap(inputID)
                }.getOrThrow()
            }
        } else {
            val beforeBeatmapID = groupIDFn()
                ?: throw IllegalArgumentException.WrongException.BeatmapID()

            getBeatmap(beforeBeatmapID)
        }

        return beatmap
    }

    override fun getBeatmapsetAndTopBeatmapFromAnyID(inputType: IDType, inputID: Long?, groupIDFn: () -> Long?): Pair<Beatmapset, Beatmap> {
        val maybeID: Long
        val maybeType: IDType

        maybeID = if (inputID == null) {
            val groupID = groupIDFn()
                ?: throw IllegalArgumentException.WrongException.BeatmapID()
            maybeType = BeatmapID

            groupID
        } else {
            maybeType = inputType

            inputID
        }

        val beatmapset = when (maybeType) {
            BeatmapID -> fetchByBeatmapID(maybeID) ?: fetchByBeatmapsetID(maybeID)
            BeatmapsetID -> fetchByBeatmapsetID(maybeID) ?: fetchByBeatmapID(maybeID)
        }

        if (beatmapset == null) {
            throw IllegalArgumentException.WrongException.BeatmapID()
        }

        return when (maybeType) {
            BeatmapID -> beatmapset to beatmapset.beatmaps?.find { it.beatmapID == maybeID }!!
            BeatmapsetID -> beatmapset to (beatmapset.getTopDiff()!!)
        }
    }

    private fun fetchByBeatmapsetID(beatmapsetID: Long): Beatmapset? {
        // 1. 优先查本地是否存在
        // if (beatmapDao.existsBeatmapsetFromExtend(beatmapsetID)) {
        // 从你的本地 Repository 中直接获取实体对象并返回
        // return extendBeatmapSetRepository.findByBeatmapsetID(beatmapsetID)
        //}

        // 2. 本地没有，走网络请求
        return runCatching { getBeatmapset(beatmapsetID) }.getOrNull()
    }

    private fun fetchByBeatmapID(beatmapID: Long): Beatmapset? {
        val localSetID = getBeatmapsetIDFromBeatmapIDByExtend(beatmapID)

        if (localSetID != null) {
            return fetchByBeatmapsetID(localSetID)
        }

        val netBeatmap = runCatching { getBeatmap(beatmapID) }.getOrNull() ?: return null

        return fetchByBeatmapsetID(netBeatmap.beatmapsetID)
    }

    /**
     * 错误包装
     */
    private fun <T : Any> request(isBackground: Boolean = false, request: (RestClient) -> T): T {
        return try {
            if (isBackground) {
                base.request(isBackground = true, request)
            } else {
                request(base.osuApiRestClient)
            }
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<HttpClientErrorException>()

            when (ex?.statusCode?.value()) {
                400 -> throw NetworkException.BeatmapException.BadRequest()
                401 -> throw NetworkException.BeatmapException.Unauthorized()
                403 -> throw NetworkException.BeatmapException.Forbidden()
                404 -> throw NetworkException.BeatmapException.NotFound()
                408 -> throw NetworkException.BeatmapException.RequestTimeout()
                422 -> throw NetworkException.BeatmapException.UnprocessableEntity()
                429 -> throw NetworkException.BeatmapException.TooManyRequests()
                500 -> throw NetworkException.BeatmapException.InternalServerError()
                502 -> throw NetworkException.BeatmapException.BadGateWay()
                503 -> throw NetworkException.BeatmapException.ServiceUnavailable()
                504 -> throw NetworkException.BeatmapException.GatewayTimeout()

                else -> {
                    if (e !is CancellationException && e !is NetworkException.BeatmapException) {
                        log.error("谱面请求：未定义的错误：", e)
                        throw NetworkException.BeatmapException.Undefined(e)
                    } else {
                        throw e
                    }
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
