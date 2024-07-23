package com.now.nowbot.util

import com.now.nowbot.util.command.CmdPattemBuilder
import com.now.nowbot.util.command.CmdPatternStatic.*
import java.util.regex.Pattern

enum class Instruction(val pattern: Pattern) {
    // #0 调出帮助
    HELP(CmdPattemBuilder.create {
        commands("help", "帮助", "h")
        group("module", "[\\s\\S]*")
    }),

    // 不太懂为什么 audio 算在帮助里, 娱乐/其他 更合适吧
    AUDIO(CmdPattemBuilder.create {
        commands("audio", "song", "a(?![AaC-RT-Zc-rt-z_])")
        column()
        group("type", "(bid|b|sid|s)")
        space()
        group("id", "\\d+")
    }),

    // #1 BOT 内部指令
    PING(CmdPattemBuilder.create {
        commands("ping", "pi$REG_IGNORE", "yumu\\?")
    }),
    BIND(CmdPattemBuilder.create {
        commands("(?<ub>ub(?![A-Za-z_]))", "(?<bi>bi$REG_IGNORE", "(?<un>un)?(?<bind>bind)")
        append("($REG_COLUMN(?<full>f))?")
        space()
        appendQQ()
        space()
        appendName()
    }),
    BAN(CmdPattemBuilder.create {
        commands {
            +"super"
            +"sp$REG_IGNORE"
            +"operate"
            +"op$REG_IGNORE"
        }
        space()
        column()
        group("operate", "(black|white|ban)?list|add|remove|(un)?ban|[lkarubw]")
        whatever()
        space()
        appendQQ()
        space()
        appendQQGroup()
        space()
        appendName()
    }),
    SWITCH(CmdPattemBuilder.create {
        commands("switch", "sw$REG_IGNORE")
        column()
        appendQQGroup()
        space()
        group("service", "\\w+")
        space()
        group("operate", "\\w+")
    }),
    ECHO(CmdPattemBuilder.create {
        commands("echo")
        column()
        group("any", "[\\s\\S]*", false)
    }),
    SERVICE_COUNT(CmdPattemBuilder.create {
        commands("servicecount", "统计服务调用", "sc$REG_IGNORE")
        startGroup {
            group("days", "\\d+", false)
            +"d"
        }
        startGroup {
            group("hours", "\\d+", false)
            +"h"
            whatever()
        }
    }),

    // #2 osu! 成绩指令
    // 我强烈建议set mod放到玩家指令这个分类里
    SET_MODE(CmdPattemBuilder.create {
        commands("setmode", "mode", "sm$REG_IGNORE", "mo$REG_IGNORE")
        appendMode()
    }),
    SCORE_PR(CmdPattemBuilder.create {
        // 这一坨没法拆好像
        commands("(?<pass>(ym)?(pass(?![sS])(?<es>es)?|p(?![a-rt-zA-RT-Z_]))|(ym)?(?<recent>(recent|r(?![a-rt-zA-RT-Z_]))))(?<s>s)?")
        `append(ModeQQUidNameRange)`()
    }),
    PR_CARD(CmdPattemBuilder.create {
        commands("(?<pass>(ym)?(passcard|pc(?![a-rt-zA-RT-Z_]))|(ym)?(?<recent>(recentcard|rc(?![a-rt-zA-RT-Z_]))))")
        `append(ModeQQUidNameRange)`()
    }),
    UU_PR(CmdPattemBuilder.create {
        commands("uu(?<pass>(pass|p$REG_IGNORE))|uu(?<recent>(recent|r$REG_IGNORE))")
        `append(ModeQQUidNameRange)`()
    }),
    SCORE(CmdPattemBuilder.create {
        commands("(?<score>(ym)?(score|s$REG_IGNORE))")
        column()
        space()
        appendBid()
        space()
        appendQQ()
        space()
        appendUid()
        space()
        appendName()
        space()
        appendMod()
    }),
    BP(CmdPattemBuilder.create {
        commands("(?<bp>(ym)?(bestperformance|best|bp(?![a-rt-zA-RT-Z_])|b(?![a-rt-zA-RT-Z_])))(?<s>s)?")
        `append(ModeQQUidNameRange)`()
    }),
    TODAY_BP(CmdPattemBuilder.create {
        commands(true, "todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t")
        `append(ModeQQUidNameRange)`()
    }),
    BP_FIX(CmdPattemBuilder.create {
        commands(true, "bpfix", "fixbp", "bestperformancefix", "bestfix", "bpf", "bf")
        `append(ModeQQUidNameRange)`()
    }),
    BP_ANALYSIS(CmdPattemBuilder.create {
        commands(true, "bpanalysis", "blue archive", "bluearchive", "bpa", "ba")
        `append(ModeQQUidNameRange)`()
    }),
    UU_BA(CmdPattemBuilder.create {
        commands("bpanalysis", "blue archive", "bluearchive", "bpa", "ba")
        `append(ModeQQUidNameRange)`()
    }),

