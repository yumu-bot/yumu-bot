package com.now.nowbot.util

import com.now.nowbot.util.command.*
import com.now.nowbot.util.command.CmdPatterBuilder
import java.util.regex.Pattern

enum class OfficialInstruction(val pattern: Pattern) {
    // #0 调出帮助
    HELP(CmdPatterBuilder.create {
        commandsOfficial("h")
        group("module", REG_ANY)
    }),

    AUDIO(CmdPatterBuilder.create {
        commandsOfficial("a")
        column()
        group("type", "(bid|b|sid|s)")
        space()
        group("id", "\\d+")
    }),

    // #1 BOT 内部指令
    PING(CmdPatterBuilder.create {
        commandsOfficial(true, "pi", "?")
    }),

    BIND(CmdPatterBuilder.create {
        commandsOfficial("(?<ub>ub(?![A-Za-z_]))", "(?<bi>bi$REG_IGNORE)", "(?<un>un)?(?<bind>bind)")
        append("($REG_COLUMN(?<full>f))?")
        space()
        appendQQId()
        space()
        appendName()
    }),

    // #2 osu! 成绩指令
    SET_MODE(CmdPatterBuilder.create {
        commandsOfficial("sm$REG_IGNORE", "mo$REG_IGNORE")
        startGroup {
            column()
            +REG_MODE
        }
    }),

    SCORE_PR(CmdPatterBuilder.create {
        // 这一坨没法拆好像
        commandOfficial("(?<pass>(?<es>es)?|p$REG_IGNORE_S)|(?<recent>(r$REG_IGNORE_S))(?<s>s)?")
        space()
        `append(ModeQQUidNameRange)`()
    }),

    PR_CARD(CmdPatterBuilder.create {
        commands("(?<pass>(pc$REG_IGNORE))", "(?<recent>(rc$REG_IGNORE))")
        space()
        `append(ModeQQUidNameRange)`()
    }),

    UU_PR(CmdPatterBuilder.create {
        append("${REG_EXCLAIM}uu(?<pass>(p$REG_IGNORE))|uu(?<recent>(r$REG_IGNORE))")
        space()
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
        command("$REG_EXCLAIM(?<bp>b$REG_IGNORE_S)(?<s>s)?")
        `append(ModeQQUidNameRange)`()
    }),

    TODAY_BP(CmdPatterBuilder.create {
        commandWithIgnore("todaybp", "todaybest", "todaybestperformance", "tbp", "tdp", "t")
        space()
        `append(ModeQQUidNameRange)`()
    }),

    BP_FIX(CmdPatterBuilder.create {
        commandOfficialWithIgnore("bf")
        space()
        `append(ModeQQUidName)`()
    }),

    BP_ANALYSIS(CmdPatterBuilder.create {
        commandOfficialWithIgnore("ba")
        space()
        `append(ModeQQUidName)`()
    }),

    UU_BA(CmdPatterBuilder.create {
        append("${REG_EXCLAIM}uu?((bp?)?a)(?<info>(-?i))?$REG_SPACE_ANY|uubpanalysis")
        space()
        `append(ModeQQUidName)`()
    }),

    // #3 osu! 玩家指令
}