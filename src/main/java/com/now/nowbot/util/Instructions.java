package com.now.nowbot.util;

import java.util.regex.Pattern;

public class Instructions {
    static String DELIMIT_COLON = "[:：]";
    static String DELIMIT_WHAT = "#";
    static String DELIMIT_A = "\\s*";
    static String DELIMIT_S = "\\s+";
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
        public RegexBuilder nullable() {
            sb.append('?');
            return this;
        }

        /**
         * 就是正则后面加一个 +
         *
         * @return this
         */
        public RegexBuilder more() {
            sb.append('+');
            return this;
        }

        /**
         * 就是正则后面加一个 *
         *
         * @return this
         */
        public RegexBuilder any() {
            sb.append('*');
            return this;
        }


        public RegexBuilder addDelimitColon() {
            sb.append(DELIMIT_COLON);
            return this;
        }

        public RegexBuilder addSplit() {
            sb.append(DELIMIT_A);
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
