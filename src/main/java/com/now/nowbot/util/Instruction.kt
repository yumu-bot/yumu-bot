package com.now.nowbot.util

import com.now.nowbot.util.command.CommandPatternBuilder
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

    BAN(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("super", "sp", "operate", "op")
        appendColonCaptureGroup(
                    MAYBE,
                    "operate",
                    "(black|white|ban)?list",
                    "add",
                    "remove",
                    "(un)?ban",
                    "[lkarubw]"
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
        appendGroup(MAYBE) {
            appendCaptureGroup("hours", REG_NUMBER, MORE)
            append('h')
            appendMatchLevel(MAYBE)
        }
    }),

    SYSTEM_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("systeminfo", "sys", "si")
    }),

    // #2 osu! 成绩指令
    SET_MODE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("setmode", "mode", "sm", "mo")
        appendColonCaptureGroup(MAYBE, FLAG_MODE, REG_MODE)
    }),

    SCORE_PR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p)|(?<recent>(recent|r)))(?<s>s)?")
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
            append(REG_EXCLAMINATION)
            appendSpace()
            append("(?<score>(ym)?(score|s))")
            appendIgnore()
        }
        appendModeBIDQQUIDNameMod()
    }),

    BP(CommandPatternBuilder.create {
        appendGroup {
            append(REG_EXCLAMINATION)
            appendSpace()
            append("(?<bp>(ym)?(bestperformance|best|bp|b))(?<s>s)?")
            appendIgnore()
        }
        appendModeQQUIDNameRange()
    }),

    TODAY_BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t")
        appendModeQQUIDNameRange()
    }),

    BP_FIX(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bpfix", "fixbp", "bestperformancefix", "bestfix", "bpf", "bf")
        appendModeQQUIDName()
    }),

    BP_ANALYSIS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bpanalysis", "blue\\s*archive", "bpa", "ba")
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
        appendCommandsIgnoreAll("friends", "friend", "f")
        appendNameAndRange()
    }),

    MUTUAL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mutual", "mu")
        appendCaptureGroup("names", REG_USERNAME_SEPERATOR, ANY)
    }),

    PP_MINUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<function>(p?p[mv\\-]|p?pmvs?|ppminus|minus|minusvs))")
        appendMode()
        appendCaptureGroup("area1", REG_USERNAME, ANY)
        appendSpace()
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("area2", REG_USERNAME, MORE)
        }
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

    GET_COVER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("getcover", "gc")
        appendGroup(MAYBE) {
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup("type", REG_COVER)
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
        append(REG_EXCLAMINATION)
        appendSpace()
        append("((?<uu>(u{1,2})(rating|ra))|(?<main>((ym)?rating|(ym)?ra|mra)))")
        appendIgnore()
        appendSpace()

        appendMatchID()
        appendMatchParam()
    }),

    SERIES_RATING(CommandPatternBuilder.create {
        append(REG_EXCLAMINATION)
        appendSpace()
        appendGroup(MAYBE, "ym")
        appendGroup(
            "((?<uu>(u{1,2})(seriesrating|series|sra|sa)))|((ym)?(?<main>(seriesrating|series|sa|sra)))|((ym)?(?<csv>(csvseriesrating|csvseries|csa|cs)))"
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
        appendCaptureGroup("keyword", "[\\w\\s-_ %*()/|\\u4e00-\\u9fa5\\uf900-\\ufa2d]", MORE)
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
        appendCaptureGroup("name", REG_WORD, MORE)
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
        appendCaptureGroup(FLAG_DATA, REG_NUMBER_SEPERATOR, ANY)
    }),

    // #6 聊天指令
    // ...
    // #7 娱乐指令

    DICE(CommandPatternBuilder.create {
        append("($REG_EXCLAMINATION|(?<dice>\\d+))\\s*(?i)(ym)?(dice|roll|d${REG_IGNORE})")
        appendCaptureGroup("number", "-?\\d", ANY)
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
        // 这里改了
        appendBID()
        appendMod()
        appendSpace()
        appendCaptureGroup("round", "[\\w\\s]", MORE)
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

    TEST_PPM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testppm", "testcost", "tp", "tc")
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
        appendCommandsIgnoreAll("testmap", "tm")
        appendID()
        appendMod()
    }),

    TEST_TAIKO_SR_CALCULATE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testtaiko", "tt")
        appendCaptureGroup(FLAG_DATA, "[xo\\s]", MORE)
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

    DEPRECATED_BPHT(CommandPatternBuilder.create {
        appendCommands("bpht")
        append("(-i)")
        appendMatchLevel(MAYBE)
    }),

    DEPRECATED_UUBA_I(CommandPatternBuilder.create {
        appendCommands("u?uba", "ua")
        append("(-?i)")
    }),

    DEPRECATED_SET(CommandPatternBuilder.create {
        appendCommands("(?<set>set)")
    }),

    DEPRECATED_AYACHI_NENE(CommandPatternBuilder.create {
        append("(?<nene>0d0(0)?)") // 无需 commands
    }),

    DEPRECATED_YMK(CommandPatternBuilder.create {
        appendCommands("(?<k>k)")
    }),

    DEPRECATED_YMX(CommandPatternBuilder.create {
        appendCommands("(?<x>x)")
    }),

    DEPRECATED_YMY(CommandPatternBuilder.create {
        appendCommands("(?<y>y)")
    }),

    ;

    fun matcher(input: CharSequence): Matcher = this.pattern.matcher(input)
}

// 检查正则
fun main() {
    for (i in Instruction.values()) {
        println("${i.name}: ${i.pattern.pattern()}")
    }
}