package com.now.nowbot.model.multiplayer;

import io.github.humbleui.skija.Color;

public enum PlayerClassification {
    BC("Big Carry", "大爹", ClassColor.BC.color),
    CA("Carry", "大哥", ClassColor.CA.color),
    MF("Main Force", "主力", ClassColor.MF.color),
    SP("Specialized", "专精", ClassColor.SP.color),
    WF("Work Force", "打工", ClassColor.WF.color),
    GE("General", "普通", ClassColor.GE.color),
    GU("Guest", "客串", ClassColor.GU.color),
    SU("Support", "抗压", ClassColor.SU.color),
    SG("Scapegoat", "背锅", ClassColor.SG.color),
    NO("Noob", "小弟", ClassColor.NO.color),
    FU("Futile", "炮灰", ClassColor.FU.color),

    SMA("Strongest Marshal", "最强元帅", BC.color),
    CMA("Competent Marshal", "称职元帅", BC.color),
    IMA("Indomitable Marshal", "不屈元帅", BC.color),
    EGE("Ever-Victorious General", "常胜将军", CA.color),
    AGE("Assiduous General", "勤奋将军", CA.color),
    SGE("Striven General", "尽力将军", CA.color),
    BMF("Breakthrough Main Force", "突破主力", MF.color),
    RMF("Reliable Main Force", "可靠主力", MF.color),
    SMF("Staunch Main Force", "坚守主力", MF.color),
    EAS("Elite Assassin", "精锐刺客", SP.color),
    NAS("Normal Assassin", "普通刺客", SP.color),
    FAS("Fake Assassin", "冒牌刺客", SP.color),
    GCW("Gold Collar Worker", "金领工人", WF.color),
    WCW("White Collar Worker", "白领工人", WF.color),
    BCW("Blue Collar Worker", "蓝领工人", WF.color),
    KPS("Key Person", "关键人", GE.color),
    CMN("Common Man", "普通人", GE.color),
    PSB("Passer-by", "路人甲", GE.color),
    MAC("Major Character", "主要角色", GU.color),
    MIC("Minor Character", "次要角色", GU.color),
    FIG("Figurant", "群众演员", GU.color),
    SAM("Stable as Mountain", "稳如泰山", SU.color),
    HAS("Hard as Stone", "坚若磐石", SU.color),
    SIN("Seriously Injured", "伤痕累累", SU.color),
    ANI("Advanced Ninja", "上等忍者", SG.color),
    MNI("Mediocre Ninja", "普通忍者", SG.color),
    LCS("Lower-class", "不入流", SG.color),
    LKD("Lucky Dog", "幸运儿", NO.color),
    QAP("Qualified Apprentice", "合格学徒", NO.color),
    BGN("Beginner", "初学者", NO.color),
    LSS("Life-saving Straw", "救命稻草", FU.color),
    LSP("Little Spark", "点点星火", FU.color),
    BDT("Burnt Dust", "湮灭尘埃", FU.color),
    ;
    
    private enum ClassColor {
        BC(Color.makeRGB(255,241,0)),
        CA(Color.makeRGB(255,152,0)),
        MF(Color.makeRGB(34,172,56)),
        SP(Color.makeRGB(179,212,101)),
        WF(Color.makeRGB(0,104,183)),
        GE(Color.makeRGB(189,189,189)),
        GU(Color.makeRGB(0,160,233)),
        SU(Color.makeRGB(96,25,134)),
        SG(Color.makeRGB(228,0,127)),
        NO(Color.makeRGB(235,104,119)),
        FU(Color.makeRGB(211,47,47)),
        ;
        
        public final int color;

        ClassColor(int color) {
            this.color = color;
        }
    }

    public final String name;
    public final String nameCN;
    public final int color;

    PlayerClassification(String name, String nameCN, int color) {
        this.name = name;
        this.nameCN = nameCN;
        this.color = color;
    }

