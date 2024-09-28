package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.enums.MaiVersion.Companion.listToString
import com.now.nowbot.model.json.MaiBestScore
import com.now.nowbot.model.json.MaiScore
import com.now.nowbot.model.json.MaiScoreSimplified
import com.now.nowbot.model.json.MaiVersionScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_DIFF
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_VERSION
import com.yumu.core.extensions.isNotNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("MAI_VERSION")
class MaiVersionScoreService(
        val maimaiApiService: MaimaiApiService,
        val imageService: ImageService
) : MessageService<MaiVersionScoreService.MaiVersionParam> {

    private val newest = MaiVersion.BUDDIES // 当前最新版本

    data class MaiVersionParam(
            val name: String?,
            val qq: Long?,
            val difficulty: MaiDifficulty,
            val versions: MutableList<MaiVersion>,
            val isMyself: Boolean = false
    )

    @JvmRecord
    data class PanelMA2Param(
            val user: MaiBestScore.User,
            val scores: MutableList<MaiScore>,
            val versions: MutableList<String>,
    ) {
        fun toMap(): Map<String, Any> {
            val out = mutableMapOf<String, Any>()

            out["user"] = user
            out["scores"] = scores
            out["versions"] = versions
            out["panel"] = "MV"

            return out
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
                    mutableListOf(newest)
                }

        if (versions.first() == MaiVersion.DEFAULT) {
            versions = mutableListOf(newest)
        }

        if (matcher.group(FLAG_NAME).isNotNull()) {
            val versionInName = MaiVersion.getVersion(matcher.group(FLAG_NAME))

            if (versionInName == MaiVersion.DEFAULT) {
                data.value =
                        MaiVersionParam(
                                matcher.group(FLAG_NAME).trim(), null, difficulty, versions, false)
                return true
            } else {
                data.value =
                        MaiVersionParam(
                                null,
                                event.sender.id,
                                difficulty,
                                mutableListOf(versionInName),
                                true)
                return true
            }
        } else if (matcher.group(FLAG_QQ_ID).isNotNull()) {
            data.value =
                    MaiVersionParam(
                            null, matcher.group(FLAG_QQ_ID).toLong(), difficulty, versions, false)
            return true
        } else if (event.isAt) {
            data.value = MaiVersionParam(null, event.target, difficulty, versions, false)
            return true
        } else {
            data.value = MaiVersionParam(null, event.sender.id, difficulty, versions, true)
            return true
        }
    }

    override fun HandleMessage(event: MessageEvent, param: MaiVersionParam) {
        val vs =
                getVersionScores(
                        param.qq, param.name, param.versions, param.isMyself, maimaiApiService)
        if (vs.scores.isEmpty())
                throw GeneralTipsException(
                        GeneralTipsException.Type.G_Null_Version, param.versions.listToString())
        if (vs.scores.size > 240 && param.versions.size > 1)
                throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Version)
        else if (vs.scores.size > 300 && param.difficulty == MaiDifficulty.DEFAULT)
                throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Version_Default)

        val full = getFullScore(param.qq, param.name, param.isMyself, maimaiApiService)
        val songs = maimaiApiService.getMaimaiSongLibraryFromDatabase()

        val user = full.getUser()
        val scores =
                MaiScoreSimplified.parseMaiScoreList(full.records, vs.scores)
                        .stream()
                        .filter { MaiDifficulty.getIndex(it.index).equalDefault(param.difficulty) }
                        .toList()
        MaiScore.insertSongData(scores, songs)

        val image =
                imageService.getPanel(
                        PanelMA2Param(user, scores, MaiVersion.getNameList(param.versions)).toMap(),
                        "MA")

        event.reply(image)
    }

    companion object {

        @JvmStatic
        fun getFullScore(
                qq: Long?,
                name: String?,
                isMyself: Boolean,
                maimaiApiService: MaimaiApiService,
        ): MaiBestScore {
            return if (qq.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiFullScores(qq!!)
                } catch (e: WebClientResponseException.BadRequest) {
                    if (isMyself) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_YouBadRequest)
                    } else {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_QQBadRequest)
                    }
                } catch (e: WebClientResponseException.Forbidden) {
                    if (isMyself) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_YouForbidden)
                    } else {
                        throw GeneralTipsException(
                                GeneralTipsException.Type.G_Maimai_PlayerForbidden)
                    }
                }
            } else if (name.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiFullScores(name!!)
                } catch (e: WebClientResponseException.BadRequest) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_NameBadRequest)
                } catch (e: WebClientResponseException.Forbidden) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_PlayerForbidden)
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            }
        }

        fun getVersionScores(
                qq: Long?,
                name: String?,
                version: MutableList<MaiVersion>,
                isMyself: Boolean,
                maimaiApiService: MaimaiApiService,
        ): MaiVersionScore {
            return if (qq.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiScoreByVersion(qq!!, version)
                } catch (e: WebClientResponseException.BadRequest) {
                    if (isMyself) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_YouBadRequest)
                    } else {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_QQBadRequest)
                    }
                } catch (e: WebClientResponseException.Forbidden) {
                    if (isMyself) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_YouForbidden)
                    } else {
                        throw GeneralTipsException(
                                GeneralTipsException.Type.G_Maimai_PlayerForbidden)
                    }
                }
            } else if (name.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiScoreByVersion(name!!, version)
                } catch (e: WebClientResponseException.BadRequest) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_NameBadRequest)
                } catch (e: WebClientResponseException.Forbidden) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_PlayerForbidden)
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            }
        }
    }
}
