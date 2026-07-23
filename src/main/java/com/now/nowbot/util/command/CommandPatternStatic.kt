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
const val CHAR_COLON: Char = ':'
const val CHAR_QUESTION: Char = '?'

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
const val FLAG_MATCH_ID: String = "match"
const val FLAG_ID: String = "id"
const val FLAG_SORT: String = "sort"
const val FLAG_SKIP: String = "skip"
const val FLAG_DAY: String = "day"
const val FLAG_RANGE: String = "range"
const val FLAG_QQ_ID: String = "qq"
const val FLAG_QQ_GROUP: String = "group"
const val FLAG_VERSION: String = "version"
const val FLAG_PAGE: String = "page"
const val FLAG_DIFF: String = "diff"
const val FLAG_TYPE: String = "type"
const val FLAG_TYPE2: String = "type2"
const val FLAG_TIME: String = "time"
const val FLAG_TEXT: String = "text"
const val FLAG_ANY: String = "any"
const val FLAG_OPERATE: String = "operate"
const val FLAG_SERVICE: String = "service"

@Language("RegExp")
const val INDICATOR_CAPS_INSENSITIVE: String = "(?i)"

@Language("RegExp")
const val PATTERN_ANYTHING: String = "[\\s\\S]"

@Language("RegExp")
const val PATTERN_ANYTHING_BUT_NO_SPACE: String = "\\S"

@Language("RegExp")
const val PATTERN_ANYTHING_BUT_NO_HASH: String = "[^:：#＃]"

@Language("RegExp")
const val PATTERN_ANYTHING_BUT_NO_PLUS: String = "[^:：＋+]"

@Language("RegExp")
const val PATTERN_ANYTHING_BUT_NO_STARS: String = "[^☆✪★*⋆]"

@Language("RegExp")
const val PATTERN_ANYTHING_BUT_NO_HASH_STARS: String = "[^☆✪★*⋆:：#＃]"

@Language("RegExp")
const val PATTERN_SPACE: String = "\\s"

/**
 * \\s+
 */
val REGEX_SPACE_MORE = Regex(PATTERN_SPACE + LEVEL_MORE)

@Language("RegExp")
const val PATTERN_STAR: String = "[⁂☆✪★*⋆＊]"

@Language("RegExp")
const val PATTERN_UID: String = "u?id"

@Language("RegExp")
const val PATTERN_FULL_STOP: String = "[.։܂۔。﹒．｡︒・、]"

@Language("RegExp")
const val PATTERN_NUMBER_12: String = "\\d{1,2}"

@Language("RegExp")
const val PATTERN_NUMBER_13: String = "\\d{1,3}"

@Language("RegExp")
const val PATTERN_NUMBER_1_100: String = "(\\d{1,2}|100)"

@Language("RegExp")
const val PATTERN_NUMBER_15: String = "\\d{1,5}"

val REGEX_NUMBER_15 = Regex(PATTERN_NUMBER_15)

val REGEX_NUMBER = Regex("^[0-9.]+$")


/**
 * 只匹配单一数字，等同于 [0-9]
 */
@Language("RegExp")
const val PATTERN_NUMBER: String = "\\d"

@Language("RegExp")
const val PATTERN_SIGNED_NUMBER: String = "[-\u2010\u2013\u2014\u2212\uFF0D\\d\\s]"

@Language("RegExp")
const val PATTERN_QQ: String = "\\d{6,10}"

@Language("RegExp")
const val PATTERN_NUMBER_MORE: String = "\\d+"

@Language("RegExp")
const val PATTERN_WORD: String = "\\w"

@Language("RegExp")
const val PATTERN_BOOLEAN: String = "([真假是否非对错tfyn]|正确|错误|true|false|yes|not?)"

@Language("RegExp")
const val PATTERN_TIME: String = "([\\-年月日天分钟秒小时0-9A-Za-z/:：\\\\]+)"

@Language("RegExp")
const val PATTERN_OPERATOR: String = "([<>＜＞][=＝]?|[=＝][=＝]?|[!！][=＝]|[≥≤])"

val REGEX_OPERATOR = Regex(PATTERN_OPERATOR)

@Language("RegExp")
const val PATTERN_OPERATOR_WITH_SPACE: String = "\\s*$PATTERN_OPERATOR\\s*"

val REGEX_OPERATOR_WITH_SPACE = Regex(PATTERN_OPERATOR_WITH_SPACE)

@Language("RegExp")
const val PATTERN_NUMBER_SEPARATOR: String = "[\\d\\-\\s_,，|:：`、]"

@Language("RegExp")
const val PATTERN_NUMBER_DECIMAL: String = "\\d+\\.?\\d*"

val REGEX_NUMBER_DECIMAL = Regex(PATTERN_NUMBER_DECIMAL)

@Language("RegExp")
const val PATTERN_PLUS: String = "[＋+]"

val REGEX_PLUS = Regex(PATTERN_PLUS)

@Language("RegExp")
const val PATTERN_GREATER: String = "[＞>]"

@Language("RegExp")
const val PATTERN_LESS: String = "[＜<]"

@Language("RegExp")
const val PATTERN_EQUAL: String = "[＝=]"

@Language("RegExp")
const val PATTERN_COLON: String = "[:：]"