    /**
     * @param ERA_index 效果的排名，0是第一，1是倒数第一
     * @param DRA_index 输出的排名，0是第一，1是倒数第一
     * @return 第一版玩家分类
     */
    public PlayerClassification getPlayerClassificationV1(Integer ERA_index, Integer DRA_index){
        if (ERA_index < 1f/6) {
            if (DRA_index < 1f/6) {
                return PlayerClassification.BC;
            } else if (DRA_index < 2f/6) {
                return PlayerClassification.CA;
            } else if (DRA_index < 4f/6) {
                return PlayerClassification.MF;
            } else {
                return PlayerClassification.SP;
            }
        } else if (ERA_index < 2f/6) {
            if (DRA_index < 2f/6) {
                return PlayerClassification.CA;
            } else if (DRA_index < 4f/6) {
                return PlayerClassification.MF;
            } else {
                return PlayerClassification.SP;
            }
        } else if (ERA_index < 4f/6) {
            if (DRA_index < 2f/6) {
                return PlayerClassification.WF;
            } else if (DRA_index < 4f/6) {
                return PlayerClassification.GE;
            } else {
                return PlayerClassification.GU;
            }
        } else if (ERA_index < 5f/6) {
            if (DRA_index < 2f/6) {
                return PlayerClassification.SU;
            } else if (DRA_index < 4f/6) {
                return PlayerClassification.SG;
            } else {
                return PlayerClassification.NO;
            }
        } else {
            if (DRA_index < 2f/6) {
                return PlayerClassification.SU;
            } else if (DRA_index < 4f/6) {
                return PlayerClassification.SG;
            } else if (DRA_index < 5f/6) {
                return PlayerClassification.NO;
            } else {
                return PlayerClassification.FU;
            }

        }
    }

    /**
     * YMRA v3.5 更新: 平衡调整种类，比如，元帅属性大多数情况下，胜场多。
     * @param ERA_index 效果的排名，0是第一，1是倒数第一
     * @param DRA_index 输出的排名，0是第一，1是倒数第一
     * @param RWS_index 胜利分配的排名，0是第一，1是倒数第一
     * @return 第二版玩家分类
     */
    public PlayerClassification getPlayerClassificationV2(Integer ERA_index, Integer DRA_index, Integer RWS_index) {

        var pc = getPlayerClassificationV1(ERA_index, DRA_index);

        switch (pc) {
            case BC -> {
                if (RWS_index < 1f/9) return PlayerClassification.SMA;
                else if (RWS_index < 3f/9) return PlayerClassification.CMA;
                else return PlayerClassification.IMA;
            }
            case FU -> {
                if (RWS_index < 6f/9) return PlayerClassification.LSS;
                else if (RWS_index < 8f/9) return PlayerClassification.LSP;
                else return PlayerClassification.BDT;
            }
            case CA -> {
                if (RWS_index < 2f/9) return PlayerClassification.EGE;
                else if (RWS_index < 4f/9) return PlayerClassification.AGE;
                else return PlayerClassification.SGE;
            }
            case MF -> {
                if (RWS_index < 2f/9) return PlayerClassification.BMF;
                else if (RWS_index < 5f/9) return PlayerClassification.RMF;
                else return PlayerClassification.SMF;
            }
            case SP -> {
                if (RWS_index < 3f/9) return PlayerClassification.EAS;
                else if (RWS_index < 6f/9) return PlayerClassification.NAS;
                else return PlayerClassification.FAS;
            }
            case WF -> {
                if (RWS_index < 2f/9) return PlayerClassification.GCW;
                else if (RWS_index < 5f/9) return PlayerClassification.WCW;
                else return PlayerClassification.BCW;
            }
            case GE -> {
                if (RWS_index < 3f/9) return PlayerClassification.KPS;
                else if (RWS_index < 6f/9) return PlayerClassification.CMN;
                else return PlayerClassification.PSB;
            }
            case GU -> {
                if (RWS_index < 4f/9) return PlayerClassification.MAC;
                else if (RWS_index < 7f/9) return PlayerClassification.MIC;
                else return PlayerClassification.FIG;
            }
            case SU -> {
                if (RWS_index < 3f/9) return PlayerClassification.SAM;
                else if (RWS_index < 6f/9) return PlayerClassification.HAS;
                else return PlayerClassification.SIN;
            }
            case SG -> {
                if (RWS_index < 4f/9) return PlayerClassification.ANI;
                else if (RWS_index < 7f/9) return PlayerClassification.MNI;
                else return PlayerClassification.LCS;
            }
            case NO -> {
                if (RWS_index < 5f/9) return PlayerClassification.LKD;
                else if (RWS_index < 7f/9) return PlayerClassification.QAP;
                else return PlayerClassification.BGN;
            }
            case null, default -> {
                return PlayerClassification.CMN;
            }
        }
    }
}
