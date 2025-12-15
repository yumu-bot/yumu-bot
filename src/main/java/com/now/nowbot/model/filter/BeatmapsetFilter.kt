package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreFilter.Companion.fit
import com.now.nowbot.model.filter.ScoreFilter.Companion.fitTime
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language

// 完全版本的 beatmapset
enum class BeatmapsetFilter(@param:Language("RegExp") val regex: Regex) {

    CREATOR("(creator|host|c|谱师|作者|谱|主)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    GUEST("((gder|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    SOURCE("(source|src|from|f|o|sc|se|来?源)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    TAG("(tags?|ta|tg|w|标签?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    GENRE("(genre|g|曲?风|风格|流派?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    LANGUAGE("(languages?|l|曲?风|风格|流派?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|d|难度名?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    STAR("(star|rating|sr|r|星数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)$REG_STAR$LEVEL_MAYBE".toRegex()),

    MODE("(mode|模式?|m)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MODE)".toRegex()),

    CATEGORY("(type|category|status|类型?|[分种]类|状态?|v|z|$REG_STAR)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    AR("(ar|approach\\s*(rate)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    CS("(cs|circle\\s*(size)?|keys?|键)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    OD("(od|overall\\s*(difficulty)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    HP("(hp|health\\s*(point)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    LENGTH("(length|drain|long|duration|长度|时?长|lh|h)(?<n>$REG_OPERATOR_WITH_SPACE$REG_TIME)".toRegex()),

    BPM("(bpm|曲速|速度|bm)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc|圆圈?|米)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SLIDER("(slider?s?|sl|long(note)?|lns?|[滑长]?条|长键|面)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SPINNER("(spin(ner)?s?|rattle|sp|转盘|[转盘])(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    TOTAL("(total|all|ttl|(hit)?objects?|tt|物件数?|总数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    CREATED_TIME("((created)?\\s*(at|time)|creat(ed)?\\s*(at|time)?|创建(时间)?|ct|ca)(?<n>$REG_OPERATOR_WITH_SPACE$REG_TIME)".toRegex()),

    RANKED_TIME("((ranked)?\\s*(at|time)|rank(ed)?\\s*(at|time)?|上架(时间)?|rt|ra)(?<n>$REG_OPERATOR_WITH_SPACE$REG_TIME)".toRegex()),

    PLAY_COUNT("(play\\s*counts?|游玩(次数)?|pc)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    FAVOURITE("(favou?rites?|favou?r|fav|fv|收藏(次数)?|收|藏)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    RANGE(REG_RANGE.toRegex());

    companion object {

        fun filterBeatmapsets(beatmapsets: List<Beatmapset>, conditions: List<List<String>>): List<Beatmapset> {

            val s = beatmapsets.toMutableList()
            val el = BeatmapsetFilter.entries.toList()

            // 最后一个筛选条件无需匹配
            conditions
                .dropLast(1)
                .forEachIndexed { index, strings ->
                    if (strings.isNotEmpty()) {
                        filterConditions(s, el[index], strings)
                    }
                }

            return s
        }

        private fun filterConditions(beatmapsets: MutableList<Beatmapset>, filter: BeatmapsetFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = Condition((c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim())

                beatmapsets.removeIf { fitBeatmapsets(it, operator, filter, condition).not() }
            }
        }


        private fun fitBeatmapsets(s: Beatmapset, operator: Operator, filter: BeatmapsetFilter, condition: Condition): Boolean {
            val long = condition.long
            val double = condition.double
            val time = condition.time
            val str = condition.condition

            // 一般这个数据都很大。如果输入很小的数，会自动给你乘 1k
            val longPlus = if (long in 1..< 100) {
                long * 1000L
            } else {
                long
            }

            val bs = s.beatmaps ?: emptyList()

            return when(filter) {
                CREATOR -> fit(operator, s.creator, str)
                GUEST -> {
                    if (long <= 0) throw RuntimeException("抱歉，本功能暂时只能输入第一位客串谱师的编号 (UID)！")
                    
                    bs.map { 
                        fit(operator, it.mapperID, long) 
                    }.toSet().contains(true)
                }
                
                BID -> bs.map {
                    fit(operator, it.beatmapID, long)
                }.toSet().contains(true)
                SID -> fit(operator, s.beatmapsetID, long)
                TITLE -> fit(operator, s.title, str) || fit(operator, s.titleUnicode, str)
                ARTIST -> fit(operator, s.artist, str) || fit(operator, s.artistUnicode, str)
                SOURCE -> fit(operator, s.source, str)
                TAG -> {
                    if (s.tags.isBlank()) {
                        return false
                    }

                    // 使用并行流
                    s.tags.split("\\s+".toRegex())
                        .filter { tag -> tag.isNotEmpty() }
                        .map { tag -> if (tag.contains('_')) tag.replace("_", "") else tag }
                        .parallelStream()  // 并行处理
                        .anyMatch { tag ->
                            fit(operator, tag, str)
                        }
                }
                
                ANY -> {
                    // 使用并行流
                    val ts = s.tags.split("\\s+".toRegex())
                        .filter { tag -> tag.isNotEmpty() }
                        .map { tag -> if (tag.contains('_')) tag.replace("_", "") else tag }
                        .parallelStream()  // 并行处理
                        .anyMatch { tag ->
                            fit(operator, tag, str)
                        }

                    ts || fit(operator, s.title, str) || fit(operator, s.titleUnicode, str) || fit(operator, s.artist, str) || fit(operator, s.artistUnicode, str) || fit(operator, s.source, str)
                }
                GENRE -> fit(operator, s.genreID.toInt(), DataUtil.getGenre(str)?.toInt() ?: return false)
                LANGUAGE -> fit(operator, s.languageID.toInt(), DataUtil.getLanguage(str)?.toInt() ?: return false)
                DIFFICULTY -> bs.map {
                    fit(operator, it.difficultyName, str)
                }.toSet().contains(true)
                STAR -> bs.map {
                    fit(operator, it.starRating, double, digit = 2, isRound = false, isInteger = true)
                }.toSet().contains(true)
                MODE -> bs.map {
                    fit(operator, it.modeInt!!, OsuMode.getMode(str).modeValue)
                }.toSet().contains(true)
                CATEGORY -> bs.map {
                    fit(operator, it.ranked, DataUtil.getStatusIndex(str) ?: return false)
                }.toSet().contains(true)
                AR -> bs.map {
                    fit(operator, it.ar, double, 2, isRound = true, isInteger = true)
                }.toSet().contains(true)
                CS -> bs.map {
                    fit(operator, it.cs, double, 2, isRound = true, isInteger = true)
                }.toSet().contains(true)
                OD -> bs.map {
                    fit(operator, it.od, double, 2, isRound = true, isInteger = true)
                }.toSet().contains(true)
                HP -> bs.map {
                    fit(operator, it.hp, double, 2, isRound = true, isInteger = true)
                }.toSet().contains(true)
                LENGTH -> {
                    val to = time.second.inWholeSeconds

                    bs.map {
                        fit(operator, it.totalLength.toLong(), to)
                    }.toSet().contains(true)
                }
                BPM -> bs.map {
                    fit(operator, it.bpm.toDouble(), double, 0, isRound = true, isInteger = true)
                }.toSet().contains(true)
                CIRCLE -> bs.map {
                    fit(operator, it.circles!!.toLong(), long, 0, isRound = true, isInteger = true)
                }.toSet().contains(true)
                SLIDER -> bs.map {
                    fit(operator, it.sliders!!.toLong(), long, 0, isRound = true, isInteger = true)
                }.toSet().contains(true)
                SPINNER -> bs.map {
                    fit(operator, it.spinners!!.toLong(), long, 0, isRound = true, isInteger = true)
                }.toSet().contains(true)
                TOTAL -> {
                    bs.map {
                        val total = it.totalNotes

                        if (total == 0) {
                            false
                        } else {
                            fit(operator, total, long)
                        }
                    }.toSet().contains(true)
                }
                CREATED_TIME -> fitTime(operator, s.submittedDate.toEpochSecond(), time)
                RANKED_TIME -> fitTime(operator, s.rankedDate?.toEpochSecond() ?: return false, time)
                PLAY_COUNT -> fit(operator, s.playCount, longPlus)
                FAVOURITE -> fit(operator, s.favouriteCount, long)

                else -> false
            }


        }

    }
}