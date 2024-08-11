package com.now.nowbot.util.command


import org.intellij.lang.annotations.Language
import java.util.regex.Pattern

class CommandPatternBuilder private constructor(start: String? = null) {

    /**
     * 加命令, 后面自带 space, 展开后为 (!ym(p|pr))\s*
     * @param commands 连续的命令 ("p", "pr")
     */
    fun commands(@Language("RegExp") vararg commands: String) {
        append(CHAR_GROUP_START)
        append(REG_EXCLAMINATION)
        appendGroup(0, "ym")
        appendSpace()
        append(commands.joinToString(CHAR_SEPARATOR.toString()))
        append(CHAR_GROUP_END)
        appendSpace()
    }

    fun commandsWithIgnore(@Language("RegExp") ignore : String?, @Language("RegExp") vararg commands: String) {
        commands(*commands)
        appendIgnore(ignore)
        appendSpace()
    }

    /**
     * 加命令, 官方机器人的匹配方式
     * @param commands 连续的命令 ("p", "pr")
     */
    fun commandsOfficial(@Language("RegExp") vararg commands: String) {
        append(CHAR_GROUP_START)
        append(CHAR_SLASH)
        append("ym")
        appendSpace()
        append(commands.joinToString(CHAR_SEPARATOR.toString()))
        append(CHAR_GROUP_END)
        appendSpace()
    }

    fun commandsOfficialWithIgnore(@Language("RegExp") ignore : String?, @Language("RegExp") vararg commands: String) {
        commandsOfficial(*commands)
        appendIgnore(ignore)
        appendSpace()
    }

    /**
     * 加 qq=(?<qq>\d+) 的匹配。
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     */
    fun appendQQID(level: Int? = 1) {
        appendCaptureGroup(FLAG_QQ_ID, REG_NUMBER, level)
        appendSpace()
    }

    /**
     * group=(?<group>\d+)
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     */
    fun appendQQGroup(level: Int? = 1) {
        appendCaptureGroup(FLAG_QQ_GROUP, REG_NUMBER, level)
        appendSpace()
    }

    /**
     * osu 合法名称
     * (?<name> X X+ X)
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     */
    fun appendName(level: Int? = 0) {
        appendCaptureGroup(FLAG_NAME, REG_NAME, level)
        appendSpace()
    }

    /**
     * uid=(?<uid>\d+)。**默认一个或更多个 (+)。注意！**
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     */
    fun appendUID(level: Int? = 1) {
        appendCaptureGroup(FLAG_UID, REG_NUMBER, level)
        appendSpace()
    }

    /**
     * (?<bid>\d+)。**默认一个或更多个 (+)。注意！**
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     */
    fun appendBID(level: Int? = 1) {
        appendCaptureGroup(FLAG_BID, REG_NUMBER, level)
        appendSpace()
    }

    /**
     * (?<sid>\d+)。**默认一个或更多个 (+)。注意！**
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     */
    fun appendSID(level: Int? = 1) {
        appendCaptureGroup(FLAG_SID, REG_NUMBER, level)
        appendSpace()
    }

    /**
     * osu 合法名称范围
     * (?<name> X X+ X)
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     */
    fun appendNameAndRange(level: Int? = 0, level2: Int? = 0) {
        appendName(level)
        appendRange(level2)
    }

    /**
     * [:：](?<mode>mode)。**默认没有或者一个 (?)。注意！**
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     */
    fun appendMode(level: Int? = 0) {
        append(CHAR_GROUP_START)
        append(REG_COLUMN)
        appendCaptureGroup(FLAG_MODE, REG_MODE, 0)
        append(CHAR_GROUP_END)
        appendMatch(level)
        appendSpace()
    }


    /**
     * (+(?<mod>mod))。**默认没有或者一个 (?)。注意！**
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     * @param level2 **加号**（**+**）的匹配等级。**默认必须出现 ()。注意！**
     */
    fun appendMod(level: Int? = 0, level2: Int? = 2) {
        append(CHAR_GROUP_START)
        append(REG_PLUS)
        appendMatch(level2)
        appendCaptureGroup(FLAG_MODE, REG_MOD, 1)
        append(CHAR_GROUP_END)
        appendMatch(level)
        appendSpace()
    }

    /**
     * (#?(?<range>0-100))范围。**默认没有或者一个 (?)。注意！**
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     * @param level2 **井号**（**#**）的匹配等级。**默认没有或者一个 (?)。注意！**
     */
    fun appendRange(level: Int? = 0, level2: Int? = 0) {
        append(CHAR_GROUP_START)
        append(REG_HYPHEN)
        appendMatch(level2)
        appendCaptureGroup(FLAG_RANGE, REG_RANGE, 0)
        append(CHAR_GROUP_END)
        appendMatch(level)
        appendSpace()
    }

