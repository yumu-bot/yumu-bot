package com.now.nowbot.util;


import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 */
public enum Instruction {
    BIND("bind", "!ymbind 直接绑定", "^[!！]\\s?(?i)ymbind$"),
    BPHT("bpht", "!ymbpht 查询bp", "^[!！]\\s?(?i)ymbpht([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    CATPANEL("cpanel", null, "^[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?"),
    HELP("help", "!ymh/!ymhelp", "^[!！](?i)(ym)?((help)|h)"),
    PAGE("page", null, "^[!！]page(\\s+(?<p1>\\w+))?(\\s?(?<p2>\\w+))?(\\s?(?<p3>\\w+))?"),
//    MPRT("rt", "<未上线> 木斗力 a.k.a MuRating 1.0，可查询比赛中选手们各自的评分！", null),
//    MPOB("ob", "<未上线> 场记板，可记录并通报某场正在进行的比赛！", null),
    PING("ping",  null, "^[!！]\\s?(?i)ymping"),
    PPM("ppm", "!ymppm[:mode][osu name] PPMinus-娱乐性实力算法，可查询不同人或不同模式（除了mania）。\n   比如：!ymppm:t muziyami", "^[!！]\\s?(?i)ymppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPMVS("ppmvs", "!ymppmvs <osu name|@某人> PPM对比，需要自己绑定，如果是ppmvs也需要对方绑定", "^[!！]\\s?(?i)ymppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUS("ppp", "!ppp [osu name] std的pp+计算", "^[!！]\\s*(?i)(ym)?ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUSVS("ppvs", "!ppvs <osu name|@某人> pp+的对比，需要自己绑定，如果是ppvs也需要对方绑定", "^[!！]\\s?(ym)?(?i)p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    SETU("setu", "!ymsetu 获取一张随机图", "^[!！](?i)(?<code>(setu))|(ymse)|(ymsetu)"),
    SONG("song", "!song <bid>或!song sid=<sid> 试听谱面", "^[!！]song\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))"),
    START("start", "!积分 刷新并查看积分", "^[!！]\\s*((积分)|(..积分))+.*"),
    KUMO("kumo", null, ".*"),
    YMP("ymp","!ymr/!ymp 简略地查询自己的游戏成绩","^[!！](?i)ym(?<isAll>[p,r])([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    YMI("ymi","!ymi 简略地查询自己的游戏信息","^[!！](?i)ymi(nfo)?([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    WIKI("wiki","!ymwiki 百科，告诉你小沐知道的一切。","^[!！](?i)ym((wiki)|w)(\\s+(?<key>[^\\s]*))?"),
    TEST("test",null,"^[!！]test.*"),
    TRANS("trans",null,"^[!！]ymtrans\\s?(?<a>[A-G#]{1,2})(?<b>\\w)"),
    SCORE("score",null,"^[!！]score\\s?(?<bid>\\d+)");

    private final Pattern regex;

    private final String name;

    private final String desc;

    Instruction(String name, String desc, String regex){
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
