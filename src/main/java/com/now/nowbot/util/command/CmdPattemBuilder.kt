package com.now.nowbot.util.command

import com.now.nowbot.util.command.CmdPatternStatic.*
import org.intellij.lang.annotations.Language
import java.util.regex.Pattern

class CmdPattemBuilder private constructor(start: String? = null) {

    /**
     * 加命令, 后面自带 space
     * @param commands 连续的命令 ("p", "pr")
     */
    fun commands(@Language("RegExp") vararg commands: String) {
        commands(false, *commands)
    }
    fun commands(ignore:Boolean, @Language("RegExp") vararg commands: String) {
        if (commands.size == 1) {
            +commands[0]
        } else {
            // (
            startGroup()
            // a|b|c|d
            +commands.joinToString(CHAR_SEPARATOR.toString())
            // )
            endGroup()
            // (?![a-zA-Z_]) 避免指令污染
             if (ignore) appendIgnoreAlphabets()
        }
        // \\s*
        space()
    }

    fun commands(commandBuild: VarargList.() -> Unit) {
        val commands = VarargList()
        commands.commandBuild()
        startGroup()
        +commands.joinToString(CHAR_SEPARATOR.toString())
        endGroup()
        space()
    }

    /**
     * 避免不必要的命令重复，所带来的指令污染。比如：!ymb 和 !ymbind。
     * 如果前面并未加 (?!ind)，则后面的指令也会在前面被错误匹配（获取叫 ind 玩家的 bp）
     * @param ignores 避免匹配
     */
    fun appendIgnores(@Language("RegExp") vararg ignores: String) {
        if (ignores.isEmpty()) return
        +"(?!"
        +ignores.joinToString(CHAR_SEPARATOR.toString())
        endGroup()
    }

    fun appendIgnoreAlphabets() {
        appendIgnores(REG_IGNORE)
    }

    fun appendQQ(whatever: Boolean = true) {
        +REG_QQ_ID
        if (whatever) whatever()
    }

    fun appendQQGroup(whatever: Boolean = true) {
        +REG_QQ_GROUP
        if (whatever) whatever()
    }

    fun appendName(whatever: Boolean = true) {
        +REG_NAME
        if (whatever) whatever()
    }

    fun appendUid(whatever: Boolean = true) {
        +REG_UID
        if (whatever) whatever()
    }

    fun appendBid(whatever: Boolean = true) {
        +REG_BID
        if (whatever) whatever()
    }

    fun appendSid(whatever: Boolean = true) {
        +REG_SID
        if (whatever) whatever()
    }

    fun appendNameAndRange(whatever: Boolean = true) {
        +REG_USER_AND_RANGE
        if (whatever) whatever()
    }

    fun appendMode(whatever: Boolean = true) {
        column(false)
        +REG_MODE
        if (whatever) whatever()
    }

    fun appendMod(whatever: Boolean = true) {
        column()
        +REG_MOD
        if (whatever) whatever()
    }

    fun appendRange(whatever: Boolean = true) {
        column()
        +REG_RANGE
        if (whatever) whatever()
    }

    fun `append(ModeQQUidNameRange)`() {
        appendMode()
        space()
        appendQQ()
        space()
        appendUid()
        space()
        appendNameAndRange()
    }

    fun `append(ModeQQUidName)`() {
        appendMode()
        space()
        appendQQ()
        space()
        appendUid()
        space()
        appendName()
    }

    // 默认就是 Any
    fun space(num: Int = -1) {
        when (num) {
            0 -> appendSpaceOnce()
            1 -> appendSpaceLeast()
            else -> appendSpaceAny()
        }
    }

    fun appendSpaceAny() {
        +(REG_SPACE)
    }

    fun appendSpaceLeast() {
        +(REG_SPACE_1P)
    }

    fun appendSpaceOnce() {
        +(REG_SPACE_01)
    }

    fun column(whatever: Boolean = true) {
        +REG_COLUMN
        if (whatever) whatever()
    }

    fun append(@Language("RegExp") str: String) {
        +str
    }

    fun group(
        name: String, @Language("RegExp") pattern: String, whatever: Boolean = true
    ) {
        startGroup()
        patternStr.append("?<$name>$pattern")
        endGroup()
        if (whatever) {
            whatever()
        }
    }

    fun startGroup(group: CmdPattemBuilder.() -> Unit) {
        +CHAR_GROUP_START
        group()
        +CHAR_GROUP_END
    }

    fun startGroup() {
        +CHAR_GROUP_START
    }

    fun endGroup() {
        +CHAR_GROUP_END
    }

    fun whatever() {
        +CHAR_WHATEVER
    }

    fun any() {
        +CHAR_ANY
    }

    fun more() {
        +CHAR_MORE
    }

    private val patternStr: StringBuilder = StringBuilder()

    init {
        start?.let {
            +it
            +REG_SPACE
        } ?: {
            +CHAR_START
            +REG_START
            +REG_SPACE
        }
    }

    fun build(doBuild: CmdPattemBuilder.() -> Unit): Pattern {
        this.doBuild()
        +CHAR_END
        return Pattern.compile(patternStr.toString())
    }

    @Language("RegExp")
    operator fun String.unaryPlus(): CmdPattemBuilder {
        patternStr.append(this)
        return this@CmdPattemBuilder
    }

    operator fun Char.unaryPlus(): CmdPattemBuilder {
        patternStr.append(this)
        return this@CmdPattemBuilder
    }

    companion object {
        /**
         * 构建出来正则开头 ^[!！/](?i)(ym)?\\s*
         */
        fun create(doBuild: CmdPattemBuilder.() -> Unit) = CmdPattemBuilder().build(doBuild)

        /**
         * 构建出来正则开头 $start\\s*
         */
        fun create(@Language("RegExp") start: String, doBuild: CmdPattemBuilder.() -> Unit) =
            CmdPattemBuilder(start).build(doBuild)
    }

    class VarargList {
        val list = mutableListOf<String>()

        operator fun String.unaryPlus() {
            list.add(this)
        }

        fun joinToString(compiled: CharSequence) = list.joinToString(compiled)
    }
}