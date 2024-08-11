package com.now.nowbot.util.command

import org.intellij.lang.annotations.Language
import java.util.regex.Pattern

class CmdPatterBuilder private constructor(start: String? = null) {

    /**
     * 加命令, 后面自带 space, 展开后为 (!ym(p|pr))\s*
     * @param commands 连续的命令 ("p", "pr")
     */
    fun commands(@Language("RegExp") vararg commands: String) {
        commandBase(false, *commands)
    }

    /**
     * 加命令, 官方机器人的匹配方式
     * @param commands 连续的命令 ("p", "pr")
     */
    fun commandsOfficial(ignore: Boolean = false, @Language("RegExp") vararg commands: String) {
        +CHAR_GROUP_START
        +CHAR_SLASH
        +"ym"
        space()
        +commands.joinToString(CHAR_SEPARATOR.toString())
        +CHAR_GROUP_END
        space()
    }

    fun commandsOfficial(@Language("RegExp") vararg commands: String) {
        commandsOfficial(false, *commands)
    }

    fun commandOfficial(ignore: Boolean = false, @Language("RegExp") command: String) {
        +CHAR_GROUP_START
        +CHAR_SLASH
        +"ym"
        space()
        +command
        +CHAR_GROUP_END
        if (ignore) appendIgnoreAlphabets()
        space()
    }

    fun commandOfficial(@Language("RegExp") command: String) {
        commandOfficial(false, command)
    }

    fun commandOfficialWithIgnore(@Language("RegExp") command: String) {
        commandOfficial(true, command)
    }

    /**
     * 添加命令开头, 不进行前面加 !ym 处理, 展开后就是 (cmd)\s*
     * @param command 命令
     * @param ignore 尾部是否必须加空格 true 就是加上 (?![a-zA-Z_])
     */
    fun command(@Language("RegExp") command: String, ignore: Boolean = false) {
        // "(cmd)\\s*"
        startGroup(false) {
            +command
        }
        // (?![a-zA-Z_]) 避免指令污染
        if (ignore) appendIgnoreAlphabets()
        // \\s*
        space()
    }

    /**
     * 添加命令开头, 末尾添加 (?![a-zA-Z_]) , 展开后是 !ym(p|pr)(?![a-zA-Z_])\s*
     * @param commands 连续的命令 ("p", "pr")
     */
    fun commandWithIgnore(@Language("RegExp") vararg commands: String) {
        commandBase(true, *commands)
    }

    /**
     * 构建命令基方法
     */
    private fun commandBase(ignore: Boolean, @Language("RegExp") vararg commands: String) {
        // "([!！](ym)?(a|b|c))\\s*"
        startGroup(false) {
            +REG_START_ALL
            startGroup(false) {
                // a|b|c|d
                +commands.joinToString(CHAR_SEPARATOR.toString())
            }
        }
        // (?![a-zA-Z_]) 避免指令污染
        if (ignore) appendIgnoreAlphabets()
        // \\s*
        space()
    }

    fun appendIgnoreAlphabets() {
        +REG_IGNORE
    }

    /**
     * 加 qq=(?<qq>\d+) 的匹配
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendQQId(whatever: Boolean = true) {
        +REG_QQ_ID
        if (whatever) whatever()
    }

    /**
     * group=(?<group>\d+)
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendQQGroup(whatever: Boolean = true) {
        +REG_QQ_GROUP
        if (whatever) whatever()
    }

    /**
     * osu 合法名称
     * (?<name>...)
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendName(whatever: Boolean = true) {
        +REG_NAME
        if (whatever) whatever()
    }

    /**
     * uid=(?<uid>\d+)
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendUid(whatever: Boolean = true) {
        +REG_UID
        if (whatever) whatever()
    }

    /**
     * (?<bid>\d+)
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendBid(whatever: Boolean = true) {
        +REG_BID
        if (whatever) whatever()
    }

    /**
     * (?<sid>\d+)
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendSid(whatever: Boolean = true) {
        +REG_SID
        if (whatever) whatever()
    }

    /**
     * osu名与范围
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendNameAndRange(whatever: Boolean = true) {
        +REG_USER_AND_RANGE
        if (whatever) whatever()
    }

    /**
     * (?<mode>可用的模式)
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendMode(whatever: Boolean = true) {
        startGroup(whatever) {
            column(false)
            +REG_MODE
        }
    }

    /**
     * (+(?<mod>可用的mod)) 必须加'+'
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendMod(whatever: Boolean = true) {
        +REG_MOD
        if (whatever) whatever()
    }

    /**
     * (+?(?<mod>mod)) 可以不用加'+'
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendModAny(whatever: Boolean = true) {
        +REG_MOD_01
        if (whatever) whatever()
    }

    /**
     * (:?(?<range>0-999))范围
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun appendRange(whatever: Boolean = true) {
        column()
        +REG_RANGE
        if (whatever) whatever()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name, range
     */
    fun `append(ModeQQUidNameRange)`() {
        appendMode()
        space()
        appendQQId()
        space()
        appendUid()
        space()
        appendNameAndRange()
    }

