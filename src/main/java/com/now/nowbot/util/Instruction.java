package com.now.nowbot.util;


import java.util.regex.Pattern;

/**
 * 所有的指令都写在这里方便进行管理
 *
 * 进阶正则 以下为java专用,其他编程语言未作尝试
 * (?:pattern) 匹配 pattern 但不捕获该匹配的子表达式,即它是一个非捕获匹配,不存储供以后使用的匹配.
 * 例子: "industr(?:y|ies)" 是比 "industry|industries" 更经济的表达式.
 * (?!pattern) 执行反向预测先行搜索的子表达式,该表达式匹配不处于匹配 pattern 的字符串的起始点的搜索字符串,不占用字符,即发生匹配后,下一匹配的搜索紧随上一匹配之后.
 * (?<!pattern)执行正向预测先行搜索的子表达式,该表达式与上条相反,同时也不占用字符 (其实 (?!) 跟 (?<!) 作用相同,区别是下次匹配的指针位置)
 * 例子: "(?<!ab)cd(?!ef)" 仅匹配非ab开头的,ef结尾的cd,且ab与ef仅作搜索用并不占用宽度,即例子对gcd中的cd也会被匹配
 * (?=pattern) 也叫零宽度正预测先行断言,它断言被匹配的字符串以表达式pattern结尾但除了结尾以外的部分,预测先行不占用字符,即发生匹配后,下一匹配的搜索紧随上一匹配之后.
 * (?<=pattern)也叫零宽度正回顾后发断言，它断言自身出现的位置的前面能匹配表达式pattern,回顾后发断言也不占用字符.
 * 例子: "(?<=\d)(匹配位置)(?=(\d{4})+$)" 仅匹配开头为数字,且长度为4n的纯数字结尾,零宽度断言并不占用字符(即不包含自身),当匹配位置为空时满足匹配且宽度为0.
 * (#正则注释) 仅作注释,会被匹配器忽略掉的文字
 * p.s. 以上的不占用字符,可理解为允许这样的匹配格式,但是已匹配的内容可能之后被重复匹配,且无法被Matcher.group()获取到,通常情况下适用于 替换文本/匹配固定'指令及参数'想获得参数值但是不想获得指令本身
 */
