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
import org.springframework.stereotype.Service

@Service("MAI_SCORE") class MaiScoreService(
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

        val difficulty = if (matcher.group(FLAG_DIFF).isNullOrBlank().not()) {
            MaiDifficulty.getDifficulty(matcher.group(FLAG_DIFF))
        } else {
            MaiDifficulty.DEFAULT
        }

        val nameOrTitleStr = (matcher.group(FLAG_NAME) ?: "").trim()

        val qqStr = (matcher.group(FLAG_QQ_ID) ?: "").trim()

        val qq = if (event.isAt) {
            event.target
        } else if (qqStr.isNotBlank()) {
            qqStr.toLong()
        } else {
            event.sender.id
        }

        if (nameOrTitleStr.isNotBlank()) {
            if (nameOrTitleStr.contains(Regex(REG_SPACE))) {
                val s = nameOrTitleStr.split(Regex(REG_SPACE))

                if (s.size == 2) {
                    if (s.first().matches(Regex(REG_NUMBER_15))) {
                        data.value = MaiScoreParam(
                            s.first().toInt(),
                            null,
                            s.last().replace(Regex(REG_QUOTATION), ""),
                            null,
                            difficulty,
                        )
                    } else {
                        data.value = MaiScoreParam(
                            null,
                            nameOrTitleStr.replace(Regex(REG_QUOTATION), ""),
                            null,
                            qq,
                            difficulty,
                        )
                    }
                } else if (s.size == 1) {
                    if (s.first().matches(Regex(REG_NUMBER_15))) {
                        data.value = MaiScoreParam(s.first().toInt(), null, null, qq, difficulty)
                    } else if (s.first().contains(Regex(REG_QUOTATION))) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID_Quotation)
                    } else {
                        data.value = MaiScoreParam(null, nameOrTitleStr, null, qq, difficulty)
                    }
                } else {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
                }
            } else {
                if (nameOrTitleStr.matches(Regex(REG_NUMBER_15))) {
                    data.value = MaiScoreParam(nameOrTitleStr.toInt(), null, null, qq, difficulty)
                } else if (nameOrTitleStr.contains(Regex(REG_QUOTATION))) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID_Quotation)
                } else {
                    data.value = MaiScoreParam(null, nameOrTitleStr, null, qq, difficulty)
                }
            }
        } else {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiScoreParam) {
        val full = getFullScoreOrNull(param.qq, param.name, maimaiApiService)

        /*
        if (full.records.isEmpty())
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Play)

         */

        val image: ByteArray

        val result: MaiSong = if (param.title != null) { // 标题搜歌模式
            val title = DataUtil.getStandardisedString(param.title)

            // 外号模式
            val s = maimaiApiService.getMaimaiAliasSong(title)

            if (s != null) {
                s
            } else {

                // 实在走不通的保底模式

                val possibles = maimaiApiService.getMaimaiPossibleSongs(title) ?: throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_ResultNotAccurate
                )

                val r = mutableListOf<Pair<MaiSong, Double>>()

                for (p in possibles) {
                    val y = DataUtil.getStringSimilarity(title, p.title)

                    if (y >= 0.4) {
                        r.add(Pair(p, y))
                    }
                }

                if (r.isEmpty()) throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_ResultNotAccurate
                )

                r.stream().sorted(Comparator.comparingDouble<Pair<MaiSong, Double>?> { it.second }.reversed())
                    .map { it.first }.toList().first()
            }
        } else if (param.id != null) { // 搜歌模式
            maimaiApiService.getMaimaiSong(param.id.toLong())
                ?: maimaiApiService.getMaimaiAliasSong(param.id.toString()) // 有的歌曲外号叫 3333
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
            val scores: List<MaiScore> = if (full != null) {
                full.records.stream().filter {
                        if (result.songID < 10000) {
                            return@filter it.songID == result.songID.toLong() || it.songID == (result.songID + 10000).toLong()
                        } else {
                            return@filter it.songID == result.songID.toLong() || it.songID == (result.songID - 10000).toLong()
                        }
                    }.toList()
            } else {
                listOf()
            }

            maimaiApiService.insertSongData(scores)

            // 判断谱面种类
            if (result.songID < 10000) {
                standard = result
                deluxe = maimaiApiService.getMaimaiSong(result.songID + 10000L) ?: MaiSong()
            } else {
                standard = maimaiApiService.getMaimaiSong(result.songID - 10000L) ?: MaiSong()
                deluxe = result
            }

            image = imageService.getPanel(
                MSPanelParam(songs = listOf(standard, deluxe), scores = scores).toMap(),
                "MS",
            )
        }

        event.reply(image)
    }

    companion object {
        @JvmStatic fun getFullScoreOrNull(
            qq: Long?,
            name: String?,
            maimaiApiService: MaimaiApiService,
        ): MaiBestScore? {
            return if (qq != null) {
                try {
                    maimaiApiService.getMaimaiFullScores(qq)
                } catch (e: Exception) {
                    return null
                }
            } else if (name != null) {
                try {
                    maimaiApiService.getMaimaiFullScores(name)
                } catch (e: Exception) {
                    return null
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            }
        }

        // UUMS
        fun getSearchResult(text: String?, maimaiApiService: MaimaiApiService): MessageChain {
            val songs = maimaiApiService.getMaimaiSongLibrary()
            val result = mutableMapOf<Double, MaiSong>()

            for (s in songs.values) {
                val similarity = DataUtil.getStringSimilarity(text, s.title)

                if (similarity >= 0.4) {
                    maimaiApiService.insertMaimaiAlias(s)
                    result[similarity] = s
                }
            }

            if (result.isEmpty()) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Result)
            }

            val sort = result.toSortedMap().reversed()

            val sb = StringBuilder("\n")

            var i = 1
            for (e in sort) {
                val code = MaiVersion.getCodeList(MaiVersion.getVersionList(e.value.info.version)).first()
                val category = MaiVersion.getCategoryAbbreviation(e.value.info.genre)

                sb.append("#${i}:").append(" ").append(String.format("%.0f", e.key * 100)).append("%").append(" ")
                    .append("[${e.value.songID}]").append(" ").append(e.value.title).append(" ").append("[${code}]")
                    .append(" / ").append("[${category}]").append("\n")

                i++

                if (i >= 6) break
            }

            val img = maimaiApiService.getMaimaiCover((sort[sort.firstKey()]?.songID ?: 0).toLong())

            sb.removeSuffix("\n")

            return MessageChain.MessageChainBuilder().addText("搜索结果：\n").addImage(img).addText(sb.toString())
                .build()
        }

        // uum
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