    // #3 osu! 玩家指令
    INFO(CmdPattemBuilder.create {
        commands("bpanalysis", "blue archive", "bluearchive", "bpa", "ba")
        `append(ModeQQUidNameRange)`()
    }),
    INFO_CARD(CmdPattemBuilder.create {
        commands("informationcard", "infocard$REG_IGNORE", "ic$REG_IGNORE")
        `append(ModeQQUidName)`()
    }),
    CSV_INFO(CmdPattemBuilder.create {
        // 给你展开了
        commands("(c(sv)?)information", "(c(sv)?)info$REG_IGNORE", "(c(sv)?)i$REG_IGNORE")
        appendMode()
        space()
        append("(?<data>[\\w\\[\\]\\s\\-_,，、|:：]+)?")
    }),
    UU_INFO(CmdPattemBuilder.create {
        // 给你展开了
        commands("uuinfo", "uui$REG_IGNORE")
        appendMode()
        // 原来是 `([:：](?<mode>[\w\d]+))?(?![\w])\s*(?<name>[0-9a-zA-Z\[\]\-_\s]*)?`
        // ==================实在搞不懂这个 ^ 是干什么用的, 简单理解为必须要一个空格
        space(1)
        appendName()
    }),
    I_MAPPER(CmdPattemBuilder.create {
        commands("mapper", "immapper", "imapper", "im")
        appendQQ()
        space()
        append("(u?id=\\s*(?<id>\\d+))?")
        space()
        appendName()
    }),
    FRIEND(CmdPattemBuilder.create {
        commands("friends?", "f$REG_IGNORE")
        appendRange()
    }),
    MUTUAL(CmdPattemBuilder.create {
        commands("mutual", "mu$REG_IGNORE")
        append("(?<names>[0-9a-zA-Z\\[\\]\\-_ ,]+)?")
    }),
    PP_MINUS(CmdPattemBuilder.create {
        commands("(?<function>(p?p[mv\\-]$REG_IGNORE|p?pmvs?|ppminus|minus|minusvs))")
        appendMode()
        space()
        append("(?<area1>[0-9a-zA-Z\\[\\]\\-_\\s]*)?")
        space()
        append("($REG_COLUMN\\s*(?<area2>[0-9a-zA-Z\\[\\]\\-_\\s]*))?")
    }),

    // #4 osu! 谱面指令

    MAP(CmdPattemBuilder.create {
        commands("beatmap", "map$REG_IGNORE", "m$REG_IGNORE")
        appendMode()
        space()
        append("(?<area1>[0-9a-zA-Z\\[\\]\\-_\\s]*)?")
        space()
        append("($REG_COLUMN\\s*(?<area2>[0-9a-zA-Z\\[\\]\\-_\\s]*))?")
    }),
    QUALIFIED_MAP(CmdPattemBuilder.create {
        commands("qualified", "qua$REG_IGNORE", "q$REG_IGNORE")
        appendMode()
        space()
        append("($REG_HASH(?<status>[-\\w]+))?\\s*(\\*?(?<sort>[-_+a-zA-Z]+))?\\s*(?<range>\\d+)?")
    }),
    LEADER_BOARD(CmdPattemBuilder.create {
        commands("qualified", "qua$REG_IGNORE", "q$REG_IGNORE")
        appendMode()
        space()
        append("($REG_HASH(?<status>[-\\w]+))?\\s*(\\*?(?<sort>[-_+a-zA-Z]+))?\\s*(?<range>\\d+)?")
    }),
    MAP_MINUS(CmdPattemBuilder.create {
        commands("mapminus", "mm$REG_IGNORE")
        appendMode()
        space()
        append("(?<id>\\d+)?\\s*(\\+(?<mod>(\\s*[EZNMFHTDRSPCLO]{2})+)|([×xX]?\\s*(?<rate>[0-9.]*)[×xX]?))?")
    }),
    NOMINATION(CmdPattemBuilder.create {
        commands("(nominat(e|ion)s?|nom(?![AC-RT-Zac-rt-z_])|n(?![AC-RT-Zac-rt-z_]))")
        column()
        // 这个mode还是特殊的mode
        group("mode", "bid|sid|s|b")
        space()
        appendSid()
    }),
    PP_PLUS(CmdPattemBuilder.create {
        // 245 个字符的正则...
        // ^[!！]\s*(?i)(ym)?(?<function>(p[px](?![A-Za-z_])|pp[pvx](?![A-Za-z_])|p?p\+|(pp)?plus|ppvs|pppvs|(pp)?plusvs|p?pa(?![A-Za-z_])|ppplusmap|pppmap|plusmap))\s*(?<area1>[0-9a-zA-Z\[\]\-_\s]*)?\s*([:：]\s*(?<area2>[0-9a-zA-Z\[\]\-_\s]*))?
        commands("(?<function>(p[px](?![A-Za-z_])|pp[pvx](?![A-Za-z_])|p?p\\+|(pp)?plus|ppvs|pppvs|(pp)?plusvs|p?pa(?![A-Za-z_])|ppplusmap|pppmap|plusmap))")
        append("(?<area1>[0-9a-zA-Z\\[\\]\\-_\\s]*)?\\s*([:：]\\s*(?<area2>[0-9a-zA-Z\\[\\]\\-_\\s]*))?")
    }),
//todo: 还差 osu! 比赛指令 娱乐指令 辅助指令 其他
}