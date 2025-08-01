package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.messageServiceImpl.MaiScoreService.Companion.Version.*
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
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
        val version: Version,
        val difficulty: MaiDifficulty,
    )

    data class MSPanelParam(val user: MaiBestScore.User?, val songs: List<MaiSong>, val scores: List<MaiScore>, val version: Version) {
        fun toMap(): Map<String, Any?> {
            return mapOf(
                "user" to user,
                "songs" to songs,
                "scores" to scores,
                "version" to version.name,
                "panel" to "MS"
            )
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

        val version = Version.getVersion(matcher.group(FLAG_VERSION))

        val nameOrTitleStr = (matcher.group(FLAG_NAME) ?: "").trim()

        val qqStr = (matcher.group(FLAG_QQ_ID) ?: "").trim()

        val qq = if (event.isAt) {
            event.target
        } else {
            qqStr.toLongOrNull() ?: event.sender.id
        }

        if (nameOrTitleStr.isNotBlank()) {
            if (nameOrTitleStr.contains(Regex(REG_SPACE))) {
                val s = nameOrTitleStr.split(Regex(REG_SPACE))

                if (s.size >= 2) {
                    if (s.first().matches(Regex(REG_NUMBER_15))) {
                        data.value = MaiScoreParam(
                            s.first().toInt(),
                            null,
                            s.last().replace(Regex(REG_QUOTATION), ""),
                            null,
                            version,
                            difficulty,
                        )
                    } else {
                        data.value = MaiScoreParam(
                            null,
                            nameOrTitleStr.replace(Regex(REG_QUOTATION), ""),
                            null,
                            qq,
                            version,
                            difficulty,
                        )
                    }
                } else if (s.size == 1) {
                    if (s.first().matches(Regex(REG_NUMBER_15))) {
                        data.value = MaiScoreParam(s.first().toInt(), null, null, qq, version, difficulty)
                    } else if (s.first().contains(Regex(REG_QUOTATION))) {
                        throw IllegalArgumentException.WrongException.Quotation()
                    } else {
                        data.value = MaiScoreParam(null, nameOrTitleStr, null, qq, version, difficulty)
                    }
                } else {
                    throw IllegalArgumentException.WrongException.BeatmapID()
                }
            } else {
                if (nameOrTitleStr.matches(Regex(REG_NUMBER_15))) {
                    data.value = MaiScoreParam(nameOrTitleStr.toInt(), null, null, qq, version, difficulty)
                } else if (nameOrTitleStr.contains(Regex(REG_QUOTATION))) {
                    throw IllegalArgumentException.WrongException.Quotation()
                } else {
                    data.value = MaiScoreParam(null, nameOrTitleStr, null, qq, version, difficulty)
                }
            }
        } else {
            throw IllegalArgumentException.WrongException.BeatmapID()
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiScoreParam) {
        val full = getFullScoreOrNull(param.qq, param.name, maimaiApiService)

        val result: MaiSong = if (param.title != null) {
            val title = DataUtil.getStandardisedString(param.title)

            // 标题搜歌模式
            val possibles = maimaiApiService.getMaimaiPossibleSongs(title)

            val r = if (!possibles.isNullOrEmpty()) {
                possibles.associateBy { it.title.getSimilarity(title) }.filter { it.key > 0.4 }.maxByOrNull { it.key }?.value
            } else {
                null
            }

            if (r != null) {
                r
            } else {
                // 外号模式
                val s = maimaiApiService.getMaimaiAliasSong(title)

                // id 也可能是外号
                val i = if (s != null) {
                    maimaiApiService.getMaimaiAlias(s.songID)?.alias
                } else null

                val sy = s?.title.getSimilarity(title) >= 0.4

                val iy = (i?.maxOfOrNull { it.getSimilarity(title) } ?: 0.0) >= 0.4

                if (s != null && (sy || iy)) {
                    s
                } else {
                    throw NoSuchElementException.ResultNotAccurate()
                }
            }
        } else if (param.id != null) { // ID 搜歌模式
            maimaiApiService.getMaimaiSong(param.id.toLong())
                ?: maimaiApiService.getMaimaiAliasSong(param.id.toString()) // 有的歌曲外号叫 3333
                ?: throw NoSuchElementException.Song(param.id)
        } else {
            throw IllegalStateException.ClassCast("舞萌成绩")
        }


        val image: ByteArray = run {

            // 获取符合的成绩
            val scores: List<MaiScore> = full?.records?.filter {
                if (result.isDeluxe) {
                    return@filter it.songID == result.songID.toLong() || it.songID == (result.songID - 10000).toLong()
                } else {
                    return@filter it.songID == result.songID.toLong() || it.songID == (result.songID + 10000).toLong()
                }
            } ?: listOf()

            maimaiApiService.insertSongData(scores)

            val anotherResult: MaiSong? = if (result.isDeluxe) {
                maimaiApiService.getMaimaiSong(result.songID - 10000L)
            } else {
                maimaiApiService.getMaimaiSong(result.songID + 10000L)
            }

            // 只有一种谱面
            if (anotherResult == null) {
                val version = if (result.isDeluxe) DX else SD

                return@run imageService.getPanel(
                    MSPanelParam(user = full?.getUser(), songs = listOf(result), scores = scores, version = version).toMap(),
                    "MS")
            } else if (scores.isNotEmpty() && param.version == ANY) {
                // 有两种谱面，有成绩，没有规定难度。此时取玩家成绩最好的那个
                val isDX = scores.maxBy { it.rating }.isDeluxe

                val songs = listOf(listOf(result, anotherResult).first { it.isDeluxe == isDX })

                return@run imageService.getPanel(
                    MSPanelParam(user = full?.getUser(), songs = songs,
                        scores = scores.filter { it.isDeluxe == isDX }, version = ANY).toMap(),
                    "MS")
            } else {
                // 有两种谱面，但是没有成绩
                val isDX = param.version == DX || param.version == ANY

                val songs = listOf(listOf(result, anotherResult).first { it.isDeluxe == isDX })

                return@run imageService.getPanel(
                    MSPanelParam(user = full?.getUser(), songs = songs, scores = scores.filter { it.isDeluxe == isDX }, version = ANY).toMap(),
                    "MS")
            }
        }

        event.reply(image)
    }

    companion object {
        private fun getFullScoreOrNull(
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
                throw NoSuchElementException.Player()
            }
        }

        // UUMS
        fun getSearchResult(text: String?, maimaiApiService: MaimaiApiService): MessageChain {
            val songs = maimaiApiService.getMaimaiSongLibrary()
            val result = mutableMapOf<Double, MaiSong>()

            for (s in songs.values) {
                val similarity = s.title.getSimilarity(text)

                if (similarity >= 0.4) {
                    maimaiApiService.insertMaimaiAlias(s)
                    result[similarity] = s
                }
            }

            if (result.isEmpty()) {
                throw NoSuchElementException.Result()
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

        enum class Version {
            DX, SD, ANY;

            companion object {
                fun getVersion(string: String?): Version {
                    if (string.isNullOrBlank()) return ANY

                    return when(string.lowercase()) {
                        "sd", "标准", "standard", "标" -> SD
                        "dx", "豪华", "deluxe" -> DX
                        else -> ANY
                    }
                }
            }
        }

        private fun String?.getSimilarity(other: String?): Double {
            return DataUtil.getStringSimilarity(other, this)
        }
    }
}
