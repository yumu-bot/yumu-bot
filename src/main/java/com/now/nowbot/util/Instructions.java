package com.now.nowbot.util;

import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 **/
public class Instructions {

    // #0 调出帮助
    public static final Pattern HELP = Pattern.compile("^[!！]\\s*(?i)(ym)?(帮助|help|h)\\s*(?<module>[\\s\\S]*)?");
    public static final Pattern AUDIO = Pattern.compile("^[!！]\\s*(?i)(ym)?(song|audio|a(?![AaC-RT-Zc-rt-z_]))\\s*([:：]?(?<type>(bid|b|sid|s)))?\\s*(?<id>\\d+)?");

    // #1 BOT 内部指令
    public static final Pattern PING = Pattern.compile("^[!！]\\s*(?i)((ym)?(ping|pi(?![A-Za-z_]))|yumu\\?)");

    public static final Pattern BIND = Pattern.compile("^[!！]\\s*(?i)(ym)?(bi(?!nd)|((ym)|(?<un>(un)))bind)\\s*([:：](?<full>f))?\\s*(qq=\\s*(?<qq>\\d+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+)?");

    public static final Pattern BAN  = Pattern.compile("^[!！]\\s*(?i)(ym)?(super|sp(?![A-Za-z_])|operate|op(?![A-Za-z_]))\\s*([:：]?(?<operate>(black)?list|add|remove|(un)?ban|[lkarub]))?\\s*(qq=\\s*(?<qq>\\d+))?\\s*(group=\\s*(?<group>\\d+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+)?");

    public static final Pattern SWITCH = Pattern.compile("^[!！]\\s*(?i)(ym)?(switch|sw(?![A-Za-z_]))\\s*(?<p1>\\w*)?\\s*(?<p2>\\w*)?");

    public static final Pattern ECHO = Pattern.compile("^[!！#]\\s*(?i)(ym)?(echo|ec(?![A-Za-z_]))\\s*(?<any>[\\s\\S]*)");

    public static final Pattern SERVICE_COUNT = Pattern.compile("^[!！]\\s*(?i)(ym)?(servicecount|统计服务调用|sc(?![A-Za-z_]))\\s*((?<days>\\d+)d)?\\s*((?<hours>\\d+)h?)?");


    // #2 osu! 成绩指令
    public static final Pattern SET_MODE = Pattern.compile("^[!！]\\s*(?i)(ym)?(setmode|mode|sm(?![A-Za-z_])|mo(?![A-Za-z_]))+\\s*(?<mode>\\w+)");

    public static final Pattern SCORE_PR = Pattern.compile("^[!！]\\s*(?i)(?<pass>(ym)?(pass(?![sS])(?<es>es)?|p(?![a-rt-zA-RT-Z_]))|(ym)?(?<recent>(recent|r(?![a-rt-zA-RT-Z_]))))(?<s>s)?\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?)?\\s*(#?(?<n>\\d+)([-－](?<m>\\d+))?)?$");

    public static final Pattern UU_PR = Pattern.compile("^[!！]\\s*(?i)(uu(?<pass>(pass|p(?![A-Za-z_])))|uu(?<recent>(recent|r(?![A-Za-z_]))))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?))?\\s*(#?(?<n>\\d+))?$");

    public static final Pattern SCORE = Pattern.compile("^[!！]\\s*(?i)(?<score>(ym)?(score|s(?![A-Za-z_])))\\s*([:：](?<mode>[\\w\\d]+))?\\s*(?<bid>\\d+)\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?\\s*(\\+(?<mod>( ?[EZNMFHTDRSPCLO]{2})+))?");

    // b ymb ymbp :0-3 name 1-100
    public static final Pattern BP = Pattern.compile("^[!！]\\s*(?i)(?<bp>(ym)?(bestperformance|best|bp(?![a-rt-zA-RT-Z_])|b(?![a-rt-zA-RT-Z_])))(?<s>s)?\\s*([:：](?<mode>\\w+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?)?\\s*(#?(?<n>\\d+)([-－](?<m>\\d+))?)?$");
    public static final Pattern TODAY_BP = Pattern.compile("^[!！]\\s*(?i)(ym)?(todaybp|(tbp|tdp|t(?![A-Za-z_])))+\\s*([:：](?<mode>[\\w\\d]+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?(?!#))?\\s*(#?\\s*(?<day>\\d*)\\s*)$");

