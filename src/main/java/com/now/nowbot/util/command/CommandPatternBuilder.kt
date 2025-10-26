package com.now.nowbot.util.command

import org.intellij.lang.annotations.Language
import java.util.regex.Pattern
import com.now.nowbot.util.command.MatchLevel.*

class CommandPatternBuilder private constructor(start: String? = null) {

    /**
     * 加命令, 后面自带 space, 展开后为 (!ym(p|pr))\s*
     * @param commands 连续的命令 ("p", "pr")
     */
    fun appendCommands(@Language("RegExp") vararg commands: String) {
        appendCommandsPrivate(*commands)
        appendSpace()
    }

    // 效果等同于 appendCommandsIgnore(null, ...)
    fun appendCommandsIgnoreAll(@Language("RegExp") vararg commands: String) {
        appendCommandsPrivate(*commands)
        appendIgnore()
        appendSpace()
    }

    fun appendCommandsIgnore(@Language("RegExp") ignore: String?, @Language("RegExp") vararg commands: String) {
        appendCommandsPrivate(*commands)
        appendIgnore(ignore)
        appendSpace()
    }

    /**
     * 加命令, 后面自带 space, 展开后为 (?ym(p|pr))\s*
     * @param commands 连续的命令 ("p", "pr")
     */
    fun appendSBCommandsIgnoreAll(@Language("RegExp") vararg commands: String) {
        appendGroup {
            append(REG_QUESTION)
            appendSpace()
            appendGroup(MAYBE, "ym")
            appendGroup(*commands)
        }
        appendIgnore()
        appendSpace()
    }

    private fun appendCommandsPrivate(@Language("RegExp") vararg commands: String) {
        appendGroup {
            append(REG_EXCLAMATION)
            appendSpace()
            appendGroup(MAYBE, "ym")
            appendGroup(*commands)
        }
    }

    // 就不到三个地方使用, 而且里面不到3行的方法很简短没必要单独写
    /**
     * 加命令, 展开后为 (!uu(p|pr))\s*
     * @param commands 连续的命令 ("p", "pr")
     */
    fun appendUU(@Language("RegExp") vararg commands: String) {
        appendUUPrivate(*commands)
        appendSpace()
    }


    // 效果等同于 appendCommandsIgnore(null, ...)
    fun appendUUIgnoreAll(@Language("RegExp") vararg commands: String) {
        appendUUPrivate(*commands)
        appendIgnore()
        appendSpace()
    }

    fun appendUUIgnore(@Language("RegExp") ignore: String?, @Language("RegExp") vararg commands: String) {
        appendCommandsPrivate(*commands)
        appendIgnore(ignore)
        appendSpace()
    }

    private fun appendUUPrivate(@Language("RegExp") vararg commands: String) {
        appendGroup {
            append(REG_EXCLAMATION)
            appendSpace()
            append("uu?")
            appendGroup(*commands)
        }
    }

    /**
     * 加命令, 官方机器人的匹配方式
     * @param commands 连续的命令 ("p", "pr")
     */
    fun appendOfficialCommands(@Language("RegExp") vararg commands: String) {
        appendOfficialCommandsPrivate(*commands)
        appendSpace()
    }

    // 效果等同于 appendOfficialCommandsIgnore(null, ...)
    fun appendOfficialCommandsIgnoreAll(@Language("RegExp") vararg commands: String) {
        appendOfficialCommandsPrivate(*commands)
        appendIgnore()
        appendSpace()
    }

    fun appendOfficialSBCommandsIgnoreAll(@Language("RegExp") vararg commands: String) {
        appendOfficialSBCommandsPrivate(*commands)
        appendIgnore()
        appendSpace()
    }

    fun appendOfficialCommandsIgnore(@Language("RegExp") ignore: String?, @Language("RegExp") vararg commands: String) {
        appendOfficialCommandsPrivate(*commands)
        appendIgnore(ignore)
        appendSpace()
    }