    /**
     * (#?(?<range>0-999))范围。**默认没有或者一个 (?)。注意！**
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     * @param level2 **井号**（**#**）的匹配等级。**默认没有或者一个 (?)。注意！**
     */
    fun appendRangeDay(level: Int? = 0, level2: Int? = 0) {
        append(CHAR_GROUP_START)
        append(REG_HYPHEN)
        appendMatch(level2)
        appendCaptureGroup(FLAG_RANGE, REG_RANGE_DAY, 0)
        append(CHAR_GROUP_END)
        appendMatch(level)
        appendSpace()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name
     */
    fun appendModeQQUIDName() {
        appendMode()
        appendQQID()
        appendUID()
        appendName()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name, range
     */
    fun appendModeQQUIDNameRange() {
        appendMode()
        appendQQID()
        appendUID()
        appendName()
        appendRange()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name, range
     */
    fun appendModeQQUIDNameRangeDay() {
        appendMode()
        appendQQID()
        appendUID()
        appendName()
        appendRangeDay()
    }

    /**
     * 添加空格。**默认添加任意个（\\s*）。注意！**
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     * @return 无
     */
    fun appendSpace(level: Int? = -1) {
        append(REG_SPACE)
        appendMatch(level)
    }

    /**
     * 添加匹配次数。{1，2}
     * appendEnd
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     * @return 无
     */
    fun appendMatch(level1: Int? = 1, level2: Int? = 2) {
        val l1 : Int?
        val l2 : Int?

        if (level1 == null && level2 == null) {
            return
        }

        if (level1 != null && level2 != null && level1 > level2) {
            l1 = level2
            l2 = level1
        } else {
            l1 = level1
            l2 = level2
        }

        append(CHAR_BRACE_START)
        if (l1 != null) append(l1)
        append(CHAR_COMMAS)
        if (l2 != null) append(l2)
        append(CHAR_BRACE_END)
    }

    /**
     * 添加匹配次数。如果不使用此方法，则是必须出现一个，否则匹配失败
     * appendEnd
     * @param level 匹配等级。-2：任意个（懒惰模式） (*?)，-1：任意个 (*)，0：没有或者一个 (?)，1：一个或更多个 (+)，2：必须出现 ()，
     * @return 无
     */
    fun appendMatch(level: Int? = 2) {
        when (level) {
            1 -> append(CHAR_1P)
            0 -> append(CHAR_01)
            -1 -> append(CHAR_ANY)
            -2 -> {
                append(CHAR_ANY)
                append(CHAR_01)
            }
            // 2 do nothing
        }
    }

    /**
     * 添加一个捕获组, 展开后就是 (? < name >...)
     * @param flag 组名
     * @param pattern 正则
     * @param whether 是否可忽略, 不传默认为 true
     */
    fun appendCaptureGroup(
        flag: String, @Language("RegExp") pattern: String, level: Int? = 2
    ) {
        appendGroup(level) {
            append("?<$flag>$pattern")
        }
    }

    /**
     * 添加一个捕获组, 展开后就是 (? < name >...)。默认**没有或者一个（0）。注意！**
     */
    fun appendCaptureGroup(
        flag: String, @Language("RegExp") pattern: String
    ) {
        appendCaptureGroup(flag, pattern, 0)
    }

    /**
     * 创建一个组, 展开后就是 (...)
     * @param group 执行一段添加组的操作
     * @param whether 是否可忽略, 不传默认为 true
     */
    fun appendGroup(level: Int? = 2, group: CommandPatternBuilder.() -> Unit) {
        append(CHAR_GROUP_START)
        this.group()
        append(CHAR_GROUP_END)
        appendMatch(level)
    }

    /**
     * 创建一个组, 展开后就是 (...)
     */
    fun appendGroup(level: Int? = 2, @Language("RegExp") vararg strs: String) {
        append(CHAR_GROUP_START)
        appendMultiple(*strs)
        append(CHAR_GROUP_END)
        appendMatch(level)
    }

    /**
     * 创建一个组, 展开后就是 (...)
     */
    fun appendGroup(@Language("RegExp") vararg strs: String) {
        appendGroup(2, *strs)
    }


    /**
     * 用分隔符来将多个隔开。a, b, c -> **a|b|c**
     */
    fun appendMultiple(@Language("RegExp") vararg strs: String) {
        append(strs.joinToString(CHAR_SEPARATOR.toString()))
    }

    /**
     * 添加随便一段正则
     */
    fun append(@Language("RegExp") vararg strs: String) {
        strs.forEach {
            str -> +str
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
     * 忽略随便一段正则。如果有输入的其他正则，则**和 append 无异**。主要是方便辨认。
     */
    fun appendIgnore(@Language("RegExp") ign: String?) {
        if (ign.isNullOrEmpty()) appendIgnore()
        else append(ign)
    }

    fun appendIgnore() {
        append(REG_IGNORE)
    }

    // 以下是构建
    private val patternStr: StringBuilder = StringBuilder()

    /**
     * 构造正则
     */
    fun build(doBuild: CommandPatternBuilder.() -> Unit): Pattern {
        this.doBuild()
        appendSpace()
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
            +REG_SPACE_ANY
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
