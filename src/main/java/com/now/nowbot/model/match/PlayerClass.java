package com.now.nowbot.model.match;

public class PlayerClass {
    String name;
    String nameCN;
    String color;

    public PlayerClass(double ERA_index, double DRA_index, double RWS_index) {
        var c = getPlayerClassEnumV2(ERA_index, DRA_index, RWS_index);

        this.name = c.name;
        this.nameCN = c.nameCN;
        this.color = c.color;
    }

    public PlayerClass(String name, String nameCN, String color) {
        this.name = name;
        this.nameCN = nameCN;
        this.color = color;
    }

    public enum ClassEnum {
        BC("Big Carry", "大爹", "#FFF100"),
        CA("Carry", "大哥", "#FF9800"),
        MF("Main Force", "主力", "#22AC38"),
        SP("Specialized", "专精", "#B3D465"),
        WF("Work Force", "打工", "#0068B7"),
        GE("General", "普通", "#BDBDBD"),
        GU("Guest", "客串", "#00A0E9"),
        SU("Support", "抗压", "#9922EE"),
        SG("Scapegoat", "背锅", "#E4007F"),
        NO("Noob", "小弟", "#EB6877"),
        FU("Futile", "炮灰", "#D32F2F"),

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

        public final String name;
        public final String nameCN;
        public final String color;

        ClassEnum(String name, String nameCN, String color) {
            this.name = name;
            this.nameCN = nameCN;
            this.color = color;
        }
    }
    /**
     * @param ERA_index 效果的排名，0是第一，1是倒数第一
     * @param DRA_index 输出的排名，0是第一，1是倒数第一
     * @return 第一版玩家分类
     */
    public static ClassEnum getPlayerClassEnumV1(double ERA_index, double DRA_index){
        if (ERA_index < 1f/6) {
            if (DRA_index < 1f/6) {
                return ClassEnum.BC;
            } else if (DRA_index < 2f/6) {
                return ClassEnum.CA;
            } else if (DRA_index < 4f/6) {
                return ClassEnum.MF;
            } else {
                return ClassEnum.SP;
            }
        } else if (ERA_index < 2f/6) {
            if (DRA_index < 2f/6) {
                return ClassEnum.CA;
            } else if (DRA_index < 4f/6) {
                return ClassEnum.MF;
            } else {
                return ClassEnum.SP;
            }
        } else if (ERA_index < 4f/6) {
            if (DRA_index < 2f/6) {
                return ClassEnum.WF;
            } else if (DRA_index < 4f/6) {
                return ClassEnum.GE;
            } else {
                return ClassEnum.GU;
            }
        } else if (ERA_index < 5f/6) {
            if (DRA_index < 2f/6) {
                return ClassEnum.SU;
            } else if (DRA_index < 4f/6) {
                return ClassEnum.SG;
            } else {
                return ClassEnum.NO;
            }
        } else {
            if (DRA_index < 2f/6) {
                return ClassEnum.SU;
            } else if (DRA_index < 4f/6) {
                return ClassEnum.SG;
            } else if (DRA_index < 5f/6) {
                return ClassEnum.NO;
            } else {
                return ClassEnum.FU;
            }

        }
    }

    /**
     * YMRA v3.5 更新: 平衡调整种类，比如，元帅属性大多数情况下，胜场多。
     * @param ERA_index 效果的排名，0是第一，1是倒数第一
     * @param DRA_index 输出的排名，0是第一，1是倒数第一
     * @param RWS_index 胜利分配的排名，0是第一，1是倒数第一
     * @return 第二版 Extended 玩家分类
     */
    public static ClassEnum getPlayerClassEnumV2(double ERA_index, double DRA_index, double RWS_index) {

        var pc = getPlayerClassEnumV1(ERA_index, DRA_index);

        switch (pc) {
            case BC -> {
                if (RWS_index < 1f/9) return ClassEnum.SMA;
                else if (RWS_index < 3f/9) return ClassEnum.CMA;
                else return ClassEnum.IMA;
            }
            case FU -> {
                if (RWS_index < 6f/9) return ClassEnum.LSS;
                else if (RWS_index < 8f/9) return ClassEnum.LSP;
                else return ClassEnum.BDT;
            }
            case CA -> {
                if (RWS_index < 2f/9) return ClassEnum.EGE;
                else if (RWS_index < 4f/9) return ClassEnum.AGE;
                else return ClassEnum.SGE;
            }
            case MF -> {
                if (RWS_index < 2f/9) return ClassEnum.BMF;
                else if (RWS_index < 5f/9) return ClassEnum.RMF;
                else return ClassEnum.SMF;
            }
            case SP -> {
                if (RWS_index < 3f/9) return ClassEnum.EAS;
                else if (RWS_index < 6f/9) return ClassEnum.NAS;
                else return ClassEnum.FAS;
            }
            case WF -> {
                if (RWS_index < 2f/9) return ClassEnum.GCW;
                else if (RWS_index < 5f/9) return ClassEnum.WCW;
                else return ClassEnum.BCW;
            }
            case GE -> {
                if (RWS_index < 3f/9) return ClassEnum.KPS;
                else if (RWS_index < 6f/9) return ClassEnum.CMN;
                else return ClassEnum.PSB;
            }
            case GU -> {
                if (RWS_index < 4f/9) return ClassEnum.MAC;
                else if (RWS_index < 7f/9) return ClassEnum.MIC;
                else return ClassEnum.FIG;
            }
            case SU -> {
                if (RWS_index < 3f/9) return ClassEnum.SAM;
                else if (RWS_index < 6f/9) return ClassEnum.HAS;
                else return ClassEnum.SIN;
            }
            case SG -> {
                if (RWS_index < 4f/9) return ClassEnum.ANI;
                else if (RWS_index < 7f/9) return ClassEnum.MNI;
                else return ClassEnum.LCS;
            }
            case NO -> {
                if (RWS_index < 5f/9) return ClassEnum.LKD;
                else if (RWS_index < 7f/9) return ClassEnum.QAP;
                else return ClassEnum.BGN;
            }
            case null, default -> {
                return ClassEnum.CMN;
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameCN() {
        return nameCN;
    }

    public void setNameCN(String nameCN) {
        this.nameCN = nameCN;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