    private fun appendOfficialCommandsPrivate(@Language("RegExp") vararg commands: String) {
        appendGroup(EXIST) {
            append(CHAR_SLASH)
            appendSpace()
            appendGroup(MAYBE, "ym")
            appendGroup(*commands)
        }
    }

    private fun appendOfficialSBCommandsPrivate(@Language("RegExp") vararg commands: String) {
        appendGroup(EXIST) {
            append(CHAR_BACK_SLASH)
            appendSpace()
            appendGroup(MAYBE, "ym")
            appendGroup(*commands)
        }
    }

    /**
     * 加 qq=(?<qq>\d+) 的匹配。
     * @param maybe 如果设置为真，则可以只匹配数字作为群号，不需要输入 group=
     */
    fun appendQQ(maybe: Boolean = false) {
        appendGroup(MAYBE) {
            append("($FLAG_QQ_ID)")
            if (maybe) append(LEVEL_MAYBE)
            append(REG_EQUAL)
            if (maybe) append(LEVEL_MAYBE)
            // qq起码5位
            appendCaptureGroup(FLAG_QQ_ID, REG_NUMBER, MORE, EXIST)
        }
        appendSpace()
    }

    /**
     * 加 (?<id>\d+) 的匹配。
     */
    fun appendID() {
        appendCaptureGroup(FLAG_ID, REG_NUMBER, MORE)
        appendSpace()
    }

    /**
     * group=(?<group>\d+)
     * @param maybe 如果设置为真，则可以只匹配数字作为群号，不需要输入 group=
     */
    fun appendQQGroup(maybe: Boolean = false) {
        appendGroup(MAYBE) {
            append("($FLAG_QQ_GROUP)")
            if (maybe) append(LEVEL_MAYBE)
            append(REG_EQUAL)
            if (maybe) append(LEVEL_MAYBE)
            appendCaptureGroup(FLAG_QQ_GROUP, REG_NUMBER, MORE, EXIST)
        }
        appendSpace()
    }

    /**
     * osu 合法名称
     * (?<name> X X+ X)
     */
    fun appendName() {
        appendCaptureGroup(FLAG_NAME, REG_NAME)
        appendSpace()
    }

    /**
     * sb 合法名称
     * (?<name> X X+ X)
     */
    fun appendSBName() {
        appendCaptureGroup(FLAG_NAME, REG_ANYTHING_BUT_NO_OPERATOR)
        appendSpace()
    }


    /**
     * osu 合法名称
     * (?<name> X X+ X)
     */
    fun append2Name() {
        appendCaptureGroup(FLAG_2_USER, REG_2_USER)
        appendSpace()
    }

    /**
     * maimai 合法名称（基本就是啥都匹配）
     * (?<name> X X+ X)
     */
    fun appendNameAnyButNoHash() {
        appendCaptureGroup(FLAG_NAME, REG_ANYTHING_BUT_NO_HASH, MORE)
        appendSpace()
    }

    /**
     * maimai 合法名称（啥都匹配）
     * (?<name> X X+ X)
     */
    fun appendNameAny() {
        appendCaptureGroup(FLAG_NAME, REG_ANYTHING, MORE)
        appendSpace()
    }

    /**
     * uid=(?<uid>\d+)。**默认一个或更多个 (+)。注意！**
     */
    fun appendUID() {
        appendGroup(MAYBE) {
            append(REG_UID)
            append(REG_EQUAL)
            appendCaptureGroup(FLAG_UID, REG_NUMBER, MORE, EXIST)
        }
        appendSpace()
    }

    /**
     * (?<bid>\d+)。**默认一个或更多个 (+)。注意！**
     */
    fun appendBID() {
        appendCaptureGroup(FLAG_BID, REG_NUMBER, MORE, MAYBE)
        appendSpace()
    }

