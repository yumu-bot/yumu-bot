package com.now.nowbot.util;

import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 **/
public class Pattern4ServiceImpl {

    // #0 调出帮助
    public static final Pattern HELP = Pattern.compile("^[!！]\\s*(?i)(ym)?(help|h)+(\\s*(?<module>[0-9a-zA-Z\\[\\]\\-_ ]*))?");
    public static final Pattern AUDIO = Pattern.compile("^[!！]\\s*(?i)(ym)?(song|audio|a(?!\\w))+\\s*([:：](?<type>[\\w\\d]+))?\\s*(?<id>\\d+)?");

    // #1 BOT 内部指令
    public static final Pattern PING = Pattern.compile("^[!！]\\s*(?i)((ym)?(ping|pi(?!\\w))|yumu\\?)");
    public static final Pattern BIND = Pattern.compile("^[!！]\\s*(?i)(ym)?(bi(?!nd)|((ym)|(?<un>(un)))bind)\\s*(qq=(?<qq>\\d+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+)?");
    public static final Pattern BAN = Pattern.compile("^[!！]\\s*(?i)(ym)?(super|sp(?!\\w)|operate|op(?!\\w))\\s*(?<operate>(black)?list|add|remove|(un)?ban|)?\\s*(qq=(?<qq>\\d+))\\s*(group=(?<group>\\d+))\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+)?");
    public static final Pattern SWITCH = Pattern.compile("^[!！]\\s*(?i)(ym)?(switch|sw(?!\\w))+(\\s+(?<p1>[\\w\\-]+))?(\\s+(?<p2>\\w+))?(\\s+(?<p3>\\w+))?(\\s+(?<p4>\\w+))?");
    public static final Pattern ECHO = Pattern.compile("^[!！#]\\s*(?i)echo\\s*(?<any>.*)");

    // #2 osu! 成绩指令
    public static final Pattern SETMODE = Pattern.compile("^[!！]\\s*(?i)(ym)?(setmode|mode|sm(?!\\w)|mo(?!\\w))+\\s*(?<mode>\\w+)");

    public static final Pattern SCOREPR = Pattern.compile("^[!！]\\s*(?i)(?<pass>(ym)?(pass(?![sS])(?<es>es)?|p(?![a-rt-zA-RT-Z_]))|(ym)?(?<recent>(recent|r(?![a-rt-zA-RT-Z_]))))(?<s>s)?\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?)?\\s*(#?(?<n>\\d+)([-－](?<m>\\d+))?)?$");

    public static final Pattern UUPR = Pattern.compile("^[!！]\\s*(?i)(uu(?<pass>(pass|p(?!\\w)))|uu(?<recent>(recent|r(?!\\w))))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?))?\\s*(#?(?<n>\\d+))?$");

    public static final Pattern SCORE = Pattern.compile("^[!！]\\s*(?i)(?<score>(ym)?(score|s(?!\\w)))\\s*([:：](?<mode>[\\w\\d]+))?\\s*(?<bid>\\d+)\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?\\s*(\\+(?<mod>( ?[EZNMFHTDRSPCLO]{2})+))?");

    // b ymb ymbp :0-3 name 1-100
    public static final Pattern BP = Pattern.compile("^[!！]\\s*(?i)(?<bp>(ym)?(bestperformance|best|bp(?![a-rt-zA-RT-Z_])|b(?![a-rt-zA-RT-Z_])))(?<s>s)?\\s*([:：](?<mode>\\w+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?)?\\s*(#?(?<n>\\d+)([-－](?<m>\\d+))?)?$");
    public static final Pattern TODAYBP = Pattern.compile("^[!！]\\s*(?i)(ym)?(todaybp|(tbp|tdp|t(?!\\w)))+\\s*([:：](?<mode>[\\w\\d]+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?(?!#))?\\s*(#?\\s*(?<day>\\d*)\\s*)$");

    public static final Pattern BPANALYSIS = Pattern.compile("^[!！]\\s*(?i)(ym)?((bpanalysis)|(blue\\s*archive)|bpa(?!\\w)|ba(?!\\w))+(\\s*[:：](?<mode>\\w+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");


