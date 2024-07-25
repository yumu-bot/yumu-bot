package com.now.nowbot.util

import com.now.nowbot.util.command.CmdPattemBuilder
import com.now.nowbot.util.command.CmdPatternStatic.REG_COLUMN
import com.now.nowbot.util.command.CmdPatternStatic.REG_EXCLAM
import com.now.nowbot.util.command.CmdPatternStatic.REG_HASH
import com.now.nowbot.util.command.CmdPatternStatic.REG_IGNORE
import java.util.regex.Matcher
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
        commands("(?<ub>ub(?![A-Za-z_]))", "(?<bi>bi$REG_IGNORE)", "(?<un>un)?(?<bind>bind)")
        append("($REG_COLUMN(?<full>f))?")
        space()
        appendQQId()
        space()
        appendName()
    }),
    BAN(CmdPattemBuilder.create {
        commands("super", "sp$REG_IGNORE", "operate", "op$REG_IGNORE")
        space()
        column()
        group("operate", "(black|white|ban)?list|add|remove|(un)?ban|[lkarubw]")
        whatever()
        space()
        appendQQId()
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
        commands("$REG_EXCLAM(ym)?(?<pass>(pass(?!s)(?<es>es)?|p(?![a-rt-zA-RT-Z_]))|(?<recent>(recent|r(?![^s\\s]))))(?<s>s)?")
        `append(ModeQQUidNameRange)`()
    }),
    PR_CARD(CmdPattemBuilder.create {
        commands("$REG_EXCLAM(ym)?(?<pass>(passcard|pc(?![a-rt-zA-RT-Z_]))|(?<recent>(recentcard|rc(?![a-rt-zA-RT-Z_]))))")
        `append(ModeQQUidNameRange)`()
    }),
    UU_PR(CmdPattemBuilder.create {
        commands("${REG_EXCLAM}uu(?<pass>(pass|p$REG_IGNORE))|uu(?<recent>(recent|r$REG_IGNORE))")
        `append(ModeQQUidNameRange)`()
    }),
    SCORE(CmdPattemBuilder.create {
        commands("(?<score>(ym)?(score|s$REG_IGNORE))")
        column()
        space()
        appendBid()
        space()
        appendQQId()
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
        appendQQId()
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
        append("(?<area1>[0-9a-zA-Z\\[\\]\\-_\\s]*)?\\s*($REG_COLUMN\\s*(?<area2>[0-9a-zA-Z\\[\\]\\-_\\s]*))?")
    }),

    // #5 osu! 比赛指令

    MATCH_LISTENER(CmdPattemBuilder.create {
        commands("(make\\s*love)", "(match)?listen(er)?", "ml$REG_IGNORE", "li$REG_IGNORE")
        group("matchid", "\\d+")
        space()
        group("operate", "info|list|start|stop|end|off|on|[lispefo]$REG_IGNORE")
    }),
    MU_RATING(CmdPattemBuilder.create {
        commands("(?<uu>(u{1,2})(rating|ra$REG_IGNORE))", "(?<main>((ym)?rating|(ym)?ra$REG_IGNORE|mra$REG_IGNORE))")
        group("matchid", "\\d+", whatever = false)
        // 这是个啥, 所有命令前面都带了 (?i), 大小写就忽略了
        // (\s*[Ee]([Zz]|a[sz]y)?\s*(?<easy>\d+\.?\d*)x?)?
        startGroup {
            space()
            append("e(z|a[zs]y)?\\s*(?<easy>\\d+\\.?\\d*)x?")
        }
        startGroup {
            space()
            group("skip", "-?\\d+")
        }
        startGroup {
            space()
            group("ignore", "-?\\d+")
        }
        startGroup {
            space()
            append("(\\[(?<remove>[\\s,，\\-|:\\d]+)])")
        }
        startGroup {
            space()
            group("rematch", "r")
        }
        startGroup {
            space()
            group("failed", "f")
        }
    }),
    SERIES_RATING(CmdPattemBuilder.create {
        commands(
            "(?<uu>(u{1,2})(seriesrating|series|sra$REG_IGNORE|sa$REG_IGNORE))",
            "(ym)?(?<main>(seriesrating|series|sa$REG_IGNORE|sra$REG_IGNORE))",
            "(ym)?(?<csv>(csvseriesrating|csvseries|csa$REG_IGNORE|cs$REG_IGNORE))"
        )
        startGroup {
            +REG_HASH
            group("name", ".+")
            +REG_HASH
        }
        space()
        group("data", "[\\d\\[\\]\\s,，|\\-]+")
        space()
        append("([Ee]([Zz]|a[sz]y)?\\s*(?<easy>\\d+\\.?\\d*)x?)?")
        space()
        group("rematch", "r")
        space()
        group("failed", "f")

    }),
    CSV_MATCH(CmdPattemBuilder.create {
        commands("csvrating", "cra?(?![^s^x\\s])", "ml$REG_IGNORE", "li$REG_IGNORE")
        group("x", "[xs]")
        space()
        group("data", "[\\d\\s,，|\\-]+")
    }),
    MATCH_ROUND(CmdPattemBuilder.create {
        commands("(match)?rounds?$REG_IGNORE", "mr$REG_IGNORE", "ro$REG_IGNORE")
        group("matchid", "\\d+")
        space()
        group("round", "\\d+")
        space()
        group("keyword", "[\\w\\s-_ %*()/|\\u4e00-\\u9fa5\\uf900-\\ufa2d]+")
    }),
    MATCH_NOW(CmdPattemBuilder.create {
        commands("monitornow", "matchnow", "mn$REG_IGNORE")
        group("matchid", "\\d+", whatever = false)
        space()
        group("round", "\\d+")
        space()
        group("keyword", "[\\w\\s-_ %*()/|\\u4e00-\\u9fa5\\uf900-\\ufa2d]+")
    }),
    MAP_POOL(CmdPattemBuilder.create {
        commands("mappool", "po$REG_IGNORE")
        appendMode(false)
        space()
        group("name", "\\w+", whatever = false)
    }),
    GET_POOL(CmdPattemBuilder.create {
        commands("getpool", "gp$REG_IGNORE")
        appendMode()
        space()
        startGroup {
            +REG_HASH
            group("name", ".+", whatever = false)
            +REG_HASH
        }
        whatever()
        space()
        group("data", "[\\w\\s,，|\\-]+")
    }),
    // #6 聊天指令
    // #7 娱乐指令

    DICE(CmdPattemBuilder.create {
        commands("([!！]|(?<dice>\\d+))\\s*(?i)(ym)?(dice|roll|d$REG_IGNORE)")
        group("number","-?\\d*")
        group("text","[\\s\\S]+")
    }),
    DRAW(CmdPattemBuilder.create {
        commands("draw","w$REG_IGNORE")
        group("d","\\d")
    })

//todo: 还差 辅助指令 其他
    ;
    fun matcher(input: CharSequence): Matcher = this.pattern.matcher(input)
}