    public static final Pattern BP_ANALYSIS = Pattern.compile("^[!！]\\s*(?i)(ym)?((bpanalysis)|(blue\\s*archive)|bpa(?![A-Za-z_])|ba(?![A-Za-z_]))+(\\s*[:：](?<mode>\\w+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");


    public static final Pattern UU_BA = Pattern.compile("^[!！]\\s*(?i)(uubpanalysis|u(u)?(ba|bpa))(?<info>(-?i))?(\\s*[:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]{3,}))?");

    // #3 osu! 玩家指令

    // i ymi yminfo :0-3 name
    public static final Pattern INFO = Pattern.compile("^[!！]\\s*(?i)(ym)?(information|info(?![A-Za-z_])|i(?![A-Za-z_]))\\s*([:：](?<mode>[\\w\\d]+))?\\s*(qq=\\s*(?<qq>\\d+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");

    public static final Pattern UU_INFO = Pattern.compile("^[!！]\\s*(?i)uu(info|i(?![A-Za-z_]))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");

    public static final Pattern I_MAPPER = Pattern.compile("^[!！]\\s*(?i)(ym)?((im?)?mapper|im(?![A-Za-z_]))+(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");


    public static final Pattern FRIEND = Pattern.compile("^[!！]\\s*(?i)(ym)?(friend(s)?|f(?![A-Za-z_]))\\s*(?<n>\\d+)?\\s*([:-]\\s*(?<m>\\d+))?");

    public static final Pattern MUTUAL = Pattern.compile("^[!！]\\s*(?i)(ym)?(mutual|mu(?![A-Za-z_]))\\s*(?<names>[0-9a-zA-Z\\[\\]\\-_ ,]+)?");

    public static final Pattern PP_MINUS = Pattern.compile("^[!！]\\s*(?i)(ym)?(ppminus|(p?(pm))(?![a-rt-uw-zA-RT-UW-Z_]))\\s*(?<vs>vs)?\\s*([:：](?<mode>[\\w\\d]+))?(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");


    // #4 osu! 谱面指令

    public static final Pattern MAP = Pattern.compile("[!！]\\s*(?i)(ym)?(beatmap|map(?![A-Za-z_])|m(?![A-Za-z_]))\\s*([:：](?<mode>\\w+))?\\s*(?<bid>\\d+)?\\s*([a%]?(?<accuracy>\\d+\\.?\\d*)[a%]?)?\\s*([cx]?(?<combo>\\d+\\.?\\d*)[cx]?)?\\s*([\\-m]?(?<miss>\\d+)[\\-m]?)?\\s*(\\+(?<mod>( ?[EZNMFHTDRSPCLO]{2})+))?");

    public static final Pattern QUALIFIED_MAP = Pattern.compile("[!！]\\s*(?i)(ym)?(qualified|qua(?![A-Za-z_])|q(?![A-Za-z_]))\\s*([:：](?<mode>\\w+))?\\s*(#+(?<status>[-\\w]+))?\\s*(\\*?(?<sort>[-_+a-zA-Z]+))?\\s*(?<range>\\d+)?");

    public static final Pattern LEADER_BOARD = Pattern.compile("^[!！]\\s*(?i)(ym)?(mapscorelist|leaderboard|leader(?![A-Za-z_])|list(?![A-Za-z_])|l(?![A-Za-z_]))\\s*([:：](?<mode>\\w+))?\\s*(?<bid>\\d+)?\\s*(?<range>\\d+)?");

    public static final Pattern MAP_MINUS = Pattern.compile("^[!！]\\s*(?i)(ym)?(mapminus|mm(?![A-Za-z_]))\\s*(?<id>\\d+)?");

    public static final Pattern NOMINATION = Pattern.compile("^[!！]\\s*(?i)(ym)?(nominat(e|ion)s?|nom(?![AC-RT-Zac-rt-z_])|n(?![AC-RT-Zac-rt-z_]))\\s*([:：]?(?<mode>(bid|sid|s|b)))?\\s*(?<sid>\\d+)?");