public enum Instruction {
    BIND("bind",    Pattern.compile("^[!！](?i)ymbind$"), "!ymbind 直接绑定"),
    BPHT("bpht",    Pattern.compile("^[!！](?i)ymbpht([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ymbpht 查询bp"),
    HELP("help",    Pattern.compile("^[!！](?i)(ym)?((help)|h)"), "!ymh/!ymhelp"),
    SWITCH("switch",Pattern.compile("^[!！](?i)switch(\\s+(?<p1>\\w+))?(\\s?(?<p2>\\w+))?(\\s?(?<p3>\\w+))?(\\s?(?<p4>\\w+))?"), null),
    PING("ping",    Pattern.compile("^[!！](?i)ymping"), null),
    PPM("ppm",    Pattern.compile("^[!！](?i)(ym)?ppm(?<vs>vs)?([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),null),
    PPPLUS("ppp",   Pattern.compile("^[!！](?i)(ym)?ppp(?![vV])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ppp [osu name] std的pp+计算"),
    PPPLUSVS("ppvs",Pattern.compile("^[!！](?i)(ym)?p([pP]*)?vs(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ppvs <osu name|@某人> pp+的对比，需要自己绑定，如果是ppvs也需要对方绑定"),
    SETU("setu",    Pattern.compile("^[!！](?i)(?<code>(setu))|(ymse)|(ymsetu)"), "!ymsetu 获取一张随机图"),
    SONG("song",    Pattern.compile("^[!！]song\\s+(((sid[:=](?<sid>\\d+))|(bid[:=](?<bid>\\d+)))|(?<id>\\d+))"), "!song <bid>或!song sid=<sid> 试听谱面"),
    START("start",  Pattern.compile("^[!！]((积分)|(..积分))+.*"), "!积分 刷新并查看积分"),
    YMP("ymp",      Pattern.compile("^[!！](?i)ym(?<isAll>[p,r])([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"!ymr/!ymp 简略地查询自己的游戏成绩"),
    YMI("ymi",      Pattern.compile("^[!！](?i)ymi(nfo)?([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"),"!ymi 简略地查询自己的游戏信息"),
    WIKI("wiki",    Pattern.compile("^[!！](?i)ym((wiki)|w)(\\s+(?<key>[^\\s]*))?"),"!ymwiki 百科，告诉你小沐知道的一切。"),
    TRANS("trans",  Pattern.compile("^[!！](ym)?trans\\s?(?<a>[A-G#]{1,2})(?<b>\\w)"),null),
    SCORE("score",  Pattern.compile("^[!！]score\\s?(?<bid>\\d+)"),null),
    UPDATE("update",Pattern.compile("^&!update$"),null),
    FRIEND("friend",Pattern.compile("^[!！](?i)ymf(\\s*(?<n>\\d+))?(\\s*[:-]\\s*(?<m>\\d+))?"),"只能查自己,参数n,n-m,最大100,太多就1-100,101-200..."),
    /*
    新建服务并指定@Service("name"),实现MessageService接口的HandleMessage,参数就从matcher.group("")来获取,,参数就是正则中(?<name>value)中的name,取值为value,当有'?'修饰时为@Nullable
     */
    TEST("test",    Pattern.compile("^[!！].*"),null),


    //TODO 待实现的指令，十万紧急，请优先完成！
//    FRIEND-ymf ymfriend [num] 获取玩家指定数量好友的信息（上限100，若超过100则发送多张图片，详情查阅群内ymf介绍文档）
//    BPS-ymbps [num]获取玩家指定数量BP的信息（上限100，详情查阅群内ymbps介绍文档）


    //TODO 次级重要的功能
//    RATING("rating", Pattern.compile("^[!！]ymra\\s?(?<matchid>\\d+)(\\s+(?<numberofround>\\d+))?(\\s*:\\s*(?<includingfail>\\d+))?(\\s*(?<numberofwarmup>\\d+))?"),null);
    RATING("rating", Pattern.compile("^[!！]ymra\\s*(?<matchid>\\d+)(\\s+(?<skipedrounds>\\d+))?(\\s+(?<includingfail>\\d))?"),null);

//    MPOB("ob", "<未上线> 场记板，可记录并通报某场正在进行的比赛！", null),
//    MPRT("rt", "<未上线> 木斗力 a.k.a MuRating 1.0，可查询比赛中选手们各自的评分！", null),

    //历史指令 存档一下
//    CATPANEL("cpanel", Pattern.compile("^[!！]\\s*(?i)cpanel(\\s+bk:(?<bk>\\d+))?(\\s+?<yl>ylbx)?"), null),
//    PPM("ppm",      Pattern.compile("^[!！](?i)(ym)?ppm(?![vV])([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"), "!ymppm[:mode][osu name] PPMinus-娱乐性实力算法，可查询不同人或不同模式（除了mania）。\n   比如：!ymppm:t muziyami"),
//    PPMVS("ppmvs",  Pattern.compile("^[!！](?i)(ym)?ppmvs([:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?(\\s*:\\s*(?<name2>[0-9a-zA-Z\\[\\]\\-_ ]+))?"), "!ymppmvs <osu name|@某人> PPM对比，需要自己绑定，如果是ppmvs也需要对方绑定"),
//    KUMO("kumo",    Pattern.compile("^none$"), null), 以后再开

/* 功能大纲备份，我懒得对齐了看到的帮我对齐一下

YumuBot功能大纲 20211111
1 内部指令（不可关闭）
1.1 服务状态-Bot服务状态，PPYAPI状态ping/ymping/"yami?"ymppy                                       ping完成部分
1.2 用户限制-封禁、解封用户，退出某群并拉入黑名单ymban/ymunban                                         待数据库
1.3 功能开关-查询模块是否开启，控制以下所有模块开关ymswitch/ymsw                                       合并到上面,待数据库
1.4 Bot基础指令查询ymhelp/ymh                                                                    完成
1.5 防刷屏机制(ymantispam/ymas)：三次或以上30秒内申请同一项指令，或是四次或以上30秒内申请多项指令均触发防刷屏。
触发时，bot发送随机一句话提示刷屏，如果再次触发则冷静5分钟。ymz指令和此通用。

2 成绩
2.1 最近成绩-最近通过ympr/ymp，最近包括失败ymrecent/ymr，用户最好成绩ymbp/ymb，用户单图最好成绩ymscore/yms，用户前后最好成绩ymbps
2.2 个人信息-基础信息yminfo/ymi，进阶信息ymx
2.3 PPminus/PPplus-单人PP+-查询ymppm/ppm/ymppp/ppp，对比PP+-查询ymppmvs/ympppvs（自己与他人，或指定的两个人）

3 查询
3.1 百科查询ymwiki/ymw
3.2 头衔图查询ymchart/ymc
3.3 赛图查询ymtour/ymt
3.4 单图信息查询ymmap/ymm
!ymmap [bid] (acc) (combo%)+[mods]
3.5 天气查询 ymweather/ymwt
3.7 谱面试听 ymaudition/yma
3.8 谱面搜索 ymsearch/ymsh
3.9 好友查询 ymfriend/ymf


4 普通娱乐
4.1 掷骰子roll/ymroll/ymo
4.2 词云ymwordcloud/ymwc
4.3 今日运势ymdivine/ymd
4.4 判断（选A还是选B）...
4.5 色图ymsetu/ymse
4.6 从网站获得的色图ymkonachan(yandere)/ymk(ymy) ...
4.7 奇特的个人对决？ymversus/ymv...

5 自定义
5.1 设置主模式ymsetmode/ymsm
5.2 修改主面板ymsetpanel/ymsp（目前不开放）
5.3 修改头图ymsetbanner/ymsb
5.4 修改卡片背景ymsetcard/ymsc

6 抽卡
6.1 积分ymgold/ymg：查询并更新积分
6.2 抽卡ymdraw/ymdw：获取抽卡基础信息，抽卡获取结果-!ymdw10 十连，!ymdw 获取信息 !ymdw1 单抽，!ymdw


7 辅助
7.1 监视多人游戏房间ymmonitor/ymmo：监视，通报成绩，获取获胜条件并自动判断，发起监视的用户修改当前得分等
7.2 木斗力ymrating/ymra，通过计算已经打完的比赛来给出评价分数。
7.3 移调ymtrans/ymtr，输入主音，给出相应的大调、和声小调、旋律小调组合。
7.4 禁言睡眠ymsleep/ymz，给予精致8小时睡眠并评价。至少1小时后，再次私聊bot!ymz即可解除禁言，但进入7小时冷却时间。（冷却时间三次以上申请sleep即触发刷屏机制！）

8 聊天
8.1 全局被动回复(ympassivechat)
8.2 私聊聊天(ymchat)-正常聊天，女友聊天

短链字母表
A/ymaudition
B/ymbestperformance
C/ymchart
D/ymdivine
E
F/ymfriend
G/ymgold
H/ymhelp            [Long term update]
I/yminfomation      [Temporary]
J
K/ymk?
L
M/ymmap
N
O/ymroll            [Finished]
P/ympassrecent      [Temporary]
Q
R/ymrecent          [Temporary]
S/ymscore
T
U
V/ymv?
W/ymwiki            [Long term update]
X
Y/ymy?
Z/ymsleep/ymz

BPS/ymbestperformances 
DW/ymdraw
MO/ymmonitor 也可能改名叫YMOB
RA/ymrating         [Temporary]
SB/ymsetbanner
SC/ymsetcard
SE/ymsetu
SH/ymsearch
SM/ymsetmode
SP/ymsetpanel
SW/ymswitch
WC/ymwordcloud
WT/ymweather


*/



    private final Pattern regex;

    private final String name;

    private final String desc;

    Instruction(String name, Pattern regex, String desc){
        this.regex = regex;
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

    public static void main(String[] args) {
        var p = Pattern.compile("^[!！](?i)ymf(\\s*(?<n>\\d+))?(\\s*[:-]\\s*(?<m>\\d+))?");
        var m = p.matcher("!ymf 4-7");
        if(m.find()){
            int s = m.group("n")==null?0:Integer.parseInt(m.group("n"))-1;
            int e = m.group("m")==null?15:Integer.parseInt(m.group("m"))-1;
            s ^= e;
            e ^= s;
            s ^= e;
            System.out.println(s+"|"+e);
        }
    }
}