    public static final Pattern UUBA = Pattern.compile("^[!！]\\s*(?i)(uubpanalysis|u(u)?(ba|bpa))(?<info>(-?i))?(\\s*[:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]{3,}))?");
    public static final Pattern BPHT = Pattern.compile("^[!！]\\s*(?i)(ym)?(?<bpht>(bpht))[\\w-]*");
    
    // #3 osu! 玩家指令

    // i ymi yminfo :0-3 name
    public static final Pattern INFO = Pattern.compile("^[!！]\\s*(?i)(ym)?(information|info(?!\\w)|i(?!\\w))\\s*([:：](?<mode>[\\w\\d]+))?\\*(qq=(?<qq>\\d+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");

    public static final Pattern UUINFO = Pattern.compile("^[!！]\\s*(?i)uu(info|i(?!\\w))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");

    public static final Pattern IMAPPER = Pattern.compile("^[!！]\\s*(?i)(ym)?((im?)?mapper|im(?!\\w))+(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");


    public static final Pattern FRIEND = Pattern.compile("^[!！]\\s*(?i)(ym)?(friend(s)?|f(?!\\S))\\s*(?<n>\\d+)?\\s*([:-]\\s*(?<m>\\d+))?");

    public static final Pattern MUTUAL = Pattern.compile("[!！]\\s*(?i)(test)?mu\\s*(?<names>[0-9a-zA-Z\\[\\]\\-_ ,]*)?");

    public static final Pattern PPMINUS = Pattern.compile("^[!！]\\s*(?i)(ym)?(ppminus|(p?(pm))(?![a-rt-uw-zA-RT-UW-Z_]))\\s*(?<vs>vs)?\\s*([:：](?<mode>[\\w\\d]+))?(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");


    // #4 osu! 谱面指令

    public static final Pattern QUALIFIEDMAP = Pattern.compile("[!！]\\s*(?i)(ym)?(qualified|qua(?!\\w)|q(?!\\w))+\\s*([:：](?<mode>\\w+))?\\s*(#+(?<status>[-\\w]+))?\\s*(\\*?(?<sort>[-_+a-zA-Z]+))?\\s*(?<range>\\d+)?");

    public static final Pattern LEADERBOARD = Pattern.compile("^[!！]\\s*(?i)(ym)?(mapscorelist|leaderboard|leader(?!\\w)|list(?!\\w)|l(?!\\w))+\\s*([:：](?<mode>\\w+))?\\s*(?<bid>\\d+)?\\s*(?<range>\\d+)?");

    public static final Pattern MAPMINUS = Pattern.compile("^[!！]\\s*(?i)(ym)?(mapminus|mm(?!\\w))+\\s*(?<id>\\d+)?");

    public static final Pattern KITA = Pattern.compile("^[!！]\\s*(?i)(ym)?(kita|k(?![a-wy-zA-WY-Z_]))+(?<noBG>([xX](?!\\w)))?\\s*(?<bid>\\d+)?\\s*(?<mod>\\w+)?\\s*(?<round>[\\w\\s]+)?");

    // #5 osu! 比赛指令

    public static final Pattern MURATING = Pattern.compile("^[!！]\\s*(?i)((?<uu>(u{1,2})(rating|ra(?!\\w)))|(?<main>((ym)?rating|(ym)?ra(?!\\w)|mra(?!\\w))))\\s*(?<matchid>\\d+)?(\\s*(?<skip>-?\\d+))?(\\s*(?<skipend>-?\\d+))?(\\s*(?<rematch>[Rr]))?(\\s*(?<keep>[Ff]))?");

    /*
    public static final Pattern URA(URAService.class,
            Pattern.compile("^[!！]\\s*(?i)(u{1,2})(rating|ra(?!\\w))+\\s*(?<matchid>\\d+)(\\s*(?<skipedrounds>\\d+))?(\\s*(?<deletendrounds>\\d+))?(\\s*(?<excludingrematch>[Rr]))?(\\s*(?<excludingfail>[Ff]))?");

     */

    public static final Pattern SERIES = Pattern.compile("^[!！]\\s*(?i)((?<uu>(u{1,2})(seriesrating|series|sra(?!\\w)|sa(?!\\w)))|(ym)?(?<main>(seriesrating|series|sa(?!\\w)|sra(?!\\w)))|(ym)?(?<csv>(csvseriesrating|csvseries|csa(?!\\w)|cs(?!\\w))))\\s*(#(?<name>.+)#)?\\s*(?<data>[\\d\\s]+)?(\\s*(?<rematch>[Rr]))?(\\s*(?<keep>[Ff]))?");