    public static final Pattern KITA = Pattern.compile("^[!！]\\s*(?i)(ym)?(kita|k(?![a-wy-zA-WY-Z_]))(?<noBG>([xX](?![A-Za-z_])))?\\s*(?<bid>\\d+)?\\s*(?<mod>\\w+)?\\s*(?<round>[\\w\\s]+)?");

    // #5 osu! 比赛指令

    public static final Pattern MATCH_LISTENER = Pattern.compile("^[!！]\\s*(?i)(ym)?(make\\s*love|(match)?listen(er)?|ml(?![A-Za-z_])|li(?![A-Za-z_]))\\s*(?<matchid>\\d+)?\\s*(?<operate>start|stop|end|off|on|[spefo](?![A-Za-z_]))?");
    public static final Pattern MU_RATING = Pattern.compile("^[!！]\\s*(?i)((?<uu>(u{1,2})(rating|ra(?![A-Za-z_])))|(?<main>((ym)?rating|(ym)?ra(?![A-Za-z_])|mra(?![A-Za-z_]))))\\s*(?<matchid>\\d+)?(\\s*(?<skip>-?\\d+))?(\\s*(?<skipend>-?\\d+))?(\\s*(?<rematch>[Rr]))?(\\s*(?<failed>[Ff]))?");

    /*
    public static final Pattern URA(URAService.class,
            Pattern.compile("^[!！]\\s*(?i)(u{1,2})(rating|ra(?![A-Za-z_]))+\\s*(?<matchid>\\d+)(\\s*(?<skipedrounds>\\d+))?(\\s*(?<deletendrounds>\\d+))?(\\s*(?<excludingrematch>[Rr]))?(\\s*(?<excludingfail>[Ff]))?");

     */

    public static final Pattern SERIES_RATING = Pattern.compile("^[!！]\\s*(?i)((?<uu>(u{1,2})(seriesrating|series|sra(?![A-Za-z_])|sa(?![A-Za-z_])))|(ym)?(?<main>(seriesrating|series|sa(?![A-Za-z_])|sra(?![A-Za-z_])))|(ym)?(?<csv>(csvseriesrating|csvseries|csa(?![A-Za-z_])|cs(?![A-Za-z_]))))\\s*(#(?<name>.+)#)?\\s*(?<data>[\\d\\s,，|\\-]+)?(\\s*(?<rematch>[Rr]))?(\\s*(?<failed>[Ff]))?");

    public static final Pattern CSV_MATCH = Pattern.compile("[!！]\\s*(?i)((ym)?(csvrating|cr(?![a-rt-wy-zA-RT-WY-Z_])|cra(?![a-rt-wy-zA-RT-WY-Z_])))\\s*(?<x>[xXsS])?\\s*(?<data>[\\d\\s,，|\\-]+)?");

    public static final Pattern MATCH_ROUND = Pattern.compile("^[!！]\\s*(?i)(ym)?(matchround(s)?|round(s)?(?![a-zA-Z_])|mr(?![a-zA-Z_])|ro(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)?\\s*(?<round>\\d+)?(\\s*(?<keyword>[\\w\\s\\d-_ %*()/|]+))?");

    public static final Pattern MATCH_NOW = Pattern.compile("^[!！]\\s*(?i)(ym)?(monitornow|matchnow|mn(?![A-Za-z_]))+\\s*(?<matchid>\\d+)(\\s*(?<skip>\\d+))?(\\s*(?<skipend>\\d+))?(\\s*(?<rematch>[Rr]))?(\\s*(?<failed>[Ff]))?");

    public static final Pattern MINI = Pattern.compile("^[!！](?i)\\s*((ym)?)((?<ymx>x(?![A-Za-z_]))|(?<ymy>y(?![A-Za-z_])))+");

