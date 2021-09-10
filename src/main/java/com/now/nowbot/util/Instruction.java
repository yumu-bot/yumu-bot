package com.now.nowbot.util;


/**
 * 所有的指令都写在这里方便进行管理
 * TODO 完善描述，help指令将返回这里的描述
 */
public enum Instruction {
    BIND("bind", "用于绑定", "^[!！]\\s?(?i)bind"),
    BPHT("bpht", "查询bp", "^[!！]\\s?(?i)bpht([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    CATPANEL("cpanel", "干嘛的？", "^[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?"),
    HELP("help", "获取帮助信息", "^[!！](?i)help"),
    PAGE("page", "这又是干嘛的？", "^[!！]page(\\s+(?<p1>\\w+))?(\\s?(?<p2>\\w+))?(\\s?(?<p3>\\w+))?"),
    PING("ping", "网络测试", "^[!！]\\s?(?i)ping"),
    PPM("ppm", "某种实力计算方法", "^[!！]\\s?(?i)ppm(?!v)([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPMVS("ppmvs", "PPM对比", "^[!！]\\s?(?i)ppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUS("ppp", "另一种pp计算方法", "^[!！]\\s*(?i)ppp(?![v])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    PPPLUSVS("ppvs", "ppp对比", "^[!！]\\s?(?i)p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),
    SETU("setu", "获取色图", "^[!！]((色图)|(涩图)|(setu))"),
    SONG("song", "获取谱面预览音频", "^[!！]\\s*(?i)song\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))"),
    START("start", "刷新积分", "^[!！]\\s*((积分)|(..积分))+"),
    KUMO("kumo", null, ".*");

    private final String regex;

    private final String name;

    private final String desc;

    Instruction(String name, String desc, String regex){
        this.regex = regex;
        this.name = name;
        this.desc = desc;
    }

    public String getRegex() {
        return regex;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
