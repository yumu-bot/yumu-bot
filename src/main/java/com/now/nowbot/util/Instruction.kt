package com.now.nowbot.util

import com.now.nowbot.util.command.CmdPatterBuilder
import com.now.nowbot.util.command.*
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class Instruction(val pattern: Pattern) {
    // #0 调出帮助
    HELP(CmdPatterBuilder.create {
        commandWithShort("help", "help", "帮助", "h")
        group("module", REG_ANY)
    }),

    // 不太懂为什么 audio 算在帮助里, 娱乐/其他 更合适吧
    AUDIO(CmdPatterBuilder.create {
        commands("audio", "song", "a(?![AaC-RT-Zc-rt-z_])")
        column()
        group("type", "(bid|b|sid|s)")
        space()
        group("id", "\\d+")
    }),

    // #1 BOT 内部指令
    PING(CmdPatterBuilder.create {
        commands("ping", "pi$REG_IGNORE", "yumu\\?")
    }),
    BIND(CmdPatterBuilder.create {
        commands("(?<ub>ub(?![A-Za-z_]))", "(?<bi>bi$REG_IGNORE)", "(?<un>un)?(?<bind>bind)")
        append("($REG_COLUMN(?<full>f))?")
        space()
        appendQQId()
        space()
        appendName()
    }),
    BAN(CmdPatterBuilder.create {
        commands("super", "sp$REG_IGNORE", "operate", "op$REG_IGNORE")
        space()
        column()
        group("operate", "(black|white|ban)?list|add|remove|(un)?ban|[lkarubw]")
        space()
        appendQQId()
        space()
        appendQQGroup()
        space()
        appendName()
    }),
    SWITCH(CmdPatterBuilder.create {
        commands("switch", "sw$REG_IGNORE")
        column()
        appendQQGroup()
        space()
        group("service", "\\w+")
        space()
        group("operate", "\\w+")
    }),
    ECHO(CmdPatterBuilder.create {
        command("echo")
        column()
        group("any", "[\\s\\S]*", false)
    }),
    SERVICE_COUNT(CmdPatterBuilder.create {
        commands("servicecount", "统计服务调用", "sc$REG_IGNORE")
        startGroup {
            group("days", "\\d+", false)
            +'d'
        }
        startGroup {
            group("hours", "\\d+", false)
            +'h'
        }
    }),

    // #2 osu! 成绩指令
    // 我强烈建议set mod放到玩家指令这个分类里
    SET_MODE(CmdPatterBuilder.create {
        commands("setmode", "mode", "sm$REG_IGNORE", "mo$REG_IGNORE")
        startGroup {
            column()
            +REG_MODE
        }
    }),
    SCORE_PR(CmdPatterBuilder.create {
        // 这一坨没法拆好像
        command("$REG_EXCLAIM(ym)?(?<pass>(pass(?!s)(?<es>es)?|p$REG_IGNORE_S)|(?<recent>(recent|r$REG_IGNORE_S)))(?<s>s)?")
        `append(ModeQQUidNameRange)`()
    }),
    PR_CARD(CmdPatterBuilder.create {
        commands("(?<pass>(passcard|pc$REG_IGNORE))", "(?<recent>(recentcard|rc$REG_IGNORE))")
        `append(ModeQQUidNameRange)`()
    }),
    UU_PR(CmdPatterBuilder.create {
        command("${REG_EXCLAIM}uu(?<pass>(pass|p$REG_IGNORE))|uu(?<recent>(recent|r$REG_IGNORE))")
        `append(ModeQQUidNameRange)`()
    }),
    SCORE(CmdPatterBuilder.create {
        command("$REG_EXCLAIM(?<score>(ym)?(score|s$REG_IGNORE))")
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
    BP(CmdPatterBuilder.create {
        command("$REG_EXCLAIM(?<bp>(ym)?(bestperformance|best|bp$REG_IGNORE_S|b$REG_IGNORE_S))(?<s>s)?")
        `append(ModeQQUidNameRange)`()
    }),
    TODAY_BP(CmdPatterBuilder.create {
        commandWithIgnore("todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t")
        `append(ModeQQUidNameRange)`()
    }),
    BP_FIX(CmdPatterBuilder.create {
        commandWithIgnore("bpfix", "fixbp", "bestperformancefix", "bestfix", "bpf", "bf")
        `append(ModeQQUidName)`()
    }),
    BP_ANALYSIS(CmdPatterBuilder.create {
        commandWithIgnore("bpanalysis", "blue archive", "bluearchive", "bpa", "ba")
        `append(ModeQQUidName)`()
    }),
    UU_BA(CmdPatterBuilder.create {
        commands("uu?((bp?)?a)(?<info>(-?i))?$REG_SPACE_ANY", "uubpanalysis")
        `append(ModeQQUidName)`()
    }),

    // #3 osu! 玩家指令
    INFO(CmdPatterBuilder.create {
        commands("information", "info$REG_IGNORE", "i$REG_IGNORE")
        appendMode()
        space()
        appendQQId()
        space()
        appendUid()
        space()
        appendName()
        space()
        startGroup {
            append(REG_HASH)
            group("day", "\\d+", whatever = false)
        }
    }),
    INFO_CARD(CmdPatterBuilder.create {
        commands("informationcard", "infocard$REG_IGNORE", "ic$REG_IGNORE")
        `append(ModeQQUidName)`()
    }),
    CSV_INFO(CmdPatterBuilder.create {
        // 给你展开了
        commands("(c(sv)?)information", "(c(sv)?)info$REG_IGNORE", "(c(sv)?)i$REG_IGNORE")
        appendMode()
        space()
        append("(?<data>[\\w\\[\\]\\s\\-_,，、|:：]+)?")
    }),
    UU_INFO(CmdPatterBuilder.create {
        // 给你展开了
        commands("uuinfo", "uui$REG_IGNORE")
        appendMode()
        // 原来是 `([:：](?<mode>[\w\d]+))?(?![\w])\s*(?<name>[0-9a-zA-Z\[\]\-_\s]*)?`
        // ==================实在搞不懂这个 ^ 是干什么用的, 简单理解为必须要一个空格
        space(1)
        appendName()
    }),
    I_MAPPER(CmdPatterBuilder.create {
        commands("mapper", "immapper", "imapper", "im")
        appendQQId()
        space()
        append("(u?id=\\s*(?<id>\\d+))?")
        space()
        appendName()
    }),
    FRIEND(CmdPatterBuilder.create {
        commands("friends?", "f$REG_IGNORE")
        appendRange()
    }),
    MUTUAL(CmdPatterBuilder.create {
        commands("mutual", "mu$REG_IGNORE")
        append("(?<names>$REG_NAME_MULTI)?")
    }),
    PP_MINUS(CmdPatterBuilder.create {
        command("$REG_EXCLAIM(?<function>(p?p[mv\\-]$REG_IGNORE|p?pmvs?|ppminus|minus|minusvs))")
        appendMode()
        space()
        group("area1", REG_NAME_ANY)
        space()
        startGroup {
            append(REG_COLUMN)
            space()
            group("area2", REG_NAME_ANY, whatever = false)
        }
    }),
    GET_ID(CmdPatterBuilder.create {
        commands("getid", "gi$REG_IGNORE")
        group("data", REG_NAME_MULTI)
    }),
    GET_NAME(CmdPatterBuilder.create {
        commands("getname", "gn")
        group("data", REG_NAME_MULTI)
    }),

    // #4 osu! 谱面指令

    MAP(CmdPatterBuilder.create {
        commands("beatmap", "map$REG_IGNORE", "m$REG_IGNORE")
        // 看花眼了
        append("([:：](?<mode>\\w+))?\\s*(?<bid>\\d+)?\\s*([a%]?(?<accuracy>$REG_NUMBER_DECIMAL)[a%]?)?\\s*([cx]?(?<combo>$REG_NUMBER_DECIMAL)[cx]?)?\\s*([\\-m]?(?<miss>\\d+)[\\-m]?)?\\s*")
        appendMod()
    }),
    QUALIFIED_MAP(CmdPatterBuilder.create {
        commands("qualified", "qua$REG_IGNORE", "q$REG_IGNORE")
        appendMode()
        space()
        append("($REG_HASH(?<status>[-\\w]+))?\\s*(\\*?(?<sort>[\\-_+a-zA-Z]+))?\\s*(?<range>\\d+)?")
    }),
    LEADER_BOARD(CmdPatterBuilder.create {
        commands("mapscorelist", "leaderboard", "leader$REG_IGNORE", "list$REG_IGNORE", "l$REG_IGNORE")
        appendMode()
        space()
        group("bid", "\\d+")
        group("range", "\\d+")
    }),
    MAP_MINUS(CmdPatterBuilder.create {
        commands("mapminus", "mm$REG_IGNORE")
        appendMode()
        space()
        append("(?<id>\\d+)?\\s*($REG_MOD|([×xX]?\\s*(?<rate>$REG_NUMBER_DECIMAL)[×xX]?))?")
    }),
    NOMINATION(CmdPatterBuilder.create {
        command("$REG_EXCLAIM(nominat(e|ion)s?|nom$REG_IGNORE_BS|n$REG_IGNORE_BS)")
        column()
        // 这个mode还是特殊的mode
        group("mode", "bid|sid|s|b")
        space()
        appendSid()
    }),
    PP_PLUS_MAP(CmdPatterBuilder.create {
        commands("p?pa$REG_IGNORE", "ppplusmap", "pppmap", "plusmap")
        appendBid()
        space()
        appendMod()
    }),
    PP_PLUS(CmdPatterBuilder.create {
        // 245 个字符的正则...
        // ^[!！]\s*(?i)(ym)?(?<function>(p[px](?![A-Za-z_])|pp[pvx](?![A-Za-z_])|p?p\+|(pp)?plus|ppvs|pppvs|(pp)?plusvs|p?pa(?![A-Za-z_])|ppplusmap|pppmap|plusmap))\s*(?<area1>[0-9a-zA-Z\[\]\-_\s]*)?\s*([:：]\s*(?<area2>[0-9a-zA-Z\[\]\-_\s]*))?
        command("$REG_EXCLAIM(ym)?(?<function>(p[px]$REG_IGNORE|pp[pvx]$REG_IGNORE|p?p\\+|(pp)?plus|ppvs|pppvs|(pp)?plusvs))")
        append("(?<area1>$REG_NAME_ANY)?\\s*($REG_COLUMN\\s*(?<area2>$REG_NAME_ANY))?")
    }),

    // #5 osu! 比赛指令

    MATCH_LISTENER(CmdPatterBuilder.create {
        commands("(make\\s*love)", "(match)?listen(er)?", "ml$REG_IGNORE", "li$REG_IGNORE")
        group("matchid", "\\d+")
        space()
        group("operate", "info|list|start|stop|end|off|on|[lispefo]$REG_IGNORE")
    }),
    MU_RATING(CmdPatterBuilder.create {
        commands("(?<uu>(u{1,2})(rating|ra$REG_IGNORE))", "(?<main>((ym)?rating|(ym)?ra$REG_IGNORE|mra$REG_IGNORE))")
        group("matchid", "\\d+", whatever = false)
        // 这是个啥, 所有命令前面都带了 (?i), 大小写就忽略了
        // (\s*[Ee]([Zz]|a[sz]y)?\s*(?<easy>\d+\.?\d*)x?)?
        startGroup {
            space()
            append("e(z|a[sz]y)?")
            space()
            group("easy", REG_NUMBER_DECIMAL, whatever = false)
            append("x?")
        }
        space()
        group("skip", "-?\\d+")
        space()
        group("ignore", "-?\\d+")
        space()
        startGroup {
            append("\\[")
            group("remove", REG_NUMBER_MULTI, whatever = false)
            append("]")
        }
        space()
        group("rematch", "r")
        space()
        group("failed", "f")
    }),
    SERIES_RATING(CmdPatterBuilder.create {
        commands(
            "(?<uu>(u{1,2})(seriesrating|series|sra$REG_IGNORE|sa$REG_IGNORE))",
            "(ym)?(?<main>(seriesrating|series|sa$REG_IGNORE|sra$REG_IGNORE))",
            "(ym)?(?<csv>(csvseriesrating|csvseries|csa$REG_IGNORE|cs$REG_IGNORE))"
        )
        startGroup {
            +REG_HASH
            group("name", REG_ANY_1P)
            +REG_HASH
        }
        space()
        group("data", "[\\d\\[\\]\\s,，|\\-]+")
        space()
        startGroup {
            append("e(z|a[sz]y)?")
            space()
            group("easy", REG_NUMBER_DECIMAL, whatever = false)
            append("x?")
        }
        space()
        group("rematch", "r")
        space()
        group("failed", "f")

    }),
    CSV_MATCH(CmdPatterBuilder.create {
        commands("csvrating", "cra?(?![^s^x\\s])")
        group("x", "[xs]")
        space()
        group("data", "[\\d\\s,，|\\-]+")
    }),
    MATCH_ROUND(CmdPatterBuilder.create {
        commands("(match)?rounds?$REG_IGNORE", "mr$REG_IGNORE", "ro$REG_IGNORE")
        group("matchid", "\\d+")
        space()
        group("round", "\\d+")
        space()
        group("keyword", "[\\w\\s-_ %*()/|\\u4e00-\\u9fa5\\uf900-\\ufa2d]+")
    }),
    MATCH_NOW(CmdPatterBuilder.create {
        commands("monitornow", "matchnow", "mn$REG_IGNORE")
        group("matchid", "\\d+")
        space()
        startGroup {
            append("e(z|a[sz]y)?")
            space()
            group("easy", REG_NUMBER_DECIMAL, whatever = false)
            append("x?")
        }
        space()
        group("skip", "\\d+")
        space()
        group("ignore", "\\d+")
        space()
        startGroup {
            append("\\[")
            group("remove", REG_NUMBER_MULTI, whatever = false)
            append("]")
        }
        space()
        group("rematch", "r")
        space()
        group("failed", "f")
    }),
    MAP_POOL(CmdPatterBuilder.create {
        commands("mappool", "po$REG_IGNORE")
        appendMode(false)
        space()
        group("name", "\\w+", whatever = false)
    }),
    GET_POOL(CmdPatterBuilder.create {
        commands("getpool", "gp$REG_IGNORE")
        appendMode()
        space()
        startGroup {
            +REG_HASH
            group("name", REG_ANY_1P, whatever = false)
            +REG_HASH
        }
        whatever()
        space()
        group("data", REG_NAME_MULTI)
    }),
    // #6 聊天指令
    // ...
    // #7 娱乐指令

    DICE(CmdPatterBuilder.create {
        command("($REG_EXCLAIM|(?<dice>\\d+))\\s*(?i)(ym)?(dice|roll|d$REG_IGNORE)")
        group("number", "-?\\d*")
        group("text", REG_ANY_1P)
    }),
    DRAW(CmdPatterBuilder.create {
        commands("draw", "w$REG_IGNORE")
        group("d", "\\d")
    }),

    // #8 辅助指令
    OLD_AVATAR(CmdPatterBuilder.create {
        commands("(old|osu)?avatar", "oa$REG_IGNORE")
        appendQQId()
        appendUid()
        appendName()
    }),
    OVER_SR(CmdPatterBuilder.create {
        commands("overstarrating", "overrating", "oversr", "or$REG_IGNORE")
        group("SR", REG_NUMBER_DECIMAL)
    }),
    TRANS(CmdPatterBuilder.create {
        commands("trans", "tr$REG_IGNORE")
        group("a", "[A-G][＃#]", whatever = false)
        group("b", "\\w", whatever = false)
    }),
    KITA(CmdPatterBuilder.create {
        commands("k(?![^x\\s])", "kita")
        group("noBG", "x$REG_IGNORE")
        space()
        // 这里改了
        appendBid()
        space()
        appendMod()
        space()
        group("round", "[\\w\\s]+")
    }),
    GROUP_STATISTICS(CmdPatterBuilder.create {
        commands("groupstat(s)?", "groupstatistic(s)?", "统计(超限)?", "gs$REG_IGNORE")
        group("group", "$REG_COLUMN?[nah]|((新人|进阶|高阶)群)", whatever = false)
    }),

    // #9 自定义
    CUSTOM(CmdPatterBuilder.create {
        commands("custom", "c$REG_IGNORE")
        column()
        group("operate", "\\w+")
        group("type", "\\w+")
    }),
    TEST_PPM(CmdPatterBuilder.create {
        commands("testppm", "testcost", "tp$REG_IGNORE", "tc$REG_IGNORE")
        appendMode()
        group("data", REG_NAME_MULTI)
    }),
    TEST_HD(CmdPatterBuilder.create {
        commands("testhd", "th$REG_IGNORE")
        appendMode()
        group("data", REG_NAME_MULTI)
    }),
    TEST_FIX(CmdPatterBuilder.create {
        commands("testfix", "tf$REG_IGNORE")
        appendMode()
        space()
        group("data", REG_NAME_MULTI)
    }),
    TEST_MAP(CmdPatterBuilder.create {
        commands("testmap", "tm$REG_IGNORE")
        group("id", "\\d+")
        space()
        appendMod()
    }),
    TEST_TAIKO_SR_CALCULATE(CmdPatterBuilder.create {
        commands("testtaiko", "tt$REG_IGNORE")
        group("data", "[xo\\s]+")
    }),
    MAP_4D_CALCULATE(CmdPatterBuilder.create("^[!！＃#]\\s*(?i)cal") {
        space()
        group("type", "ar|od|cs|hp", whatever = false)
        space()
        group("value", REG_NUMBER_DECIMAL, whatever = false)
        space()
        appendModAny()

    }),
    DEPRECATED_BPHT(CmdPatterBuilder.create {
        commands("(?<bpht>bpht)")
        append("(-i)")
        whatever()
    }),
    DEPRECATED_SET(CmdPatterBuilder.create {
        commands("(?<set>set)")
    }),
    DEPRECATED_AYACHI_NENE(CmdPatterBuilder.create {
        commands("(?<nene>0d0(0)?)")
    }),
    DEPRECATED_YMX(CmdPatterBuilder.create {
        commands("(?<x>x)")
    }),
    DEPRECATED_YMY(CmdPatterBuilder.create {
        commands("(?<y>y)")
    }),
    ;

    fun matcher(input: CharSequence): Matcher = this.pattern.matcher(input)
}