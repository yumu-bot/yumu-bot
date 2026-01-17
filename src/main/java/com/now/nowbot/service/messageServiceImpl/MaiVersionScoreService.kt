package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.entity.ServiceCallStatistic
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

        @field:JsonProperty("plate_list")
        val plateList: List<MaiPlateList> = listOf(),

        @field:JsonProperty("count")
        val count: Int = 0,

        @field:JsonProperty("finished")
        val finished: Int = 0,
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
        val plate: PlateType,
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
                    data.value = MaiVersionParam(null, qq, MaiVersion.newestVersion, PlateType.SHOU, isMyself = true)
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

        val songs = getSongList(param.version, param.plate)

        val full = if (param.qq != null) {
            maimaiApiService.getMaimaiFullScores(param.qq)
        } else {
            maimaiApiService.getMaimaiFullScores(param.name!!)
        }

        val user = full.getUser()

        val scores = full.records

        val plates = parseList(songs, param.plate, scores)

        val res = MaiVersionResponse(user, plates, plates.sumOf { it.count }, plates.sumOf { it.finished })

        val image = imageService.getPanel(res, "MV")

        event.reply(image)

        return ServiceCallStatistic.building(event)
    }

    private fun getSongList(version: MaiVersion, plate: PlateType): List<MaiSong> {
        val collectionName = if (version == MaiVersion.ALL_FINALE && plate == PlateType.HASHA) {
            "覇者"
        } else {
            version.abbreviation + plate.character
        }

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

    private fun parseList(songs: List<MaiSong>, plate: PlateType, scores: List<MaiScore>): List<MaiPlateList> {
        val starMap = mapOf(
            "15" to 15.0..< 15.001,
            "14+" to 14.6 ..< 15.0,
            "14" to 14.0 ..< 14.6,
            "13+" to 13.6 ..< 14.0,
            "13" to 13.0 ..< 13.6,
            "12+" to 12.6 ..< 13.0,
            "12-" to 0.0 ..< 12.6
            // 如果需要统计更低难度，可以在此继续添加区间
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
                    val completed = isCompleted(plate, score)

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

    // 辅助类，用于承载扁平化后的数据
    private data class IndexedSong(val song: MaiSong, val index: Byte, val star: Double)

    companion object {
        private val plateTypeRegex = Regex("^(.*?)(ap|fc|sss|fsd|f?dx|舞舞|[神将极極])\\s*$")
        private val baShouRegex = Regex("[霸覇]者?|all\\s*finale|afn|fnl+")

        private fun isCompleted(plate: PlateType, score: MaiScore?): Boolean {
            if (score == null) return false

            return when(plate) {
                PlateType.GOKU -> score.combo.isNotEmpty()
                PlateType.SHIN -> score.combo == "ap" || score.combo == "app"
                PlateType.MAIMAI -> score.sync == "fsd" || score.sync == "fsdp"
                PlateType.SHOU -> score.achievements >= 100.0
                PlateType.HASHA -> true
            }
        }

        fun getPair(versionStr: String, plateStr: String): Pair<MaiVersion, PlateType> {
            var version = MaiVersion.getVersion(versionStr)

            if (version == MaiVersion.DEFAULT) {
                throw NoSuchElementException.MaiVersion()
            }

            if (version == MaiVersion.MAIMAI) {
                version = MaiVersion.PLUS
            }

            val plate = PlateType.getPlateType(
                plateStr.ifEmpty {
                    throw NoSuchElementException.MaiPlateType()
                }
            )

            if (version == MaiVersion.PLUS && plate == PlateType.SHOU) {
                throw NoSuchElementException.MaiPlateShinShou()
            }

            return version to plate
        }

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

        enum class PlateType(val character: String, val required: String) {
            GOKU("極", "fc"), // 极=fc
            SHIN("神", "ap"), // 神=ap
            MAIMAI("舞舞", "fdx"), // 舞=fdx
            SHOU("将", "sss"), // 将=sss
            HASHA("覇", "pass") // 覇
            ;

            companion object {
                fun getPlateType(input: String = ""): PlateType {
                    return when(input.lowercase().trim()) {
                        "fc", "極", "极" -> GOKU
                        "ap", "神" -> SHIN
                        "fsd", "fdx", "dx", "舞舞" -> MAIMAI
                        "pass", "覇", "覇者", "霸", "霸者" -> HASHA
                        "" -> throw NoSuchElementException.MaiPlateType()
                        else -> SHOU
                    }
                }
            }
        }
    }

    /*


    data class MaiVersionParam(
            val name: String?,
            val qq: Long?,
            val difficulty: MaiDifficulty,
            val versions: List<MaiVersion>,
            val page: Int,
            val isMyself: Boolean = false
    )

    @JvmRecord
    data class PanelMA2Param(
        val user: MaiBestScore.User,
        val scores: List<MaiScore>,
        val versions: List<String>,
        val page: Int = 1,
        val maxPage: Int = 1
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "user" to user,
                "scores" to scores,
                "versions" to versions,
                "page" to page,
                "max_page" to maxPage,
                "panel" to "MV"
            )
        }
    }

    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<MaiVersionParam>
    ): Boolean {
        val matcher = Instruction.MAI_VERSION.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val difficulty = MaiDifficulty.getDifficulty(matcher.group(FLAG_DIFF))

        val nameStr: String = matcher.group(FLAG_NAME)?.trim()  ?: ""
        val versionStr: String = matcher.group(FLAG_VERSION)?.trim() ?: ""
        val qq = if (event.hasAt()) {
            event.target
        } else {
            matcher.group(FLAG_QQ_ID)?.toLongOrNull()
        }

        val hasName = nameStr.isNotEmpty()
        val hasVersion = versionStr.isNotEmpty()
        val hasPageInVersionStr = hasVersion && versionStr.matches(REG_NUMBER_1_100.toRegex())
        val hasVersionInNameStr = hasName && MaiVersion.getVersionList(matcher.group(FLAG_VERSION)).contains(MaiVersion.DEFAULT)

        val page = if (hasPageInVersionStr) {
            versionStr.toIntOrNull() ?: 1
        } else {
            1
        }

        val versions: List<MaiVersion>

        if (qq != null) {
            versions = if (hasVersionInNameStr) {
                MaiVersion.getVersionListOrNewest(versionStr)
            } else {
                MaiVersion.getVersionListOrNewest(nameStr)
            }

            data.value = MaiVersionParam(null, qq, difficulty, versions, page, false)
        } else if (hasName) {
            if (hasVersionInNameStr) {
                versions = MaiVersion.getVersionListOrNewest(nameStr)

                data.value = MaiVersionParam(null, event.sender.id, difficulty, versions, page, false)
            } else if (hasVersion) {
                versions = MaiVersion.getVersionListOrNewest(versionStr)

                data.value = MaiVersionParam(nameStr, null, difficulty, versions, page, false)
            } else {
                versions = listOf(MaiVersion.newestVersion)

                data.value = MaiVersionParam(nameStr, null, difficulty, versions, page, false)
            }
        } else {
            versions = if (hasVersion) {
                MaiVersion.getVersionListOrNewest(versionStr)
            } else {
                listOf(MaiVersion.newestVersion)
            }

            data.value = MaiVersionParam(null, event.sender.id, difficulty, versions, page, false)
        }

        return true
    }

    override fun handleMessage(event: MessageEvent, param: MaiVersionParam): ServiceCallStatistic? {
        val vs = getVersionScores(param.qq, param.name, param.versions, maimaiApiService)

        if (vs.scores.isEmpty()) {
            throw NoSuchElementException.VersionScore(param.versions.listToString())
        }

        /*

        if (vs.scores.size > 240 && param.versions.size > 1) {
            throw IllegalArgumentException.ExceedException.Version()
        } else if (vs.scores.size > 300 && param.difficulty == MaiDifficulty.DEFAULT) {
            throw IllegalArgumentException.ExceedException.VersionDifficulty()
        }
         */

        val full = getFullScore(param.qq, param.name, maimaiApiService)

        val user = full.getUser()
        val scores = MaiScoreSimplified.parseMaiScoreList(full.records, vs.scores).filter {
            MaiDifficulty.getDifficulty(it.index).equalDefault(param.difficulty)
        }

        maimaiApiService.insertSongData(scores)
        maimaiApiService.insertMaimaiAliasForScore(scores)
        maimaiApiService.insertPosition(scores, 0)

        val page = DataUtil.splitPage(scores, param.page, 50)

        val image = imageService.getPanel(
            PanelMA2Param(user, page.first, MaiVersion.getNameList(param.versions), page.second, page.third).toMap(), "MA")

        event.reply(image)

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mais" to vs.scores.map { it.songID }
            ))
        }
    }

    companion object {

        @JvmStatic
        fun getFullScore(
            qq: Long?,
            name: String?,
            maimaiApiService: MaimaiApiService,
        ): MaiBestScore {
            return if (qq != null) {
                maimaiApiService.getMaimaiFullScores(qq)
            } else if (!name.isNullOrBlank()) {
                maimaiApiService.getMaimaiFullScores(name)
            } else {
                throw NoSuchElementException.Player()
            }
        }

        fun getVersionScores(
            qq: Long?,
            name: String?,
            version: List<MaiVersion>,
            maimaiApiService: MaimaiApiService,
        ): MaiVersionScore {
            return if (qq != null) {
                maimaiApiService.getMaimaiScoreByVersion(qq, version)
            } else if (!name.isNullOrBlank()) {
                maimaiApiService.getMaimaiScoreByVersion(name, version)
            } else {
                throw NoSuchElementException.Player()
            }
        }
    }

     */
}
