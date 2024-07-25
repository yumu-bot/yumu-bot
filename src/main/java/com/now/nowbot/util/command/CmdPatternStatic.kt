package com.now.nowbot.util.command

import org.intellij.lang.annotations.Language

object CmdPatternStatic {
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

    const val FLAG_USER_AND_RANGE: String = "ur"
    const val FLAG_MOD: String = "mod"
    const val FLAG_MODE: String = "mode"
    const val FLAG_NAME: String = "name"
    const val FLAG_UID: String = "uid"
    const val FLAG_BID: String = "bid"
    const val FLAG_SID: String = "sid"
    const val FLAG_ID: String = "id"
    const val FLAG_RANGE: String = "range"
    const val FLAG_QQ_ID: String = "qq"
    const val FLAG_QQ_GROUP: String = "group"

    @Language("RegExp")
    val REG_USER_AND_RANGE: String =
        "(?<$FLAG_USER_AND_RANGE>([0-9a-zA-Z\\[\\]\\-_][0-9a-zA-Z\\[\\]\\-_ ]+[0-9a-zA-Z\\[\\]\\-_])?([#＃]?((\\d{1,3})[\\-－ ])?(\\d{1,3}))?)?"

    @Language("RegExp")
    val REG_START: String = "(?i)"

    @Language("RegExp")
    val REG_START_SORT: String = "/ym"

    @Language("RegExp")
    val REG_START_ALL: String = "[!！](ym)?"

    @Language("RegExp")
    val REG_SPACE: String = "\\s*"

    @Language("RegExp")
    val REG_SPACE_1P: String = "\\s+"

    @Language("RegExp")
    val REG_SPACE_01: String = "\\s?"

    @Language("RegExp")
    val REG_COLUMN: String = "[:：]"

    @Language("RegExp")
    val REG_HASH: String = "[#＃]"

    @Language("RegExp")
    val REG_HYPHEN: String = "[\\-－]"

    @Language("RegExp")
    val REG_EXCLAM: String = "[!！]"

    @Language("RegExp")
    val REG_IGNORE: String = "(?![a-z_])"

    @Language("RegExp")
    val REG_NAME: String = "(?<$FLAG_NAME>[0-9a-zA-Z\\[\\]\\-_][0-9a-zA-Z\\[\\]\\-_ ]+[0-9a-zA-Z\\[\\]\\-_])"

    @Language("RegExp")
    val REG_QQ_ID: String = "(qq=\\s*(?<$FLAG_QQ_ID>\\d{5,}))"

    @Language("RegExp")
    val REG_QQ_GROUP: String = "(group=\\s*(?<$FLAG_QQ_GROUP>\\d{5,}))"

    @Language("RegExp")
    val REG_UID: String = "(uid=(?<$FLAG_UID>\\d+))"

    @Language("RegExp")
    val REG_MOD: String = "(\\+?(?<$FLAG_MOD>(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)+))"

    @Language("RegExp")
    val REG_MODE: String = "(?<$FLAG_MODE>osu|taiko|ctb|fruits?|mania|std|0|1|2|3|o|m|c|f|t)"

    @Language("RegExp")
    val REG_RANGE: String = "(?<$FLAG_RANGE>(100|\\d{1,2})([\\-－]\\d{1,3})?)"

    @Language("RegExp")
    val REG_RANGE_DAY: String = "(?<$FLAG_RANGE>\\d{1,3}([\\-－]\\d{1,3})?)"

    @Language("RegExp")
    val REG_ID: String = "(?<$FLAG_ID>\\d+)"

    @Language("RegExp")
    val REG_BID: String = "(?<$FLAG_BID>\\d+)"

    @Language("RegExp")
    val REG_SID: String = "(?<$FLAG_SID>\\d+)"
}