    /**
     * (?<sid>\d+)。**默认一个或更多个 (+)。注意！**
     */
    fun appendSID() {
        appendCaptureGroup(FLAG_SID, REG_NUMBER, MORE, MAYBE)
        appendSpace()
    }

    fun appendNameAndRange() {
        appendCaptureGroup(
                    FLAG_USER_AND_RANGE,
                    "$REG_NAME?$REG_SPACE$LEVEL_ANY($REG_HASH?(($REG_NUMBER_13)$REG_HYPHEN)?($REG_NUMBER_13))?",
                    EXIST,
                    MAYBE,
                )
        appendSpace()
    }

    fun appendSBNameAndRange() {
        appendCaptureGroup(
            FLAG_USER_AND_RANGE,
            "$REG_ANYTHING_BUT_NO_OPERATOR?$REG_SPACE$LEVEL_ANY($REG_HASH?(($REG_NUMBER_13)$REG_HYPHEN)?($REG_NUMBER_13))?",
            EXIST,
            MAYBE,
        )
        appendSpace()
    }

    /**
     * [:：](?<mode>mode)。
     * level 匹配等级。**默认没有或者一个 (?)。注意！**
     */
    fun appendMode() {
        appendGroup(MAYBE) {
            appendSpace()
            append(REG_COLON)
            appendSpace()
            appendCaptureGroup(FLAG_MODE, REG_MODE, EXIST)
        }
        appendSpace()
    }


    /**
     * (+(?<mod>mod))。
     * @param plusMust '+' 是否必须
     */
    fun appendMod(plusMust: Boolean = false, bodyMust: Boolean = false) {
        val level = if (bodyMust) EXIST else MAYBE
        appendGroup(level) {
            append(REG_PLUS)
            if (!plusMust) append('?')
            appendSpace()
            appendCaptureGroup(FLAG_MOD, REG_MOD, MORE, EXIST)
        }
        appendSpace()
    }

    /**
     * (#?(?<range>0-200))范围。**默认没有或者一个 (?)。注意！**
     * @param hashMust '#' 是否必须
     */
    fun appendRange(hashMust: Boolean = false) {
        appendGroup(MAYBE) {
            append(REG_HASH)
            if (!hashMust) append('?')
            appendSpace()
            appendCaptureGroup(FLAG_RANGE, REG_RANGE)
        }
        appendSpace()
    }

    /**
     * (#?(?<range>0-999))范围。
     */
    private fun appendRangeDay() {
        appendGroup(MAYBE) {
            append(REG_HASH)
            appendMatchLevel(MAYBE)
            appendSpace()
            appendCaptureGroup(FLAG_RANGE, REG_RANGE_DAY, EXIST)
        }
        appendSpace()
    }

    fun appendMatchID() {
        appendCaptureGroup(FLAG_MATCHID, REG_NUMBER, MORE, MAYBE)
        appendSpace()
    }

