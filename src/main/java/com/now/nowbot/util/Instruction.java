package com.now.nowbot.util;


import com.now.nowbot.service.MessageService.*;

import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 **/
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

public enum Instruction {

    // #0 调出帮助
    HELP(HelpService.class,
            Pattern.compile("^[!！](?i)(ym)?((help)|h)+(\\s*(?<module>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    // #1 BOT 内部指令
    PING(PingService.class,
            Pattern.compile("^[!！](?i)ymping")),

    BIND(BindService.class,
            Pattern.compile("^[!！](?i)((ymbind)|((ym)?bi)|((ym)?(?<un>un)bind))(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+))?")),

    BAN(BanService.class,
            Pattern.compile("[!！]\\s*(?i)(?<un>un)?ban\\s*(?<serv>\\w+)\\s*(?<gf>[gf])\\s*(?<qq>\\d+)?")),

    SWITCH(SwitchService.class,
            Pattern.compile("^[!！](?i)ymsw(itch)?(\\s+(?<p1>\\w+))?(\\s+(?<p2>\\w+))?(\\s+(?<p3>\\w+))?(\\s+(?<p4>\\w+))?")),

    // BOT 自己更新的功能，现在因为可以 SSH 远程更新，所以几乎不用了
    UPDATE(UpdateService.class,
            Pattern.compile("^&!update$")),
    // BOT 加群确认的功能
    GROUP(JoinGroupService.class,    Pattern.compile("(是|确认|(?i)y)|(否|取消|(?i)n)")),

    // #2 osu! 成绩指令
    SETMODE(SetModeService.class,
            Pattern.compile("^[!！]\\s*(?i)ymmode\\s*(?<mode>\\w+)")),

    YMP(YmpService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(?<isAll>[p,r])\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    SCORE(ScoreService.class,
            Pattern.compile("^[!！]\\s*((ymscore)|((ym)?s(?!core)))([:：](?<mode>[\\w\\d]+))?\\s?(?<bid>\\d+)")),

    // b ymb ymbp :0-3 name 1-100
    BP(BpShowService.class,
            Pattern.compile("^[!！]\\s*(?i)((ymbp)|((ym)?b(?!p)))\\s*([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)\\s*#)?\\s*(?<n>\\d+)(-(?<m>\\d+))?")),

    TODAYBP(TodayBpService.class,
            Pattern.compile("^[!！](?i)tbp(\\s*[:：](?<mode>[\\w\\d]+))?(\\s*#(?<day>\\w{0,3}))?")),

    BPHT(BphtService.class,
            Pattern.compile("^[!！](?i)ymbpht(?<info>-i)?([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    // #3 osu! 玩家指令

    // i ymi yminfo :0-3 name
    INFO(InfoService.class,
            Pattern.compile("^[!！]\\s*(?i)((yminfo)|((ym)?i(?!nfo)))\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    UUI(UUIService.class,
            Pattern.compile("^[!！]\\s*(?i)((uuinfo)|(uui))\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    FRIEND(FriendService.class,
            Pattern.compile("^[!！]\\s*(?i)ymf(\\s*(?<n>\\d+))?(\\s*[:-]\\s*(?<m>\\d+))?")),

    MUTUAL(MutualFriendService.class,
            Pattern.compile("[!！]\\s*(?i)(test)?mu\\s*(?<names>[0-9a-zA-Z\\[\\]\\-_ ,]*)?")),

    PPM(PPmService.class,
            Pattern.compile("^[!！](?i)(ym)?ppm(?<vs>vs)?([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

//    PPPLUS(PpPlusService.class,
//            Pattern.compile("^[!！](?i)(ym)?ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),
//
//    PPPLUSVS(PpPlusVsService.class,
//            Pattern.compile("^[!！](?i)(ym)?p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    // #4 osu! 谱面指令

    SONG(SongService.class,
            Pattern.compile("^[!！]\\s*(song)\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))")),

    // #5 osu! 比赛指令

    RATING(RatingService.class,
            Pattern.compile("^[!！]ymra\\s*(?<matchid>\\d+)(\\s+(?<skipedrounds>\\d+))?(\\s+(?<deletendrounds>\\d+))?(\\s+(?<includingfail>\\d))?")),

    MAPPOOL(MapPoolService.class,
            Pattern.compile("^[!！]map")),

//    START(StartService.class,
//            Pattern.compile("^[!！]((积分)|(..积分))+.*")),

    WIKI(WikiService.class,
            Pattern.compile("^[!！]\\s*(?i)ym((wiki)|w)(\\s+(?<key>\\s*))?")),

    TRANS(TransService.class,
            Pattern.compile("^[!！]\\s*(ym)?trans\\s?(?<a>[A-G#]{1,2})(?<b>\\w)")),

    OVERSR(OverSRService.class,
            Pattern.compile("^[!！](?i)(ym)?((oversr)|or)+(\\s+(?<SR>[0-9.]*))?")),

    CATPANEL(CatpanelService.class,
            Pattern.compile("[!！]\\s*(testbg)(\\s*(?<r>qc))?(\\s+(?<bk>\\d{1,3}))?(\\s*(?<yl>ylbx))?")),


    /*
    新建服务并指定@Service("aClass"),实现MessageService接口的HandleMessage,参数就从matcher.group("")来获取,,参数就是正则中(?<aClass>value)中的name,取值为value,当有'?'修饰时为@Nullable
     */
    TEST(TestService.class,
            Pattern.compile("^[!！]\\s*test.*")),

    TESTID(TestGetId.class,    Pattern.compile("^[!！]\\s*testid\\s*(?<ids>((\\d)+(,)?)+)")),

    TESTRA(TestRaService.class,    Pattern.compile("[!！]\\s*testra(\\s+(?<id>\\d+))")),

    TESTPPM(TestPPMService.class,    Pattern.compile("[!！]\\s*testppm(\\s*[:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))")),

    TESTINFO(InfoLegacyService.class,    Pattern.compile("[!！]\\s*(testinfo)([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))")),

    TESTMT(TestMt4.class,
            Pattern.compile("^[!！]testmt\\s*(?<data>[ox ]+)")),


    TESTMAP(TestMapServer.class,
            Pattern.compile("^[!！]testmap\\s*(?<d>\\d+)(\\s*(?<mode>[\\w\\d,]+))?")),


//    SETU(SetuService.class,
//            Pattern.compile("^[!！](?i)(?<code>(setu))|(ymse)|(ymsetu)")),
    ;



//    MPOB("ob", "<未上线> 场记板，可记录并通报某场正在进行的比赛！", null),
//    MPRT("rt", "<未上线> 木斗力 a.k.a MuRating 1.0，可查询比赛中选手们各自的评分！", null),

    //历史指令 存档一下
//    PPM("ppm",      Pattern.compile("^[!！](?i)(ym)?ppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<aClass>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ymppm[:mode][osu aClass] PPMinus-娱乐性实力算法，可查询不同人或不同模式（除了mania）。\n   比如：!ymppm:t muziyami"),
//    PPMVS("ppmvs",  Pattern.compile("^[!！](?i)(ym)?ppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<aClass>[0-9a-zA-Z\\[\\]\\-_ ]*))?(\\s*:\\s*(?<name2>[0-9a-zA-Z\\[\\]\\-_ ]+))?"), "!ymppmvs <osu aClass|@某人> PPM对比，需要自己绑定，如果是ppmvs也需要对方绑定"),
//    KUMO("kumo",    Pattern.compile("^none$"), null), 以后再开

    private final Pattern regex;

    private final Class aClass;

    Instruction(Class<? extends MessageService> aClass, Pattern regex){
        this.regex = regex;
        this.aClass = aClass;
    }

    public Pattern getRegex() {
        return regex;
    }

    public Class<? extends MessageService> getaClass() {
        return aClass;
    }

    public static void main(String[] args) {
        var p = Instruction.BIND.regex;
        var m = p.matcher("!bi  xx");
        if (m.matches()) {
            System.out.println("ok************");
            System.out.println(m.group("un"));
            System.out.println(m.group("name"));
            return;
        } else {
            return;
        }
//        var rp = valueOf("ban".toUpperCase());
//        var rm = rp.regex.matcher("!ban ymf f 22");
//        System.out.println(rm.find());
//        System.out.println(rm.group("gf"));

    }
}