    public static final Pattern CSVRATING = Pattern.compile("[!！]\\s*(?i)((ym)?(csvrating|cr(?![a-wy-zA-WY-Z_])|cra(?![a-wy-zA-WY-Z_])))+\\s*(?<x>[xX])?\\s*(?<id>\\d+)?");


    public static final Pattern ROUND = Pattern.compile("^[!！]\\s*(?i)(ym)?(matchround(s)?|round(s)?(?![a-zA-Z_])|mr(?![a-zA-Z_])|ro(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)?\\s*(?<round>\\d+)?(\\s*(?<keyword>[\\w\\s\\d-_ %*()/|]+))?");

    public static final Pattern MONITORNOW = Pattern.compile("^[!！]\\s*(?i)(ym)?(monitornow|monow|mn(?!\\w))+\\s*(?<matchid>\\d+)(\\s*(?<skip>\\d+))?(\\s*(?<skipend>\\d+))?(\\s*(?<rematch>[Rr]))?(\\s*(?<keep>[Ff]))?");

    public static final Pattern MINI = Pattern.compile("^[!！](?i)\\s*((ym)?)((?<ymx>x(?!\\w))|(?<ymy>y(?!\\w)))+");

    public static final Pattern MAPPOOL = Pattern.compile("^[!！]\\s*(?i)(ym)?(mappool|pool|po(?!\\w))\\s*(#(?<name>.+)#)?\\s*(?<data>[\\w\\d\\s]+)?");

    /*
    public static final Pattern START = Pattern.compile("^[!！]((积分)|(..积分))+.*");

     */

    public static final Pattern WIKI = Pattern.compile("^[!！]\\s*(?i)(ym)?((wiki)|w(?!\\w))\\s*(?<key>\\s*)?");

    public static final Pattern TRANS = Pattern.compile("^[!！]\\s*((?i)(ym)?((tr)(?!\\w)|(trans)))\\s*(?<a>[A-G#]{1,2})(?<b>\\w)");

    public static final Pattern OVERSR = Pattern.compile("^[!！]\\s*(?i)(ym)?(overstarrating|overrating|oversr|or(?!\\w))(\\s+(?<SR>[0-9.]*))?");



    public static final Pattern OLDAVATAR = Pattern.compile("^[!！]\\s*(?i)(ym)?(oldavatar|oa(?!\\w))\\s*(qq=(?<qq>\\d+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");

    public static final Pattern DRAW = Pattern.compile("^[!！]\\s*(?i)(ym)?(draw|d(?!raw))+(\\s+(?<d>\\d+))?");

    public static final Pattern COUNTMSGLEGACY = Pattern.compile("^#统计(?<d>(新人)|(进阶)|(高阶))群管理$");

    public static final Pattern COUNTMSG = Pattern.compile("^[!！]\\s*(?i)(ym)?((cm(?!\\w))|(countmessage)|(countmsg))+\\s*(?<d>(n)|(a)|(h))");


    /*
    public static final Pattern TEST = Pattern.compile("!testname (?<ids>[0-9a-zA-Z\\[\\]\\-_ ,]+)");

     */


    public static final Pattern TESTPPM = Pattern.compile("[!！]\\s*(?i)testppm(\\s*[:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))");


    public static final Pattern MAP4DCALCULATE = Pattern.compile("#cal\\s*(?<type>ar|od|cs|hp)\\s*(?<value>\\d+(\\.\\d+)?)\\s*\\+?(?<mods>([ezhdtr]{2})+)?");
    public static final Pattern TESTTAIKOSRCALCULATE = Pattern.compile("^[!！]\\s*(?i)testmt\\s*(?<data>[ox ]+)");

    public static final Pattern TESTMAP = Pattern.compile("^[!！]\\s*(?i)testmap\\s*(?<d>\\d+)(\\s*(?<mode>[\\w\\d,]+))?");

    public static final Pattern TESTCOUNTMSG = Pattern.compile("^#test统计(?<d>\\d+) (?<d1>\\d+)");
}