    fun appendMatchParam() {
        appendGroup(MAYBE) {
            appendSpace()
            append("e(z|a[sz]y)?")
            appendSpace()
            appendCaptureGroup("easy", REG_NUMBER_DECIMAL)
            append("[×xX]?")
        }

        appendSpace()
        appendCaptureGroup("skip", "-?\\d+")
        appendSpace()
        appendCaptureGroup("ignore", "-?\\d+")
        appendSpace()
        appendGroup(MAYBE) {
            append("\\[")
            appendCaptureGroup("remove", REG_NUMBER_SEPERATOR, MORE)
            append("]")
        }
        appendSpace()
        appendCaptureGroup("rematch", "r")
        appendSpace()
        appendCaptureGroup("failed", "f")
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid
     */
    fun appendModeQQUID() {
        appendMode()
        appendQQ()
        appendUID()
    }

    /**
     * 添加一个复合匹配组, 包括 qq, uid, name
     */
    fun appendQQUIDName() {
        appendQQ()
        appendUID()
        appendName()
    }

    /**
     * 添加一个复合匹配组, 包括 qq, uid, name
     */
    fun appendQQUIDSBName() {
        appendQQ()
        appendUID()
        appendSBName()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name
     */
    fun appendModeQQUIDName() {
        appendMode()
        appendQQ()
        appendUID()
        appendName()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name, range
     */
    fun appendModeQQUIDNameRange() {
        appendMode()
        appendQQ()
        appendUID()
        appendNameAndRange()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name, range
     */
    fun appendModeQQUIDSBNameRange() {
        appendMode()
        appendQQ()
        appendUID()
        appendSBNameAndRange()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, bid, qq, uid, name, mod
     */
    fun appendModeBIDQQUIDNameMod() {
        appendMode()
        appendBID()
        appendQQ()
        appendUID()
        appendName()
        appendMod()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, bid, qq, uid, name, mod
     */
    fun appendModeBIDQQUIDSBNameMod() {
        appendMode()
        appendBID()
        appendQQ()
        appendUID()
        appendSBName()
        appendMod()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name, rangeday
     */
    fun appendModeQQUIDNameRangeDay() {
        appendMode()
        appendQQ()
        appendUID()
        appendName()
        appendRangeDay()
    }

    /**
     * 添加空格。**默认添加任意个（\\s*）。注意！**
     * @param level 匹配等级。
     * @return 无
     */
    fun appendSpace(level: MatchLevel = ANY) {
        append(REG_SPACE)
        appendMatchLevel(level)
    }

    /**
     * 添加匹配次数范围。{1，2}
     * @return 无
     */
    fun appendMatchArea(less: Int? = 1, more: Int? = 2) {
        val l1: Int?
        val l2: Int?

        if (less == null && more == null) {
            return
        }

        if (less != null && more != null && less > more) {
            l1 = more
            l2 = less
        } else {
            l1 = less
            l2 = more
        }

        append(CHAR_BRACE_START)
        if (l1 != null) append(l1)
        append(CHAR_COMMAS)
        if (l2 != null) append(l2)
        append(CHAR_BRACE_END)
    }

    /**
     * 添加匹配次数。如果不使用此方法，则是必须出现一个，否则匹配失败（什么都不加）
     * appendEnd
     * @param level 匹配等级。
     * @return 无
     */
    fun appendMatchLevel(level: MatchLevel) {
        when (level) {
            ANY_LAZY -> {
                append(LEVEL_ANY)
                append(LEVEL_MAYBE)
            }

            ANY -> append(LEVEL_ANY)
            MAYBE -> append(LEVEL_MAYBE)
            MORE -> append(LEVEL_MORE)
            EXIST -> return // do nothing
        }
    }

    /**
     * 添加一个捕获组, 展开后就是 (? < name >...)?。
     * @param flag 组名
     * @param str 正则
     * @param contentLevel 匹配等级。注意这个匹配是在**组里**的，也就是 (?<>abc **这里** )
     * @param bodyLevel 匹配等级。注意这个匹配是在**组外**的，也就是 (?<>abc ) **这里**，默认没有或者一个 (?)。
     */
    fun appendCaptureGroup(
        flag: String,
        @Language("RegExp") str: String,
        contentLevel: MatchLevel,
        bodyLevel: MatchLevel
    ) {
        require(flag.matches("[a-zA-Z0-9_]+".toRegex())) { "捕获组的标志位：$flag 不合语法。\nIllegal capture group name." }

        appendGroup(bodyLevel) {
            append("?<${flag}>${str}")
            appendMatchLevel(contentLevel)
        }
    }


    /**
     * 添加一个捕获组, 展开后就是 (? < name >...)?。
     * @param contentLevel 匹配等级一般是 MORE/EXIST。注意这个匹配是在**组里**的，也就是 (?<>abc **这里** )
     */
    fun appendCaptureGroup(
        flag: String,
        @Language("RegExp") str: String,
        contentLevel: MatchLevel
    ) {
        appendCaptureGroup(flag, str, contentLevel, MAYBE)
    }

    /**
     * 添加一个捕获组, 展开后就是 (? < name >...)?。
     */
    fun appendCaptureGroup(
        flag: String, @Language("RegExp") str: String
    ) {
        appendCaptureGroup(flag, str, EXIST, MAYBE)
    }

    /**
     * 创建一个组, 展开后就是 (...)
     * @param group 执行一段添加组的操作
     * @param level 匹配等级。
     */
    fun appendGroup(level: MatchLevel = EXIST, group: CommandPatternBuilder.() -> Unit) {
        append(CHAR_GROUP_START)
        this.group()
        append(CHAR_GROUP_END)
        appendMatchLevel(level)
    }

    /**
     * 创建一个组, 展开后就是 (...)
     */
    fun appendGroup(level: MatchLevel = EXIST, @Language("RegExp") vararg strs: String) {
        append(CHAR_GROUP_START)
        appendSeperator(*strs)
        append(CHAR_GROUP_END)
        appendMatchLevel(level)
    }

    // 不到三个地方使用, 而且里面不到3行的方法很简短没必要单独写
    /**
     * 创建一个组, 展开后就是 (...|xxx|aaa)
     */
    fun appendGroup(@Language("RegExp") vararg strs: String) {
        appendGroup(EXIST, *strs)
    }

    /**
     * 用分隔符来将多个隔开。a, b, c -> **a|b|c**
     */
    fun appendSeperator(@Language("RegExp") vararg strs: String) {
        append(strs.joinToString(CHAR_SEPARATOR.toString()))
    }

    /**
     * 创建一个捕获组，前面带任意符号。([prefix](?< name >.....))?
     * @param prefix 前缀，比如 REG_COLON
     * @param flag 捕获组名称
     * @param content 要匹配的内容
     * @param contentLevel 内容匹配等级。和内容连用。
     * @param prefixLevel 前缀匹配等级。如果设为 MAYBE 则可以匹配不带前缀的情况。
     */
    private fun appendPrefixCaptureGroup(
        prefix: String,
        flag: String,
        content: String,
        contentLevel: MatchLevel? = EXIST,
        prefixLevel: MatchLevel? = EXIST,
    ) {
        append(CHAR_GROUP_START)
        append(prefix)
        appendMatchLevel(prefixLevel ?: EXIST)
        appendGroup {
            append("?<$flag>${content}")
            appendMatchLevel(contentLevel ?: EXIST)
        }
        append(CHAR_GROUP_END)
        appendMatchLevel(MAYBE)
    }

    /**
     * 创建一个捕获组，前面带冒号, ([：:](.....))?
     * @param flag 捕获组名称
     * @param content 要匹配的内容
     * @param contentLevel 内容匹配等级。和内容连用。
     * @param prefixLevel 前缀匹配等级。如果设为 MAYBE 则可以匹配不带前缀的情况。
     */
    fun appendColonCaptureGroup(
        flag: String,
        @Language("RegExp") content: String,
        contentLevel: MatchLevel = EXIST,
        prefixLevel: MatchLevel = EXIST
    ) {
        appendPrefixCaptureGroup(
            prefix = REG_COLON,
            flag = flag,
            content = content,
            contentLevel = contentLevel,
            prefixLevel = prefixLevel
        )
    }


    /**
     * 创建一个捕获组，前面带井号, ([##](.....))?
     * @param flag 捕获组名称
     * @param content 要匹配的内容
     * @param contentLevel 内容匹配等级。和内容连用。
     * @param prefixLevel 前缀匹配等级。如果设为 MAYBE 则可以匹配不带前缀的情况。
     */
    fun appendHashCaptureGroup(
        flag: String,
        @Language("RegExp") content: String,
        contentLevel: MatchLevel = EXIST,
        prefixLevel: MatchLevel = EXIST
    ) {
        appendPrefixCaptureGroup(
            prefix = REG_HASH,
            flag = flag,
            content = content,
            contentLevel = contentLevel,
            prefixLevel = prefixLevel
        )
    }

    /**
     * 创建一个捕获组，前面带星号, ([**](.....))?
     * @param flag 捕获组名称
     * @param content 要匹配的内容
     * @param contentLevel 内容匹配等级。和内容连用。
     * @param prefixLevel 前缀匹配等级。如果设为 MAYBE 则可以匹配不带前缀的情况。
     */
    fun appendStarCaptureGroup(
        flag: String,
        @Language("RegExp") content: String,
        contentLevel: MatchLevel = EXIST,
        prefixLevel: MatchLevel = EXIST
    ) {
        appendPrefixCaptureGroup(
            prefix = REG_STAR,
            flag = flag,
            content = content,
            contentLevel = contentLevel,
            prefixLevel = prefixLevel
        )
    }

    fun appendAtLeastSpaceGroup(
        flag: String,
        @Language("RegExp") content: String,
        contentLevel: MatchLevel
    ) {
        appendGroup(MAYBE) {
            appendSpace(MORE) // 至少需要一个空格区分开来
            appendCaptureGroup(flag, content, contentLevel)
        }
    }

    /**
     * 添加随便一段正则
     */
    fun append(@Language("RegExp") vararg strs: String) {
        strs.forEach { str ->
            +str
        }
    }

    /**
     * 添加随便一段正则
     */
    fun append(@Language("RegExp") str: String) {
        +str
    }

    /**
     * 添加随便一段正则
     */
    fun append(@Language("RegExp") c: Char) {
        +c
    }

    /**
     * 添加随便一段正则
     */
    fun append(@Language("RegExp") i: Int) {
        +(i.toString())
    }

    /**
     * 忽略随便一段正则。如果有输入的其他正则，则使用后向忽略包围起来。
     * @param forward 是否是前向忽略。(?<!...)
     */
    fun appendIgnore(@Language("RegExp") ign: String? = REG_IGNORE, forward: Boolean = false) {
        append('(')
        append('?')
        if (forward) append('<')
        append('!')

        if (ign.isNullOrEmpty()) {
            append(REG_IGNORE)
        } else {
            append(ign)
        }

        append(')')
    }

    // 以下是构建
    private val patternStr: StringBuilder = StringBuilder()

    /**
     * 构造正则
     */
    fun build(doBuild: CommandPatternBuilder.() -> Unit): Pattern {
        this.doBuild()
        if (!patternStr.endsWith("\\s*")) appendSpace()
        append(CHAR_FINAL)
        return Pattern.compile(patternStr.toString())
    }

    /**
     * 重载操作符, 但是 idea 不太好整正则语法提示, 所以不好使
     */
    operator fun String.unaryPlus(): CommandPatternBuilder {
        patternStr.append(this)
        return this@CommandPatternBuilder
    }

    operator fun Char.unaryPlus(): CommandPatternBuilder {
        patternStr.append(this)
        return this@CommandPatternBuilder
    }

    /**
     * 初始化, 如果 start 为空, 则默认为 ^(?i)\s*, 否则就是 start
     */
    init {
        if (start != null) {
            +start
        } else {
            +CHAR_BEGIN
            +REG_CAPS_INSENSITIVE
            +REG_SPACE
            +LEVEL_ANY
        }
    }

    companion object {
        /**
         * 构建出来正则开头 ^(?i)
         */
        fun create(doBuild: CommandPatternBuilder.() -> Unit) = CommandPatternBuilder().build(doBuild)

        /**
         * 构建出来正则开头 $start
         */
        fun create(@Language("RegExp") start: String, doBuild: CommandPatternBuilder.() -> Unit) =
            CommandPatternBuilder(start).build(doBuild)
    }
}