    public static final Pattern MAP_POOL = Pattern.compile("^[!！]\\s*(?i)(ym)?(mappool|po(?![A-Za-z_]))\\s*(id=\\s*(?<id>\\d+))?\\s*(?<name>\\w+)");
    /*
    public static final Pattern ADD_POOL = Pattern.compile("^[!！]\\s*(?i)(ym)?(addpool|ap(?![A-Za-z_]))\\s*(id=\\s*(?<id>\\d+))?\\s*(?<name>\\w+)");

     */
    public static final Pattern GET_POOL = Pattern.compile("^[!！]\\s*(?i)(ym)?(getpool|gp(?![A-Za-z_]))\\s*(#(?<name>.+)#)?\\s*(?<data>[\\w\\d\\s,，|\\-]+)?");

    /*
    public static final Pattern START = Pattern.compile("^[!！]((积分)|(..积分))+.*");

     */

    public static final Pattern WIKI = Pattern.compile("^[!！]\\s*(?i)(ym)?((wiki)|w(?![A-Za-z_]))\\s*(?<key>\\s*)?");

    public static final Pattern TRANS = Pattern.compile("^[!！]\\s*((?i)(ym)?((tr)(?![A-Za-z_])|(trans)))\\s*(?<a>[A-G#]{1,2})(?<b>\\w)");

    public static final Pattern OVER_SR = Pattern.compile("^[!！]\\s*(?i)(ym)?(overstarrating|overrating|oversr|or(?![A-Za-z_]))(\\s+(?<SR>[0-9.]*))?");


    public static final Pattern OLD_AVATAR = Pattern.compile("^[!！]\\s*(?i)(ym)?((old|osu)?avatar|oa(?![A-Za-z_]))\\s*(qq=\\s*(?<qq>\\d+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");

    public static final Pattern DRAW = Pattern.compile("^[!！]\\s*(?i)(ym)?(draw|d(?!raw))\\s*(?<d>\\d+)?");

    public static final Pattern COUNT_MESSAGE_LEGACY = Pattern.compile("^#统计(?<d>(新人)|(进阶)|(高阶))群管理$");

    public static final Pattern COUNT_MESSAGE = Pattern.compile("^[!！]\\s*(?i)(ym)?(cm(?![A-Za-z_])|countmessage|countmsg)\\s*(?<d>(n)|(a)|(h))");

    public static final Pattern GROUP_STATISTICS = Pattern.compile("^[!！]\\s*(?i)(ym)?(gs(?![A-Za-z_])|groupstat(s)?|groupstatistic(s)?|统计(超限)?)\\s*(?<group>[:：]?[nah]|((新人|进阶|高阶)群))(?!\\w)");

    // #9 自定义
    public static final Pattern CUSTOM = Pattern.compile("^[!！]\\s*(?i)(ym)?(custom|c(?![AD-Zad-z_]))\\s*([:：]?(?<type>(b|c|banner|card)))?");

    /*
    public static final Pattern TEST = Pattern.compile("!testname (?<ids>[0-9a-zA-Z\\[\\]\\-_ ,]+)");

     */


    public static final Pattern TEST_PPM = Pattern.compile("[!！]\\s*(?i)testppm(\\s*[:：](?<mode>[\\w\\d]+))?\\s*(?<data>[\\[\\]\\w\\d\\s\\-_,，|:]+)?");


    public static final Pattern MAP_4D_CALCULATE = Pattern.compile("[!！#]\\s*(?i)cal\\s*(?<type>ar|od|cs|hp)\\s*(?<value>\\d+(\\.\\d+)?)\\s*\\+?(?<mods>([ezhdtrnc]+))?");
    public static final Pattern TEST_TAIKO_SR_CALCULATE = Pattern.compile("^[!！]\\s*(?i)testmt\\s*(?<data>[ox ]+)");

    public static final Pattern TEST_MAP = Pattern.compile("^[!！]\\s*(?i)testmap\\s*(?<id>\\d+)\\s*(\\+(?<mod>[\\w\\d\\s,，|\\-]+))?");



    // #-1 已弃用

    public static final Pattern DEPRECATED_BPHT = Pattern.compile("^[!！]\\s*(?i)(ym)?(?<bpht>(bpht))[\\s\\S]*");

    public static final Pattern DEPRECATED_SET = Pattern.compile("^[!！]\\s*(?i)ym(?<set>(set))[\\s\\S]*");
}
