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
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_DIFF
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_VERSION
import com.yumu.core.extensions.isNotNull
import org.springframework.stereotype.Service

@Service("MAI_VERSION")
class MaiVersionScoreService(
        val maimaiApiService: MaimaiApiService,
        val imageService: ImageService
) : MessageService<MaiVersionScoreService.MaiVersionParam> {

    private val newest = MaiVersion.PRISM // 当前最新版本

    data class MaiVersionParam(
            val name: String?,
            val qq: Long?,
            val difficulty: MaiDifficulty,
            val versions: List<MaiVersion>,
            val isMyself: Boolean = false
    )

    @JvmRecord
    data class PanelMA2Param(
        val user: MaiBestScore.User,
        val scores: List<MaiScore>,
        val versions: List<String>,
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "user" to user,
                "scores" to scores,
                "versions" to versions,
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

        val difficulty =
                if (matcher.group(FLAG_DIFF).isNotNull()) {
                    MaiDifficulty.getDifficulty(matcher.group(FLAG_DIFF))
                } else {
                    MaiDifficulty.DEFAULT
                }

        var versions =
                if (matcher.group(FLAG_VERSION).isNotNull()) {
                    MaiVersion.getVersionList(matcher.group(FLAG_VERSION))
                } else {
                    listOf(newest)
                }

        if (versions.first() == MaiVersion.DEFAULT) {
            versions = listOf(newest)
        }

        if (matcher.group(FLAG_NAME).isNotNull()) {
            val versionInName = MaiVersion.getVersion(matcher.group(FLAG_NAME))

            if (versionInName == MaiVersion.DEFAULT) {
                data.value = MaiVersionParam(matcher.group(FLAG_NAME).trim(), null, difficulty, versions, false)
            } else {
                data.value = MaiVersionParam(null, event.sender.id, difficulty, listOf(versionInName), true)
            }
        } else if (matcher.group(FLAG_QQ_ID).isNotNull()) {
            data.value = MaiVersionParam(null, matcher.group(FLAG_QQ_ID).toLong(), difficulty, versions, false)
        } else if (event.isAt) {
            data.value = MaiVersionParam(null, event.target, difficulty, versions, false)
        } else {
            data.value = MaiVersionParam(null, event.sender.id, difficulty, versions, true)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiVersionParam) {
        val vs = getVersionScores(param.qq, param.name, param.versions, maimaiApiService)

        if (vs.scores.isEmpty()) {
            throw NoSuchElementException.VersionScore(param.versions.listToString())
        }

        if (vs.scores.size > 240 && param.versions.size > 1) {
            throw IllegalArgumentException.ExceedException.Version()
        } else if (vs.scores.size > 300 && param.difficulty == MaiDifficulty.DEFAULT) {
            throw IllegalArgumentException.ExceedException.VersionDifficulty()
        }

        val full = getFullScore(param.qq, param.name, maimaiApiService)

        val user = full.getUser()
        val scores = MaiScoreSimplified.parseMaiScoreList(full.records, vs.scores).filter {
            MaiDifficulty.getIndex(it.index).equalDefault(param.difficulty)
        }

        maimaiApiService.insertSongData(scores)
        maimaiApiService.insertMaimaiAliasForScore(scores)
        maimaiApiService.insertPosition(scores, true)

        val image = imageService.getPanel(
            PanelMA2Param(user, scores, MaiVersion.getNameList(param.versions)).toMap(), "MA")

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
