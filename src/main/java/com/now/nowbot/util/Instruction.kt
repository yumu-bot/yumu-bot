package com.now.nowbot.util

import com.now.nowbot.util.command.*
import com.now.nowbot.util.command.MatchLevel.*
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class Instruction(val pattern: Pattern) {
    // #0 调出帮助
    HELP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("h", "完整帮助")
        appendCaptureGroup("module", PATTERN_ANYTHING, ANY, MAYBE)
    }),

    SIMPLIFIED_HELP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("help", "helps", "帮助", "文档")
        appendCaptureGroup("module", PATTERN_ANYTHING, ANY, MAYBE)
    }),

    // #1 BOT 内部指令
    PING(CommandPatternBuilder.create {
        appendGroup {
            appendGroup {
                appendCommandsIgnoreAll("ping", "pi", "探测")
            }
            append(CHAR_SEPARATOR)
            appendGroup {
                append("yumu${PATTERN_SPACE}${LEVEL_ANY}${PATTERN_QUESTION}")
            }
        }
    }),

    BIND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<ub>ub|解绑|解除绑定)", "(?<bi>bi|绑定)", "(?<un>un)?(?<bind>bind)")
        appendColonCaptureGroup("full", "f")
        appendSpace()
        appendQQ()
        appendName()
    }),

    SB_BIND(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(?<ub>ub|解绑|解除绑定)", "(?<bi>bi|绑定)", "(?<un>un)?(?<bind>bind)")
        appendQQ()
        appendSBName()
    }),

    SERVICE_SWITCH_ON(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(switch|service)\\s*on", "so", "服务开启", "服务打开", "服务启用")
        appendQQOrQQGroup()
        appendCaptureGroup(FLAG_SERVICE, "[^\\d#＃]", MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_NAME, PATTERN_ANYTHING, MORE)
    }),

    SERVICE_SWITCH_OFF(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(switch|service)\\s*off", "sf", "服务关闭", "服务禁用")
        appendQQOrQQGroup()
        appendCaptureGroup(FLAG_SERVICE, "[^\\d#＃]", MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_NAME, PATTERN_ANYTHING, MORE)
    }),

    SERVICE_SWITCH_LIST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(switch|service)\\s*list", "sl", "服务列表")
        appendQQOrQQGroup()
        appendCaptureGroup(FLAG_SERVICE, "\\D", MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_NAME, PATTERN_ANYTHING, MORE)
    }),

    ECHO(CommandPatternBuilder.create {
        append("(#echo\\s*|([!！]\\s*(ym)?(echo|回声|e[co])(?![A-Za-z\\-_])))")
        appendSpace()
        appendQQOrQQGroup(true, MORE) {
            appendCaptureGroup(FLAG_ANY, "(?:(?!(?:group[＝=])?\\d{6,10}\\b).)+", EXIST, EXIST)
        }
    }),

    REVOKE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("revoke", "rv", "撤回")
    }),

    SERVICE_COUNT(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("servicecount", "sc", "统计服务调用", "统计")
        appendCaptureGroup(FLAG_TIME,
            PATTERN_TIME,
            ANY
        )
    }),

    SYSTEM_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("system\\s*info", "sys(tem)?", "si", "sy", "系统")
    }),

    CHECK(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("check", "ck", "检查")
        appendQQUIDName()
    }),

    VIEW_GROUP_MODE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("view\\s*group\\s*mode", "vm", "查看(群聊)?模式")
        appendCaptureGroup(FLAG_RANGE,
            PATTERN_NUMBER,
            ANY
        )
    }),

    GROUP_COLLECT(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("group\\s*collect(ion)?", "go", "群组回收")
        appendQQOrQQGroup(isDefaultGroup = true)
    }),

    // #2 osu! 成绩指令
    SET_MODE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("set\\s*(game\\s*)?mode", "(game\\s*)?mode", "sm", "mo", "(游戏)?模式")
        appendColonCaptureGroup(FLAG_MODE, PATTERN_MODE, prefixLevel = MAYBE)
        appendSpace()
        appendQQUIDName()
    }),

    SB_SET_MODE(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("set\\s*(game\\s*)?mode", "(game\\s*)?mode", "sm", "mo", "(游戏|偏偏要上班|ppysb|sb(服|\\s*)?)?模式")
        appendColonCaptureGroup(FLAG_MODE, PATTERN_MODE, prefixLevel = MAYBE)
        appendSpace()
        appendQQUIDSBName()
    }),

    SET_GROUP_MODE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(set)?\\s*group\\s*mode", "gm", "群聊?模式")
        appendColonCaptureGroup(FLAG_MODE, PATTERN_MODE, prefixLevel = MAYBE)
        appendSpace()
        appendQQ(isDefaultGroup = true)
        appendQQGroup(isDefaultGroup = true)
    }),

    SCORE_PR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p|通过成绩)|(?<recent>(recent|r|最近成绩)))((?<s>s)|(?<w>(show|w)))?")
        appendModeQQUIDNameRange()
        appendIgnore(PATTERN_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(PATTERN_HYPHEN)
        appendRange()
    }),

    RECENT_BEST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("recents?\\s*bests?|rbs?|red\\s*bull|(最近)?优秀成绩")
        appendModeQQUIDNameRange()
    }),

    SB_SCORE_PR(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p|通过成绩)|(?<recent>(recent|r|最近成绩)))((?<s>s)|(?<w>(show|w)))?")
        appendModeQQUIDSBNameRange()
        appendIgnore(PATTERN_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(PATTERN_HYPHEN)
        appendRange()
    }),

    PR_CARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<pass>(passcard|pc|通过卡片?))", "(?<recent>(recentcard|rc|最近卡片?))")
        appendModeQQUIDNameRange()
    }),

    UU_PR(CommandPatternBuilder.create {
        appendUUIgnoreAll("(?<pass>(pass(?!s)(?<es>es)?|p)|(?<recent>(recent|r)))")
        appendModeQQUIDNameRange()
        appendIgnore(PATTERN_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(PATTERN_HYPHEN)
        appendRange()
    }),

    SCORE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(score|s)", "成绩")
        appendModeBIDQQUIDNameMod()
    }),

    SCORE_SHOW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(score|s)\\s*(show|w)", "展示成绩")
        appendModeBIDQQUIDNameMod()
    }),

    UU_SCORE(CommandPatternBuilder.create {
        appendUUIgnoreAll("score", "s")
        appendModeBIDQQUIDNameMod()
    }),

    SCORES(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("scores", "ss", "多成绩")
        appendModeBIDQQUIDNameMod()
    }),

    SB_SCORE(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(score|s|成绩)")
        appendModeBIDQQUIDSBNameMod()
    }),

    SB_SCORE_SHOW(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(score|s)\\s*(show|w)")
        appendModeBIDQQUIDSBNameMod()
    }),

    SB_SCORES(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("scores", "ss", "多成绩")
        appendModeBIDQQUIDSBNameMod()
    }),

    BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(bestperformance|best|bp|b|最[好佳]成绩)((?<s>s)|(?<w>show|w))?")
        appendModeQQUIDNameRange()
        appendIgnore(PATTERN_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(PATTERN_HYPHEN)
        appendRange()
    }),

    SB_BP(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(bestperformance|best|bp|b|最[好佳]成绩)((?<s>s)|(?<w>show|w))?")
        appendModeQQUIDSBNameRange()
        appendIgnore(PATTERN_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(PATTERN_HYPHEN)
        appendRange()
    }),

    TODAY_BP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t", "今日(最[好佳])?成绩")
        appendModeQQUIDNameRange()
        appendIgnore(PATTERN_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(PATTERN_HYPHEN)
        appendRange()
    }),

    SB_TODAY_BP(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t", "今日(最[好佳])?成绩")
        appendModeQQUIDSBNameRange()
        appendIgnore(PATTERN_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendIgnore(PATTERN_HYPHEN)
        appendRange()
    }),

    BP_FIX(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(b(p|est))\\s*fix", "fix(b(p|est))", "bestperformancefix", "bpf", "bf", "boy\\s*friends?", "修补(最[好佳])?成绩")
        appendModeQQUIDNameRange()
    }),

    BP_ANALYSIS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(b(p|est))\\s*analysis", "blue\\s*archive", "bpa", "ba", "分析(最[好佳])?成绩")
        appendModeQQUIDName()
    }),

    BP_ANALYSIS_LEGACY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(b(p|est))\\s*analysis\\s*legacy", "bpal", "bal", "al")
        appendModeQQUIDName()
    }),

    UU_BA(CommandPatternBuilder.create {
        appendUUIgnoreAll("(bp?)?a", "((b(p|est))\\s*analysis)")
        appendModeQQUIDName()
    }),

    BP_HISTORY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(b(p|est))\\s*history", "bh", "bph", "历史(最[好佳])?(成绩)?切片")
        appendModeQQUIDName()
    }),

    TOP_PLAYS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("top\\s*plays?", "top", "tp")

        appendMode()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_12, prefixLevel = MAYBE)
    }),

    // #3 osu! 玩家指令
    INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("information", "info", "i", "(玩家|个人)?信息|玩家")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, PATTERN_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    SB_INFO(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("information", "info", "i", "(玩家|个人)?信息|玩家")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, PATTERN_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    WAIFU_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("info(rmation)?s?\\s*(wif[ei]|waifu)", "iw", "waifu", "wife", "i\\s*wanna", "(老婆)?信息|老婆")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, PATTERN_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    LEGACY_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("info(rmation)?s?\\s*(legacy)", "il")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, PATTERN_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    INFO_CARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("informationcard", "infocard", "ic", "(玩家|个人)?信息卡片?", "(玩家|个人)卡片?")
        appendModeQQUIDName()
    }),

    CSV_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(c(sv)?)information", "(c(sv)?)info", "(c(sv)?)i")
        appendMode()
        appendCaptureGroup("data", PATTERN_USERNAME_SEPARATOR, ANY)
    }),

    UU_INFO(CommandPatternBuilder.create {
        appendUUIgnoreAll("info", "i")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DAY, PATTERN_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    I_MAPPER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mapper", "immapper", "imapper", "im", "(谱师|作者)信息|(谱师|作者)")
        appendQQUIDName()
    }),

    FRIEND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("friends?", "fuck", "f", "([好朋]|好朋)友(信息|关系)?", "操")
        appendColonCaptureGroup(FLAG_SORT, PATTERN_ANYTHING_BUT_NO_SPACE, MORE)
        appendQQ()
        appendUID()
        appendNameAndRange()
        appendIgnore(PATTERN_OPERATOR)
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendIgnore(PATTERN_HYPHEN)
        appendRange()
    }),

    MUTUAL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mutual", "mua?", "亲亲", "主页链接|主页|链接")
        appendCaptureGroup("names", PATTERN_USERNAME_SEPARATOR, ANY)
    }),

    PP_MINUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(p?p[m\\-])", "(pp)?minus", "表现分减")
        appendModeQQUID()
        append2Name()
    }),

    PP_MINUS_VS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(p?pv)", "p?pm(inus)?vs", "表现分减对抗")
        appendModeQQUID()
        append2Name()
    }),

    PP_MINUS_LEGACY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(p?pl)", "(pp)?minus\\s*legacy")
        appendModeQQUID()
        append2Name()
    }),

    TEAM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("tm", "team", "clan", "战队(信息)?")
        appendModeQQUIDName()
        appendStarCaptureGroup(FLAG_ID, PATTERN_NUMBER, MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    SKILL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("skills?", "k", "(玩家)?技[巧术]")
        appendModeQQUID()
        append2Name()
    }),

    SKILL_VS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("skills?\\s*v(ersu)?s", "kv", "(玩家)?技[巧术]对抗")
        appendModeQQUID()
        append2Name()
    }),

    ETX(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("elite(ronix)?", "ex", "et", "etx", "精英分数?")
        appendModeQQUID()
        append2Name()
    }),

    ETX_VS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("elite(ronix)?\\s*v(ersu)?s", "ev", "etx\\s*vs", "精英分数?对抗")
        appendModeQQUID()
        append2Name()
    }),

    BADGE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("badge", "bd", "(主页)?(奖牌|牌子)")
        appendQQUIDName()
    }),

    GUEST_DIFFICULTY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(get)?\\s*guest", "guest\\s*diff(er)?", "gd(er)?", "客串(谱师|作者)?")
        appendModeQQUIDNameRange()
    }),

    QUICK_PLAY_INFO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("quick\\s*play\\s*info", "rank(ed)?\\s*play\\s*info", "qi", "ri", "iq", "ir", "(排位|(快速)?匹配|快匹)信息")
        appendModeQQUIDName()
    }),

    GET_ID(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*id", "gi")
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, ANY)
    }),

    GET_NAME(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*name", "gn")
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, ANY)
    }),

    // #4 osu! 谱面指令
    AUDIO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("audio", "song", "a")
        appendSpace()
        appendBIDOrSID()
    }),

    MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("beatmap", "map", "m")

        appendMode()
        appendBIDOrSID()
        appendSpace()
        appendCaptureGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_PLUS, MORE)
        appendSpace()

        appendMod()
    }),

    MAP_LAZER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("beatmap\\s*la[zs]er", "map\\s*la[zs]er", "mz")

        appendMode()
        appendBIDOrSID()
        appendSpace()
        appendCaptureGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_PLUS, MORE)
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
            appendCaptureGroup(FLAG_SORT, "[\\-_+a-zA-Z]", MORE)
        }

        appendSpace()
        appendGroup(MAYBE) {
            append("genre=")
            appendSpace()
            appendCaptureGroup("genre", "\\w", MORE)
        }

        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    LEADER_BOARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("leader\\s*board", "leader", "list", "l")

        appendMode()
        appendBIDOrSID()

        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, MAYBE)
        appendStarCaptureGroup(FLAG_TYPE2, PATTERN_WORD, MORE)
        appendSpace()

        appendMod()
    }),

    GROUP_LEADER_BOARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("group\\s*leaderboard", "group\\s*leader", "g(roup)?\\s*list", "good\\s*luck", "gl", "lg")

        appendMode()
        appendBIDOrSID()
        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, MAYBE)
        appendMod()
    }),

    LEGACY_LEADER_BOARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("legacy\\s*leaderboard", "legacy\\s*leader", "legacy\\s*list", "love\\s*live", "ll")

        appendMode()
        appendBIDOrSID()

        appendAtLeastSpaceGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, MAYBE)
        appendStarCaptureGroup(FLAG_TYPE2, PATTERN_WORD, MORE)
        appendSpace()

        appendMod()
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
            appendCaptureGroup("rate", PATTERN_NUMBER_DECIMAL, MORE)
            append("[×xX]")
            appendMatchLevel(MAYBE)
        }
    }),

    NOMINATION(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(nominat(e|ion)s?|nom|n)", "(谱面)?(提名|上架(流程)?)")
        appendBIDOrSID()
    }),

    PP_PLUS_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("p?pa", "ppplusmap", "pppmap", "plusmap")
        appendBID()
        appendMod()
    }),

    PP_PLUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(?<function>(p[px]|pp[pvx]|p?p\\+|(pp)?plus|ppvs|pppvs|(pp)?plusvs))")
        appendCaptureGroup("area1", PATTERN_USERNAME, ANY)
        appendSpace()
        appendColonCaptureGroup("area2", PATTERN_USERNAME, ANY)
    }),

    EXPLORE(
        CommandPatternBuilder.create {
            appendCommandsIgnoreAll("explore", "exp", "e", "find", "search")
            appendColonCaptureGroup(FLAG_TYPE, PATTERN_ANYTHING_BUT_NO_SPACE, MORE)
            appendSpace()
            appendCaptureGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH, MORE)
            appendSpace()
            appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, MAYBE)
        }
    ),

    EXPLORE_MOST_PLAYED(
        CommandPatternBuilder.create {
            appendCommandsIgnoreAll("(explore|exp|e|find)\\s*([mp]|most|played|mp|pm|play)")
            appendColonCaptureGroup(FLAG_TYPE, PATTERN_ANYTHING_BUT_NO_SPACE, MORE)
            appendSpace()
            appendCaptureGroup(FLAG_ANY, PATTERN_ANYTHING_BUT_NO_HASH, MORE)
            appendSpace()
            appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, MAYBE)
        }
    ),

    RECOMMEND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("recommend(ed)?", "rec", "j", "推荐(谱面)?")
        appendModeQQUIDName()
    }),

    POPULAR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("popular\\s*(group)?", "pu", "pg", "(热门|流行)(谱面)?")
        appendMode()
        appendQQGroup(isDefaultGroup = true)
        appendRange()
    }),

    GET_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*(((beat)?map)|diff(icult(y|ies))?)", "get\\s*map", "ga", "获取难度")
        appendID()
        appendMod()
    }),

    GET_NEWBIE_MAP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*new(bie)?\\s*(beat)?map", "get\\s*map", "gw", "获取(新人(群?玩家)?)?谱面")
        appendMode()
        appendCaptureGroup(FLAG_ID, PATTERN_NUMBER_SEPARATOR, MORE)
        appendMod()
    }),

    GET_NEWBIE_SET(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*new(bie)?\\s*(beatmap)?set", "get\\s*(map)?set", "gy", "获取(新人(群?玩家)?)?谱面[集组]")
        appendMode()
        appendCaptureGroup(FLAG_ID, PATTERN_NUMBER_SEPARATOR, MORE)
        appendMod()
    }),

    GET_NEWBIE_SCORE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*new(bie)?\\s*score", "get\\s*score", "gx", "获取(新人(群?玩家)?)?成绩")
        appendMode()
        appendID()
        appendCaptureGroup("accuracy", "($PATTERN_NUMBER_DECIMAL)", MAYBE)
        appendSpace()
        appendCaptureGroup("combo", "($PATTERN_NUMBER_DECIMAL)", MAYBE)
        appendSpace()
        appendCaptureGroup("pp", "($PATTERN_NUMBER_DECIMAL)", MAYBE)
        appendSpace()
        appendCaptureGroup("rank", "(ssh|ss|sh|[abcdsx])", MAYBE)
        appendSpace()
        appendMod()
    }),

    GET_NEWBIE_BEST(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*new(bie)?\\s*((best)|s)", "get\\s*best", "获取(新人(群?玩家)?)?最[好佳](成绩)?")
        appendModeQQUIDName()
        appendStarCaptureGroup(FLAG_TIME, PATTERN_TIME, MAYBE)
        appendSpace()
        appendHashCaptureGroup(FLAG_RANGE, PATTERN_NUMBER_13, MAYBE)
    }),

    GET_NEWBIE_PLAYER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*new(bie)?\\s*(user|player)", "get\\s*(user|player)", "gu", "获取新人(群?玩家)?")
        appendModeQQUIDName()
        appendHashCaptureGroup(FLAG_DATA, PATTERN_NUMBER_DECIMAL, MAYBE)
    }),

    GET_BG(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*background", "get\\s*bg", "gb", "bg", "获取(谱面)?背景")
        appendSpace()
        appendCaptureGroup(FLAG_DATA, PATTERN_NUMBER_SEPARATOR, MORE)
    }),

    GET_COVER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("get\\s*cover", "gc")
        appendColonCaptureGroup(FLAG_TYPE, PATTERN_COVER, MORE)
        appendSpace()
        appendCaptureGroup(FLAG_DATA, PATTERN_NUMBER_SEPARATOR, MORE)
    }),

    VIEW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("view", "v")
        appendBIDOrSID()
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    VIEW_VARIATION(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(var(iation)?|sv)\\s*view", "view\\s*(var(iation)?|sv)", "view\\s*(\\+|plus)", "v[\\s:：]*v")
        appendBIDOrSID()
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    // #5 osu! 比赛指令
    MATCH_LISTENER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("make\\s*love", "(match)?listen(er)?", "ml", "li")

        appendMatchID()
        appendSpace()
        appendCaptureGroup(FLAG_OPERATE, "info|list|start|stop|end|off|on|[lispefo]")
        appendSpace()
        appendHashCaptureGroup(FLAG_SKIP, PATTERN_NUMBER_1_100, contentLevel = MORE, prefixLevel = MAYBE)
        appendIgnore()
    }),

    MU_RATING(CommandPatternBuilder.create {
        append(PATTERN_EXCLAMATION)
        appendSpace()
        append("((?<uu>(u{1,2})(rating|ra))|(?<main>((ym)?rating|((ym)?ra)|(mra))))")
        appendIgnore()
        appendSpace()

        appendMatchID()
        appendMatchParam()
    }),

    SERIES_RATING(CommandPatternBuilder.create {
        append(PATTERN_EXCLAMATION)
        appendSpace()
        appendGroup(MAYBE, "ym")
        appendGroup(
            "(?<uu>(u{1,2})(seriesrating|series|sra|sa))|((ym)?(?<main>(seriesrating|series|sa|sra)))|((ym)?(?<csv>(csvseriesrating|csvseries|csa|cs)))"
        )
        appendSpace()
        appendGroup(MAYBE) {
            append(PATTERN_HASH)
            appendCaptureGroup("name", PATTERN_ANYTHING, MORE)
            append(PATTERN_HASH)
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
        appendCaptureGroup(FLAG_DATA, PATTERN_NUMBER_SEPARATOR, MORE)
    }),

    MATCH_ROUND(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(match)?\\s*rounds?", "ro")
        appendMatchID()
        appendSpace()
        appendCaptureGroup("round", PATTERN_NUMBER, MORE)
        appendSpace()
        appendCaptureGroup("keyword", PATTERN_ANYTHING, MORE) // "[\\w\\s-_ %*()/|\\u4e00-\\u9fa5\\uf900-\\ufa2d]"
    }),

    MATCH_NOW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("monitor\\s*now", "match\\s*now", "mn")
        appendMatchID()
        appendMatchParam()
    }),

    MATCH_RECENT(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("match\\s*recents?", "mr")
        appendMode()
        appendCaptureGroup(FLAG_MATCH_ID, PATTERN_NUMBER_SEPARATOR, MORE)
        appendQQ()
        appendUID()
        appendName()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_12, MAYBE)
    }),

    QUICK_PLAY(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("quick\\s*play", "rank(ed)?\\s*play", "qp", "rp")
        appendModeQQUIDNameRange()
    }),

    MAP_POOL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mappool", "po")
        appendMode()
        appendSpace()
        appendCaptureGroup(FLAG_NAME, PATTERN_ANYTHING, MORE)
    }),

    GET_POOL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("getpool", "gp")
        appendMode()
        appendSpace()
        appendGroup(MAYBE) {
            append(PATTERN_HASH)
            appendCaptureGroup(FLAG_NAME, PATTERN_ANYTHING, MORE)
            append(PATTERN_HASH)
        }
        appendCaptureGroup(FLAG_DATA, PATTERN_ANYTHING, ANY)
    }),

    CALCULATE_NEWBIE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(calculate|cal|csv)\\s*newbie", "cn")
        appendMode()
        appendCaptureGroup(FLAG_DATA, PATTERN_ANYTHING_BUT_NO_HASH_STARS, MORE)
        appendSpace()
        appendHashCaptureGroup(FLAG_BID, PATTERN_ANYTHING_BUT_NO_STARS, MORE)
        appendSpace()
        appendStarCaptureGroup(FLAG_SID, PATTERN_NUMBER, MORE)
    }),

    // #6 聊天指令

    BILI_USER(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("bili\\s*user", "bu")
        appendID()
    }),

    // ...
    // #7 娱乐指令

    DICE(CommandPatternBuilder.create {
        append("($PATTERN_EXCLAMATION|(?<dice>\\d+))\\s*(?i)(ym)?(dice|roll|d(?!${PATTERN_IGNORE}))")
        appendCaptureGroup("number", "$PATTERN_HYPHEN?($PATTERN_NUMBER_DECIMAL)")
        appendSpace()
        appendCaptureGroup(FLAG_TEXT, PATTERN_ANYTHING, MORE)
    }),

    GUESS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("guess", "g")
        appendMode()
        appendQQ()
        appendUID()
        appendNameAnyButNoHash()
        appendHashCaptureGroup(FLAG_ID, PATTERN_NUMBER_13)
    }),

    GUESS_GIVE_UP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("guess\\s*(give\\s*up)|good\\s*game", "gg", "ggwp")
    }),

    DRAW(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("draw", "w")
        appendCaptureGroup("d", PATTERN_NUMBER)
    }),

    // #8 辅助指令
    OLD_AVATAR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(old|osu)?\\s*avatar", "oa", "o")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, ANY)
    }),

    OLD_AVATAR_CARD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(old|osu)?\\s*(card|chicken)", "oishi", "oc")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, ANY)
    }),

    SB_OLD_AVATAR(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(old|osu)?\\s*avatar", "oa", "o")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, ANY)
    }),

    SB_OLD_AVATAR_CARD(CommandPatternBuilder.create {
        appendSBCommandsIgnoreAll("(old|osu)?\\s*(card|chicken)", "oc")
        appendModeQQUID()
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, ANY)
    }),

    OSU_AVATAR_PROFILE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("(old|osu)?\\s*profile", "op")
    }),

    OVER_SR(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("over\\s*starrating", "over\\s*rating", "overstar", "oversr", "or")
        appendCaptureGroup("SR", PATTERN_NUMBER_DECIMAL)
    }),

    TRANS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("trans", "tr")
        appendCaptureGroup("a", "[A-G][＃#]?", EXIST)
        appendCaptureGroup("b", PATTERN_NUMBER, EXIST)
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
        appendColonCaptureGroup("operate", PATTERN_WORD, contentLevel = MORE, prefixLevel = MAYBE)
        appendSpace()
        appendColonCaptureGroup("type", PATTERN_WORD, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    TEST_SKILL(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testskill", "tl")
        appendMode()
        appendCaptureGroup(FLAG_DATA, PATTERN_NUMBER_SEPARATOR, MORE)
    }),

    TEST_PPM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testppm", "te", "tpm")
        appendModeQQUID()
        append2Name()
    }),

    CSV_PPM(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("csvppm", "cm")
        appendMode()
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, MORE)
    }),

    TEST_HD(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testhd", "th")
        appendMode()
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, MORE)
    }),

    TEST_FIX(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("testfix", "tf")
        appendMode()
        appendCaptureGroup(FLAG_DATA, PATTERN_USERNAME_SEPARATOR, MORE)
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
            appendCaptureGroup("rate", PATTERN_NUMBER_DECIMAL, MORE)
            append("[×xX]")
            appendMatchLevel(MAYBE)
        }
    }),

    MAP_4D_CALCULATE(CommandPatternBuilder.create {
        appendCommands("cal", "calculate", "cl")
        appendCaptureGroup("type", "ar|od|cs|hp", EXIST, EXIST)
        appendSpace()
        appendCaptureGroup("value", PATTERN_NUMBER_DECIMAL, EXIST, EXIST)
        appendSpace()
        appendMod(plusMust = true)
    }),

    TEST_MATCH_START(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("teststart", "ts")
        appendID()
        appendSpace()
        appendCaptureGroup("round", PATTERN_NUMBER, MORE)
    }),

    EASTER_AYACHI_NENE(CommandPatternBuilder.create {
        append("(?<nene>0d0(0)?)")
    }),

    EASTER_WHAT(CommandPatternBuilder.create {
        append(PATTERN_EXCLAMATION)
        appendMatchLevel(MORE)
        appendSpace()
        append("(gsm|干什么)")
        appendSpace()
        append(PATTERN_EXCLAMATION)
        appendMatchLevel(ANY)
        appendIgnore()
    }),

    // #10 辅助指令

    REFRESH_HELP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("refresh\\s*help", "rh")
    }),

    REFRESH_FILE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("refresh\\s*file", "rf")
        appendCaptureGroup("bid", PATTERN_NUMBER, MORE)
    }),

    UPDATE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("update", "ut", "ue")
        appendColonCaptureGroup(FLAG_MODE, PATTERN_ANYTHING_BUT_NO_SPACE, contentLevel = MORE, prefixLevel = MAYBE)
        appendSpace()
        appendCaptureGroup(FLAG_ANY, PATTERN_ANYTHING, contentLevel = MORE)
    }),

    FETCH(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("fetch", "fh", "fe")
        appendColonCaptureGroup(FLAG_ANY, PATTERN_ANYTHING, contentLevel = MORE, prefixLevel = MAYBE)
    }),



    // #11 maimai & CHUNITHM

    MAI_SCORE(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(score|song)", "ms")
        appendColonCaptureGroup(FLAG_DIFF, PATTERN_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendQQ()
        appendCaptureGroup(FLAG_VERSION, PATTERN_MAI_CABINET)
        appendSpace()
        appendNameAnyButNoHashStars()
        appendStarCaptureGroup(FLAG_DATA, PATTERN_ANYTHING_BUT_NO_HASH, MORE)
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100)
    }),

    MAI_VERSION(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*version", "mv")
        appendColonCaptureGroup(FLAG_DIFF, PATTERN_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendQQ()
        appendNameAnyButNoHash()
        appendHashCaptureGroup(FLAG_VERSION, PATTERN_ANYTHING, contentLevel = MORE, prefixLevel = MAYBE)
    }),

    MAI_SEEK(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(seek)", "mk")
        appendCaptureGroup(FLAG_NAME, PATTERN_ANYTHING, MORE)
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
        appendColonCaptureGroup(FLAG_DIFF, PATTERN_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendNameAnyButNoHash()
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, MAYBE)
    }),

    MAI_VERSUS(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*v(ersu)?s", "xvs?", "x\\s*video")
        appendModeQQUID()
        append2Name()
    }),

    MAI_AP(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(((all\\s?)?perfect)|(ap))", "xp")
        appendQQ()
        appendNameAnyButNoHash()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    MAI_FC(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*((full\\s?combo)|(fc))", "xc")
        appendQQ()
        appendNameAnyButNoHash()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, contentLevel = MAYBE, prefixLevel = MAYBE)
    }),

    MAI_AUDIO(CommandPatternBuilder.create {
        appendCommandsIgnoreAll("mai(mai)?\\s*(audio)", "xa")
        appendColonCaptureGroup(FLAG_DIFF, PATTERN_ANYTHING_BUT_NO_SPACE, MORE)
        appendSpace()
        appendNameAnyButNoHash()
        appendSpace()
        appendHashCaptureGroup(FLAG_PAGE, PATTERN_NUMBER_1_100, MAYBE)
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
        if (i != Instruction.UPDATE) continue

        println("${i.name}: ${i.pattern.pattern()}")
    }
}