package com.now.nowbot.util

import com.now.nowbot.util.command.*
import com.now.nowbot.util.command.MatchLevel.*
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class OfficialInstruction(val pattern: Pattern) {
    // #0 调出帮助
    HELP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("h", "help")
    }),

    // #1 BOT 内部指令
    PING(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ping", "pi")
    }),

    // #2 osu! 成绩指令
    SET_MODE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("set\\s*(game\\s*)?mode", "(game\\s*)?mode", "sm", "mo")
        appendColonCaptureGroup(FLAG_MODE, REG_MODE, prefixLevel = MAYBE)
    }),

    SB_SET_MODE(CommandPatternBuilder.create {
        appendOfficialSBCommandsIgnoreAll("set\\s*(game\\s*)?mode", "(game\\s*)?mode", "sm", "mo")
        appendColonCaptureGroup(FLAG_MODE, REG_MODE, prefixLevel = MAYBE)
    }),

    SCORE_PASS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("p", "pass")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE_PASS_SHOW(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("pw", "pass(es)?\\s*show")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE_PASSES(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ps", "passes")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE_RECENT(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("r", "recent")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE_RECENT_SHOW(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("rw", "recents?\\s*show")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE_RECENTS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("rs", "recents")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    PR_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("pc", "pass\\s*card")
        appendModeQQUIDNameRange()
    }),

    RE_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("rc", "recent\\s*card")
        appendModeQQUIDNameRange()
    }),

    UU_PR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("uu?\\s*(?<pass>(pass(?!s)(?<es>es)?|p)|(?<recent>(recent|r)))")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("s", "score")
        appendModeBIDQQUIDNameMod()
    }),

    UU_SCORE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("uu?\\s*(score|s)")
        appendModeBIDQQUIDNameMod()
    }),

    SCORE_SHOW(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("sw", "scores?\\s*show")
        appendModeBIDQQUIDNameMod()
    }),

    SCORES(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ss", "scores")
        appendModeBIDQQUIDNameMod()
    }),

    BP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("b", "best")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    BP_SHOW(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bw", "bests?\\s*show")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    BPS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bs", "bests")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    TODAY_BP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("t", "today\\s*(bests?|bp)")
        appendModeQQUIDNameRange()
    }),

    BP_FIX(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bf", "(bests?|bp)\\s*fix")
        appendModeQQUIDNameRange()
    }),

    BP_ANALYSIS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ba", "(bests?|bp)\\s*(analys(e|sis))")
        appendModeQQUIDName()
    }),

    UU_BA(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("uu?b?a")
        appendModeQQUIDName()
    }),

    // #3 osu! 玩家指令
    INFO(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("i", "info")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, REG_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    INFO_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("ic", "info\\s*card")
        appendModeQQUIDName()
    }),

    I_MAPPER(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("im", "im?\\s*mapper")
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
        appendModeQQUIDName()
        appendStarCaptureGroup(FLAG_ID, REG_NUMBER, MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    SKILL(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("k", "skills?")
        appendModeQQUID()
        append2Name()
    }),

    SKILL_VS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("kv", "skills?\\s*v(ersu)?s")
        appendModeQQUID()
        append2Name()
    }),

    BADGE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("bd", "badge")
        appendQQUIDName()
    }),

    GUEST_DIFFICULTY(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("guest", "guest\\s*diff(er)?", "gd(er)?")
        appendModeQQUIDNameRange()
    }),

    // #4 osu! 谱面指令
    MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("m", "map", "beatmap")

        appendMode()
        appendBID()
        appendSpace()
        appendCaptureGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_PLUS, MORE)
        appendSpace()

        appendMod()
    }),

    QUALIFIED_MAP(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("q", "qualified")
        appendMode()

        appendSpace()
        appendGroup(MAYBE) {
            append("status=")
            appendSpace()
            appendCaptureGroup("status", "[\\-\\w]", MORE)
        }

        appendSpace()
        appendGroup(MAYBE) {
            append("sort=")
            appendSpace()
            appendCaptureGroup("sort", "[\\-_+a-zA-Z]", MORE)
        }

        appendSpace()
        appendGroup(MAYBE) {
            append("genre=")
            appendSpace()
            appendCaptureGroup("genre", "\\w", MORE)
        }

        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    LEADER_BOARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("leaderboard", "leader", "list", "l")

        appendMode()
        appendBID()
        appendRange()

        appendStarCaptureGroup(FLAG_TYPE, REG_WORD, MORE)
        appendSpace()
    }),

    MAP_MINUS(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("mm", "map\\s*minus")
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

        appendColonCaptureGroup(FLAG_MODE, REG_BID_SID, prefixLevel = MAYBE)
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
        appendColonCaptureGroup("area2", REG_USERNAME, ANY)
    }),

    EXPLORE(
        CommandPatternBuilder.create {
            appendOfficialCommandsIgnoreAll("explore", "exp", "e", "find")
            appendColonCaptureGroup(FLAG_TYPE, REG_ANYTHING_BUT_NO_SPACE, MORE)
            appendSpace()
            appendCaptureGroup(FLAG_ANY, REG_ANYTHING, MORE)
            appendSpace()
            appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, MAYBE)
        }
    ),

    EXPLORE_MOST_PLAYED(
        CommandPatternBuilder.create {
            appendOfficialCommandsIgnoreAll("(explore|exp|e|find)\\s*([mp]|most|played|mp|pm|play)")
            appendColonCaptureGroup(FLAG_TYPE, REG_ANYTHING_BUT_NO_SPACE, MORE)
            appendSpace()
            appendCaptureGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH, MORE)
            appendSpace()
            appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, MAYBE)
        }
    ),

    // #5 osu! 比赛指令

    MU_RATING(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("(?<main>(ra))")
        appendMatchID()
        appendMatchParam()
    }),

    MATCH_NOW(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("mn", "match\\s*now")

        appendMatchID()
        appendMatchParam()
    }),

    // #6 聊天指令
    // ...
    // #7 娱乐指令
    DICE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("d", "dice")
        appendCaptureGroup("number", "-?\\d", ANY)
        appendCaptureGroup("text", REG_ANYTHING, MORE)
    }),

    // #8 辅助指令
    OLD_AVATAR(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("oa", "(old|osu)?\\s*avatar")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    OLD_AVATAR_CARD(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("oc", "old\\s*(avatar\\s*)?card")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    OSU_AVATAR_PROFILE(CommandPatternBuilder.create {
        appendOfficialCommandsIgnoreAll("op")
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
    for (i in OfficialInstruction.entries) {
        if (i != OfficialInstruction.EXPLORE) continue

        println("${i.name}: ${i.pattern.pattern()}")
    }
}