package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.MaiDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_VERSION
import org.springframework.stereotype.Service

@Service("MAI_VERSION")
class MaiVersionScoreService(
    val maiDao: MaiDao,
    val imageService: ImageService
) : MessageService<MaiVersionScoreService.MaiVersionParam> {

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

        return null
    }

    private fun getSongList(version: MaiVersion, plate: PlateType): List<Int> {
        val c = maiDao.findLxMaiCollections("plate")
            .firstOrNull {
                it.name.equals(version.abbreviation + plate.character, true)
            } ?: throw NoSuchElementException.MaiCollection()

        return c.required?.firstOrNull()?.songs?.map { it.songID }
            ?: throw NoSuchElementException.MaiCollection()
    }

    companion object {
        private val plateTypeRegex = Regex("^(.*?)(ap|fc|sss|fsd|f?dx|舞舞|[神将极極])\\s*$")

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
            val matchResult = plateTypeRegex.find(input)

            return if (matchResult != null) {
                val main = matchResult.groupValues[1]
                val suffix = matchResult.groupValues[2]

                main to suffix
            } else {
                throw NoSuchElementException.MaiVersion()
            }
        }

        enum class PlateType(val character: String) {
            GOKU("極"), // 极=fc
            SHIN("神"), // 神=ap
            MAIMAI("舞舞"), // 舞=fdx
            SHOU("将"), // 将=sss
            ;

            companion object {
                fun getPlateType(input: String = ""): PlateType {
                    return when(input.lowercase().trim()) {
                        "fc", "極", "极" -> GOKU
                        "ap", "神" -> SHIN
                        "fsd", "fdx", "dx", "舞舞" -> MAIMAI
                        "" -> throw NoSuchElementException.MaiPlateType()
                        else -> SHOU
                    }
                }
            }
        }


        fun removeSpecificSuffixes(input: String): String {
            val suffixes = listOf("ap", "fc", "sss", "神", "舞", "将", "极", "")
            var result = input
            // 只移除一次，找到第一个匹配的就停止
            for (suffix in suffixes) {
                if (result.endsWith(suffix)) {
                    result = result.removeSuffix(suffix)
                    break
                }
            }
            return result
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
