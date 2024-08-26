package com.now.nowbot.util

import com.now.nowbot.util.command.*
import com.now.nowbot.util.command.MatchLevel.*
import com.now.nowbot.util.command.CommandPatternBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class OfficialInstruction(val pattern: Pattern) {
    // #0 调出帮助
    // 与官方 bot 使用上有差异先不发
    HELP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("help")
    }),

    // 没处理编码暂时不发音频
    AUDIO(CommandPatternBuilder.create {
        appendOfficialCommandsIgnore(REG_IGNORE_BS, "a")
        appendColonCaptureGroup(MAYBE, "type", "b", "s")
        appendSpace()
        appendCaptureGroup(FLAG_ID, REG_NUMBER, MORE, MAYBE)
    }),

    // #1 BOT 内部指令
    // 官方 bot 有指令自动填充, 长点没事, 而且一个功能不要多指令
    PING(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ping")
    }),

    //bind 不在这里绑定
    BIND(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<ub>ub)", "(?<bi>bi)")
        appendColonCaptureGroup("full", "f")
        appendQQID()
        appendName()
    }),

    // #2 osu! 成绩指令
    SET_MODE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("setmode")
        appendColonCaptureGroup(MAYBE, FLAG_MODE, REG_MODE)
    }),

    SCORE_PR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("pass")
        appendModeQQUIDNameRange()
    }),

    SCORE_PRS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("passall")
        appendModeQQUIDNameRange()
    }),

    SCORE_RE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("recent")
        appendModeQQUIDNameRange()
    }),

    SCORE_RES(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("recentall")
        appendModeQQUIDNameRange()
    }),

    PR_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("passcard")
        appendModeQQUIDNameRange()
    }),

    RE_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("recentcard")
        appendModeQQUIDNameRange()
    }),

    SCORE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("score")
        appendModeBIDQQUIDNameMod()
    }),

    BP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bp")
        appendModeQQUIDNameRange()
    }),

    TODAY_BP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("todaybp")
        appendModeQQUIDNameRange()
    }),

    BP_FIX(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bpfix")
        appendModeQQUIDName()
    }),

    BP_ANALYSIS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bpanalysis")
        appendModeQQUIDName()
    }),

    // #3 osu! 玩家指令
    INFO(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("info")
        appendModeQQUIDName()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendMatchLevel(EXIST)
            appendCaptureGroup(FLAG_DAY, REG_NUMBER)
        }
    }),

    INFO_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("infocard")
        appendModeQQUIDName()
    }),

    I_MAPPER(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("imapper")
        appendQQUIDName()
    }),

    PP_MINUS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ppm")
        appendMode()
        appendCaptureGroup("area1", REG_USERNAME, ANY)
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("area2", REG_USERNAME, MORE)
        }
    }),

    PP_MINUS_VS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ppmvs")
        appendMode()
        appendCaptureGroup("area1", REG_USERNAME, MORE)
        appendGroup(MAYBE) {
            // ':' 与前面 mode 冲突
            append(REG_HASH)
            appendSpace()
            appendCaptureGroup("area2", REG_USERNAME, MORE)
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
            appendCaptureGroup("miss", REG_NUMBER, MORE)
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
        appendSID()
    }),

    PP_PLUS_MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("p?pa")
        appendBID()
        appendMod()
    }),

    PP_PLUS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<function>(p[px]|pp[pvx]|p?p\\+))")
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

    OVER_SR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("or")
        appendCaptureGroup("SR", REG_NUMBER_DECIMAL)
    }),

    // #9 自定义
    ;

    fun matcher(text:String):Matcher {
        return pattern.matcher(text)
    }
}

// 检查正则
fun main() {
    val test: String? = "/ymppmvs 2654 :aaaadddd"
    for (i in OfficialInstruction.values()) {
        if (test != null) {
            if (i.pattern.matcher(test).find()) {
                println("${i.name}: ${i.pattern.pattern()}")
            }
        } else {
            println("${i.name}: ${i.pattern.pattern()}")
        }
    }
}