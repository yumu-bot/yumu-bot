package com.now.nowbot.util

import com.now.nowbot.util.command.*
import com.now.nowbot.util.command.MatchLevel.*
import com.now.nowbot.util.command.CommandPatternBuilder
import java.util.regex.Pattern

enum class OfficialInstruction(val pattern: Pattern) {
    // #0 调出帮助
    HELP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("h")
        appendCaptureGroupColonAndContentAreMathLevel("module", REG_ANYTHING, ANY, MAYBE)
    }),

    AUDIO(CommandPatternBuilder.create {
        appendOfficialCommandsIgnore(REG_IGNORE_BS, "a")
        appendColonCaptureGroupColonIsMatchLevel(MAYBE, "type", "b", "s")
        appendSpace()
        appendCaptureGroupColonAndContentAreMathLevel(FLAG_ID, REG_NUMBER, MORE, MAYBE)
    }),

    // #1 BOT 内部指令
    PING(CommandPatternBuilder.create {
        appendGroup {
            appendOfficialCommandsIgnoreAll("pi")
        }
        append(CHAR_SEPARATOR)
        appendGroup {
            append("yumu${REG_SPACE}${LEVEL_ANY}${REG_QUESTION}")
        }
    }),

    BIND(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<ub>ub)", "(?<bi>bi)")
        appendColonCaptureGroup("full", "f")
        appendQQID()
        appendName()
    }),

    // #2 osu! 成绩指令
    SET_MODE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("sm", "mo")
        appendColonCaptureGroupColonIsMatchLevel(MAYBE, FLAG_MODE, REG_MODE)
    }),

    SCORE_PR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<pass>(p)|(?<recent>(r)))(?<s>s)?")
        appendModeQQUIDNameRange()
    }),

    PR_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<pass>(pc))", "(?<recent>(rc))")
        appendModeQQUIDNameRange()
    }),

    SCORE(CommandPatternBuilder.create {
        appendGroup {
            append(CHAR_SLASH)
            append("(?<score>s)")
            appendIgnore()
        }
        appendModeBIDQQUIDNameMod()
    }),

    BP(CommandPatternBuilder.create {
        appendGroup {
            append(CHAR_SLASH)
            append("(?<bp>(b))(?<s>s)?")
            appendIgnore()
        }
        appendModeQQUIDNameRange()
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

    // #3 osu! 玩家指令
    INFO(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("i")
        appendModeQQUIDName()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendMatchLevel(EXIST)
            appendCaptureGroup(FLAG_DAY, REG_NUMBER)
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
        appendOfficialCommandsIgnoreAll("(?<function>(p?p[mv\\-]))")
        appendMode()
        appendCaptureGroupContentIsMathLevel("area1", REG_USERNAME, ANY)
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroupContentIsMathLevel("area2", REG_USERNAME, MORE)
        }
    }),

    // #4 osu! 谱面指令
    MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("m")

        appendMode()
        appendBID()

        appendGroup(MAYBE) {
            append("[a%]?")
            appendCaptureGroup("accuracy", REG_NUMBER_DECIMAL)
            append("[a%]?")
        }
        appendSpace()
        appendGroup(MAYBE) {
            append("[cx]?")
            appendCaptureGroup("combo", REG_NUMBER_DECIMAL)
            append("[cx]?")
        }
        appendSpace()
        appendGroup(MAYBE) {
            append("[\\-m]?")
            appendCaptureGroupContentIsMathLevel("miss", REG_NUMBER, MORE)
            append("[\\-m]?")
        }
        appendSpace()

        appendMod()
    }),

    QUALIFIED_MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("q")
        appendMode()

        appendGroup(MAYBE) {
            append(REG_HASH)
            appendSpace()
            appendCaptureGroupContentIsMathLevel("status", "[-\\w]", MORE)
        }
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_STAR)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroupContentIsMathLevel("sort", "[\\-_+a-zA-Z]", MORE)
        }
        appendSpace()
        appendGroup(MAYBE) {
            appendCaptureGroupContentIsMathLevel("range", REG_NUMBER, MORE)
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
        appendID()
        appendMod()
        appendGroup(MAYBE) {
            append("[×xX]")
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroupContentIsMathLevel("rate", REG_NUMBER_DECIMAL, MORE)
            append("[×xX]")
            appendMatchLevel(MAYBE)
        }
    }),

    NOMINATION(CommandPatternBuilder.create {
        appendOfficialCommandsIgnore(REG_IGNORE_BS, "n")

        appendColonCaptureGroupColonIsMatchLevel(MAYBE, "mode", "b", "s")
        appendSID()
    }),

    PP_PLUS_MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("p?pa")
        appendBID()
        appendMod()
    }),

    PP_PLUS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<function>(p[px]|pp[pvx]|p?p\\+))")
        appendCaptureGroupContentIsMathLevel("area1", REG_USERNAME, ANY)
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroupContentIsMathLevel("area2", REG_USERNAME, ANY)
        }
    }),

    // #5 osu! 比赛指令

    MU_RATING(CommandPatternBuilder.create {
        append(CHAR_SLASH)
        append("ym")
        append("(?<main>(ra))")
        appendIgnore()
        appendSpace()
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
        appendCaptureGroupContentIsMathLevel("number", "-?\\d", ANY)
        appendCaptureGroupContentIsMathLevel("text", REG_ANYTHING, MORE)
    }),

    // #8 辅助指令
    OLD_AVATAR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("oa")
        appendQQID()
        appendUID()
        appendCaptureGroupContentIsMathLevel(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    OVER_SR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("or")
        appendCaptureGroup("SR", REG_NUMBER_DECIMAL)
    }),

    // #9 自定义
}

// 检查正则
fun main() {
    for (i in OfficialInstruction.values()) {
        println("${i.name}: ${i.pattern.pattern()}")
    }
}