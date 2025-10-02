package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.filter.ScoreFilter.Companion.fit
import com.now.nowbot.model.filter.ScoreFilter.Companion.fitTime
import com.now.nowbot.model.filter.ScoreFilter.Companion.getBoolean
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import java.time.ZoneOffset
import kotlin.math.roundToLong

enum class MicroUserFilter(@Language("RegExp") val regex: Regex) {
    USERNAME("(user|name|username|玩家名称?|玩家|名称?|u|n)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    ID("((user\\s*)?id|uid|(玩家)?编号|id|i)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    ACTIVE("(active|活跃|e)(?<n>$REG_OPERATOR_WITH_SPACE$REG_BOOLEAN)".toRegex()),

    BOT("((ro)?bot|机器人?|人机|b)(?<n>$REG_OPERATOR_WITH_SPACE$REG_BOOLEAN)".toRegex()),

    ONLINE("(online|在线|上线|o)(?<n>$REG_OPERATOR_WITH_SPACE$REG_BOOLEAN)".toRegex()),

    DELETE("(deleted?|被?删除|del|d)(?<n>$REG_OPERATOR_WITH_SPACE$REG_BOOLEAN)".toRegex()),

    SUPPORTER("(support(er)?|支持者?|撒泼特|会员|s?vip|v)(?<n>$REG_OPERATOR_WITH_SPACE$REG_BOOLEAN)".toRegex()),

    SUPPORT_LEVEL("(support(er)?\\s*level|(支持者?|撒泼特|会员)等级|s?vip(lv|level)?|vl|sl|v)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    LAST_VISIT("(last\\s*visit(\\s*time)?|last|visit|time|seen|上线(时间)?|vt|t)(?<n>$REG_OPERATOR_WITH_SPACE$REG_TIME)".toRegex()),

    PM_ONLY("(pm\\s*only|(好友)?私信|陌生人(私信)?|y)(?<n>$REG_OPERATOR_WITH_SPACE$REG_BOOLEAN)".toRegex()),

    COUNTRY("(country(\\s*(name|code))?|code|国家|地区|c)(?<n>$REG_OPERATOR_WITH_SPACE\\w{2,})".toRegex()),

    MUTUAL("(mutual|互相关注|互关|互?粉|mu|m)(?<n>$REG_OPERATOR_WITH_SPACE$REG_BOOLEAN)".toRegex()),

    // 以下是 statistics 存在时可以匹配的内容

    TEAM("(team|战?队|队伍|团队|公会|tm)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    PP("(performance(\\s*points?)?|表现分?|pp|p)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|acc?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)[%％]?".toRegex()),

    COMBO("(combo|连击数?|cb)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL[xX]?)".toRegex()),

    LEVEL("(levels?|等?级|经验|lv|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SSH("(rank\\s*(ss|x)h|rssh|rxh|xh)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SS("(rank\\s*(ss|x)|rss|rx|x)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SH("(rank\\s*sh|rsh|rh)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    S("(rank\\s*s|rs|s)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    A("(rank\\s*a|ra)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    RANKING("((global\\s*)?rank(\\s*ing)?|(全球)?排名|k)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    PLAY_COUNT("(play\\s*counts?|(游玩)?次数|pc)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    PLAY_TIME("(play\\s*times?|(游玩)?次数|pt)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    TOTAL_HITS("(total\\s*hits?|总?击打次?数|tth|th)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    RANGE(REG_RANGE.toRegex())

    ;

    companion object {
        fun filterUsers(users: List<MicroUser>, conditions: List<List<String>>): List<MicroUser> {
            val s = users.toMutableList()
            val el = entries.toList()

            // 最后一个筛选条件无需匹配
            conditions
                .dropLast(1)
                .forEachIndexed { index, strings ->
                    if (strings.isNotEmpty()) {
                        filterConditions(s, el[index], strings)
                    }
                }

            return s.toList()
        }

        private fun filterConditions(users: MutableList<MicroUser>, filter: MicroUserFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = (c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim()

                users.removeIf { fitUser(it, operator, filter, condition).not() }
            }
        }

        private fun fitUser(it: MicroUser, operator: Operator, filter: MicroUserFilter, condition: String): Boolean {
            val long = condition.toLongOrNull() ?: -1L
            val double = condition.toDoubleOrNull() ?: -1.0
            val boolean = getBoolean(condition)

            // 一般这个数据都很大。如果输入很小的数，会自动给你乘 1k
            val longPlus = if (long in 1..< 100) {
                long * 1000L
            } else {
                long
            }

            return when (filter) {
                USERNAME -> fit(operator, it.username, condition)
                ID -> fit(operator, it.userID, long)
                ACTIVE -> fit(operator, it.isActive, boolean)
                BOT -> fit(operator, it.isBot, boolean)
                ONLINE -> fit(operator, it.isOnline, boolean)
                DELETE -> fit(operator, it.isDeleted, boolean)
                SUPPORTER -> fit(operator, it.isSupporter, boolean)
                LAST_VISIT -> fitTime(operator,
                    it.lastVisitTime?.toEpochSecond(ZoneOffset.ofHours(8)),
                    condition)
                PM_ONLY -> fit(operator, it.pmFriendsOnly, boolean)
                COUNTRY -> if (condition.length <= 2) {
                    fit(operator, it.country?.code, condition)
                } else {
                    fit(operator, it.country?.name, condition)
                }
                MUTUAL -> fit(operator, it.isMutual, boolean)
                TEAM -> if (long >= 0L) {
                    fit(operator, it.team?.id?.toLong(), long)
                            || fit(operator, it.team?.name, condition)
                            || fit(operator, it.team?.shortName, condition)
                } else {
                    fit(operator, it.team?.name, condition)
                            || fit(operator, it.team?.shortName, condition)
                }

                SUPPORT_LEVEL -> fit(operator,
                    (it.supportLevel ?: (-1).toByte()).toLong(), long)

                PP -> fit(operator, it.statistics?.pp?.roundToLong(), longPlus)

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
                PLAY_COUNT -> fit(operator, it.statistics?.playCount, longPlus)
                PLAY_TIME -> fit(operator, it.statistics?.playCount, longPlus)
                TOTAL_HITS -> fit(operator, it.statistics?.playCount, longPlus)
                else -> false
            }
        }
    }
}