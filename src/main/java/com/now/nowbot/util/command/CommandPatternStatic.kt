package com.now.nowbot.util.command

import org.intellij.lang.annotations.Language

const val CHAR_HASH: Char = '#'
const val CHAR_HASH_FULL: Char = '＃'
const val CHAR_EQUAL: Char = '='
const val CHAR_SEPARATOR: Char = '|'
const val CHAR_BEGIN: Char = '^'
const val CHAR_FINAL: Char = '$'
const val CHAR_BRACE_START: Char = '{'
const val CHAR_BRACE_END: Char = '}'
const val CHAR_COMMAS: Char = ','
const val CHAR_GROUP_START: Char = '('
const val CHAR_GROUP_END: Char = ')'
const val CHAR_SLASH: Char = '/'

const val LEVEL_MAYBE: Char = '?'
const val LEVEL_ANY: Char = '*'
const val LEVEL_MORE: Char = '+'

const val CHAR_NAME: String = "[0-9a-zA-Z\\[\\]\\-_]"
const val CHAR_NAME_WITH_SPACE: String = "[0-9a-zA-Z\\[\\]\\-_ ]"

const val FLAG_USER_AND_RANGE: String = "ur"
const val FLAG_MOD: String = "mod"
const val FLAG_MODE: String = "mode"
const val FLAG_NAME: String = "name"
const val FLAG_DATA: String = "data"
const val FLAG_UID: String = "uid"
const val FLAG_BID: String = "bid"
const val FLAG_SID: String = "sid"
const val FLAG_MATCHID: String = "matchid"
const val FLAG_ID: String = "id"
const val FLAG_DAY: String = "day"
const val FLAG_RANGE: String = "range"
const val FLAG_QQ_ID: String = "qq"
const val FLAG_QQ_GROUP: String = "group"

@Language("RegExp")
val REG_CAPS_INSENSITIVE: String = "(?i)"

@Language("RegExp")
val REG_ANYTHING: String = "[\\s\\S]"

@Language("RegExp")
val REG_SPACE: String = "\\s"

@Language("RegExp")
val REG_STAR: String = "\\*"

@Language("RegExp")
val REG_NUMBER_12: String = "\\d{1,2}"

@Language("RegExp")
val REG_NUMBER_13: String = "\\d{1,3}"

@Language("RegExp")
val REG_NUMBER: String = "\\d"

@Language("RegExp")
val REG_WORD: String = "\\w"

@Language("RegExp")
val REG_NUMBER_SEPERATOR: String = "[\\d\\-\\s_,，|:：`、]"

@Language("RegExp")
val REG_NUMBER_DECIMAL: String = "\\d+\\.?\\d*"

@Language("RegExp")
val REG_PLUS: String = "\\+"

@Language("RegExp")
val REG_COLON: String = "[:：]"

@Language("RegExp")
val REG_QUESTION: String = "[?？]"

@Language("RegExp")
val REG_HASH: String = "[#＃]"

@Language("RegExp")
val REG_HYPHEN: String = "[\\-－—]"

@Language("RegExp")
val REG_EXCLAMINATION: String = "[!！]"

@Language("RegExp")
val REG_IGNORE: String = "(?![A-Za-z\\-_])"

@Language("RegExp")
// 用在多成绩的指令里（可能有 s）
val REG_IGNORE_S: String = "(?![^s:：\\d\\s])"

@Language("RegExp")
// 用在有时候需要匹配 bid 和 sid 的指令里
val REG_IGNORE_BS: String = "(?![^bs:：\\d\\s])"

@Language("RegExp")
val REG_NAME: String = "($CHAR_NAME$CHAR_NAME_WITH_SPACE+$CHAR_NAME)"

@Language("RegExp")
val REG_USERNAME_SEPERATOR: String = "[0-9a-zA-Z\\[\\]\\-\\s_,，|:：`、]"

@Language("RegExp")
val REG_USERNAME: String = "[0-9a-zA-Z\\[\\]\\-\\s_]"

@Language("RegExp")
val REG_MOD: String = "(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)"

@Language("RegExp")
val REG_MODE: String = "(osu|taiko|ctb|fruits?|mania|std|0|1|2|3|o|m|c|f|t)"

@Language("RegExp")
val REG_RANGE: String = "((100|$REG_NUMBER_12)($REG_HYPHEN$REG_NUMBER_13)?)"

@Language("RegExp")
val REG_RANGE_DAY: String = "($REG_NUMBER_13($REG_HYPHEN$REG_NUMBER_13)?)"
