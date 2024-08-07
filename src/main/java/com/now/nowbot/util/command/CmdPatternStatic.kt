package com.now.nowbot.util.command

import org.intellij.lang.annotations.Language

const val CHAR_HASH: Char = '#'
const val CHAR_HASH_FULL: Char = '＃'
const val CHAR_WHATEVER: Char = '?'
const val CHAR_ANY: Char = '*'
const val CHAR_MORE: Char = '+'
const val CHAR_SEPARATOR: Char = '|'
const val CHAR_START: Char = '^'
const val CHAR_END: Char = '$'
const val CHAR_GROUP_START: Char = '('
const val CHAR_GROUP_END: Char = ')'

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
const val FLAG_ID: String = "id"
const val FLAG_RANGE: String = "range"
const val FLAG_QQ_ID: String = "qq"
const val FLAG_QQ_GROUP: String = "group"

@Language("RegExp")
val REG_START: String = "(?i)"

@Language("RegExp")
val REG_START_SORT: String = "\\\\ym"

@Language("RegExp")
val REG_START_ALL: String = "[!！](ym)?"

@Language("RegExp")
val REG_ANY: String = "[\\s\\S]*"

@Language("RegExp")
val REG_ANY_1P: String = "[\\s\\S]+"

@Language("RegExp")
val REG_SPACE: String = "\\s"

@Language("RegExp")
val REG_SPACE_ANY: String = "\\s*"

@Language("RegExp")
val REG_SPACE_1P: String = "\\s+"

@Language("RegExp")
val REG_SPACE_01: String = "\\s?"

@Language("RegExp")
val REG_NUMBER_12: String = "\\d{1,2}"

@Language("RegExp")
val REG_NUMBER_13: String = "\\d{1,3}"

@Language("RegExp")
val REG_NUMBER_5P: String = "\\d{5,}"

@Language("RegExp")
val REG_NUMBER_1P: String = "\\d+"

@Language("RegExp")
val REG_NUMBER_MULTI: String = "[\\d\\-\\s_,，|:：`、]*"

@Language("RegExp")
val REG_NUMBER_DECIMAL: String = "\\d+\\.?\\d*"

@Language("RegExp")
val REG_PLUS: String = "\\+"

@Language("RegExp")
val REG_PLUS_01: String = "\\+?"

@Language("RegExp")
val REG_COLUMN: String = "[:：]"

@Language("RegExp")
val REG_HASH: String = "[#＃]"

@Language("RegExp")
val REG_HYPHEN: String = "[\\-－—]"

@Language("RegExp")
val REG_EXCLAIM: String = "[!！]"

@Language("RegExp")
val REG_IGNORE: String = "(?![A-Za-z\\-_])"

@Language("RegExp")
// 用在多成绩的指令里（可能有 s）
val REG_IGNORE_S: String = "(?![^s:：\\d\\s])"

@Language("RegExp")
// 用在有时候需要匹配 bid 和 sid 的指令里
val REG_IGNORE_BS: String = "(?![^bs:：\\d\\s])"

@Language("RegExp")
val REG_USER_AND_RANGE: String =
    "(?<$FLAG_USER_AND_RANGE>($CHAR_NAME$CHAR_NAME_WITH_SPACE+$CHAR_NAME)?$REG_SPACE_ANY($REG_HASH?(($REG_NUMBER_13)$REG_HYPHEN)?($REG_NUMBER_13))?)?"

@Language("RegExp")
val REG_NAME: String = "(?<$FLAG_NAME>$CHAR_NAME$CHAR_NAME_WITH_SPACE+$CHAR_NAME)"

@Language("RegExp")
val REG_NAME_MULTI: String = "[0-9a-zA-Z\\[\\]\\-\\s_,，|:：`、]+"

@Language("RegExp")
val REG_NAME_ANY: String = "[0-9a-zA-Z\\[\\]\\-\\s_]*"

@Language("RegExp")
val REG_QQ_ID: String = "(qq=$REG_SPACE_ANY(?<$FLAG_QQ_ID>$REG_NUMBER_5P))"

@Language("RegExp")
val REG_QQ_GROUP: String = "(group=$REG_SPACE_ANY(?<$FLAG_QQ_GROUP>$REG_NUMBER_5P))"

@Language("RegExp")
val REG_UID: String = "(uid=$REG_SPACE_ANY(?<$FLAG_UID>$REG_NUMBER_1P))"

@Language("RegExp")
// 加号必须要
val REG_MOD: String = "($REG_PLUS(?<$FLAG_MOD>(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)+))"

@Language("RegExp")
// 加号不一定要
val REG_MOD_01: String = "($REG_PLUS_01(?<$FLAG_MOD>(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)+))"

@Language("RegExp")
val REG_MODE: String = "(?<$FLAG_MODE>osu|taiko|ctb|fruits?|mania|std|0|1|2|3|o|m|c|f|t)"

@Language("RegExp")
val REG_RANGE: String = "(?<$FLAG_RANGE>(100|$REG_NUMBER_12)($REG_HYPHEN$REG_NUMBER_13)?)"

@Language("RegExp")
val REG_RANGE_DAY: String = "(?<$FLAG_RANGE>$REG_NUMBER_13($REG_HYPHEN$REG_NUMBER_13)?)"

@Language("RegExp")
val REG_ID: String = "(?<$FLAG_ID>$REG_NUMBER_1P)"

@Language("RegExp")
val REG_BID: String = "(?<$FLAG_BID>$REG_NUMBER_1P)"

@Language("RegExp")
val REG_SID: String = "(?<$FLAG_SID>$REG_NUMBER_1P)"

