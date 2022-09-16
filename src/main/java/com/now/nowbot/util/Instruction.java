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
    BIND(BindService.class,
            Pattern.compile("^[!！](?i)(ym)?(?<un>un)?bind(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+))?")),

    BPHT(BphtService.class,
            Pattern.compile("^[!！](?i)ymbpht(?<info>-i)?([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    TODAYBP(TodayBpService.class,
            Pattern.compile("^[!！](?i)tbp(\\s*[:：](?<mode>[\\w\\d]+))?(\\s*#(?<day>\\w{0,3}))?")),

    HELP(HelpService.class,
            Pattern.compile("^[!！](?i)(ym)?((help)|h)")),

    SWITCH(SwitchService.class,
            Pattern.compile("^[!！](?i)ymsw(itch)?(\\s+(?<p1>\\w+))?(\\s+(?<p2>\\w+))?(\\s+(?<p3>\\w+))?(\\s+(?<p4>\\w+))?")),

    PING(PingService.class,
            Pattern.compile("^[!！](?i)ymping")),

    PPM(PPmService.class,
            Pattern.compile("^[!！](?i)(ym)?ppm(?<vs>vs)?([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

//    PPPLUS(PpPlusService.class,
//            Pattern.compile("^[!！](?i)(ym)?ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),
//
//    PPPLUSVS(PpPlusVsService.class,
//            Pattern.compile("^[!！](?i)(ym)?p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    SETU(SetuService.class,
            Pattern.compile("^[!！](?i)(?<code>(setu))|(ymse)|(ymsetu)")),

    SONG(SongService.class,
            Pattern.compile("^[!！]song\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))")),

    START(StartService.class,
            Pattern.compile("^[!！]((积分)|(..积分))+.*")),

    YMP(YmpService.class,
            Pattern.compile("^[!！](?i)ym(?<isAll>[p,r])([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    YMI(YmiService.class,
            Pattern.compile("^[!！](?i)ymi(nfo)?([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?")),

    WIKI(WikiService.class,
            Pattern.compile("^[!！](?i)ym((wiki)|w)(\\s+(?<key>[^\\s]*))?")),

    TRANS(TransService.class,
            Pattern.compile("^[!！](ym)?trans\\s?(?<a>[A-G#]{1,2})(?<b>\\w)")),

    SCORE(ScoreService.class,
            Pattern.compile("^[!！]score\\s?(?<bid>\\d+)")),

    UPDATE(UpdateService.class,
            Pattern.compile("^&!update$")),

    SETMODE(SetModeService.class,
            Pattern.compile("^[!！](?i)ymmode\\s*(?<mode>\\w+)")),

    FRIEND(FriendService.class,
            Pattern.compile("^[!！](?i)ymf(\\s*(?<n>\\d+))?(\\s*[:-]\\s*(?<m>\\d+))?")),

    CATPANEL(CatpanelService.class,
            Pattern.compile("[!！]testbg(\\s*(?<r>qc))?(\\s+(?<bk>\\d{1,3}))?(\\s*(?<yl>ylbx))?")),

    MUTUAL(MutualFriendService.class,
            Pattern.compile("[!！](?i)(test)?mu\\s*(?<names>[0-9a-zA-Z\\[\\]\\-_ ,]*)?")),

    BAN(BanService.class,
            Pattern.compile("[!！](?i)(?<un>un)?ban\\s*(?<serv>\\w+)\\s*(?<gf>[gf])\\s*(?<qq>\\d+)?")),
    /*
    新建服务并指定@Service("aClass"),实现MessageService接口的HandleMessage,参数就从matcher.group("")来获取,,参数就是正则中(?<aClass>value)中的name,取值为value,当有'?'修饰时为@Nullable
     */
    TEST(TestService.class,
            Pattern.compile("^[!！]test.*")),

    TESTID(TestGetId.class,    Pattern.compile("^[!！]testid\\s*(?<ids>((\\d)+(,)?)+)")),

    TESTRA(TestRaService.class,    Pattern.compile("[!！]testra(\\s+(?<id>\\d+))")),

    TESTPPM(TestPPMService.class,    Pattern.compile("!testppm([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))")),

    GROUP(JoinGroupService.class,    Pattern.compile("(是)|(否)")),
    INFO(InfoService.class,    Pattern.compile("[!！]testinfo([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))")),


    //TODO 待实现的指令，十万紧急，请优先完成！
//    BPS-ymbps [num]获取玩家指定数量BP的信息（上限100，详情查阅群内ymbps介绍文档）


    //TODO 次级重要的功能
//    RATING("rating", Pattern.compile("^[!！]ymra\\s?(?<matchid>\\d+)(\\s+(?<numberofround>\\d+))?(\\s*:\\s*(?<includingfail>\\d+))?(\\s*(?<numberofwarmup>\\d+))?"),null);
    RATING(RatingService.class,
            Pattern.compile("^[!！]ymra\\s*(?<matchid>\\d+)(\\s+(?<skipedrounds>\\d+))?(\\s+(?<deletendrounds>\\d+))?(\\s+(?<includingfail>\\d))?")),

    TESTMT(TestMt4.class,
            Pattern.compile("^[!！]testmt\\s*(?<data>[ox ]+)")),

    MAPPOOL(MapPoolService.class,
            Pattern.compile("^[!！]map")),

    BPSHOW(BpShowService.class,
            Pattern.compile("^[!！]bp\\s*(?<n>\\d)"));
            ;

//    MPOB("ob", "<未上线> 场记板，可记录并通报某场正在进行的比赛！", null),
//    MPRT("rt", "<未上线> 木斗力 a.k.a MuRating 1.0，可查询比赛中选手们各自的评分！", null),

    //历史指令 存档一下
//    PPM("ppm",      Pattern.compile("^[!！](?i)(ym)?ppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<aClass>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ymppm[:mode][osu aClass] PPMinus-娱乐性实力算法，可查询不同人或不同模式（除了mania）。\n   比如：!ymppm:t muziyami"),
//    PPMVS("ppmvs",  Pattern.compile("^[!！](?i)(ym)?ppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<aClass>[0-9a-zA-Z\\[\\]\\-_ ]*))?(\\s*:\\s*(?<name2>[0-9a-zA-Z\\[\\]\\-_ ]+))?"), "!ymppmvs <osu aClass|@某人> PPM对比，需要自己绑定，如果是ppmvs也需要对方绑定"),
//    KUMO("kumo",    Pattern.compile("^none$"), null), 以后再开

/* 功能大纲备份，我懒得对齐了看到的帮我对齐一下

YumuBot功能大纲 20211119
1 内部指令（不可关闭）
1.1 服务状态-Bot服务状态，PPYAPI状态ping/ymping/"yami?"ymppy                                       ping完成部分
1.2 用户限制-封禁、解封用户，退出某群并拉入黑名单ymban/ymunban                                         待数据库
1.3 功能开关-查询模块是否开启，控制以下所有模块开关ymswitch/ymsw                                       合并到上面,待数据库
1.4 Bot基础指令查询ymhelp/ymh
1.5 防刷屏机制ymantispam/ymas开启后，三次或以上30秒内申请同一项指令，或是四次或以上30秒内申请多项指令均触发防刷屏。
触发时，bot发送随机一句话提示刷屏，如果再次触发则冷静5分钟。ymz指令和此通用。

2 成绩
2.1 最近成绩-最近通过ympr/ymp，最近包括失败ymrecent/ymr，用户最好成绩ymbp/ymb，用户单图最好成绩ymscore/yms，用户前后最好成绩ymbps...待定
2.2 个人信息-基础信息yminfo/ymi，进阶信息ymx
2.3 PPminus/PPplus-单人PP+-查询ymppm/ppm/ymppp/ppp，对比PP+-查询ymppmvs/ympppvs（自己与他人，或指定的两个人）

3 查询
3.1 百科查询ymwiki/ymw
3.2 头衔图查询，课题功能ymcourse/ymc
3.3 赛图查询ymtour/ymt
3.4 单图信息查询ymmap/ymm
!ymmap [bid] (acc) (combo%)+[mods]
3.7 谱面试听 ymaudition/yma [bid]
3.8 谱面搜索 ymsearch/ymsh
3.9 好友查询 ymfriend/ymf [num]
3.10 用户查询 ymmutual/ymmu [bid/username] 输出用户的主页链接
3.11 今日历史记录查询(ymrecent拓展) ymtoday/ymtd 默认查询用户24h内所有成绩并列出，最多100条。同时需要查询用户bp，并进行对比，若有新bp则加发光效果
3.12 谱师查询 ymmapper/ymmr


4 普通娱乐
4.1 掷骰子roll/ymroll/ymo
4.2 词云ymwordcloud/ymwc
4.3 今日运势ymdivine/ymd
4.4 判断（选A还是选B）...
4.5 色图ymsetu/ymse
4.6 从网站获得的色图ymkonachan(yandere)/ymk(ymy) ...待定
4.7 奇特的个人对决？ymversus/ymv ...待定

5 自定义
5.1 设置主模式ymsetmode/ymsm
5.2 修改主面板ymsetpanel/ymsp（目前不开放）
5.3 修改头图ymsetbanner/ymsb
5.4 修改卡片背景ymsetcard/ymsc

6 抽卡
6.1 积分ymgold/ymg：查询并更新积分
6.2 抽卡ymdraw/ymdw：获取抽卡基础信息，抽卡获取结果-!ymdw10 十连，!ymdw 获取信息 !ymdw1 单抽，!ymdw


7 辅助
7.1 移调ymtrans/ymtr，输入主音，给出相应的大调、和声小调、旋律小调组合。
7.2 禁言睡眠ymsleep/ymz，给予精致8小时睡眠并评价。至少1小时后，再次私聊bot!ymz即可解除禁言，但进入7小时冷却时间。（冷却时间三次以上申请sleep即触发刷屏机制！）
7.3 天气 ymweather/ymwt [city]

8 聊天
8.1 全局被动回复(ympassivechat)
8.2 私聊聊天(ymchat)-正常聊天，女友聊天

9 比赛专用
9.1 进入主页ymtournament/ymt
主页其实就是一张图片或者面板，介绍功能，通报最近的比赛或者elo变化。从otsu获取。

9.2 ELO查询ymelo/yme [uid]
查询某人的elo，走爆炸鸽的API

9.3 监视比赛ymmonitor/ymmo：监视，通报成绩，获取获胜条件并自动判断，允许发起监视的用户修改当前得分或者数据等
ymmo(:)(ratingmode) [mpid]

若监听到房间已关闭，或者请求的用户输入
!ymmo closed，则关闭监听并按ymra:m(木斗力)面板输出

9.4 木斗力ymrating/ymra [bid] (warmup) (includingfail?)，通过计算已经打完的比赛来给出评价分数。

9.5 斗力拓展 ymra:m 默认 ymra:p 光斗力 ymra:g 高斗力 ymra:s osuplus斗力...待定
使用第三方算法计算。

9.6 图池查询 ympool/ympo [string]，类似于ymwiki，默认展示出现有的所有图池，输入对应图池编号（如MP5S12R1）获得图池展示。

9.7 图池录入 ymaddpool/ymap [poolname] [a long string]可允许玩家录入图池。然后发送到bot群审核（审核图即录入展示的界面），通过审核，bot即会记录下此图池并存档。

同时需要存储非r/a/l谱面（最好是获取到osu文件）和非r/a/l谱面的banner头图。
用户查询的时候，需要对比谱面状态非ranked/approved/loved的谱面。若官网无数据，则**保留本地文件**，并在谱面上作提示（比如变灰之类，但是旧数据不能丢），若官网有数据，则**替换本地文件**

9.8 上传比赛 ymuploadmatch/ymum [bid] (warmup) (includingfail?)
匹配方式和ymra一样把比赛上传到otsu供爆炸鸽记录。然后唤起ymra:p(光斗力)并输出结果。


短链字母表
A/ymaudition
B/ymbestperformance
C/ymcourse
D/ymdivine
E/ymelo
F/ymfriend
G/ymgold
H/ymhelp            [Long term update]
I/yminfomation      [Temporary]
J
K
L
M/ymmap
N
O/ymroll            [Finished]
P/ympassrecent      [Temporary]
Q
R/ymrecent          [Temporary]
S/ymscore
T/ymtournament
U
V
W/ymwiki            [Long term update]
X
Y
Z/ymsleep/ymz

AS/ymantispam
AP/ymaddpool
DW/ymdraw
MO/ymmonitor 也可能改名叫YMOB
MU/ymmutual
MR/ymmapper
PO/ympool
RA/ymrating         [Temporary]
SB/ymsetbanner
SC/ymsetcard
SE/ymsetu
SH/ymsearch
SM/ymsetmode
SP/ymsetpanel
SW/ymswitch
TD/ymtoday
UM/ymuploadmatch
WC/ymwordcloud
WT/ymweather

PPM/ymppm/ppm/ppmvs
PPP/ymppp/ppp/pppvs


没有缩写的功能
ymban/ymunban
*/



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
        var p = Pattern.compile("^[!！](?i)(ym)?(?<un>un)?bind(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+))?");
        var m = p.matcher("!bind   ffff");
        var rp = valueOf("ban".toUpperCase());
        var rm = rp.regex.matcher("!ban ymf f 22");
        System.out.println(rm.find());
        System.out.println(rm.group("gf"));
        while (m.find()){
//            int s = m.group("n")==null?0:Integer.parseInt(m.group("n"))-1;
//            int e = m.group("m")==null?15:Integer.parseInt(m.group("m"))-1;
//            s ^= e;
//            e ^= s;
//            s ^= e;
            System.out.println(m.group("name"));
        }
    }
}
