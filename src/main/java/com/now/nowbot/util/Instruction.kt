package com.now.nowbot.util

import com.now.nowbot.util.command.CmdPatterBuilder
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
            appendCommandsIgnoreAll("ping", "pi")
        }
        append(CHAR_SEPARATOR)
        appendGroup {
            append("yumu${REG_SPACE}${CHAR_ANY}${REG_QUESTION}")
        }
    }),

    BIND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<ub>ub)", "(?<bi>bi)", "(?<un>un)?(?<bind>bind)")
        appendColonCaptureGroup("full", "f")
        appendQQID()
        appendName()
    }),

    BAN(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("super", "sp", "operate", "op")
        appendColonCaptureGroup(MAYBE, "operate", "(black|white|ban)?list", "add", "remove", "(un)?ban", "[lkarubw]")
        appendQQID()
        appendQQGroup()
        appendName()
    }),

    SWITCH(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("switch", "sw")
        appendColonCaptureGroup(MAYBE, FLAG_QQ_GROUP, "${FLAG_QQ_GROUP}=${REG_NUMBER}")
        appendSpace()
        appendCaptureGroup("service", REG_WORD, MORE)
        appendSpace(MORE)
        appendCaptureGroup("operate", REG_WORD, MORE)
    }),

    ECHO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("echo", "ec")
        appendCaptureGroup("any", REG_ANYTHING, ANY)
    }),

    SERVICE_COUNT(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("servicecount", "统计服务调用", "ec")

        appendGroup {
           appendCaptureGroup("days", REG_NUMBER, MORE)
           append('d')
        }
        appendGroup {
            appendCaptureGroup("hours", REG_NUMBER, MORE)
            append('h')
        }
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
        appendGroup() {
            append(REG_EXCLAMINATION)
            appendSpace()
            append("(?<score>(ym)?(score|s))")
            appendIgnore()
        }
        appendModeBIDQQUIDNameMod()
    }),

    BP(CommandPatternBuilder.create {

    }),

    TODAY_BP(CommandPatternBuilder.create {

    }),

    BP_FIX(CommandPatternBuilder.create {

    }),

    BP_ANALYSIS(CommandPatternBuilder.create {

    }),

    UU_BA(CommandPatternBuilder.create {

    }),

    // #3 osu! 玩家指令
    INFO(CommandPatternBuilder.create {

    }),

    INFO_CARD(CommandPatternBuilder.create {

    }),

    CSV_INFO(CommandPatternBuilder.create {

    }),

    UU_INFO(CommandPatternBuilder.create {

    }),

    I_MAPPER(CommandPatternBuilder.create {

    }),

    FRIEND(CommandPatternBuilder.create {

    }),

    MUTUAL(CommandPatternBuilder.create {

    }),

    PP_MINUS(CommandPatternBuilder.create {

    }),

    GET_ID(CommandPatternBuilder.create {

    }),

    GET_NAME(CommandPatternBuilder.create {

    }),

    // #4 osu! 谱面指令
    MAP(CommandPatternBuilder.create {

    }),

    QUALIFIED_MAP(CommandPatternBuilder.create {


    }),

    LEADER_BOARD(CommandPatternBuilder.create {

    }),

    MAP_MINUS(CommandPatternBuilder.create {

    }),

    NOMINATION(CommandPatternBuilder.create {

    }),

    PP_PLUS_MAP(CommandPatternBuilder.create {

    }),

    PP_PLUS(CommandPatternBuilder.create {

    }),

        // ^[!！]\s*(?i)(ym)?(?<function>(p[px](?![A-Za-z_])|pp[pvx](?![A-Za-z_])|p?p\+|(pp)?plus|ppvs|pppvs|(pp)?plusvs|p?pa(?![A-Za-z_])|ppplusmap|pppmap|plusmap))\s*(?<area1>[0-9a-zA-Z\[\]\-_\s]*)?\s*([:：]\s*(?<area2>[0-9a-zA-Z\[\]\-_\s]*))?

    // #5 osu! 比赛指令
    MATCH_LISTENER(CommandPatternBuilder.create {

    }),

    MU_RATING(CommandPatternBuilder.create {

    }),

    SERIES_RATING(CommandPatternBuilder.create {

    }),

    CSV_MATCH(CommandPatternBuilder.create {

    }),

    MATCH_ROUND(CommandPatternBuilder.create {

    }),

    MATCH_NOW(CommandPatternBuilder.create {

    }),

    MAP_POOL(CommandPatternBuilder.create {

    }),

    GET_POOL(CommandPatternBuilder.create {

    }),

    // #6 聊天指令
    // ...
    // #7 娱乐指令

    DICE(CommandPatternBuilder.create {

    }),

    DRAW(CommandPatternBuilder.create {

    }),

    // #8 辅助指令
    OLD_AVATAR(CommandPatternBuilder.create {

    }),

    OVER_SR(CommandPatternBuilder.create {

    }),

    TRANS(CommandPatternBuilder.create {

    }),

    KITA(CommandPatternBuilder.create {

    }),

    GROUP_STATISTICS(CommandPatternBuilder.create {

    }),

    // #9 自定义
    CUSTOM(CommandPatternBuilder.create {

    }),

    TEST_PPM(CommandPatternBuilder.create {

    }),

    TEST_HD(CommandPatternBuilder.create {

    }),

    TEST_FIX(CommandPatternBuilder.create {

    }),

    TEST_MAP(CommandPatternBuilder.create {

    }),

    TEST_TAIKO_SR_CALCULATE(CommandPatternBuilder.create {

    }),

    MAP_4D_CALCULATE(CommandPatternBuilder.create("^[!！＃#]\\s*(?i)cal") {

    }),

    DEPRECATED_BPHT(CommandPatternBuilder.create {

    }),

    DEPRECATED_SET(CommandPatternBuilder.create {

    }),

    DEPRECATED_AYACHI_NENE(CommandPatternBuilder.create {

    }),

    DEPRECATED_YMK(CommandPatternBuilder.create {

    }),

    DEPRECATED_YMX(CommandPatternBuilder.create {

    }),

    DEPRECATED_YMY(CommandPatternBuilder.create {

    }),

    ;

    fun matcher(input: CharSequence): Matcher = this.pattern.matcher(input)
}