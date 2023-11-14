package com.now.nowbot.model.multiplayer;

import io.github.humbleui.skija.Color;

public enum PlayerClassification {
    // 这一段是 YMRA v1.2 更新内容
    BC("Big Carry", Color.makeRGB(255,241,0)), //大爹
    CA("Carry", Color.makeRGB(255,152,0)), //大哥
    MF("Main Force", Color.makeRGB(34,172,56)), //主力
    SP("Specialized",Color.makeRGB(179,212,101)), //专精
    WF("Work Force", Color.makeRGB(0,104,183)), //打工
    GE("General",Color.makeRGB(189,189,189)), //普通
    GU("Guest", Color.makeRGB(0,160,233)), //客串
    SU("Support",Color.makeRGB(96,25,134)), //抗压
    SG("Scapegoat", Color.makeRGB(228,0,127)), //背锅
    NO("Noob", Color.makeRGB(235,104,119)), //小弟
    FU("Futile", Color.makeRGB(211,47,47)), //炮灰

    SMA("Strongest Marshal", Color.makeRGB(255,241,0)),//最强元帅
    CMA("Competent Marshal", Color.makeRGB(255,241,0)),//称职元帅
    IMA("Indomitable Marshal", Color.makeRGB(255,241,0)),//不屈元帅
    EGE("Ever-Victorious General", Color.makeRGB(255,152,0)),//常胜将军
    AGE("Assiduous General", Color.makeRGB(255,152,0)),//勤奋将军
    SGE("Striven General", Color.makeRGB(255,152,0)),//尽力将军
    BMF("Breakthrough Main Force", Color.makeRGB(34,172,56)),//突破主力
    RMF("Reliable Main Force", Color.makeRGB(34,172,56)),//可靠主力
    SMF("Staunch Main Force", Color.makeRGB(34,172,56)),//坚守主力
    EAS("Elite Assassin", Color.makeRGB(179,212,101)),//精锐刺客
    NAS("Normal Assassin", Color.makeRGB(179,212,101)),//普通刺客
    FAS("Fake Assassin", Color.makeRGB(179,212,101)),//冒牌刺客
    GCW("Gold Collar Worker", Color.makeRGB(0,104,183)),//金领工人
    WCW("White Collar Worker", Color.makeRGB(0,104,183)),//白领工人
    BCW("Blue Collar Worker", Color.makeRGB(0,104,183)),//蓝领工人
    KPS("Key Person", Color.makeRGB(189,189,189)),//关键人
    CMN("Common Man", Color.makeRGB(189,189,189)),//普通人
    PSB("Passer-by", Color.makeRGB(189,189,189)),//路人甲
    MAC("Major Character", Color.makeRGB(0,160,233)),//主要角色
    MIC("Minor Character", Color.makeRGB(0,160,233)),//次要角色
    FIG("Figurant", Color.makeRGB(0,160,233)),//群众演员
    SAM("Stable as Mountain", Color.makeRGB(96,25,134)),//稳如泰山
    HAS("Hard as Stone", Color.makeRGB(96,25,134)),//坚若磐石
    SIN("Seriously Injured", Color.makeRGB(96,25,134)),//伤痕累累
    ANI("Advanced Ninja", Color.makeRGB(228,0,127)),//上等忍者
    MNI("Mediocre Ninja", Color.makeRGB(228,0,127)),//普通忍者
    LCS("Lower-class", Color.makeRGB(228,0,127)),//不入流
    LKD("Lucky Dog", Color.makeRGB(235,104,119)),//幸运儿
    QAP("Qualified Apprentice", Color.makeRGB(235,104,119)),//合格学徒
    BGN("Beginner", Color.makeRGB(235,104,119)),//初学者
    LSS("Life-saving Straw", Color.makeRGB(211,47,47)),//救命稻草
    LSP("Little Spark", Color.makeRGB(211,47,47)),//点点星火
    BDT("Burnt Dust", Color.makeRGB(211,47,47)),//湮灭尘埃

    // 这一段是 YMRA v3.5 更新内容
    ;

    public final String name;
    public final int color;

    PlayerClassification(String name, int color) {
        this.name = name;
        this.color = color;
    }
}
