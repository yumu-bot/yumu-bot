package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.filter.ScoreFilter.Companion.fit
import com.now.nowbot.model.filter.ScoreFilter.Companion.fitTime
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import java.time.ZoneOffset
import kotlin.math.roundToLong

enum class MicroUserFilter(@param:Language("RegExp") val regex: Regex) {
    USERNAME("(user|name|username|玩家名称?|玩家|名称?|u|n)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NAME)".toRegex()),

    ID("((user\\s*)?id|uid|(玩家)?编号|id|i)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    ACTIVE("(active|活跃|e)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_BOOLEAN)".toRegex()),

    BOT("((ro)?bot|机器人?|人机|b)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_BOOLEAN)".toRegex()),

    ONLINE("(online|在线|上线|o)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_BOOLEAN)".toRegex()),

    DELETE("(deleted?|被?删除|del|d)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_BOOLEAN)".toRegex()),

    SUPPORTER("(support(er)?|支持者?|撒泼特|会员|s?vip|v)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_BOOLEAN)".toRegex()),

    SUPPORT_LEVEL("(support(er)?\\s*level|(支持者?|撒泼特|会员)等级|s?vip(lv|level)?|vl|sl|v)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    LAST_VISIT("(last\\s*visit(\\s*time)?|last|visit|time|seen|上线(时间)?|vt|t)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_TIME)".toRegex()),

    PM_ONLY("(pm\\s*only|(好友)?私信|陌生人(私信)?|y)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_BOOLEAN)".toRegex()),

    COUNTRY("(country(\\s*(name|code))?|code|国家|地区|c)(?<n>$PATTERN_OPERATOR_WITH_SPACE\\w{2,})".toRegex()),

    MUTUAL("(mutual|互相关注|互关|互?粉|mu|m)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_BOOLEAN)".toRegex()),

    // 以下是 statistics 存在时可以匹配的内容

    TEAM("(team|战?队|队伍|团队|公会|tm)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NAME)".toRegex()),

    PP("(performance(\\s*points?)?|表现分?|pp|p)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|acc?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)[%％]?".toRegex()),

    COMBO("(combo|连击数?|cb)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL[xX]?)".toRegex()),

    LEVEL("(levels?|等?级|经验|lv|l)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    SSH("(rank\\s*(ss|x)h|rssh|rxh|xh)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    SS("(rank\\s*(ss|x)|rss|rx|x)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    SH("(rank\\s*sh|rsh|rh)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    S("(rank\\s*s|rs|s)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    A("(rank\\s*a|ra)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    RANKING("((global\\s*)?rank(\\s*ing)?|(全球)?排名|k)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    PLAY_COUNT("(play\\s*counts?|(游玩)?次数|pc)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    PLAY_TIME("(play\\s*times?|(游玩)?次数|pt)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    TOTAL_HITS("(total\\s*hits?|总?击打次?数|tth|th)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    RANGE(PATTERN_RANGE.toRegex())

    ;

    companion object {
        val regexes: List<Regex> by lazy { entries.map { it.regex } }

        fun filterUsers(users: List<MicroUser>, conditions: List<List<String>>): List<MicroUser> {
            val s = users.toMutableList()

            // 最后一个筛选条件无需匹配
            conditions
                .dropLast(1)
                .forEachIndexed { index, strings ->
                    if (strings.isNotEmpty()) {
                        filterConditions(s, entries[index], strings)
                    }
                }

            return s.toList()
        }

        private fun filterConditions(users: MutableList<MicroUser>, filter: MicroUserFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = Condition((c.split(REGEX_OPERATOR_WITH_SPACE).lastOrNull() ?: "").trim())

                users.removeIf { fitUser(it, operator, filter, condition).not() }
            }
        }

        private fun fitUser(it: MicroUser, operator: Operator, filter: MicroUserFilter, condition: Condition): Boolean {
            val long = condition.long
            val double = condition.double
            val boolean = condition.boolean
            val str = condition.condition


            return when (filter) {
                USERNAME -> fit(operator, it.username, str)
                ID -> fit(operator, it.userID, long)
                ACTIVE -> fit(operator, it.isActive, boolean)
                BOT -> fit(operator, it.isBot, boolean)
                ONLINE -> fit(operator, it.isOnline, boolean)
                DELETE -> fit(operator, it.isDeleted, boolean)
                SUPPORTER -> fit(operator, it.isSupporter, boolean)
                LAST_VISIT -> fitTime(operator, it.lastVisitTime?.toEpochSecond(ZoneOffset.UTC), str)
                PM_ONLY -> fit(operator, it.pmFriendsOnly, boolean)
                COUNTRY -> if (str.length <= 2) {
                    fit(operator, it.country?.code, str)
                } else {
                    fit(operator, it.country?.name, str)
                }
                MUTUAL -> fit(operator, it.isMutual, boolean)
                TEAM -> if (long >= 0L) {
                    fit(operator, it.team?.id?.toLong(), long)
                            || fit(operator, it.team?.name, str)
                            || fit(operator, it.team?.shortName, str)
                } else {
                    fit(operator, it.team?.name, str)
                            || fit(operator, it.team?.shortName, str)
                }

                SUPPORT_LEVEL -> fit(operator,
                    (it.supportLevel ?: (-1).toByte()).toLong(), long)

                PP -> fit(operator, it.statistics?.pp?.roundToLong(), long)

                ACCURACY -> {
                    val acc = when {
                        double > 10000.0 || double <= 0.0 -> throw IllegalArgumentException.WrongException.Henan()
                        double > 100.0 -> double / 10000.0
                        double > 1.0 -> double / 100.0
                        else -> double
                    } // 0-1

                    fit(operator, it.statistics?.accuracy, acc, digit = 2, isRound = true, isInteger = true)
                }

                COMBO -> fit(operator, it.statistics?.maxCombo?.toLong(), long)
                LEVEL -> fit(operator, it.statistics?.levelCurrent?.toLong(), long)
                SS -> fit(operator, it.statistics?.countSS?.toLong(), long)
                SSH -> fit(operator, it.statistics?.countSSH?.toLong(), long)
                S -> fit(operator, it.statistics?.countS?.toLong(), long)
                SH -> fit(operator, it.statistics?.countSH?.toLong(), long)
                A -> fit(operator, it.statistics?.countA?.toLong(), long)
                RANKING -> {
                    val rk = if (long == -1L) {
                        Long.MAX_VALUE
                    } else {
                        long
                    }

                    fit(operator, it.statistics?.globalRank ?: Long.MAX_VALUE, rk)
                }
                PLAY_COUNT -> fit(operator, it.statistics?.playCount, long)
                PLAY_TIME -> fit(operator, it.statistics?.playTime, long)
                TOTAL_HITS -> fit(operator, it.statistics?.totalHits, long)
                else -> false
            }
        }
    }
}