package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreFilter.Companion.fit
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language

enum class MostPlayedBeatmapFilter(@param:Language("RegExp") val regex: Regex) {
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    GUEST("((gd(er)?|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    SOURCE("(source|src|from|f|o|sc|来?源)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|d|难度名?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    STAR("(star|rating|sr|r|星数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)$REG_STAR$LEVEL_MAYBE".toRegex()),

    LENGTH("(length|drain|time|长度|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE($REG_COLON$REG_NUMBER_MORE)?)".toRegex()),

    // GENERAL("(general|bool(ean)?|常规?|总览?|布尔值?|值|e)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    MODE("(mode|模式?|m)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MODE)".toRegex()),

    CATEGORY("(type|category|status|类型?|[分种]类|状态?|v|z|$REG_STAR)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    GENRE("(genre|g|曲?风|风格|流派?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    LANGUAGE("(languages?|l|曲?风|风格|流派?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    // 独有
    COUNT("(counts?|n|游玩(次数)?|游|玩|次数)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),


    RANGE(REG_RANGE.toRegex());

    companion object {

        fun filterMostPlayBeatmaps(beatmaps: List<Beatmap>, conditions: List<List<String>>): List<Beatmap> {
            val s = beatmaps.toMutableList()
            val el = MostPlayedBeatmapFilter.entries.toList()

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

        private fun filterConditions(beatmaps: MutableList<Beatmap>, filter: MostPlayedBeatmapFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = Condition((c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim())

                beatmaps.removeIf { fitMostPlayedBeatmap(it, operator, filter, condition).not() }
            }
        }

        private fun fitMostPlayedBeatmap(b: Beatmap, operator: Operator, filter: MostPlayedBeatmapFilter, condition: Condition): Boolean {
            val long = condition.long
            val double = condition.double
            val seconds = condition.seconds
            val str = condition.condition

            val s = b.beatmapset!!

            return when (filter) {
                CREATOR -> fit(operator, s.creator, str)
                GUEST -> {
                    if (long <= 0) throw RuntimeException("抱歉，本功能暂时只能输入第一位客串谱师的编号 (UID)！")

                    fit(operator, b.mapperID, long)
                }

                BID -> fit(operator, b.beatmapID, long)
                SID -> fit(operator, s.beatmapsetID, long)
                TITLE -> (fit(operator, s.title, str) || fit(operator, s.titleUnicode, str))
                ARTIST -> (fit(operator, s.artist, str) || fit(operator, s.artistUnicode, str))
                SOURCE -> fit(operator, s.source, str)
                ANY -> fit(operator, s.title, str)
                        || fit(operator, s.titleUnicode, str)
                        || fit(operator, s.artist, str)
                        || fit(operator, s.artistUnicode, str)
                        || fit(operator, s.source, str)
                DIFFICULTY -> fit(operator, b.difficultyName, str)
                STAR -> fit(operator, b.starRating, double, digit = 2, isRound = false, isInteger = true)
                LENGTH -> {
                    val compare = b.totalLength.toLong()

                    val to = seconds.second.inWholeSeconds

                    fit(operator, compare, to)
                }
                MODE -> fit(operator, b.mode.modeValue.toInt(), OsuMode.getMode(str).modeValue.toInt())
                CATEGORY -> fit(operator, DataUtil.getStatus(b.status), DataUtil.getStatus(str))
                GENRE -> fit(operator, s.genreID.toInt(), DataUtil.getGenre(str)?.toInt())
                LANGUAGE -> fit(operator, s.languageID.toInt(), DataUtil.getLanguage(str)?.toInt())
                COUNT -> fit(operator, b.currentPlayCount?.toLong(), long)
                else -> false
            }
        }
    }

}