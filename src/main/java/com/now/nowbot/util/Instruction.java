package com.now.nowbot.util;


import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 * TODO 完善描述，help指令将返回这里的描述
 */
public enum Instruction {
    BIND("bind", "!ymbind 直接绑定", "^[!！]\\s?(?i)ymbind$"),
    BPHT("bpht", "!ymbpht 查询bp", "^[!！]\\s?(?i)ymbpht([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    CATPANEL("cpanel", null, "^[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?"),
    HELP("help", "!ymh/!ymhelp", "^[!！](?i)ym((help)|h)"),
    PAGE("page", null, "^[!！]page(\\s+(?<p1>\\w+))?(\\s?(?<p2>\\w+))?(\\s?(?<p3>\\w+))?"),
    PING("ping",  null, "^[!！]\\s?(?i)ymping"),
    PPM("ppm", "!ymppm[:mode][osu name] 某种娱乐性实力计算方法,mode及name可选\n   比如：!ymppm:t muziyami", "^[!！]\\s?(?i)ymppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPMVS("ppmvs", "!ymppmvs <osu name|@某人> PPM对比，需要自己绑定，如果是ppmvs也需要对方绑定", "^[!！]\\s?(?i)ymppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUS("ppp", "!(ym)?ppp [osu name] std的pp+计算", "^[!！]\\s*(?i)ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUSVS("ppvs", "!(ym)?ppvs <osu name|@某人> pp+的对比.需要自己绑定,如果是对比@对象也需要被对比人绑定", "^[!！]\\s?(?i)p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    SETU("setu", "!setu 获取一张随机美图", "^[!！]((色图)|(涩图)|(setu))"),
    SONG("song", "!song <bid>或!song sid=<sid> 试听谱面", "^[!！]song\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))"),
    START("start", "!积分 刷新并查看积分", "^[!！]\\s*((积分)|(..积分))+"),
    KUMO("kumo", null, ".*"),
    YMP("ymp","!ymr/!ymp 简略地查询自己的成绩","^[!！](?i)ym(?<isAll>[p,r])([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    YMI("ymi","!ymi 简略地查询自己的信息","^[!！](?i)ym((info)|i)([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    WIKI("wiki","!ymwiki 告诉你我知道的一切","^[!！](?i)ym((wiki)|w)((?<key>[^\\s]*))?"),
    TEST("test",null,".*"),
    TRANS("trans",null,"^[!！]ymtrans\\s?(?<a>[A-G#]{1,2})(?<b>\\w)");

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
