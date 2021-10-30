package com.now.nowbot.util;


import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 *
 * 进阶正则 以下为java专用,其他编程语言未作尝试
 * (?:pattern) 匹配 pattern 但不捕获该匹配的子表达式,即它是一个非捕获匹配,不存储供以后使用的匹配.
 * 例子: "industr(?:y|ies)" 是比 "industry|industries" 更经济的表达式.
 * (?!pattern) 执行反向预测先行搜索的子表达式,该表达式匹配不处于匹配 pattern 的字符串的起始点的搜索字符串,不占用字符,即发生匹配后,下一匹配的搜索紧随上一匹配之后.
 * (?<!pattern)执行正向预测先行搜索的子表达式,该表达式与上条相反,同时也不占用字符
 * 例子: "(?<!ab)cd(?!ef)" 仅匹配非ab开头的,ef结尾的cd,且ab与ef仅作搜索用并不占用宽度,即例子对gcd中的cd也会被匹配
 * (?=pattern) 也叫零宽度正预测先行断言,它断言被匹配的字符串以表达式pattern结尾但除了结尾以外的部分,预测先行不占用字符,即发生匹配后,下一匹配的搜索紧随上一匹配之后.
 * (?<=pattern)也叫零宽度正回顾后发断言，它断言自身出现的位置的前面能匹配表达式pattern,回顾后发断言也不占用字符.
 * 例子: "(?<=\d)(匹配位置)(?=(\d{4})+$)" 仅匹配开头为数字,且长度为4n的纯数字结尾,零宽度断言并不占用字符(即不包含自身),当匹配位置为空时满足匹配且宽度为0.
 * (#正则注释) 仅作注释,会被匹配器忽略掉的文字
 * p.s. 以上的不占用字符,可理解为允许这样的匹配格式,但是已匹配的内容可能之后被重复匹配,且无法被Matcher.group()获取到,通常情况下适用于 替换文本/匹配固定'指令及参数'想获得参数值但是不想获得指令本身
 */
public enum Instruction {
    BIND("bind",    Pattern.compile("^[!！](?i)ymbind$"), "!ymbind 直接绑定"),
    BPHT("bpht",    Pattern.compile("^[!！](?i)ymbpht([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ymbpht 查询bp"),
    HELP("help",    Pattern.compile("^[!！](?i)(ym)?((help)|h)"), "!ymh/!ymhelp"),
    SWITCH("switch",Pattern.compile("^[!！](?i)switch(\\s+(?<p1>\\w+))?(\\s?(?<p2>\\w+))?(\\s?(?<p3>\\w+))?(\\s?(?<p4>\\w+))?"), null),
    PING("ping",    Pattern.compile("^[!！](?i)ymping"), null),
    PPM("ppm",      Pattern.compile("^[!！](?i)(ym)?ppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ymppm[:mode][osu name] PPMinus-娱乐性实力算法，可查询不同人或不同模式（除了mania）。\n   比如：!ymppm:t muziyami"),
    PPMVS("ppmvs",  Pattern.compile("^[!！](?i)(ym)?ppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?(\\s*:\\s*(?<name2>[0-9a-zA-Z\\[\\]\\-_ ]+))?"), "!ymppmvs <osu name|@某人> PPM对比，需要自己绑定，如果是ppmvs也需要对方绑定"),
    PPPLUS("ppp",   Pattern.compile("^[!！](?i)(ym)?ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ppp [osu name] std的pp+计算"),
    PPPLUSVS("ppvs",Pattern.compile("^[!！](?i)(ym)?p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ppvs <osu name|@某人> pp+的对比，需要自己绑定，如果是ppvs也需要对方绑定"),
    SETU("setu",    Pattern.compile("^[!！](?i)(?<code>(setu))|(ymse)|(ymsetu)"), "!ymsetu 获取一张随机图"),
    SONG("song",    Pattern.compile("^[!！]song\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))"), "!song <bid>或!song sid=<sid> 试听谱面"),
    START("start",  Pattern.compile("^[!！]((积分)|(..积分))+.*"), "!积分 刷新并查看积分"),
    KUMO("kumo",    Pattern.compile("^none$"), null),
    YMP("ymp",      Pattern.compile("^[!！](?i)ym(?<isAll>[p,r])([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"!ymr/!ymp 简略地查询自己的游戏成绩"),
    YMI("ymi",      Pattern.compile("^[!！](?i)ymi(nfo)?([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"!ymi 简略地查询自己的游戏信息"),
    WIKI("wiki",    Pattern.compile("^[!！](?i)ym((wiki)|w)(\\s+(?<key>[^\\s]*))?"),"!ymwiki 百科，告诉你小沐知道的一切。"),
    TRANS("trans",  Pattern.compile("^[!！](ym)?trans\\s?(?<a>[A-G#]{1,2})(?<b>\\w)"),null),
    SCORE("score",  Pattern.compile("^[!！]score\\s?(?<bid>\\d+)"),null),
    /*
    新建服务并指定@Service("name"),实现MessageService接口的HandleMessage,参数就从matcher.group("")来获取,,参数就是正则中(?<name>value)中的name,取值为value,当有'?'修饰时为@Nullable
     */
    TEST("test",    Pattern.compile("^[!！].*"),null),
    TESTPPM("testppm",    Pattern.compile("^[!！](?i)test(ym)?ppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),null),


    //TODO 待实现的指令
//    RATING("rating", Pattern.compile("^[!！]ymra\\s?(?<matchid>\\d+)(\\s+(?<numberofround>\\d+))?(\\s*:\\s*(?<includingfail>\\d+))?(\\s*(?<numberofwarmup>\\d+))?"),null);
    RATING("rating", Pattern.compile("^[!！]ymra\\s*(?<matchid>\\d+)(\\s+(?<skipedrounds>\\d+))?(\\s+(?<includingfail>\\d))?"),null);
    /*********************   下面可能会合并为一个功能   **************************/
//    MPOB("ob", "<未上线> 场记板，可记录并通报某场正在进行的比赛！", null),
//    MPRT("rt", "<未上线> 木斗力 a.k.a MuRating 1.0，可查询比赛中选手们各自的评分！", null),
    /************************************************************************/
    //历史指令 存档一下
//    CATPANEL("cpanel", Pattern.compile("^[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?"), null),

    private final Pattern regex;

    private final String name;

    private final String desc;

    Instruction(String name, Pattern regex, String desc){
        this.regex = regex;
        this.name = name;
        this.desc = desc;
    }

    public Pattern getRegex() {
        return regex;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
