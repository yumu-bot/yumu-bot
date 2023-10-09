package com.now.nowbot.util;


import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.MessageServiceImpl.*;

import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 **/

public enum Instruction {

    // #0 调出帮助
    HELP(HelpService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(help|h)+(\\s*(?<module>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    // #1 BOT 内部指令
    PING(PingService.class,
            Pattern.compile("^[!！]\\s*(?i)((ym)?(ping|pi(?!\\w))+|yumu)")),

    BIND(BindService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(bi(?!nd)|((ym)|(?<un>(un)))bind)(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+))?")),


    SWITCH(SwitchService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(switch|sw(?!\\w))+(\\s+(?<p1>\\w+))?(\\s+(?<p2>\\w+))?(\\s+(?<p3>\\w+))?(\\s+(?<p4>\\w+))?")),


    // #2 osu! 成绩指令
    SETMODE(SetModeService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(setmode|mode|sm(?![a-zA-Z_]))+\\s*(?<mode>\\w+)")),

    SCOREPR(PassRecentService.class,
            Pattern.compile("^[!！]\\s*(?i)((ym)?(?<pass>(pass|p(?![a-zA-Z_])))|(ym)?(?<recent>(recent|r(?!\\w))))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?))?\\s*(#?(?<n>\\d+)([-－](?<m>\\d+))?)?$")),
            //Pattern.compile("^[!！]\\s*(?i)(ym)?((?<pass>(pass|p(?![a-zA-Z_])))|(?<recent>(recent|r(?!\\w))))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    UUPR(UUPRService.class,
            Pattern.compile("^[!！]\\s*(?i)(uu(?<pass>(pass|p(?![a-zA-Z_])))|uu(?<recent>(recent|r(?!\\w))))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?))?\\s*(#?(?<n>\\d+))?$")),

    SCORE(ScoreService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(score|s(?![a-zA-Z_]))+\\s*([:：](?<mode>[\\w\\d]+))?\\s*(?<bid>\\d+)\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?\\s*(\\+(?<mod>( ?[EZNMFHTDRSPCLO]{2})+))?")),
            //Pattern.compile("^[!！]\s*(?i)(ym)?(score|s(?![a-zA-Z_]))+\s*([:：](?<mode>[\w\d]+))?\s*(?<bid>\d+)\s*(\+(?<mod>( ?[EZNMFHTDRSPCLO]{2})+))?")),

    // b ymb ymbp :0-3 name 1-100
    BP(BPService.class,
            null),
    TODAYBP(TodayBPService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(todaybp|(tbp|tdp|t(?![a-zA-Z_])))+\\s*([:：](?<mode>[\\w\\d]+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?(?!#))?\\s*(#?\\s*(?<day>\\d*)\\s*)$")),

    BPA(BPAnalysisService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?((bpanalysis)|(blue\\s*archive)|bpa(?![a-zA-Z_])|ba(?![a-zA-Z_]))+(\\s*[:：](?<mode>\\w+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    BPHT(BphtService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(bpht|ht(?![a-zA-Z_]))+(?<info>-i)?(\\s*[:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    // #3 osu! 玩家指令

    // i ymi yminfo :0-3 name
    INFO(InfoService.class,
            Pattern.compile("^[!！]\\s*(?i)((ym)?information|yminfo(?![a-zA-Z_])|(ym)?i(?![a-zA-Z_]))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?")),

    UUINFO(UUIService.class,
            Pattern.compile("^[!！]\\s*(?i)uu(info|i(?![a-zA-Z_]))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?")),
    IM(IMapperService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?((im?)?mapper|im(?![a-zA-Z_]))+(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),


    FRIEND(FriendService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(friend(s)?|f(?![a-zA-Z_]))+\\s*(?<n>\\d+)?(\\s*[:-]\\s*(?<m>\\d+))?")),

    MUTUAL(MutualFriendService.class,
            Pattern.compile("[!！]\\s*(?i)(test)?mu\\s*(?<names>[0-9a-zA-Z\\[\\]\\-_ ,]*)?")),

    PPM(PPMinusService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(ppminus|(p?(pm))(?![a-rt-uw-zA-RT-UW-Z_]))\\s*(?<vs>vs)?\\s*([:：](?<mode>[\\w\\d]+))?(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),


    // #4 osu! 谱面指令

    QUAMAP(QualifiedMapService.class,
            Pattern.compile("[!！]\\s*(?i)(ym)?(qualified|qua(?![a-zA-Z_])|q(?![a-zA-Z_]))+\\s*([:：](?<mode>\\w+))?\\s*(#+(?<status>[-\\w]+))?\\s*(\\*?(?<sort>[-_+a-zA-Z]+))?\\s*(?<range>\\d+)?")),

    LEADER(LeaderBoardService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(mapscorelist|leaderboard|leader(?![a-zA-Z_])|list(?![a-zA-Z_])|l(?![a-zA-Z_]))+\\s*([:：](?<mode>\\w+))?\\s*(?<bid>\\d+)?\\s*(?<range>\\d+)?")),

    MM(MapMinusService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(mapminus|mm(?![a-zA-Z_]))+\\s*(?<id>\\d+)?")),

    KITA(KitaService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(kita|k(?![a-wy-zA-WY-Z_]))+(?<noBG>([xX](?![a-zA-Z_])))?\\s*(?<bid>\\d+)?\\s*(?<mod>\\w+)?\\s*(?<round>[\\w\\s]+)?")),

    // #5 osu! 比赛指令


    MRA(MRAService.class,
            Pattern.compile("^[!！]\\s*(?i)((ym)?rating|(ym)?ra(?![a-zA-Z_])|mra(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)(\\s*(?<skipedrounds>\\d+))?(\\s*(?<deletendrounds>\\d+))?(\\s*(?<excludingrematch>[Rr]))?(\\s*(?<excludingfail>[Ff]))?")),

    URA(URAService.class,
            Pattern.compile("^[!！]\\s*(?i)(u{1,2})(rating|ra(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)(\\s*(?<skipedrounds>\\d+))?(\\s*(?<deletendrounds>\\d+))?(\\s*(?<excludingrematch>[Rr]))?(\\s*(?<excludingfail>[Ff]))?")),

    CRA(CRAService.class,
            Pattern.compile("[!！]\\s*(?i)((ym)?(csvrating|cr(?![a-wy-zA-WY-Z_])|cra(?![a-wy-zA-WY-Z_])))+\\s*(?<x>[xX])?\\s*(?<id>\\d+)?")),

    MONOW(MonitorNowService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(monitornow|monow|mn(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)(\\s*(?<skipedrounds>\\d+))?(\\s*(?<deletendrounds>\\d+))?(\\s*(?<excludingrematch>[Rr]))?(\\s*(?<excludingfail>[Ff]))?")),

    MINI(MiniPanelService.class,
            Pattern.compile("^[!！](?i)\\s*((ym)?)((?<ymx>x(?!\\w))|(?<ymy>y(?!\\w)))+")),

    MAPPOOL(MapPoolService.class,
            Pattern.compile("^[!！]\\s*(?i)map")),

//    START(StartService.class,
//            Pattern.compile("^[!！]((积分)|(..积分))+.*")),

    WIKI(WikiService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?((wiki)|w(?![a-zA-Z_]))+(\\s*(?<key>\\s*))?")),

    TRANS(TransService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(trans)+\\s(?<a>[A-G#]{1,2})(?<b>\\w)")),

    OVERSR(OverSRService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?((oversr)|or(?![a-zA-Z_]))+(\\s+(?<SR>[0-9.]*))?")),

    SQL(SqlService.class,
            Pattern.compile("^[!！]sql\\s")),

    DRAW(DrawService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(draw|d(?!raw))+(\\s+(?<d>\\d+))?")),

    COUNTMSGLEGACY(CountQQMessageService.class,
            Pattern.compile("^#统计(?<d>(新人)|(进阶)|(高阶))群管理$")),

    COUNTMSG(CountQQMessageService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?((cm(?![a-zA-Z_]))|(countmessage)|(countmsg))+\\s*(?<d>(n)|(a)|(h))")),

    SETU(SetuService.class,
            Pattern.compile("^[!！]\\s*(?i)(ym)?(setu|se(?![a-zA-Z_]))+(\\s+(?<source>\\d+))?")),
    /*
    新建服务并指定@Service("aClass"),实现MessageService接口的HandleMessage,参数就从matcher.group("")来获取,,参数就是正则中(?<aClass>value)中的name,取值为value,当有'?'修饰时为@Nullable
     */
    TEST(TestService.class,
            Pattern.compile("^[!！]\\s*test.*")),

    TESTID(TestGetId.class,
            Pattern.compile("^[!！]\\s*(?i)testid\\s*(?<ids>((\\d)+(,)?)+)")),

    TESTPPM(TestPPMService.class,
            Pattern.compile("[!！]\\s*(?i)testppm(\\s*[:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))")),


    TESTTAIKOSTARCALCULATE(TestMt4.class,
            Pattern.compile("^[!！]\\s*(?i)testmt\\s*(?<data>[ox ]+)")),

    TESTMAP(TestMapServer.class,
            Pattern.compile("^[!！]\\s*(?i)testmap\\s*(?<d>\\d+)(\\s*(?<mode>[\\w\\d,]+))?")),

    TESTCOUNTMSG(CountQQMessageService.class,
            Pattern.compile("^#test统计(?<d>\\d+) (?<d1>\\d+)")),

    // 临时添加, 随便搞一个不可能触发的正则
    BAN(BanService.class,
            null),
    AUDIO(AudioService.class,
            null),
    ;


//    MPOB("ob", "<未上线> 场记板，可记录并通报某场正在进行的比赛！", null),
//    MPRT("rt", "<未上线> 木斗力 a.k.a MuRating 1.0，可查询比赛中选手们各自的评分！", null),

    //历史指令 存档一下
//    PPM0("ppm",      Pattern.compile("^[!！](?i)(ym)?ppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<aClass>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ymppm[:mode][osu aClass] PPM-娱乐性实力算法，可查询不同人或不同模式（除了mania）。\n   比如：!ymppm:t muziyami"),
//    PPMVS("ppmvs",  Pattern.compile("^[!！](?i)(ym)?ppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<aClass>[0-9a-zA-Z\\[\\]\\-_ ]*))?(\\s*:\\s*(?<name2>[0-9a-zA-Z\\[\\]\\-_ ]+))?"), "!ymppmvs <osu aClass|@某人> PPM对比，需要自己绑定，如果是ppmvs也需要对方绑定"),
//    KUMO("kumo",    Pattern.compile("^none$"), null), 以后再开

    private final Pattern regex;

    private final Class aClass;

    Instruction(Class<? extends MessageService> aClass, Pattern regex) {
        this.regex = regex;
        this.aClass = aClass;
    }

    public Pattern getRegex() {
        return regex;
    }

    public Class<? extends MessageService> getaClass() {
        return aClass;
    }
}
