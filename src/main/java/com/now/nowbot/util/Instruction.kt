package com.now.nowbot.util

import com.now.nowbot.util.command.*
import com.now.nowbot.util.command.MatchLevel.*
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class Instruction(val pattern: Pattern) {
    // #0 调出帮助
    HELP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("help", "helps", "帮助", "h")
        appendCaptureGroup("module", REG_ANYTHING, ANY, MAYBE)
    }),

    AUDIO(CommandPatternBuilder.create {
        appendCommandsIgnore(REG_IGNORE_BS, "audio", "song", "a")
        appendColonCaptureGroup(MAYBE, "type", "bid", "b", "sid", "s")
        appendSpace()
        appendCaptureGroup(FLAG_ID, REG_NUMBER, MORE, MAYBE)
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
        appendQQID()
        appendName()
    }),

    SB_BIND(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(?<ub>ub)", "(?<bi>bi)", "(?<un>un)?(?<bind>bind)")
        appendQQID()
        appendName()
    }),

    BAN(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("super", "sp", "operate", "op")
        appendColonCaptureGroup(
            MAYBE, "operate", "(black|white|ban)?list", "add", "remove", "(un)?ban", "[lkarubw]"
        )
        appendIgnore()
        appendSpace()
        appendQQID()
        appendQQGroup()
        appendName()
    }),

    SERVICE_SWITCH(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("switch", "sw")
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup(FLAG_QQ_GROUP, REG_NUMBER, MORE)
        }
        appendSpace()
        appendCaptureGroup("service", REG_WORD, MORE)
        appendSpace()
        appendCaptureGroup("operate", REG_WORD, MORE)
    }),

    ECHO(CommandPatternBuilder.create {
        append("#echo ")
        appendCaptureGroup("any", ".*", EXIST, EXIST)
    }),

    SERVICE_COUNT(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("servicecount", "统计服务调用", "sc")

        appendGroup(MAYBE) {
            appendCaptureGroup("days", REG_NUMBER, MORE)
            append('d')
        }
        appendSpace()
        appendGroup(MAYBE) {
            appendCaptureGroup("hours", REG_NUMBER, MORE)
            append('h')
            appendMatchLevel(MAYBE)
        }
    }),

    SYSTEM_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("systeminfo", "sys(tem)?", "si", "sy")
    }),

    CHECK(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("check", "ck")
        appendQQUIDName()
    }),

    // #2 osu! 成绩指令
    SET_MODE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("setmode", "mode", "sm", "mo")
        appendColonCaptureGroup(MAYBE, FLAG_MODE, REG_MODE)
        appendSpace()
        appendQQUIDName()
    }),

    SB_SET_MODE(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("setmode", "mode", "sm", "mo")
        appendColonCaptureGroup(MAYBE, FLAG_MODE, REG_MODE)
        appendSpace()
        appendQQUIDName()
    }),

    SET_GROUP_MODE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(set)?group\\s*mode", "gm")
        appendColonCaptureGroup(MAYBE, FLAG_MODE, REG_MODE)
        appendQQID(maybe = true)
        appendQQGroup(maybe = true)
    }),

    GROUP_LIST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("group\\s*list", "gl")
        appendCaptureGroup(FLAG_RANGE,
            REG_NUMBER,
            ANY
        )
    }),

    SCORE_PR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p)|(?<recent>(recent|r)))(?<s>s)?")
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

    SB_SCORE_PR(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p)|(?<recent>(recent|r)))(?<s>s)?")
        appendModeQQUIDNameRange()
    }),

    PR_CARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<pass>(passcard|pc))", "(?<recent>(recentcard|rc))")
        appendModeQQUIDNameRange()
    }),

    UU_PR(CommandPatternBuilder.create {
        appendUUIgnoreAll("(?<pass>(pass|p))", "(?<recent>(recent|r))")
        appendModeQQUIDNameRange()
    }),

    SCORE(CommandPatternBuilder.create {
        appendGroup {
            append(REG_EXCLAMATION)
            appendSpace()
            append("(?<score>(ym)?(score|s))")
            appendIgnore()
        }
        appendModeBIDQQUIDNameMod()
    }),

    SCORES(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("scores", "ss")
        appendModeBIDQQUIDNameMod()
    }),

    BP(CommandPatternBuilder.create {
        appendGroup {
            append(REG_EXCLAMATION)
            appendSpace()
            append("(?<bp>(ym)?(bestperformance|best|bp|b))(?<s>s)?")
            appendIgnore()
        }
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

    TODAY_BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t")
        appendModeQQUIDNameRange()
    }),

    BP_FIX(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bpfix", "fixbp", "bestperformancefix", "bestfix", "bpf", "bf", "boy\\s*friends?")
        appendModeQQUIDName()
    }),

    BP_ANALYSIS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bp\\s*analysis", "blue\\s*archive", "bpa", "ba")
        appendModeQQUIDName()
    }),

    BP_ANALYSIS_LEGACY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bp\\s*analysis\\s*legacy", "bpal", "bal", "al")
        appendModeQQUIDName()
    }),

    BP_QUERY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bp\\s*query", "bq")
        appendMode()
        appendCaptureGroup("text", ".", MORE)
    }),

    UU_BA(CommandPatternBuilder.create {
        appendUUIgnoreAll("(bp?)?a", "(bp\\s*analysis)")
        appendModeQQUIDName()
    }),

    // #3 osu! 玩家指令
    INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("information", "info", "i")
        appendModeQQUIDName()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendSpace()
            appendCaptureGroup(FLAG_DAY, REG_NUMBER, MORE)
        }
    }),

    INFO2(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testinformation", "testinfo", "ti", "ji", "j", "juice")
        appendModeQQUIDName()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendSpace()
            appendCaptureGroup(FLAG_DAY, REG_NUMBER, MORE)
        }
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
        appendMode()
        appendName()
    }),

    I_MAPPER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mapper", "immapper", "imapper", "im")
        appendQQUIDName()
    }),

    FRIEND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("friends?", "f")
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("sort", "[\\-_+1247a-zA-Z]", MORE)
        }
        appendNameAndRange()

        /*
        appendGroup(MAYBE) {
            append(REG_STAR)
            appendSpace()
            appendCaptureGroup("country",
                REG_WORD,
                MORE
            )
        }

         */
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
        appendCommandsIgnoreAll("skills?", "k")
        appendModeQQUIDName()
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("vs",
                REG_USERNAME,
                MORE
            )
        }
    }),

    SKILL_VS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("skills?\\s*v(ersu)?s", "kv")
        appendModeQQUIDName()
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("vs",
                REG_USERNAME,
                MORE
            )
        }
    }),

    BADGE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("badge", "bd")
        appendQQUIDName()
    }),

    GUEST_DIFFICULTY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("guest", "guest\\s*diff(er)?", "gd(er)?")
        appendModeQQUIDNameRange()
    }),

    GET_ID(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("getid", "gi")
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    GET_NAME(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("getname", "gn")
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    // #4 osu! 谱面指令
    MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("beatmap", "map", "m")

        appendMode()
        appendBID()
        appendGroup()
        appendSpace()

        appendCaptureGroup("any", REG_ANYTHING_BUT_NO_PLUS, MORE)
        appendSpace()

        appendMod()
    }),

    QUALIFIED_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("qualified", "qua", "q")
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
        appendCommandsIgnoreAll("leaderboard", "leader", "list", "l")
        appendCaptureGroup("type", "bid|b|sid|s")
        appendMode()
        appendBID()
        appendRange()
    }),

    LEGACY_LEADER_BOARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("legacy\\s*leaderboard", "legacy\\s*leader", "legacy\\s*list", "ll")
        appendCaptureGroup("type", "bid|b|sid|s")
        appendMode()
        appendBID()
        appendRange()
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

        appendColonCaptureGroup(MAYBE, "mode", "bid", "sid", "b", "s")
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
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("area2", REG_USERNAME, ANY)
        }
    }),

    SEARCH(CommandPatternBuilder.create {
        appendCommandsIgnore(REG_IGNORE, "search", "find", "o", "look(up)?")
        appendColonCaptureGroup(MAYBE, "mode", "bid", "sid", "b", "s")
        appendSpace()
    }),

    RECOMMEND(CommandPatternBuilder.create {
        appendCommandsIgnore(REG_IGNORE, "recommend(ed)?", "rec", "e")
        appendModeQQUIDName()
    }),

    GET_BG(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("getbackground", "getbg", "gb")
        appendSpace()
        appendCaptureGroup(FLAG_DATA, REG_NUMBER_SEPERATOR, MORE)
    }),

    GET_COVER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("getcover", "gc")
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("type", REG_COVER, MORE)
        }
        appendSpace()
        appendCaptureGroup(FLAG_DATA, REG_NUMBER_SEPERATOR, MORE)
    }),

    REFRESH_FILE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("refresh\\s*file", "rf")
        appendCaptureGroup("bid", REG_NUMBER, MORE)
    }),

    // #5 osu! 比赛指令
    MATCH_LISTENER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("make\\s*love", "(match)?listen(er)?", "ml", "li")

        appendMatchID()
        appendSpace()
        appendCaptureGroup("operate", "info|list|start|stop|end|off|on|[lispefo]")
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
        appendCommandsIgnoreAll("(match)?rounds?", "mr", "ro")
        appendMatchID()
        appendSpace()
        appendCaptureGroup("round", REG_NUMBER, MORE)
        appendSpace()
        appendCaptureGroup("keyword", REG_ANYTHING, MORE) // "[\\w\\s-_ %*()/|\\u4e00-\\u9fa5\\uf900-\\ufa2d]"
    }),

    MATCH_NOW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("monitornow", "matchnow", "mn")
        appendMatchID()
        appendMatchParam()
    }),

    MAP_POOL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mappool", "po")
        appendMode()
        appendSpace()
        appendCaptureGroup("name", REG_ANYTHING, MORE)
    }),

    GET_POOL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("getpool", "gp")
        appendMode()
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendCaptureGroup("name", REG_ANYTHING, MORE)
            append(REG_HASH)
        }
        appendCaptureGroup(FLAG_DATA, REG_ANYTHING, ANY)
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
        appendCaptureGroup("number", "${REG_HYPHEN}?\\d", ANY)
        appendSpace()
        appendCaptureGroup("text", REG_ANYTHING, MORE)
    }),

    DRAW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("draw", "w")
        appendCaptureGroup("d", REG_NUMBER)
    }),

    // #8 辅助指令
    OLD_AVATAR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(old|osu)?\\s*avatar", "oa")
        appendQQID()
        appendUID()
        appendCaptureGroup(FLAG_DATA, REG_USERNAME_SEPERATOR, ANY)
    }),

    OSU_AVATAR_CARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(old|osu)?\\s*card", "oc")
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
        appendSpace() // 这里改了
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
        appendCommandsIgnoreAll("groupstat(s)?", "groupstatistic(s)?", "统计(超限)?", "gs")
        appendColonCaptureGroup(MAYBE, "group", "[nah]|((新人|进阶|高阶)群)")
    }),

    // #9 自定义
    CUSTOM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("custom", "c")
        appendColonCaptureGroup(MAYBE, "operate", "${REG_WORD}${LEVEL_MORE}")
        appendSpace()
        appendColonCaptureGroup(MAYBE, "type", "${REG_WORD}${LEVEL_MORE}")
    }),

    TEST_MAP_MINUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testmapminus", "testmm", "tmm", "tn")
        appendMode()
        appendCaptureGroup(FLAG_DATA, REG_NUMBER_SEPERATOR, MORE)
    }),

    TEST_PPM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testppm", "tp")
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

    TEST_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testmap", "testdiff", "td")
        appendID()
        appendMod()
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

    // #11 maimai & CHUNITHM

    MAI_SCORE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(score|song)", "ms")
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup(FLAG_DIFF, REG_DIFF)
        }
        appendQQID()
        appendSpace()
        appendCaptureGroup(FLAG_VERSION, "dx|sd|deluxe|standard|标准|豪华")
        appendSpace()
        appendNameAny()
    }),

    MAI_VERSION(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*version", "mv")
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup(FLAG_DIFF, REG_DIFF)
        }
        appendQQID()
        appendNameAnyButNoHash()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup(FLAG_VERSION, REG_ANYTHING_BUT_NO_SPACE, MORE)
        }
    }),

    MAI_UPDATE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("update\\s*mai(mai)?", "um")
    }),

    MAI_SEEK(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(seek)", "mk")
        appendCaptureGroup(FLAG_NAME, REG_ANYTHING, MORE)
    }),

    MAI_DIST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*dist(ribution)?", "md", "mark\\s*down")
        appendQQID()
        appendNameAnyButNoHash()
    }),

    MAI_SEARCH(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*search", "mh")
        appendQQID()
        appendNameAnyButNoHash()
    }),

    MAI_FIND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*find", "mf", "mother\\s*fucker")
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup(
                FLAG_DIFF, REG_ANYTHING_BUT_NO_SPACE, MORE
            )
        }
        appendSpace()
        appendCaptureGroup("any", REG_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup(FLAG_VERSION, REG_ANYTHING_BUT_NO_STARS, MORE)
        }
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_STAR)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup(
                "score", REG_NUMBER, MORE
            )
        }
    }),

    MAI_AP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*((all\\s?)?perfect|ap)", "mp", "xp")
        appendQQID()
        appendNameAnyButNoHash()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, EXIST)
        }
    }),

    MAI_FC(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(full\\s?combo|fc)", "mc", "xc")
        appendQQID()
        appendNameAnyButNoHash()
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup(FLAG_PAGE, REG_NUMBER_1_100, EXIST)
        }
    }),

    // 必须放在其他 mai 指令后面
    MAI_BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(best|best50|b50)?", "mb", "x")
        appendQQID()
        appendNameAnyButNoHash()
        appendRange()
    }),

    CHU_BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("chu(nithm)?\\s*(best|best40|b40)?", "cb", "y")
        appendQQID()
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
        if (i != Instruction.SB_SET_MODE) continue

        println("${i.name}: ${i.pattern.pattern()}")
    }
}