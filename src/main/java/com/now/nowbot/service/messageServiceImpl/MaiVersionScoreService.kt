package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.enums.MaiVersion.Companion.listToString
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiScoreSimplified
import com.now.nowbot.model.maimai.MaiVersionScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.springframework.stereotype.Service

@Service("MAI_VERSION")
class MaiVersionScoreService(
        val maimaiApiService: MaimaiApiService,
        val imageService: ImageService
) : MessageService<MaiVersionScoreService.MaiVersionParam> {

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
        val qq = if (event.isAt) {
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

    override fun HandleMessage(event: MessageEvent, param: MaiVersionParam) {
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
        maimaiApiService.insertPosition(scores, true)

        val page = DataUtil.splitPage(scores, param.page, 50)

        val image = imageService.getPanel(
            PanelMA2Param(user, page.first, MaiVersion.getNameList(param.versions), page.second, page.third).toMap(), "MA")

        event.reply(image)
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
}