val REGEX_COLON = Regex(PATTERN_COLON)

@Language("RegExp")
const val PATTERN_QUESTION: String = "[?？]"

@Language("RegExp")
const val PATTERN_HASH: String = "[#＃]"

val REGEX_HASH = Regex(PATTERN_HASH)

@Language("RegExp")
const val PATTERN_HYPHEN: String = "[\\-－—~～〜]"

val REGEX_HYPHEN = Regex(PATTERN_HYPHEN)

@Language("RegExp")
const val PATTERN_SLASH: String = "[/\\\\]"

val REGEX_HYPHEN_OR_SLASH = Regex("($PATTERN_HYPHEN|$PATTERN_SLASH)")

@Language("RegExp")
const val PATTERN_EXCLAMATION: String = "[!！]"

@Language("RegExp")
const val PATTERN_LEFT_BRACKET: String = "[\\[{『【［｢]"

@Language("RegExp")
const val PATTERN_RIGHT_BRACKET: String = "[]}』】］｣]"

@Language("RegExp")
const val PATTERN_QUOTATION : String = "[“”\"«»《》＂‟]"

@Language("RegExp")
const val PATTERN_IGNORE: String = "[A-Za-z\\-_]"

@Language("RegExp")
const val PATTERN_NAME: String = "($CHAR_NAME$CHAR_NAME_WITH_SPACE+$CHAR_NAME)"

@Language("RegExp")
const val PATTERN_ANYTHING_BUT_NO_OPERATOR: String = "(\\S[^#＃！!≥≤<>＜＞＝=]+)"

@Language("RegExp")
const val PATTERN_MAI_DIFFICULTY = "(\\b(((1[0-4])|[1-9])(($PATTERN_PLUS?$PATTERN_QUESTION?)|(\\.[0-9])?))|(15(\\.0)?)\\b)"

@Language("RegExp")
const val PATTERN_MAI_CABINET = "(sd|standard|标准|标|dx|deluxe|豪华|utage|宴(会场?)?)"

@Language("RegExp")
const val PATTERN_MAI_RANGE = "(?!$PATTERN_HASH)\\s*$PATTERN_MAI_DIFFICULTY$PATTERN_HYPHEN?$PATTERN_MAI_DIFFICULTY?\\s*(?<!$PATTERN_HASH)"

val REGEX_MAI_RANGE = Regex(PATTERN_MAI_RANGE)

@Language("RegExp")
const val PATTERN_RANGE_ONLY = "^\\s*$PATTERN_HASH?\\s*(\\d{1,3}$PATTERN_HYPHEN+)?\\d{1,3}\\s*$"

val REGEX_RANGE_ONLY = Regex(PATTERN_RANGE_ONLY)

/**
 * 这个分隔符会分隔空格
 */
@Language("RegExp")
const val PATTERN_SEPARATOR: String = "[\\s,，|:：`、]+"

val REGEX_SEPARATOR = Regex(PATTERN_SEPARATOR)

@Language("RegExp")
const val PATTERN_SEPARATOR_NO_SPACE: String = "[,，|:：`、]+"

val REGEX_SEPARATOR_NO_SPACE = Regex(PATTERN_SEPARATOR_NO_SPACE)

@Language("RegExp")
const val PATTERN_USERNAME_SEPARATOR: String = "[0-9a-zA-Z\\[\\]\\-\\s\\n_,，|:：`、]"

@Language("RegExp")
const val PATTERN_USERNAME: String = "[0-9a-zA-Z\\[\\]\\-\\s_]"

@Language("RegExp")
const val PATTERN_MOD: String = "([1-9a-zA-Z][a-zA-Z]|[vV]2)"

@Language("RegExp")
const val PATTERN_MODE: String = "(((osu|taiko|catch|ctb|fruits?|mania([47]k)?|std)\\s*(relax|autopilot|ap)?)|((rx|relax)\\s*[0-3])|[012345678]|[omcft]|[ocf]r|oa|[47]k)"

// @Language("RegExp")
// const val PATTERN_DIFF: String = "([0-4aebrm]|bsc|adv|exp|mas|rem|rms|ba|ad|ex|ma|re|basic|advanced|expert|master|re[:：]?\\s*master)"

@Language("RegExp")
const val PATTERN_RANGE: String = "((200)|(1?$PATTERN_NUMBER_12)$PATTERN_HYPHEN)?((200)|(1?$PATTERN_NUMBER_12))"

val REGEX_RANGE = Regex(PATTERN_RANGE)

@Language("RegExp")
const val PATTERN_RANGE_DAY: String = "($PATTERN_NUMBER_13($PATTERN_HYPHEN$PATTERN_NUMBER_13)?)"

@Language("RegExp")
const val PATTERN_COVER: String = "[A-Za-z@12]"

@Language("RegExp")
const val PATTERN_2_USER: String = "$PATTERN_USERNAME+\\s*($PATTERN_SEPARATOR_NO_SPACE|\\s+vs\\s+)?\\s*$PATTERN_USERNAME*"

@Language("RegExp")
const val PATTERN_ANYTHING_MORE: String = PATTERN_ANYTHING + LEVEL_MORE

val REGEX_CRLF = Regex("\n")