    /**
     * 添加一个复合匹配组, 包括 mode, qq, uid, name
     */
    fun `append(ModeQQUidName)`() {
        appendMode()
        space()
        appendQQId()
        space()
        appendUid()
        space()
        appendName()
    }

    /**
     * 添加一个空格
     * @param num 0 为可以没有或者有一个空格, 1 为至少一个空格, 其他为任意个空格, 默认为任意个空格
     */
    fun space(num: Int = -1) {
        when (num) {
            0 -> appendSpaceOnce()
            1 -> appendSpaceLeast()
            else -> appendSpaceAny()
        }
    }

    fun appendSpaceAny() {
        +(REG_SPACE_ANY)
    }

    fun appendSpaceLeast() {
        +(REG_SPACE_1P)
    }

    fun appendSpaceOnce() {
        +(REG_SPACE_01)
    }

    /**
     * 冒号
     */
    fun column(whatever: Boolean = true) {
        startGroup(whatever) {
            +REG_COLUMN
            +REG_SPACE_ANY
        }
    }

    /**
     * 添加一段正则
     */
    fun append(@Language("RegExp") str: String) {
        +str
    }

    /**
     * 添加一个捕获组, 展开后就是 (?<name>pattern)
     * @param name 组名
     * @param pattern 正则
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun group(
        name: String, @Language("RegExp") pattern: String, whatever: Boolean = true
    ) {
        startGroup(false) {
            patternStr.append("?<$name>$pattern")
        }
        if (whatever) {
            whatever()
        }
    }

    /**
     * 创建一个组, 展开后就是 (pattern)
     * @param group 执行一段添加组的操作
     * @param whatever 是否可忽略, 不传默认为 true
     */
    fun startGroup(whatever: Boolean = true, group: CmdPatterBuilder.() -> Unit) {
        +CHAR_GROUP_START
        this.group()
        +CHAR_GROUP_END
        if (whatever) whatever()
    }

    /**
     * 末尾加个'?'
     */
    fun whatever() {
        +CHAR_WHATEVER
    }

    /**
     * 末尾加个'*'
     */
    fun any() {
        +CHAR_ANY
    }

    /**
     * 末尾加个'+'
     */
    fun more() {
        +CHAR_MORE
    }

    private val patternStr: StringBuilder = StringBuilder()

    /**
     * 构造正则
     */
    fun build(doBuild: CmdPatterBuilder.() -> Unit): Pattern {
        this.doBuild()
        space()
        +CHAR_END
        return Pattern.compile(patternStr.toString())
    }

    /**
     * 重载操作符, 但是 idea 不太好整正则语法提示, 所以不好使
     */
    operator fun String.unaryPlus(): CmdPatterBuilder {
        patternStr.append(this)
        return this@CmdPatterBuilder
    }

    operator fun Char.unaryPlus(): CmdPatterBuilder {
        patternStr.append(this)
        return this@CmdPatterBuilder
    }

    /**
     * 初始化, 如果 start 为空, 则默认为 ^(?i)\s*, 否则就是 start
     */
    init {
        if (start != null) {
            +start
        } else {
            +CHAR_START
            +REG_START
            +REG_SPACE_ANY
        }
    }

    companion object {
        /**
         * 构建出来正则开头 ^(?i)
         */
        fun create(doBuild: CmdPatterBuilder.() -> Unit) = CmdPatterBuilder().build(doBuild)

        /**
         * 构建出来正则开头 $start
         */
        fun create(@Language("RegExp") start: String, doBuild: CmdPatterBuilder.() -> Unit) =
            CmdPatterBuilder(start).build(doBuild)
    }
}