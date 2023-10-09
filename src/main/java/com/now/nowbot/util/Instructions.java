package com.now.nowbot.util;

import java.util.regex.Pattern;

// * 正则
// * (?:pattern) 匹配 pattern 但不捕获该匹配的子表达式,即它是一个非捕获匹配,不存储供以后使用的匹配.
// * 例子: "industr(?:y|ies)" 是比 "industry|industries" 更经济的表达式.
// * (?!pattern) 执行反向预测先行搜索的子表达式,该表达式匹配不处于匹配 pattern 的字符串的起始点的搜索字符串,不占用字符,即发生匹配后,下一匹配的搜索紧随上一匹配之后.
// * (?<!pattern)执行正向预测先行搜索的子表达式,该表达式与上条相反,同时也不占用字符 (其实 (?!) 跟 (?<!) 作用相同,区别是下次匹配的指针位置)
// * 例子: "(?<!ab)cd(?!ef)" 仅匹配非ab开头的,ef结尾的cd,且ab与ef仅作搜索用并不占用宽度,即例子对gcd中的cd也会被匹配
// * (?=pattern) 也叫零宽度正预测先行断言,它断言被匹配的字符串以表达式pattern结尾但除了结尾以外的部分,预测先行不占用字符,即发生匹配后,下一匹配的搜索紧随上一匹配之后.
// * (?<=pattern)也叫零宽度正回顾后发断言，它断言自身出现的位置的前面能匹配表达式pattern,回顾后发断言也不占用字符.
// * 例子: "(?<=\d)(匹配位置)(?=(\d{4})+$)" 仅匹配开头为数字,且长度为4n的纯数字结尾,零宽度断言并不占用字符(即不包含自身),当匹配位置为空时满足匹配且宽度为0.
// * (#正则注释) 仅作注释,会被匹配器忽略掉的文字
// * p.s. 以上的不占用字符,可理解为允许这样的匹配格式,但是已匹配的内容可能之后被重复匹配,且无法被Matcher.group()获取到,通常情况下适用于 替换文本/匹配固定'指令及参数'想获得参数值但是不想获得指令本身

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
