package com.now.nowbot.util;


import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 * TODO 完善描述，help指令将返回这里的描述
 */
public enum Instruction {
    BIND("bind", "!ymbind 用于绑定,不需要带上用户名", "^[!！]\\s?(?i)ymbind$"),
    BPHT("bpht", "!ymbpht 查询bp", "^[!！]\\s?(?i)ymbpht([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    CATPANEL("cpanel", null, "^[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?"),
    HELP("help", null, "^[!！](?i)ymhelp$"),
    PAGE("page", null, "^[!！]page(\\s+(?<p1>\\w+))?(\\s?(?<p2>\\w+))?(\\s?(?<p3>\\w+))?"),
    PING("ping",  null, "^[!！]\\s?(?i)ymping"),
    PPM("ppm", "!ymppm[:mode][osu name] 某种实力计算方法,mode及name可选\n   实例!ymppm:t muziyami", "^[!！]\\s?(?i)ymppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPMVS("ppmvs", "!ymppmvs <osu name|@某人> PPM对比,需要自己绑定,如果是对比@对象也需要被对比人绑定", "^[!！]\\s?(?i)ymppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUS("ppp", "!(ym)?ppp [osu name] std的pp+计算", "^[!！]\\s*(?i)ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUSVS("ppvs", "!(ym)?ppvs <osu name|@某人> pp+的对比.需要自己绑定,如果是对比@对象也需要被对比人绑定", "^[!！]\\s?(?i)p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    SETU("setu", "!setu 获取色图", "^[!！]((色图)|(涩图)|(setu))"),
    SONG("song", "!song <bid>或!song sid=<sid> 获取谱面预览音频", "^[!！]song\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))"),
    START("start", "!积分 刷新并查看积分", "^[!！]\\s*((积分)|(..积分))+"),
    KUMO("kumo", null, ".*"),
    YMP("ymp",null,"^[!！](?i)ym(?<isAll>[p,r])([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    WIKI("wiki",null,"^[!！](?i)ym((wiki)|w)((?<key>[^\\s]*))?"),
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
