package com.now.nowbot.model.filter

import com.now.nowbot.util.command.PATTERN_NAME
import com.now.nowbot.util.command.PATTERN_NUMBER_MORE
import com.now.nowbot.util.command.PATTERN_OPERATOR_WITH_SPACE
import com.now.nowbot.util.command.REGEX_OPERATOR_WITH_SPACE
import org.intellij.lang.annotations.Language

enum class ScoreExtendedFilter(@param:Language("RegExp") val regex: Regex)  {
    SCORE_ID("((score\\s*)?(\\s_)?id|eid|id?|(成绩)(编号|号)?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    USER("(((us|play)ers?)|u|玩家)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NAME)".toRegex()),

    ;

    companion object {
        val regexes: List<Regex> by lazy { ScoreExtendedFilter.entries.map { it.regex } }

        fun getExtend(conditions: List<List<String>>): Pair<Long?, String?> {
            if (conditions.size < 2) return Pair(null, null)

            val scoreID = conditions[0].firstOrNull()?.split(REGEX_OPERATOR_WITH_SPACE)?.lastOrNull()?.toLongOrNull()
            val username = conditions[1].firstOrNull()?.split(REGEX_OPERATOR_WITH_SPACE)?.lastOrNull()?.trim()

            return scoreID to username
        }
    }
}