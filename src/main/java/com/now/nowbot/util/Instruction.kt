package com.now.nowbot.util

import com.now.nowbot.util.command.*
import com.now.nowbot.util.command.MatchLevel.*
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class Instruction(val pattern: Pattern) {
    // #0 调出帮助
    HELP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("h")
        appendCaptureGroup("module", REG_ANYTHING, ANY, MAYBE)
    }),

    SIMPLIFIED_HELP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("help", "helps", "帮助")
        appendCaptureGroup("module", REG_ANYTHING, ANY, MAYBE)
    }),

    // #1 BOT 内部指令
    PING(CommandPatternBuilder.create {
        appendGroup {
            appendGroup {
                appendCommandsIgnoreAll("ping", "pi")
            }
            append(CHAR_SEPARATOR)
            appendGroup {
                append("yumu${REG_SPACE}${LEVEL_ANY}${REG_QUESTION}")
            }
        }
    }),

    BIND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<ub>ub)", "(?<bi>bi)", "(?<un>un)?(?<bind>bind)")
        appendColonCaptureGroup("full", "f")
        appendSpace()
        appendQQ()
        appendName()
    }),

    SB_BIND(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(?<ub>ub)", "(?<bi>bi)", "(?<un>un)?(?<bind>bind)")
        appendQQ()
        appendSBName()
    }),

    BAN(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("super", "sp", "operate", "op")
        appendColonCaptureGroup("operate", "((black|white|ban)?list|add|remove|(un)?ban|[lkarubw])", prefixLevel = MAYBE)
        appendSpace()
        appendIgnore()
        appendSpace()
        appendQQ()
        appendQQGroup()
        appendName()
    }),

    SERVICE_SWITCH_ON(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(switch|service)\\s*on", "so")
        appendQQOrQQGroup()
        appendCaptureGroup(FLAG_SERVICE, "[^\\d#＃]", MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_NAME, REG_ANYTHING, MORE)
    }),

    SERVICE_SWITCH_OFF(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(switch|service)\\s*off", "sf")
        appendQQOrQQGroup()
        appendCaptureGroup(FLAG_SERVICE, "[^\\d#＃]", MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_NAME, REG_ANYTHING, MORE)
    }),

    SERVICE_SWITCH_LIST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(switch|service)\\s*list", "sl")
        appendQQOrQQGroup()
        appendCaptureGroup(FLAG_SERVICE, "\\D", MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_NAME, REG_ANYTHING, MORE)
    }),

    ECHO(CommandPatternBuilder.create {
        append("(#echo\\s*|([!！]\\s*(ym)?(echo|e[co])(?![A-Za-z\\-_])))")
        appendSpace()
        appendQQOrQQGroup(true, MORE) {
            appendCaptureGroup(FLAG_ANY, "(?:(?!(?:group[＝=])?\\d{6,10}\\b).)+", EXIST, EXIST)
        }
    }),

    REVOKE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("revoke", "rv", "撤回")
    }),

    SERVICE_COUNT(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("servicecount", "统计服务调用", "sc")
        appendCaptureGroup(FLAG_TIME,
            REG_TIME,
            ANY
        )
    }),

    SYSTEM_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("systeminfo", "sys(tem)?", "si", "sy")
    }),

    CHECK(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("check", "ck")
        appendQQUIDName()
    }),

    GROUP_LIST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("group\\s*list", "gl")
        appendCaptureGroup(FLAG_RANGE,
            REG_NUMBER,
            ANY
        )
    }),


    // #2 osu! 成绩指令
    SET_MODE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("set\\s*(game\\s*)?mode", "(game\\s*)?mode", "sm", "mo")
        appendColonCaptureGroup(FLAG_MODE, REG_MODE, prefixLevel = MAYBE)
        appendSpace()
        appendQQUIDName()
    }),

    SB_SET_MODE(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("set\\s*(game\\s*)?mode", "(game\\s*)?mode", "sm", "mo")
        appendColonCaptureGroup(FLAG_MODE, REG_MODE, prefixLevel = MAYBE)
        appendSpace()
        appendQQUIDSBName()
    }),

    SET_GROUP_MODE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(set)?\\s*group\\s*mode", "gm")
        appendColonCaptureGroup(FLAG_MODE, REG_MODE, prefixLevel = MAYBE)
        appendSpace()
        appendQQ(isDefaultGroup = true)
        appendQQGroup(isDefaultGroup = true)
    }),

    SCORE_PR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p)|(?<recent>(recent|r)))((?<s>s)|(?<w>(show|w)))?")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    RECENT_BEST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("recents?\\s*bests?|rbs?|red\\s*bull")
        appendModeQQUIDNameRange()
    }),

    SB_SCORE_PR(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p)|(?<recent>(recent|r)))((?<s>s)|(?<w>(show|w)))?")
        appendModeQQUIDSBNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    PR_CARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<pass>(passcard|pc))", "(?<recent>(recentcard|rc))")
        appendModeQQUIDNameRange()
    }),

    UU_PR(CommandPatternBuilder.create {
        appendUUIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p)|(?<recent>(recent|r)))")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SCORE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(score|s)")
        appendModeBIDQQUIDNameMod()
    }),

    SCORE_SHOW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(score|s)\\s*(show|w)")
        appendModeBIDQQUIDNameMod()
    }),

    UU_SCORE(CommandPatternBuilder.create {
        appendUUIgnoreAll("score", "s")
        appendModeBIDQQUIDNameMod()
    }),

    SCORES(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("scores", "ss")
        appendModeBIDQQUIDNameMod()
    }),

    SB_SCORE(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(score|s)")
        appendModeBIDQQUIDSBNameMod()
    }),

    SB_SCORE_SHOW(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(score|s)\\s*(show|w)")
        appendModeBIDQQUIDSBNameMod()
    }),

    SB_SCORES(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("scores", "ss")
        appendModeBIDQQUIDSBNameMod()
    }),

    BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(bestperformance|best|bp|b)((?<s>s)|(?<w>show|w))?")
        appendModeQQUIDNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    SB_BP(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(bestperformance|best|bp|b)((?<s>s)|(?<w>show|w))?")
        appendModeQQUIDSBNameRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    TODAY_BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t")
        appendModeQQUIDNameRange()
    }),

    SB_TODAY_BP(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t")
        appendModeQQUIDSBNameRange()
    }),

    BP_FIX(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bpfix", "fixbp", "bestperformancefix", "bestfix", "bpf", "bf", "boy\\s*friends?")
        appendModeQQUIDNameRange()
    }),

    BP_ANALYSIS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bp\\s*analysis", "blue\\s*archive", "bpa", "ba")
        appendModeQQUIDName()
    }),

    BP_ANALYSIS_LEGACY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bp\\s*analysis\\s*legacy", "bpal", "bal", "al")
        appendModeQQUIDName()
    }),

    UU_BA(CommandPatternBuilder.create {
        appendUUIgnoreAll("(bp?)?a", "(bp\\s*analysis)")
        appendModeQQUIDName()
    }),

    // #3 osu! 玩家指令
    INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("information", "info", "i")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, REG_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    SB_INFO(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("information", "info", "i")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, REG_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    WAIFU_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("info(rmation)?s?\\s*(wif[ei]|waifu)", "iw", "waifu", "wife", "i\\s*wanna")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, REG_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    LEGACY_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("info(rmation)?s?\\s*(legacy)", "il")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, REG_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    TEST_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testinformation", "testinfo", "ti")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, REG_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    INFO_CARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("informationcard", "infocard", "ic")
        appendModeQQUIDName()
    }),

    CSV_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(c(sv)?)information", "(c(sv)?)info", "(c(sv)?)i")
        appendMode()
        appendCaptureGroup("data", REG_USERNAME_SEPERATOR, ANY)
    }),

    UU_INFO(CommandPatternBuilder.create {
        appendUUIgnoreAll("info", "i")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, REG_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    I_MAPPER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mapper", "immapper", "imapper", "im")
        appendQQUIDName()
    }),

    FRIEND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("friends?", "fuck", "f")
        appendColonCaptureGroup("sort", REG_ANYTHING_BUT_NO_SPACE, MORE)
        appendQQ()
        appendUID()
        appendNameAndRange()
        appendIgnore(REG_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendIgnore(REG_HYPHEN)
        appendRange()
    }),

    MUTUAL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mutual", "mu")
        appendCaptureGroup("names", REG_USERNAME_SEPERATOR, ANY)
    }),

    PP_MINUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(p?p[m\\-])", "(pp)?minus")
        appendModeQQUID()
        append2Name()
    }),

    PP_MINUS_VS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(p?pv)", "p?pm(inus)?vs")
        appendModeQQUID()
        append2Name()
    }),

    PP_MINUS_LEGACY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(p?pl)", "(pp)?minus\\s*legacy")
        appendModeQQUID()
        append2Name()
    }),

    TEAM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("tm", "team", "clan")
        appendModeQQUIDName()
        appendStarCaptureGroup(FLAG_ID, REG_NUMBER, MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    SKILL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("skills?", "k")
        appendModeQQUID()
        append2Name()
    }),

    SKILL_VS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("skills?\\s*v(ersu)?s", "kv")
        appendModeQQUID()
        append2Name()
    }),

    BADGE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("badge", "bd")
        appendQQUIDName()
    }),

    GUEST_DIFFICULTY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(get)?\\s*guest", "guest\\s*diff(er)?", "gd(er)?")
        appendModeQQUIDNameRange()
    }),

    GET_ID(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*id", "gi")
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    GET_NAME(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*name", "gn")
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    // #4 osu! 谱面指令
    AUDIO(CommandPatternBuilder.create {
        appendCommandsIgnore(REG_IGNORE_BS, "audio", "song", "a")
        appendColonCaptureGroup(FLAG_TYPE, REG_BID_SID, prefixLevel = MAYBE)
        appendSpace()
        appendCaptureGroup(FLAG_ID, REG_NUMBER, MORE, MAYBE)
    }),

    MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("beatmap", "map", "m")

        appendMode()
        appendBID()
        appendSpace()
        appendCaptureGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_PLUS, MORE)
        appendSpace()

        appendMod()
    }),

    QUALIFIED_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("qualified", "qua", "q")
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
        appendCommandsIgnoreAll("leaderboard", "leader", "list", "l")

        appendMode()
        appendBID()
        appendRange()

        appendStarCaptureGroup(FLAG_TYPE, REG_WORD, MORE)
        appendSpace()

        appendMod()
    }),

    LEGACY_LEADER_BOARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("legacy\\s*leaderboard", "legacy\\s*leader", "legacy\\s*list", "love\\s*live", "ll")

        appendMode()
        appendBID()
        appendRange()

        appendStarCaptureGroup(FLAG_TYPE, REG_WORD, MORE)
        appendSpace()

        appendMod()
    }),

    TOP_PLAYS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("top\\s*plays?", "top", "tp")

        appendMode()
        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_12, prefixLevel = MAYBE)
    }),

    MAP_MINUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mapminus", "mm")
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
        appendCommandsIgnore(REG_IGNORE_BS, "(nominat(e|ion)s?|nom|n)")

        appendColonCaptureGroup(FLAG_MODE, REG_BID_SID, prefixLevel = MAYBE)
        appendSpace()
        appendSID()
    }),

    PP_PLUS_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("p?pa", "ppplusmap", "pppmap", "plusmap")
        appendBID()
        appendMod()
    }),

    PP_PLUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<function>(p[px]|pp[pvx]|p?p\\+|(pp)?plus|ppvs|pppvs|(pp)?plusvs))")
        appendCaptureGroup("area1", REG_USERNAME, ANY)
        appendSpace()
        appendColonCaptureGroup("area2", REG_USERNAME, ANY)
    }),

    EXPLORE(
        CommandPatternBuilder.create {
            appendCommandsIgnoreAll("explore", "exp", "e", "find", "search")
            appendColonCaptureGroup(FLAG_TYPE, REG_ANYTHING_BUT_NO_SPACE, MORE)
            appendSpace()
            appendCaptureGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH, MORE)
            appendSpace()
            appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, MAYBE)
        }
    ),

    EXPLORE_MOST_PLAYED(
        CommandPatternBuilder.create {
            appendCommandsIgnoreAll("(explore|exp|e|find)\\s*([mp]|most|played|mp|pm|play)")
            appendColonCaptureGroup(FLAG_TYPE, REG_ANYTHING_BUT_NO_SPACE, MORE)
            appendSpace()
            appendCaptureGroup(FLAG_ANY, REG_ANYTHING_BUT_NO_HASH, MORE)
            appendSpace()
            appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, MAYBE)
        }
    ),

    RECOMMEND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("recommend(ed)?", "rec", "j")
        appendModeQQUIDName()
    }),

    POPULAR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("popular\\s*(group)?", "pu", "pg")
        appendMode()
        appendQQGroup(isDefaultGroup = true)
        appendRange()
    }),

    GET_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*beatmap", "get\\s*map", "ga")
        appendID()
        appendMod()
    }),

    GET_NEWBIE_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*new(bie)?\\s*(beat)?map", "get\\s*map", "gw")
        appendMode()
        appendID()
        appendMod()
    }),

    GET_NEWBIE_SCORE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*new(bie)?\\s*score", "get\\s*score", "gx")
        appendMode()
        appendID()
        appendCaptureGroup("accuracy", "($REG_NUMBER_DECIMAL)", MAYBE)
        appendSpace()
        appendCaptureGroup("combo", "($REG_NUMBER_DECIMAL)", MAYBE)
        appendSpace()
        appendCaptureGroup("pp", "($REG_NUMBER_DECIMAL)", MAYBE)
        appendSpace()
        appendCaptureGroup("rank", "(ssh|ss|sh|[abcdsx])", MAYBE)
        appendSpace()
        appendMod()
    }),

    GET_NEWBIE_PLAYER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*new(bie)?\\s*(user|player)", "get\\s*(user|player)", "gu")
        appendModeQQUIDName()
    }),

    GET_BG(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*background", "get\\s*bg", "gb", "bg")
        appendSpace()
        appendCaptureGroup(FLAG_DATA, REG_NUMBER_SEPERATOR, MORE)
    }),

    GET_COVER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*cover", "gc")
        appendColonCaptureGroup(FLAG_TYPE, REG_COVER, MORE)
        appendSpace()
        appendCaptureGroup(FLAG_DATA, REG_NUMBER_SEPERATOR, MORE)
    }),

    // #5 osu! 比赛指令
    MATCH_LISTENER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("make\\s*love", "(match)?listen(er)?", "ml", "li")

        appendMatchID()
        appendSpace()
        appendCaptureGroup("operate", "info|list|start|stop|end|off|on|[lispefo]")
        appendSpace()
        appendHashCaptureGroup("skip", REG_NUMBER_1_100, contentLevel = MORE, prefixLevel = MAYBE)
        appendIgnore()
    }),

    MU_RATING(CommandPatternBuilder.create {
        append(REG_EXCLAMATION)
        appendSpace()
        append("((?<uu>(u{1,2})(rating|ra))|(?<main>((ym)?rating|((ym)?ra)|(mra))))")
        appendIgnore()
        appendSpace()

        appendMatchID()
        appendMatchParam()
    }),

    SERIES_RATING(CommandPatternBuilder.create {
        append(REG_EXCLAMATION)
        appendSpace()
        appendGroup(MAYBE, "ym")
        appendGroup(
            "(?<uu>(u{1,2})(seriesrating|series|sra|sa))|((ym)?(?<main>(seriesrating|series|sa|sra)))|((ym)?(?<csv>(csvseriesrating|csvseries|csa|cs)))"
        )
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendCaptureGroup("name", REG_ANYTHING, MORE)
            append(REG_HASH)
        }
        appendSpace()
        appendCaptureGroup(FLAG_DATA, "[\\d\\[\\]\\s,，|\\-]", MORE)
        appendSpace()
        appendMatchParam()
    }),

    CSV_MATCH(CommandPatternBuilder.create {
        appendCommands("csvrating", "cra?(?![^s^x\\s])")
        appendCaptureGroup("x", "[xs]")
        appendSpace()
        appendCaptureGroup(FLAG_DATA, REG_NUMBER_SEPERATOR, MORE)
    }),

    MATCH_ROUND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(match)?\\s*rounds?", "ro")
        appendMatchID()
        appendSpace()
        appendCaptureGroup("round", REG_NUMBER, MORE)
        appendSpace()
        appendCaptureGroup("keyword", REG_ANYTHING, MORE) // "[\\w\\s-_ %*()/|\\u4e00-\\u9fa5\\uf900-\\ufa2d]"
    }),

    MATCH_NOW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("monitor\\s*now", "match\\s*now", "mn")
        appendMatchID()
        appendMatchParam()
    }),

    MATCH_RECENT(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("match\\s*recents?", "mr")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_MATCHID, REG_NUMBER_SEPERATOR, MAYBE)
    }),

    MAP_POOL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mappool", "po")
        appendMode()
        appendSpace()
        appendCaptureGroup(FLAG_NAME, REG_ANYTHING, MORE)
    }),

    GET_POOL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("getpool", "gp")
        appendMode()
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendCaptureGroup(FLAG_NAME, REG_ANYTHING, MORE)
            append(REG_HASH)
        }
        appendCaptureGroup(FLAG_DATA, REG_ANYTHING, ANY)
    }),

    CALCULATE_NEWBIE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(calculate|cal|csv)\\s*newbie", "cn")
        appendMode()
        appendCaptureGroup(FLAG_DATA, REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_BID, REG_ANYTHING_BUT_NO_STARS, MORE)
        appendSpace()
        appendStarCaptureGroup(FLAG_SID, REG_NUMBER, MORE)
    }),

    // #6 聊天指令

    BILI_USER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bili\\s*user", "bu")
        appendID()
    }),

    // ...
    // #7 娱乐指令

    DICE(CommandPatternBuilder.create {
        append("($REG_EXCLAMATION|(?<dice>\\d+))\\s*(?i)(ym)?(dice|roll|d(?!${REG_IGNORE}))")
        appendCaptureGroup("number", "$REG_HYPHEN?($REG_NUMBER_DECIMAL)")
        appendSpace()
        appendCaptureGroup(FLAG_TEXT, REG_ANYTHING, MORE)
    }),

    DRAW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("draw", "w")
        appendCaptureGroup("d", REG_NUMBER)
    }),

    // #8 辅助指令
    OLD_AVATAR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(old|osu)?\\s*avatar", "oa", "o")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    OLD_AVATAR_CARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(old|osu)?\\s*(card|chicken)", "oishi", "oc")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    SB_OLD_AVATAR(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(old|osu)?\\s*avatar", "oa", "o")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    SB_OLD_AVATAR_CARD(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(old|osu)?\\s*(card|chicken)", "oc")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    OSU_AVATAR_PROFILE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(old|osu)?\\s*profile", "op")
    }),

    OVER_SR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("over\\s*starrating", "over\\s*rating", "overstar", "oversr", "or")
        appendCaptureGroup("SR", REG_NUMBER_DECIMAL)
    }),

    TRANS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("trans", "tr")
        appendCaptureGroup("a", "[A-G][＃#]?", EXIST)
        appendCaptureGroup("b", REG_NUMBER, EXIST)
    }),

    KITA(CommandPatternBuilder.create {
        appendCommands("kt(?![^x\\s])", "kita")
        appendCaptureGroup("noBG", "x")
        appendIgnore()
        appendSpace()
        appendBID()
        appendMod()
        appendSpace()
        appendCaptureGroup("round", "[\\w\\s]", MORE)
    }),

    TAKE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("tk", "take(\\s*name)?", "claim(\\s*name)?")
        appendQQUIDName()
    }),

    GROUP_STATISTICS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("groupstat(s)?", "groupstatistic(s)?", "统计(超限)?", "gg")
        appendColonCaptureGroup("group", "[nah]|((新人|进阶|高阶)群)", prefixLevel = MAYBE)
    }),

    // #9 自定义
    CUSTOM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("custom", "c")
        appendColonCaptureGroup("operate", REG_WORD, contentLevel = MORE, prefixLevel = MAYBE)
        appendSpace()
        appendColonCaptureGroup("type", REG_WORD, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    TEST_MAP_MINUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testmapminus", "testmm", "tmm", "tn")
        appendMode()
        appendCaptureGroup(FLAG_DATA, REG_NUMBER_SEPERATOR, MORE)
    }),

    TEST_PPM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testppm", "te", "tpm")
        appendModeQQUID()
        append2Name()
    }),

    CSV_PPM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("csvppm", "cm")
        appendMode()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, MORE)
    }),

    TEST_HD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testhd", "th")
        appendMode()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, MORE)
    }),

    TEST_FIX(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testfix", "tf")
        appendMode()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, MORE)
    }),

    TEST_TAIKO_SR_CALCULATE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testtaiko", "tt")
        appendCaptureGroup(FLAG_DATA, "[xo\\s（）()]", MORE)
    }),

    TEST_TYPE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testtype", "ty")
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

    MAP_4D_CALCULATE(CommandPatternBuilder.create {
        appendCommands("cal", "calculate", "cl")
        appendCaptureGroup("type", "ar|od|cs|hp", EXIST, EXIST)
        appendSpace()
        appendCaptureGroup("value", REG_NUMBER_DECIMAL, EXIST, EXIST)
        appendSpace()
        appendMod(plusMust = true)
    }),

    TEST_MATCH_START(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("teststart", "ts")
        appendID()
        appendSpace()
        appendCaptureGroup("round", REG_NUMBER, MORE)
    }),

    EASTER_AYACHI_NENE(CommandPatternBuilder.create {
        append("(?<nene>0d0(0)?)")
    }),

    EASTER_WHAT(CommandPatternBuilder.create {
        append(REG_EXCLAMATION)
        appendMatchLevel(MORE)
        appendSpace()
        append("(gsm|干什么)")
        appendSpace()
        append(REG_EXCLAMATION)
        appendMatchLevel(ANY)
        appendIgnore()
    }),

    // #10 辅助指令

    REFRESH_HELP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("refresh\\s*help", "rh")
    }),

    REFRESH_FILE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("refresh\\s*file", "rf")
        appendCaptureGroup("bid", REG_NUMBER, MORE)
    }),

    UPDATE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("update", "ut", "ue")
        appendColonCaptureGroup(FLAG_ANY, REG_ANYTHING, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    FETCH(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("fetch", "fh", "fe")
        appendColonCaptureGroup(FLAG_ANY, REG_ANYTHING, contentLevel = MORE, prefixLevel = MAYBE)
    }),



    // #11 maimai & CHUNITHM

    MAI_SCORE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(score|song)", "ms")
        appendColonCaptureGroup(FLAG_DIFF, REG_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendQQ()
        appendSpace()
        appendCaptureGroup(FLAG_VERSION, REG_MAI_CABINET)
        appendSpace()
        appendNameAnyButNoHash()
        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100)
    }),

    MAI_VERSION(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*version", "mv")
        appendColonCaptureGroup(FLAG_DIFF, REG_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendQQ()
        appendNameAnyButNoHash()
        appendHashCaptureGroup(FLAG_VERSION, REG_ANYTHING, contentLevel = MORE, prefixLevel = MAYBE)
    }),
    MAI_SEEK(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(seek)", "mk")
        appendCaptureGroup(FLAG_NAME, REG_ANYTHING, MORE)
    }),

    MAI_DIST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*dist(ribution)?", "md", "mark\\s*down")
        appendQQ()
        appendNameAnyButNoHash()
    }),

    /*
    MAI_SEARCH(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*search", "mh")
        appendQQ()
        appendNameAnyButNoHash()
    }),

     */

    MAI_FIND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*find", "mf", "mother\\s*fucker", "mai\\s*song")
        appendColonCaptureGroup(FLAG_DIFF, REG_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendNameAnyButNoHash()
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, MAYBE)
    }),

    MAI_AP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(((all\\s?)?perfect)|(ap))", "xp")
        appendQQ()
        appendNameAnyButNoHash()
        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    MAI_FC(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*((full\\s?combo)|(fc))", "xc")
        appendQQ()
        appendNameAnyButNoHash()
        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    MAI_AUDIO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(audio)", "xa")
        appendColonCaptureGroup(FLAG_DIFF, REG_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendNameAnyButNoHash()
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, MAYBE)
    }),

    // 必须放在其他 mai 指令后面
    MAI_BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(best|best50|b50)?(?!(full\\s?combo)|(fc)|(all\\s?)?perfect|(ap)|find|dist(ribution)?|seek|version|score|song)", "mb", "x")
        appendQQ()
        appendNameAnyButNoHash()
        appendRange()
    }),

    CHU_BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("chu(nithm)?\\s*(best|best40|b40)?", "cb", "y")
        appendQQ()
        appendNameAnyButNoHash()
        appendRange()
    }),

    PLAY_STATISTIC(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("ppst")
    }), ;

    fun matcher(input: CharSequence): Matcher = this.pattern.matcher(input)
}

// 检查正则
fun main() {
    for (i in Instruction.entries) {
        if (i != Instruction.ECHO) continue

        println("${i.name}: ${i.pattern.pattern()}")
    }
}