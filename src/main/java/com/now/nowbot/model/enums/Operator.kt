package com.now.nowbot.model.enums

import com.now.nowbot.util.command.PATTERN_EQUAL
import com.now.nowbot.util.command.PATTERN_EXCLAMATION
import com.now.nowbot.util.command.PATTERN_GREATER
import com.now.nowbot.util.command.PATTERN_LESS
import org.intellij.lang.annotations.Language

enum class Operator(@param:Language("RegExp") val regex: Regex) {
    // 不等于
    NE("$PATTERN_EXCLAMATION$PATTERN_EQUAL|$PATTERN_LESS$PATTERN_GREATER|≠".toRegex()),

    // 完全等于
    XQ("$PATTERN_EQUAL$PATTERN_EQUAL|≌".toRegex()),

    // 大于等于
    GE("$PATTERN_GREATER$PATTERN_EQUAL|≥".toRegex()),

    // 大于
    GT(PATTERN_GREATER.toRegex()),

    // 小于等于
    LE("$PATTERN_LESS$PATTERN_EQUAL|≤".toRegex()),

    // 小于
    LT(PATTERN_LESS.toRegex()),

    // （约）等于
    EQ("$PATTERN_EQUAL|≈".toRegex());

    companion object {
        fun getOperator(string: String): Operator {
            Operator.entries.forEach {
                if (string.contains(it.regex)) {
                    return it
                }
            }

            return EQ

            // throw IllegalArgumentException.WrongException.Operator()
        }

        fun Operator.getText(): String {
            return when (this) {
                XQ,
                EQ -> "="
                NE -> "!="
                GE -> ">="
                GT -> ">"
                LE -> "<="
                LT -> "<"
            }
        }
    }
}