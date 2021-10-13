package com.now.nowbot.util;


import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 */
public enum Instruction {
    BIND("bind", "^[!！]\\s?(?i)ymbind$", "!ymbind 直接绑定"),
    BPHT("bpht", "^[!！]\\s?(?i)ymbpht([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?", "!ymbpht 查询bp"),
    CATPANEL("cpanel", "^[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?", null),
    HELP("help", "^[!！](?i)(ym)?((help)|h)", "!ymh/!ymhelp"),
    PAGE("page", "^[!！]page(\\s+(?<p1>\\w+))?(\\s?(?<p2>\\w+))?(\\s?(?<p3>\\w+))?", null),
    PING("ping",  null, "^[!！](?i)ymping"),
    PPM("ppm", "^[!！]\\s?(?i)(ym)?ppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?", "!ymppm[:mode][osu name] PPMinus-娱乐性实力算法，可查询不同人或不同模式（除了mania）。\n   比如：!ymppm:t muziyami"),
    PPMVS("ppmvs","^[!！]\\s?(?i)(ym)?ppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?(\\s*:\\s*(?<name2>[0-9a-zA-Z\\[\\]\\-_ ]+))?", "!ymppmvs <osu name|@某人> PPM对比，需要自己绑定，如果是ppmvs也需要对方绑定"),
    PPPLUS("ppp", "^[!！]\\s*(?i)(ym)?ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?", "!ppp [osu name] std的pp+计算"),
    PPPLUSVS("ppvs", "^[!！]\\s?(ym)?(?i)p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?", "!ppvs <osu name|@某人> pp+的对比，需要自己绑定，如果是ppvs也需要对方绑定"),
    SETU("setu", "^[!！](?i)(?<code>(setu))|(ymse)|(ymsetu)", "!ymsetu 获取一张随机图"),
    SONG("song", "^[!！]song\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))", "!song <bid>或!song sid=<sid> 试听谱面"),
    START("start", "^[!！]\\s*((积分)|(..积分))+.*", "!积分 刷新并查看积分"),
    KUMO("kumo", ".*", null),
    YMP("ymp", "^[!！](?i)ym(?<isAll>[p,r])([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?","!ymr/!ymp 简略地查询自己的游戏成绩"),
    YMI("ymi", "^[!！](?i)ymi(nfo)?([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?","!ymi 简略地查询自己的游戏信息"),
    WIKI("wiki", "^[!！](?i)ym((wiki)|w)(\\s+(?<key>[^\\s]*))?","!ymwiki 百科，告诉你小沐知道的一切。"),
    TRANS("trans", "^[!！]ymtrans\\s?(?<a>[A-G#]{1,2})(?<b>\\w)",null),
    SCORE("score", "^[!！]score\\s?(?<bid>\\d+)",null),

    //TODO 待实现的指令
    /*********************   下面可能会合并为一个功能   **************************/
//    MPOB("ob", "<未上线> 场记板，可记录并通报某场正在进行的比赛！", null),
//    MPRT("rt", "<未上线> 木斗力 a.k.a MuRating 1.0，可查询比赛中选手们各自的评分！", null),
    /************************************************************************/
    TEST("test",null,"^[!！]test.*");

    private final Pattern regex;

    private final String name;

    private final String desc;

    Instruction(String name, String regex, String desc){
        this.regex = Pattern.compile(regex);
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
