package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.json.MaiBestScore
import com.now.nowbot.model.json.MaiScore
import com.now.nowbot.model.json.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import com.yumu.core.extensions.isNotNull
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.stream.Collectors

//@Service("MAI_SCORE")
class MaiScoreService(private val maimaiApiService: MaimaiApiService) :
        MessageService<MaiScoreService.MaiScoreParam> {

    data class MaiScoreParam(
            val id: Int?,
            val title: String?,
            val name: String?,
            val qq: Long?,
            val difficulty: MaiDifficulty
    )

    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<MaiScoreParam>,
    ): Boolean {
        val matcher = Instruction.MAI_SCORE.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val difficulty =
                if (matcher.group(FLAG_DIFF).isNotNull()) {
                    MaiDifficulty.getDifficulty(matcher.group(FLAG_DIFF))
                } else {
                    MaiDifficulty.DEFAULT
                }

        val nameStr = (matcher.group(REG_NAME) ?: "").trim()

        val qq =
                if (event.isAt) {
                    event.target
                } else {
                    event.sender.id
                }

        if (StringUtils.hasText(nameStr)) {
            if (nameStr.contains(Regex(REG_SPACE))) {
                val s = nameStr.split(Regex(REG_SPACE))

                if (s.size == 2) {
                    if (s.first().matches(Regex(REG_NUMBER_15))) {
                        data.value =
                                MaiScoreParam(
                                        s.first().toInt(),
                                        null,
                                        s.last().replace(Regex(REG_QUOTATION), ""),
                                        null,
                                        difficulty)
                    } else {
                        data.value =
                                MaiScoreParam(
                                        null,
                                        s.first(),
                                        s.last().replace(Regex(REG_QUOTATION), ""),
                                        null,
                                        difficulty)
                    }
                } else if (s.size == 1) {
                    if (s.first().matches(Regex(REG_NUMBER_15))) {
                        data.value = MaiScoreParam(s.first().toInt(), null, null, qq, difficulty)
                    } else if (s.first().contains(Regex(REG_QUOTATION))) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID_Quotation)
                    } else {
                        data.value = MaiScoreParam(null, nameStr, null, qq, difficulty)
                    }
                } else {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
                }
            } else {
                if (nameStr.matches(Regex(REG_NUMBER_15))) {
                    data.value = MaiScoreParam(nameStr.toInt(), null, null, qq, difficulty)
                } else if (nameStr.contains(Regex(REG_QUOTATION))) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID_Quotation)
                } else {
                    data.value = MaiScoreParam(null, nameStr, null, qq, difficulty)
                }
            }
        } else {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiScoreParam) {
        val isMyself = (param.qq == event.sender.id)

        val full = getFullScore(param.qq, param.name, isMyself = isMyself, maimaiApiService)

        if (full.records.isEmpty())
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Play)

        val score: MaiScore

        if (param.title != null) {
            val sort =
                    maimaiApiService.getMaimaiPossibleSongs(param.title)
                            ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_Result)

            if (sort.keys.max() < 0.4) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_ResultNotAccurate)
            }

            val expect = sort.entries.first().value

            score =
                    full.records
                            .stream()
                            .collect(Collectors.toMap(MaiScore::songID) { it })[
                                    expect.songID.toLong()]
                            ?: throw GeneralTipsException(
                                    GeneralTipsException.Type.G_Null_ResultNotAccurate)

            /*
            event.reply(getSearchResult(sort))
            return

             */
        } else if (param.id != null) {}
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

        fun getSearchResult(text : String?, maimaiApiService: MaimaiApiService): MessageChain {
            val songs = maimaiApiService.getMaimaiSongLibrary()
            val result = mutableMapOf<Double, MaiSong>()

            for (s in songs) {
                val similarity = DataUtil.getStringSimilarity(text, s.value.title)

                if (similarity >= 0.4) {
                    result[similarity] = s.value
                }
            }

            if (result.isEmpty()) {
                throw TipsException("没有找到结果！")
            }

            val sort = result.toSortedMap().reversed()

            val sb = StringBuilder("\n")

            var i = 1
            for(e in sort) {
                val code = MaiVersion.getCodeList(MaiVersion.getVersionList(e.value.info.version)).first()
                val category = MaiVersion.getCategoryAbbreviation(e.value.info.genre)

                sb.append("#${i}:").append(" ")
                    .append(String.format("%.0f", e.key * 100)).append("%").append(" ")
                    .append("[${e.value.songID}]").append(" ")
                    .append(e.value.title).append(" ")
                    .append("[${code}]").append(" / ")
                    .append("[${category}]").append("\n")

                i++

                if (i >= 6) break
            }

            val img = maimaiApiService.getMaimaiCover((sort[sort.firstKey()]?.songID ?: 0).toLong())

            sb.removeSuffix("\n")

            return MessageChain.MessageChainBuilder().addText("搜索结果：\n").addImage(img).addText(sb.toString()).build()
        }

        fun getScoreMessage(score: MaiScore, image: ByteArray): MessageChain {
            val sb = MessageChain.MessageChainBuilder()

            sb.addImage(image)
            sb.addText("\n")
            sb.addText(
                    "[${score.type}] ${score.title} [${score.difficulty} ${score.level}] (${score.star})\n")
            sb.addText(
                    "${String.format("%.4f", score.achievements)}% ${getRank(score.rank)} // ${score.rating} ra\n")
            sb.addText("[${getCombo(score.combo)}] [${getSync(score.sync)}] // id ${score.songID}")

            return sb.build()
        }

        fun getRank(rate: String?): String {
            return (rate ?: "?").uppercase().replace('P', '+')
        }

        fun getCombo(combo: String?): String {
            return when (combo?.lowercase()) {
                "" -> "C"
                "fc" -> "FC"
                "fcp" -> "FC+"
                "ap" -> "AP"
                "app" -> "AP+"
                else -> "?"
            }
        }

        fun getSync(sync: String?): String {
            return when (sync?.lowercase()) {
                "" -> "1P"
                "sync" -> "SY"
                "fs" -> "FS"
                "fsp" -> "FS+"
                "fsd" -> "FDX"
                "fsdp" -> "FDX+"
                else -> "?"
            }
        }
    }
}
