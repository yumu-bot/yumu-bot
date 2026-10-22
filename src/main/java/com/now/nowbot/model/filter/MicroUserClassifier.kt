package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import java.time.ZoneOffset
import kotlin.math.roundToLong

/**
 * [MicroUserFilter] 的名字形式分类器。
 *
 * 条目顺序与 [MicroUserFilter] 保持一致（末尾的 RANGE 由
 * [com.now.nowbot.util.command.SymbolHead.RANGE] 处理），因此可以按 ordinal / name 与过滤器条目一一对应。
 */
enum class MicroUserClassifier(
    @param:Language("RegExp") override val prefix: Regex,
    override val valueType: FilterValueType,
    @param:Language("RegExp") override val suffix: Regex? = null,
    @param:Language("RegExp") override val valueRegex: Regex? = null,
): CommandClassifier {
    USERNAME("(user|name|username|玩家名称?|玩家|名称?|u|n)".toRegex(), FilterValueType.NAME),

    ID("((user\\s*)?id|uid|(玩家)?编号|id|i)".toRegex(), FilterValueType.INTEGER),

    ACTIVE(
        "(active|活跃|e)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_BOOLEAN$"),
    ),

    BOT(
        "((ro)?bot|机器人?|人机|b)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_BOOLEAN$"),
    ),

    ONLINE(
        "(online|在线|上线|o)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_BOOLEAN$"),
    ),

    DELETE(
        "(deleted?|被?删除|del|d)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_BOOLEAN$"),
    ),

    SUPPORTER(
        "(support(er)?|支持者?|撒泼特|会员|s?vip|v)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_BOOLEAN$"),
    ),

    SUPPORT_LEVEL("(support(er)?\\s*level|(支持者?|撒泼特|会员)等级|s?vip(lv|level)?|vl|sl|v)".toRegex(), FilterValueType.INTEGER),

    LAST_VISIT("(last\\s*visit(\\s*time)?|last|visit|time|seen|上线(时间)?|vt|t)".toRegex(), FilterValueType.TIME),

    PM_ONLY(
        "(pm\\s*only|(好友)?私信|陌生人(私信)?|y)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_BOOLEAN$"),
    ),

    COUNTRY(
        "(country(\\s*(name|code))?|code|国家|地区|c)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^\\w{2,}$"),
    ),

    MUTUAL(
        "(mutual|互相关注|互关|互?粉|mu|m)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_BOOLEAN$"),
    ),

    TEAM("(team|战?队|队伍|团队|公会|tm)".toRegex(), FilterValueType.NAME),

    PP("(performance(\\s*points?)?|表现分?|pp|p)".toRegex(), FilterValueType.INTEGER),

    ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|acc?)".toRegex(), FilterValueType.DECIMAL, "[%％]?".toRegex()),

    COMBO("(combo|连击数?|cb)".toRegex(), FilterValueType.DECIMAL, "[xX]?".toRegex()),

    LEVEL("(levels?|等?级|经验|lv|l)".toRegex(), FilterValueType.INTEGER),

    SSH("(rank\\s*(ss|x)h|rssh|rxh|xh)".toRegex(), FilterValueType.INTEGER),

    SS("(rank\\s*(ss|x)|rss|rx|x)".toRegex(), FilterValueType.INTEGER),

    SH("(rank\\s*sh|rsh|rh)".toRegex(), FilterValueType.INTEGER),

    S("(rank\\s*s|rs|s)".toRegex(), FilterValueType.INTEGER),

    A("(rank\\s*a|ra)".toRegex(), FilterValueType.INTEGER),

    RANKING("((global\\s*)?rank(\\s*ing)?|(全球)?排名|k)".toRegex(), FilterValueType.INTEGER),

    PLAY_COUNT("(play\\s*counts?|(游玩)?次数|pc)".toRegex(), FilterValueType.INTEGER),

    PLAY_TIME("(play\\s*times?|(游玩)?时间|pt)".toRegex(), FilterValueType.INTEGER),

    TOTAL_HITS("(total\\s*hits?|总?击打次?数|tth|th)".toRegex(), FilterValueType.INTEGER),
    ;

    companion object {
        fun Collection<MicroUser>.classify(ctx: StripContext): List<MicroUser> {
            return classify(ctx.syntaxes)
        }

        fun Collection<MicroUser>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): List<MicroUser> {
            return classifyBy(syntaxes, entries) { user, operator, filter, condition ->
                fitUser(user, operator, filter, condition)
            }
        }

        fun <K> Map<K, MicroUser>.classify(ctx: StripContext): Map<K, MicroUser> {
            return classify(ctx.syntaxes)
        }

        fun <K> Map<K, MicroUser>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): Map<K, MicroUser> {
            return classifyValuesBy(syntaxes, MicroUserClassifier.entries) { user, operator, filter, condition ->
                fitUser(user, operator, filter, condition)
            }
        }

        private fun fitUser(
            it: MicroUser,
            operator: Operator,
            filter: MicroUserClassifier,
            condition: Condition,
        ): Boolean {
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

                SUPPORT_LEVEL -> fit(operator, (it.supportLevel ?: (-1).toByte()).toLong(), long)
                PP -> fit(operator, it.statistics?.pp?.roundToLong(), long)
                ACCURACY -> {
                    val acc = when {
                        double > 10000.0 || double <= 0.0 -> throw IllegalArgumentException.WrongException.Henan()
                        double > 100.0 -> double / 10000.0
                        double > 1.0 -> double / 100.0
                        else -> double
                    } // 0-1

                    fit(operator, it.statistics?.accuracy, acc)
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
            }
        }
    }
}
