package com.now.nowbot.util.command;

import org.intellij.lang.annotations.Language;

public class CmdPatternStatic {
    public static final char CHAR_HASH        = '#';
    public static final char CHAR_HASH_FULL   = '＃';
    public static final char CHAR_WHATEVER    = '?';
    public static final char CHAR_ANY         = '*';
    public static final char CHAR_MORE        = '+';
    public static final char CHAR_SEPARATOR   = '|';
    public static final char CHAR_START       = '^';
    public static final char CHAR_END         = '$';
    public static final char CHAR_GROUP_START = '(';
    public static final char CHAR_GROUP_END   = ')';

    public static final String FLAG_USER_AND_RANGE = "ur";
    public static final String FLAG_MODE           = "mode";
    public static final String FLAG_NAME           = "name";
    public static final String FLAG_UID            = "uid";
    public static final String FLAG_QQ             = "qq";

    @Language("RegExp")
    static final        String REG_USER_AND_RANGE = "(?<ur>([0-9a-zA-Z\\[\\]\\-_][0-9a-zA-Z\\[\\]\\-_ ]+[0-9a-zA-Z\\[\\]\\-_])?([#＃]?((\\d{1,3})[\\-－ ])?(\\d{1,3}))?)?";
    @Language("RegExp")
    public static final String REG_START          = "(?i)([!！](ym)?)|(/ym)";

    @Language("RegExp")
    public static final String REG_SPACE          = "\\s*";
    @Language("RegExp")
    public static final String REG_SPACE_1P       = "\\s+";
    @Language("RegExp")
    public static final String REG_SPACE_01       = "\\s?";
    @Language("RegExp")
    public static final String REG_COLUMN         = "[:：]";
    @Language("RegExp")
    public static final String REG_HASH           = "[#＃]";
    @Language("RegExp")
    public static final String REG_HYPHEN         = "[\\-－]";
    @Language("RegExp")
    public static final String REG_IGNORE         = "(?![A-Za-z_])";
    @Language("RegExp")
    public static final String REG_NAME           = "(?<name>[0-9a-zA-Z\\[\\]\\-_][0-9a-zA-Z\\[\\]\\-_ ]+[0-9a-zA-Z\\[\\]\\-_])";
    @Language("RegExp")
    public static final String REG_QQ_ID          = "(qq=\\s*(?<qq>\\d{5,}))";
    @Language("RegExp")
    public static final String REG_QQ_GROUP       = "(group=\\s*(?<group>\\d{5,}))";
    @Language("RegExp")
    public static final String REG_UID            = "(uid=(?<uid>\\d+))";
    @Language("RegExp")
    public static final String REG_MOD            = "(\\+?(?<mod>(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)+))";
    @Language("RegExp")
    public static final String REG_MODE           = "(?<mode>osu|taiko|ctb|fruits?|mania|std|0|1|2|3|o|m|c|f|t)";
    @Language("RegExp")
    public static final String REG_RANGE          = "(?<range>(100|\\d{1,2})([\\-－]\\d{1,3})?)";
    @Language("RegExp")
    public static final String REG_RANGE_DAY      = "(?<range>\\d{1,3}([\\-－]\\d{1,3})?)";
    @Language("RegExp")
    public static final String REG_ID             = "(?<id>\\d+)";
    @Language("RegExp")
    public static final String REG_BID            = "(?<bid>\\d+)";
    @Language("RegExp")
    public static final String REG_SID            = "(?<sid>\\d+)";
}
