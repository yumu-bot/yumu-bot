package com.now.nowbot.util

import com.now.nowbot.util.command.*
import com.now.nowbot.util.command.MatchLevel.*
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class OfficialInstruction(val pattern: Pattern) {
    // #0 调出帮助
    // 与官方 bot 使用上有差异先不发
    HELP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("h")
    }),

    // #1 BOT 内部指令
    PING(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ping", "pi")
    }),

    // #2 osu! 成绩指令
    SET_MODE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("mode", "mo")
        appendColonCaptureGroup(MAYBE, FLAG_MODE, REG_MODE)
    }),

    SB_SET_MODE(CommandPatternBuilder.create {
        appendOfficialSBCommandsIgnoreAll("mode", "mo")
        appendColonCaptureGroup(MAYBE, FLAG_MODE, REG_MODE)
    }),

    SCORE_PASS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("p")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendGroup(MAYBE) {
            appendSpace(MORE) // 至少需要一个空格区分开来
            appendCaptureGroup("any",
                REG_ANYTHING_BUT_NO_HASH_STARS,
                MORE
            )
        }
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE_PASSES(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ps")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendGroup(MAYBE) {
            appendSpace(MORE) // 至少需要一个空格区分开来
            appendCaptureGroup("any",
                REG_ANYTHING_BUT_NO_HASH_STARS,
                MORE
            )
        }
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE_RECENT(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("r")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendGroup(MAYBE) {
            appendSpace(MORE) // 至少需要一个空格区分开来
            appendCaptureGroup("any",
                REG_ANYTHING_BUT_NO_HASH_STARS,
                MORE
            )
        }
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE_RECENTS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("rs")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendGroup(MAYBE) {
            appendSpace(MORE) // 至少需要一个空格区分开来
            appendCaptureGroup("any",
                REG_ANYTHING_BUT_NO_HASH_STARS,
                MORE
            )
        }
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    PR_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("pc")
        appendModeQQUIDNameRange()
    }),

    RE_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("rc")
        appendModeQQUIDNameRange()
    }),

    SCORE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("s")
        appendModeBIDQQUIDNameMod()
    }),

    SCORES(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ss")
        appendModeBIDQQUIDNameMod()
    }),

    BP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("b")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendGroup(MAYBE) {
            appendSpace(MORE) // 至少需要一个空格区分开来
            appendCaptureGroup(
                "any", REG_ANYTHING_BUT_NO_HASH_STARS, MORE
            )
        }
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    BPS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bs")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendGroup(MAYBE) {
            appendSpace(MORE) // 至少需要一个空格区分开来
            appendCaptureGroup(
                "any", REG_ANYTHING_BUT_NO_HASH_STARS, MORE
            )
        }
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    TODAY_BP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("t")
        appendModeQQUIDNameRange()
    }),

    BP_FIX(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bf")
        appendModeQQUIDName()
    }),

    BP_ANALYSIS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ba")
        appendModeQQUIDName()
    }),

    UU_BA(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ua")
        appendModeQQUIDName()
    }),

    // #3 osu! 玩家指令
    INFO(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("i")
        appendModeQQUIDName()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendSpace()
            appendCaptureGroup(FLAG_DAY, REG_NUMBER, MORE)
        }
    }),

    INFO_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ic")
        appendModeQQUIDName()
    }),

    I_MAPPER(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("im")
        appendQQUIDName()
    }),

    PP_MINUS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("pm")
        appendModeQQUID()
        append2Name()
    }),

    PP_MINUS_VS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("pv")
        appendModeQQUID()
        append2Name()
    }),

    PP_MINUS_LEGACY(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("pl")
        appendModeQQUID()
        append2Name()
    }),

    TEAM(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("tm", "team", "clan")
        appendQQUIDName()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup(
                "team", REG_NUMBER, MORE
            )
        }
    }),

    SKILL(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("skills?", "k")
        appendModeQQUID()
        append2Name()
    }),

    SKILL_VS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("skills?\\s*v(ersu)?s", "kv")
        appendModeQQUID()
        append2Name()
    }),

    BADGE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("badge", "bd")
        appendQQUIDName()
    }),

    GUEST_DIFFICULTY(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("guest", "guest\\s*diff(er)?", "gd(er)?")
        appendModeQQUIDNameRange()
    }),

    // #4 osu! 谱面指令
    MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("m")

        appendMode()
        appendBID()
        appendGroup()
        appendSpace()

        appendCaptureGroup("any", REG_ANYTHING_BUT_NO_PLUS, MORE)
        appendSpace()

        appendMod()
    }),

    QUALIFIED_MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("q")
        appendMode()

        appendGroup(MAYBE) {
            append(REG_HASH)
            appendSpace()
            appendCaptureGroup("status", "[-\\w]", MORE)
        }
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_STAR)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup("sort", "[\\-_+a-zA-Z]", MORE)
        }
        appendSpace()
        appendGroup(MAYBE) {
            appendCaptureGroup("range", REG_NUMBER, MORE)
        }

    }),

    LEADER_BOARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("l")
        appendMode()
        appendBID()
        appendRange()
    }),

    MAP_MINUS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("mm")
        appendMode()
        appendBID()
        appendMod()
        appendGroup(MAYBE) {
            append("[×xX]")
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup("rate", REG_NUMBER_DECIMAL, MORE)
            append("[×xX]")
            appendMatchLevel(MAYBE)
        }
    }),

    NOMINATION(CommandPatternBuilder.create {
        appendOfficialCommandsIgnore(REG_IGNORE_BS, "n")

        appendColonCaptureGroup(MAYBE, "mode", "b", "s")
        appendSpace()
        appendSID()
    }),

    PP_PLUS_MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("pa")
        appendBID()
        appendMod()
    }),

    // px, pp
    PP_PLUS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<function>(px|pp))")
        appendCaptureGroup("area1", REG_USERNAME, ANY)
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("area2", REG_USERNAME, ANY)
        }
    }),

    // #5 osu! 比赛指令

    MU_RATING(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<main>(ra))")
        appendMatchID()
        appendMatchParam()
    }),

    MATCH_NOW(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("mn")

        appendMatchID()
        appendMatchParam()
    }),

    // #6 聊天指令
    // ...
    // #7 娱乐指令
    DICE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("d")
        appendCaptureGroup("number", "-?\\d", ANY)
        appendCaptureGroup("text", REG_ANYTHING, MORE)
    }),

    // #8 辅助指令
    OLD_AVATAR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("oa")
        appendQQID()
        appendUID()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    OSU_AVATAR_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("oc")
    }),

    OVER_SR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("or")
        appendCaptureGroup("SR", REG_NUMBER_DECIMAL)
    }),

    // #9 自定义
    ;

    fun matcher(text: String): Matcher {
        return pattern.matcher(text)
    }
}

// 检查正则
fun main() {
    val test: String = "/i"
    for (i in OfficialInstruction.entries) {
        if (test != null) {
            if (i.pattern.matcher(test).find()) {
                println("${i.name}: ${i.pattern.pattern()}")
            }
        } else {
            println("${i.name}: ${i.pattern.pattern()}")
        }
    }
}