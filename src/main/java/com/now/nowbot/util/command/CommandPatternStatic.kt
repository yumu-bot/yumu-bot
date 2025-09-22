package com.now.nowbot.util.command

import org.intellij.lang.annotations.Language

const val CHAR_HASH: Char = '#'
const val CHAR_HASH_FULL: Char = '＃'
// const val CHAR_EQUAL: Char = '='
const val CHAR_SEPARATOR: Char = '|'
const val CHAR_BEGIN: Char = '^'
const val CHAR_FINAL: Char = '$'
const val CHAR_BRACE_START: Char = '{'
const val CHAR_BRACE_END: Char = '}'
const val CHAR_COMMAS: Char = ','
const val CHAR_GROUP_START: Char = '('
const val CHAR_GROUP_END: Char = ')'
const val CHAR_SLASH: Char = '/'
const val CHAR_BACK_SLASH: Char = '\\'

const val LEVEL_MAYBE: Char = '?'
const val LEVEL_ANY: Char = '*'
const val LEVEL_MORE: Char = '+'

const val CHAR_NAME: String = "[0-9a-zA-Z\\[\\]\\-_]"
const val CHAR_NAME_WITH_SPACE: String = "[0-9a-zA-Z\\[\\]\\-_ ]"

const val FLAG_USER_AND_RANGE: String = "ur"
const val FLAG_2_USER: String = "u2"
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
const val FLAG_VERSION: String = "version"
const val FLAG_PAGE: String = "page"
const val FLAG_DIFF: String = "diff"
const val FLAG_TYPE: String = "type"

@Language("RegExp")
val REG_CAPS_INSENSITIVE: String = "(?i)"

@Language("RegExp")
val REG_ANYTHING: String = "[\\s\\S]"

@Language("RegExp")
val REG_ANYTHING_BUT_NO_SPACE: String = "\\S"

@Language("RegExp")
val REG_ANYTHING_BUT_NO_HASH: String = "[^:：#＃]"

@Language("RegExp")
val REG_ANYTHING_BUT_NO_PLUS: String = "[^:：＋+]"

@Language("RegExp")
val REG_ANYTHING_BUT_NO_STARS: String = "[^☆✪★*⋆]"

@Language("RegExp")
val REG_ANYTHING_BUT_NO_HASH_STARS: String = "[^☆✪★*⋆:：#＃]"

@Language("RegExp")
val REG_SPACE: String = "\\s"

@Language("RegExp")
val REG_STAR: String = "[⁂☆✪★*⋆＊]"

@Language("RegExp")
val REG_UID: String = "u?id"

@Language("RegExp")
val REG_FULL_STOP: String = "[.։܂۔。﹒．｡︒・、]"

@Language("RegExp")
val REG_NUMBER_12: String = "\\d{1,2}"

@Language("RegExp")
val REG_NUMBER_13: String = "\\d{1,3}"

@Language("RegExp")
val REG_NUMBER_1_100: String = "(\\d{1,2}|100)"

@Language("RegExp")
val REG_NUMBER_15: String = "\\d{1,5}"

/**
 * 只匹配单一数字，等同于 [0-9]
 */
@Language("RegExp")
val REG_NUMBER: String = "\\d"

@Language("RegExp")
val REG_NUMBER_MORE: String = "\\d+"

@Language("RegExp")
val REG_WORD: String = "\\w"

@Language("RegExp")
val REG_BOOLEAN: String = "([真假是否非对错tfyn]|正确|错误|true|false|yes|not?)"

@Language("RegExp")
val REG_TIME: String = "([年月日天分钟秒小时0-9A-Za-z]+)"

@Language("RegExp")
val REG_OPERATOR: String = "([<>＜＞][=＝]?|[=＝][=＝]?|[!！][=＝]|[≥≤])"

@Language("RegExp")
val REG_OPERATOR_WITH_SPACE: String = "\\s*$REG_OPERATOR\\s*"

@Language("RegExp")
val REG_NUMBER_SEPERATOR: String = "[\\d\\-\\s_,，|:：`、]"

@Language("RegExp")
val REG_NUMBER_DECIMAL: String = "\\d+\\.?\\d*"

