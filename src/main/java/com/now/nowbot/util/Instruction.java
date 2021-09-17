package com.now.nowbot.util;


import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 * TODO 完善描述，help指令将返回这里的描述
 */
public enum Instruction {
    BIND("bind", "用于绑定", "^[!！]\\s?(?i)ymbind$"),
    BPHT("bpht", "查询bp", "^[!！]\\s?(?i)ymbpht([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    CATPANEL("cpanel", null, "^[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?"),
    HELP("help", null, "^[!！](?i)ymhelp$"),
    PAGE("page", null, "^[!！]page(\\s+(?<p1>\\w+))?(\\s?(?<p2>\\w+))?(\\s?(?<p3>\\w+))?"),
    PING("ping", "网络测试", "^[!！]\\s?(?i)ymping"),
    PPM("ppm", "某种实力计算方法", "^[!！]\\s?(?i)ymppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPMVS("ppmvs", "PPM对比", "^[!！]\\s?(?i)ymppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUS("ppp", "另一种pp计算方法", "^[!！]\\s*(?i)ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUSVS("ppvs", "ppp对比", "^[!！]\\s?(?i)p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    SETU("setu", "获取色图", "^[!！]((色图)|(涩图)|(setu))"),
    SONG("song", "获取谱面预览音频", "^[!！]\\s*csong\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))"),
    START("start", "刷新积分", "^[!！]\\s*((积分)|(..积分))+"),
    KUMO("kumo", null, ".*"),
    YMP("ymp",null,"^[!！](?i)ym(?<isAll>[p,r])([:：](?<mode>[\\w\\d]+))?(?![pP])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    WIKI("wiki",null,"^[!！](?i)ym((wiki)|w)(\\s+(?<key>[^\\s]*))?");

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
