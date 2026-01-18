package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.MaiPlateType
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_VERSION
import org.springframework.stereotype.Service
import kotlin.collections.fold

@Service("MAI_VERSION")
class MaiVersionScoreService(
    val maiDao: MaiDao,
    val maimaiApiService: MaimaiApiService,
    val imageService: ImageService
) : MessageService<MaiVersionScoreService.MaiVersionParam> {

    data class MaiVersionResponse(
        @field:JsonProperty("user")
        val user: MaiBestScore.User,

        @field:JsonProperty("plate")
        val plateID: Int = 0,

        @field:JsonProperty("plate_list")
        val plateList: List<MaiPlateList> = listOf(),

        @field:JsonProperty("count_15")
        val count15: Int = 0,

        @field:JsonProperty("finished_15")
        val finished15: Int = 0,

        @field:JsonProperty("count_12")
        val count12: Int = 0,

        @field:JsonProperty("finished_12")
        val finished12: Int = 0,
    )

    data class MaiPlateList(
        @field:JsonProperty("star")
        val star: String = "",

        @field:JsonProperty("count")
        val count: Int = 0,

        @field:JsonProperty("finished")
        val finished: Int = 0,

        @field:JsonProperty("progress")
        val progress: List<MaiPlateProgress> = listOf(),
    )

    data class MaiPlateProgress(
        @field:JsonProperty("title")
        val title: String = "",

        @field:JsonProperty("song_id")
        val songID: Int = 0,

        @field:JsonProperty("index")
        val index: Byte = 0,

        @field:JsonProperty("star")
        val star: Double = 0.0,

        @field:JsonProperty("score")
        val score: MaiScore? = null,

        @field:JsonProperty("required")
        val required: String = "",

        @field:JsonProperty("completed")
        val completed: Boolean = false,
    )

    data class MaiVersionParam(
        val name: String?,
        val qq: Long?,
        val version: MaiVersion,
        val plate: MaiPlateType,
        val isMyself: Boolean = false
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiVersionParam>
    ): Boolean {
        val matcher = Instruction.MAI_VERSION.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val nameStr: String = matcher.group(FLAG_NAME)?.trim() ?: ""

        val versionStr: String = matcher.group(FLAG_VERSION)?.trim() ?: ""

        val qq = if (event.hasAt()) {
            event.target
        } else {
            matcher.group(FLAG_QQ_ID)?.toLongOrNull() ?: event.sender.id
        }

        if (qq == event.sender.id) {
            if (nameStr.isNotEmpty()) {
                if (versionStr.isNotEmpty()) {
                    val (v, p) = getPairStr(versionStr)
                    val (version, plate) = getPair(v, p)

                    data.value = MaiVersionParam(nameStr, null, version, plate, isMyself = false)
                } else {
                    val (v, p) = getPairStr(nameStr)
                    val (version, plate) = getPair(v, p)

                    data.value = MaiVersionParam(null, qq, version, plate, isMyself = true)
                }
            } else {
                if (versionStr.isNotEmpty()) {
                    val (v, p) = getPairStr(versionStr)
                    val (version, plate) = getPair(v, p)

                    data.value = MaiVersionParam(null, qq, version, plate, isMyself = true)
                } else {
                    data.value = MaiVersionParam(null, qq, MaiVersion.newestVersion, MaiPlateType.SHOU, isMyself = true)
                }
            }
        } else {
            val (v, p) = getPairStr(nameStr + versionStr)
            val (version, plate) = getPair(v, p)

            data.value = MaiVersionParam(null, qq, version, plate, isMyself = false)
        }

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: MaiVersionParam
    ): ServiceCallStatistic? {
        val plateName = MaiPlateType.getPlateName(param.version, param.plate)

        val songs = getSongList(plateName)

        val full = if (param.qq != null) {
            maimaiApiService.getMaimaiFullScores(param.qq)
        } else {
            maimaiApiService.getMaimaiFullScores(param.name!!)
        }

        val plateID = maiDao.getLxMaiPlateIDMap()[plateName] ?: 0
        val user = full.getUser(plateID = plateID)

        val scores = full.records

        val plates = parseList(songs, param.plate, scores)

        val countSum = plates.sumOf { it.count }
        val finishedSum = plates.sumOf { it.finished }

        val count12 = plates.findLast { it.star == "12-" }?.count ?: 0
        val finished12 = plates.findLast { it.star == "12-" }?.finished ?: 0

        val res = MaiVersionResponse(user,
            plateID,
            plates,
            countSum,
            finishedSum,
            count12, finished12
        )

        val image = imageService.getPanel(res, "MV")

        event.reply(image)

        return ServiceCallStatistic.building(event)
    }

    private fun getSongList(collectionName: String): List<MaiSong> {
        val c = maiDao.findLxMaiCollections("plate")
            .firstOrNull {
                it.name.equals(collectionName, true)
            } ?: throw NoSuchElementException.MaiCollection()

        val req = c.required?.firstOrNull() ?: throw NoSuchElementException.MaiCollection()

        val songs = (req.songs ?: listOf()).mapNotNull {
            maiDao.findLxMaiSongByID(it.songID)?.toMaiSong(
                it.type.equals("dx", true)
            )
        }

        return songs
    }

    private fun parseList(songs: List<MaiSong>, plate: MaiPlateType, scores: List<MaiScore>): List<MaiPlateList> {
        val starMap = mapOf(
            "15" to 15.0..< 15.001,
            "14+" to 14.6 ..< 15.0,
            "14" to 14.0 ..< 14.6,
            "13+" to 13.6 ..< 14.0,
            "13" to 13.0 ..< 13.6,
            "12+" to 12.6 ..< 13.0,
            "12-" to 0.0 ..< 12.6
        )

        // 1. 预处理最高分：保持不变，依然使用 independentID 作为 Key
        val bestScores: Map<Long, MaiScore> = scores
            .fold(mutableMapOf()) { acc, score ->
                val currentMax = acc[score.independentID]
                if (currentMax == null || score.achievements > currentMax.achievements) {
                    acc[score.independentID] = score
                }
                acc
            }

        // 2. 扁平化所有歌曲的难度：将一首歌转为多个 (Song, DifficultyIndex) 对象
        val allDifficultyEntries = songs.flatMap { song ->
            song.star.mapIndexed { index, star ->
                if (star > 0 && index in 0..3) {
                    IndexedSong(song, index.toByte(), star)
                } else null
            }.filterNotNull()
        }

        // 3. 根据 starMap 分组并构造结果
        val resultList = starMap.map { (label, range) ->
            val progressList = allDifficultyEntries
                .filter { it.star in range } // 落在当前分数段
                .map { entry ->
                    // 计算 independentID: 歌曲ID * 10 + 难度索引
                    val independentID = entry.song.songID * 10L + entry.index
                    val score = bestScores[independentID]
                    val completed = MaiPlateType.isCompleted(plate, score)

                    MaiPlateProgress(
                        entry.song.title,
                        entry.song.songID,
                        entry.index,
                        entry.star,
                        score,
                        plate.required,
                        completed
                    )
                }
                .sortedByDescending { it.star } // 同一分段内按具体定数降序
                .sortedBy { it.completed }

            // 2. 在这里提取统计信息（示例：完成数 / 总数）
            val finishedCount = progressList.count { it.completed }
            val totalCount = progressList.size

            // 3. 针对 "12-" 分组，返回空列表，其他分组保留数据
            val finalProgress = if (label == "12-") {
                emptyList()
            } else {
                progressList
            }

            MaiPlateList(label, totalCount, finishedCount, finalProgress)
        }

        return resultList
    }

    companion object {
        private val plateTypeRegex = Regex("^(.*?)(ap|fc|sss|fsd|f?dx|舞舞|[神将极極])\\s*$")
        private val baShouRegex = Regex("[霸覇]者?|all\\s*finale|afn|fnl+")

        // 辅助类，用于承载扁平化后的数据
        private data class IndexedSong(val song: MaiSong, val index: Byte, val star: Double)

        fun getPairStr(input: String): Pair<String, String> {
            if (input.contains(baShouRegex)) {
                return "allfinale" to "覇"
            }

            val matchResult = plateTypeRegex.find(input)

            return if (matchResult != null) {
                val main = matchResult.groupValues[1]
                val suffix = matchResult.groupValues[2]

                main to suffix
            } else {
                throw NoSuchElementException.MaiVersion()
            }
        }

        fun getPair(versionStr: String, plateStr: String): Pair<MaiVersion, MaiPlateType> {
            var version = MaiVersion.getVersion(versionStr)

            if (version == MaiVersion.DEFAULT) {
                throw NoSuchElementException.MaiVersion()
            }

            if (version == MaiVersion.MAIMAI) {
                version = MaiVersion.PLUS
            }

            val plate = MaiPlateType.getPlateType(
                plateStr.ifEmpty {
                    throw NoSuchElementException.MaiPlateType()
                }
            )

            if (version == MaiVersion.PLUS && plate == MaiPlateType.SHOU) {
                throw NoSuchElementException.MaiPlateShinShou()
            }

            return version to plate
        }
    }
}