@Language("RegExp")
val REG_PLUS: String = "[＋+]"

@Language("RegExp")
val REG_GREATER: String = "[＞>]"

@Language("RegExp")
val REG_LESS: String = "[＜<]"

@Language("RegExp")
val REG_EQUAL: String = "[＝=]"

@Language("RegExp")
val REG_COLON: String = "[:：]"

@Language("RegExp")
val REG_QUESTION: String = "[?？]"

@Language("RegExp")
val REG_HASH: String = "[#＃]"

@Language("RegExp")
val REG_HYPHEN: String = "[\\-－—~～〜]"

@Language("RegExp")
val REG_EXCLAMATION: String = "[!！]"

@Language("RegExp")
val REG_LEFT_BRACKET: String = "[\\[{『【［｢]"

@Language("RegExp")
val REG_RIGHT_BRACKET: String = "[]}』】］｣]"

@Language("RegExp")
val REG_QUOTATION : String = "[“”\"«»《》＂‟]"

@Language("RegExp")
val REG_IGNORE: String = "[A-Za-z\\-_]"

@Language("RegExp")
// 用在有时候需要匹配 bid 和 sid 的指令里
val REG_IGNORE_BS: String = "[^bs:：\\d\\s]"

@Language("RegExp")
val REG_NAME: String = "($CHAR_NAME$CHAR_NAME_WITH_SPACE+$CHAR_NAME)"

@Language("RegExp")
val REG_ANYTHING_BUT_NO_OPERATOR: String = "(\\S[^#＃！!≥≤<>＜＞＝=]+)"

@Language("RegExp")
val REG_MAI_DIFFICULTY = "((((1[0-4])|[1-9])(($REG_PLUS?$REG_QUESTION?)|(\\.[0-9])?))|(15(\\.0)?))"

@Language("RegExp")
val REG_MAI_CABINET = "(sd|standard|标准|标|dx|deluxe|豪华)"

@Language("RegExp")
val REG_MAI_RANGE = "$REG_MAI_DIFFICULTY\\s*$REG_HYPHEN?\\s*$REG_MAI_DIFFICULTY?"

@Language("RegExp")
val RANGE_ONLY = "^\\s*$REG_HASH?\\s*(\\d{1,3}$REG_HYPHEN+)?\\d{1,3}\\s*$".toRegex()

/**
 * 这个分隔符会分隔空格
 */
@Language("RegExp")
val REG_SEPERATOR: String = "[\\s,，|:：`、]+"

@Language("RegExp")
val REG_SEPERATOR_NO_SPACE: String = "[,，|:：`、]+"

@Language("RegExp")
val REG_USERNAME_SEPERATOR: String = "[0-9a-zA-Z\\[\\]\\-\\s\\n_,，|:：`、]"

@Language("RegExp")
val REG_USERNAME: String = "[0-9a-zA-Z\\[\\]\\-\\s_]"

@Language("RegExp")
val REG_MOD: String = "([1-9a-zA-Z][a-zA-Z])"

@Language("RegExp")
val REG_MODE: String = "((osu|taiko|ctb|fruits?|mania|std)\\s*(relax|autopilot|ap)|((rx|relax)\\s*[0-3])|[01234568]|o|m|c|f|t|[ocf]r|oa)"

@Language("RegExp")
val REG_DIFF: String = "([0-4baemr]|bsc|adv|exp|mas|rem|rms|ba|ad|ex|ma|re|basic|advanced|expert|master|re[:：]?\\s*master)"

@Language("RegExp")
val REG_RANGE: String = "((200)|(1?$REG_NUMBER_12)$REG_HYPHEN)?((200)|(1?$REG_NUMBER_12))"

@Language("RegExp")
val REG_RANGE_DAY: String = "($REG_NUMBER_13($REG_HYPHEN$REG_NUMBER_13)?)"

@Language("RegExp")
val REG_COVER: String = "[A-Za-z@12]"

@Language("RegExp")
val REG_2_USER: String = "$REG_USERNAME+\\s*($REG_SEPERATOR_NO_SPACE|\\s+vs\\s+)?\\s*$REG_USERNAME*"

