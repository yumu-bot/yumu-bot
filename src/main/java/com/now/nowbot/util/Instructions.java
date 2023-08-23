package com.now.nowbot.util;

import java.util.regex.Pattern;

public class Instructions {
    static String COLON = "[:：]";
    static String DELIMIT_WHAT = "#";
    static String SPACE_0P = "\\s*";
    static String SPACE_1P = "\\s+";
    static String SPACE_01 = "\\s?";
    static String WORD_0P = "\\w*";
    static String WORD_1P = "\\w+";
    static String WORD_01 = "\\w?";
    static String NUMBER_0P = "\\d*";
    static String NUMBER_1P = "\\d+";
    static String NUMBER_01 = "\\d?";
    static String NO_ADD_WORD = "(?![a-zA-Z_])";
    static Pattern OSU_MODE = Pattern.compile("(?<mode>(?i)[0-3otcfm]|osu|taiko|catch|fruits|mania)");
    static Pattern OSU_NAME = Pattern.compile("(?<name>[a-zA-Z0-9\\[\\]\\-_ ]{3,15})");
    static Pattern OSU_UID = Pattern.compile("(?<uid>\\d{1,8})");

    public static class RegexBuilder {
        StringBuilder sb;
        boolean hasAt;

        public RegexBuilder(String s) {
            sb = new StringBuilder("^(?<start>").append(s).append(')');
        }

        public RegexBuilder addRegex(String reg) {
            sb.append(reg);
            return this;
        }
        public RegexBuilder addRegex(Pattern reg) {
            sb.append(reg.pattern());
            return this;
        }

        public RegexBuilder addOsuName() {
            sb.append(OSU_NAME.pattern());
            return this;
        }

        public RegexBuilder addOsuMode() {
            sb.append(OSU_MODE.pattern());
            return this;
        }

        public RegexBuilder addOsuUid() {
            sb.append(OSU_UID.pattern());
            return this;
        }

        public RegexBuilder groupStart(String group) {
            sb.append("(?<").append(group.trim()).append('>');
            return this;
        }
        public RegexBuilder groupStart() {
            sb.append('(');
            return this;
        }

        public RegexBuilder groupEnd() {
            sb.append(')');
            return this;
        }

        /**
         * 就是正则后面加一个 ?
         *
         * @return this
         */
        public RegexBuilder i01() {
            sb.append('?');
            return this;
        }

        /**
         * 就是正则后面加一个 +
         *
         * @return this
         */
        public RegexBuilder i1P() {
            sb.append('+');
            return this;
        }

        /**
         * 就是正则后面加一个 *
         *
         * @return this
         */
        public RegexBuilder i0P() {
            sb.append('*');
            return this;
        }


        public RegexBuilder addColon() {
            sb.append(COLON);
            return this;
        }

        public RegexBuilder addSpace0P() {
            sb.append(SPACE_0P);
            return this;
        }

        public RegexBuilder addSpace1P() {
            sb.append(SPACE_1P);
            return this;
        }

        public RegexBuilder addSpace01() {
            sb.append(SPACE_01);
            return this;
        }

        public RegexBuilder addWord1P() {
            sb.append(WORD_1P);
            return this;
        }

        public RegexBuilder addWord0P() {
            sb.append(WORD_0P);
            return this;
        }

        public RegexBuilder addWord01() {
            sb.append(WORD_01);
            return this;
        }

        public RegexBuilder addNumber1P() {
            sb.append(NUMBER_1P);
            return this;
        }

        public RegexBuilder addNumber0P() {
            sb.append(NUMBER_0P);
            return this;
        }

        public RegexBuilder addNumber01() {
            sb.append(NUMBER_01);
            return this;
        }

        public Pattern build() {
            return Pattern.compile(sb.toString());
        }
    }

    static class RegexResut {

    }

    public static RegexBuilder getRegexBuilder(String start){
        return new RegexBuilder(start);
    }
    public static RegexBuilder getRegexBuilder(Pattern start){
        return new RegexBuilder(start.pattern());
    }

}
