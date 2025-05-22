package com.now.nowbot.model.enums

import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.command.REG_EQUAL
import com.now.nowbot.util.command.REG_EXCLAMATION
import com.now.nowbot.util.command.REG_GREATER
import com.now.nowbot.util.command.REG_LESS
import org.intellij.lang.annotations.Language

enum class Operator(@Language("RegExp") val regex: Regex) {
    // 不等于
    NE("$REG_EXCLAMATION$REG_EQUAL|≠".toRegex()),

    // 完全等于
    XQ("$REG_EQUAL$REG_EQUAL|≌".toRegex()),

    // 大于等于
    GE("$REG_GREATER$REG_EQUAL|≥".toRegex()),

    // 大于
    GT(REG_GREATER.toRegex()),

    // 小于等于
    LE("$REG_LESS$REG_EQUAL|≤".toRegex()),

    // 小于
    LT(REG_LESS.toRegex()),

    // （约）等于
    EQ("$REG_EQUAL|≈".toRegex());

    companion object {
        fun getOperator(string: String): Operator {
            Operator.entries.forEach {
                if (string.contains(it.regex)) {
                    return it
                }
            }

            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_S, "逻辑运算符")
        }
    }
}