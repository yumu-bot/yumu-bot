package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.json.MaiBestScore
import com.now.nowbot.model.json.MaiScore
import com.now.nowbot.model.json.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import com.yumu.core.extensions.isNotNull
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("MAI_SCORE")
class MaiScoreService(
        private val maimaiApiService: MaimaiApiService,
        private val imageService: ImageService,
) : MessageService<MaiScoreService.MaiScoreParam> {

    data class MaiScoreParam(
            val id: Int?,
            val title: String?,
            val name: String?,
            val qq: Long?,
            val difficulty: MaiDifficulty,
    )

    data class MSPanelParam(val songs: List<MaiSong>, val scores: List<MaiScore>) {
        fun toMap(): Map<String, Any> {
            val out = mutableMapOf<String, Any>()

            out["songs"] = songs
            out["scores"] = scores

            out["panel"] = "MS"

            return out
        }
    }

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

        val nameStr = (matcher.group(FLAG_NAME) ?: "").trim()

        val qqStr = (matcher.group(FLAG_QQ_ID) ?: "").trim()

        val qq =
                if (event.isAt) {
                    event.target
                } else if (StringUtils.hasText(qqStr)) {
                    qqStr.toLong()
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
                                        difficulty,
                                )
                    } else {
                        data.value =
                                MaiScoreParam(
                                        null,
                                        s.first(),
                                        s.last().replace(Regex(REG_QUOTATION), ""),
                                        null,
                                        difficulty,
                                )
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
        val full = getFullScoreOrEmpty(param.qq, param.name, maimaiApiService)

        /*
        if (full.records.isEmpty())
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Play)

         */

        val image: ByteArray
        val result: MaiSong

        if (param.title != null) {
            // 成绩模式

            val possibles =
                    maimaiApiService.getMaimaiPossibleSongs(DataUtil.getStandardisedString(param.title))
                            ?: throw GeneralTipsException(
                                    GeneralTipsException.Type.G_Null_ResultNotAccurate
                            )

            val r = mutableMapOf<Double, MaiSong>()

            for (p in possibles) {
                val y = DataUtil.getStringSimilarity(param.title, p.title)

                if (y >= 0.4) {
                    r[y] = p
                }
            }

            if (r.isEmpty())
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_ResultNotAccurate)

            result =
                    r.entries
                            .stream()
                            .sorted(
                                    Comparator.comparingDouble<
                                                    MutableMap.MutableEntry<Double, MaiSong>?
                                            > {
                                                it.key
                                            }
                                            .reversed()
                            )
                            .map { it.value }
                            .toList()
                            .first()
        } else if (param.id != null) {
            // 谱面模式

            result =
                    maimaiApiService.getMaimaiSong(param.id.toLong())
                            ?: throw GeneralTipsException(
                                    GeneralTipsException.Type.G_Null_Song,
                                    param.id,
                            )
        } else {
            throw GeneralTipsException(
                    GeneralTipsException.Type.G_Malfunction_Classification,
                    "舞萌成绩",
            )
        }

        run {
            val standard: MaiSong
            val deluxe: MaiSong

            // 获取符合的成绩
            val scores: List<MaiScore> =
                    full.records
                            .stream()
                            .filter {
                                if (result.songID < 10000) {
                                    return@filter it.songID == result.songID.toLong() ||
                                            it.songID == (result.songID + 10000).toLong()
                                } else {
                                    return@filter it.songID == result.songID.toLong() ||
                                            it.songID == (result.songID - 10000).toLong()
                                }
                            }
                            .toList()

            MaiScore.insertSongData(scores, maimaiApiService)

            // 判断谱面种类
            if (result.songID < 10000) {
                standard = result
                deluxe = maimaiApiService.getMaimaiSong(result.songID + 10000L) ?: MaiSong()
            } else {
                standard = maimaiApiService.getMaimaiSong(result.songID - 10000L) ?: MaiSong()
                deluxe = result
            }

            image =
                    imageService.getPanel(
                            MSPanelParam(songs = mutableListOf(standard, deluxe), scores = scores)
                                    .toMap(),
                            "MS",
                    )
        }

        event.reply(image)
    }

    companion object {
        @JvmStatic
        fun getFullScoreOrEmpty(
            qq: Long?,
            name: String?,
            maimaiApiService: MaimaiApiService,
        ): MaiBestScore {
            return if (qq.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiFullScores(qq!!)
                } catch (e: WebClientResponseException) {
                    return MaiBestScore()
                }
            } else if (name.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiFullScores(name!!)
                } catch (e: WebClientResponseException) {
                    return MaiBestScore()
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            }
        }

        fun getSearchResult(text: String?, maimaiApiService: MaimaiApiService): MessageChain {
            val songs = maimaiApiService.getMaimaiSongLibrary()
            val result = mutableMapOf<Double, MaiSong>()

            for (s in songs) {
                val similarity = DataUtil.getStringSimilarity(text, s.value.title)

                if (similarity >= 0.4) {
                    result[similarity] = s.value
                }
            }

            if (result.isEmpty()) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Result)
            }

            val sort = result.toSortedMap().reversed()

            val sb = StringBuilder("\n")

            var i = 1
            for (e in sort) {
                val code =
                        MaiVersion.getCodeList(MaiVersion.getVersionList(e.value.info.version))
                                .first()
                val category = MaiVersion.getCategoryAbbreviation(e.value.info.genre)

                sb.append("#${i}:")
                        .append(" ")
                        .append(String.format("%.0f", e.key * 100))
                        .append("%")
                        .append(" ")
                        .append("[${e.value.songID}]")
                        .append(" ")
                        .append(e.value.title)
                        .append(" ")
                        .append("[${code}]")
                        .append(" / ")
                        .append("[${category}]")
                        .append("\n")

                i++

                if (i >= 6) break
            }

            val img = maimaiApiService.getMaimaiCover((sort[sort.firstKey()]?.songID ?: 0).toLong())

            sb.removeSuffix("\n")

            return MessageChain.MessageChainBuilder()
                    .addText("搜索结果：\n")
                    .addImage(img)
                    .addText(sb.toString())
                    .build()
        }

        //uum
        fun getScoreMessage(score: MaiScore, image: ByteArray): MessageChain {
            val sb = MessageChain.MessageChainBuilder()

            sb.addImage(image)
            sb.addText("\n")
            sb.addText(
                    "[${score.type}] ${score.title} [${score.difficulty} ${score.level}] (${score.star})\n"
            )
            sb.addText(
                    "${String.format("%.4f", score.achievements)}% ${getRank(score.rank)} // ${score.rating} ra\n"
            )
            sb.addText("[${getCombo(score.combo)}] [${getSync(score.sync)}] // id ${score.songID}")

            return sb.build()
        }

        private fun getRank(rate: String?): String {
            return (rate ?: "?").uppercase().replace('P', '+')
        }

        private fun getCombo(combo: String?): String {
            return when (combo?.lowercase()) {
                "" -> "C"
                "fc" -> "FC"
                "fcp" -> "FC+"
                "ap" -> "AP"
                "app" -> "AP+"
                else -> "?"
            }
        }

        private fun getSync(sync: String?): String {
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
