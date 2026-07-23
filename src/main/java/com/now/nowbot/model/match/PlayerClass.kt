package com.now.nowbot.model.match

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class PlayerClass(
    val english: String,
    val chinese: String,
    val color: String,
    val category: String = ""
) {
    BC("Big Carry", "大爹", "#FFF100", "BC"),
    CA("Carry", "大哥", "#FF9800", "CA"),
    MF("Main Force", "主力", "#22AC38", "MF"),
    SP("Specialized", "专精", "#B3D465", "SP"),
    WF("Work Force", "打工", "#0068B7", "WF"),
    GE("General", "普通", "#BDBDBD", "GE"),
    GU("Guest", "客串", "#00A0E9", "GU"),
    SU("Support", "抗压", "#9922EE", "SU"),
    SG("Scapegoat", "背锅", "#E4007F", "SG"),
    NO("Noob", "小弟", "#EB6877", "NO"),
    FU("Futile", "炮灰", "#D32F2F", "FU"),

    SMA("Strongest Marshal", "最强元帅", BC.color, BC.category),
    CMA("Competent Marshal", "称职元帅", BC.color, BC.category),
    IMA("Indomitable Marshal", "不屈元帅", BC.color, BC.category),

    EGE("Ever-Victorious General", "常胜将军", CA.color, CA.category),
    AGE("Assiduous General", "勤奋将军", CA.color, CA.category),
    SGE("Striven General", "尽力将军", CA.color, CA.category),

    BMF("Breakthrough Main Force", "突破主力", MF.color, MF.category),
    RMF("Reliable Main Force", "可靠主力", MF.color, MF.category),
    SMF("Staunch Main Force", "坚守主力", MF.color, MF.category),

    EAS("Elite Assassin", "精锐刺客", SP.color, SP.category),
    NAS("Normal Assassin", "普通刺客", SP.color, SP.category),
    FAS("Fake Assassin", "冒牌刺客", SP.color, SP.category),

    GCW("Gold Collar Worker", "金领工人", WF.color, WF.category),
    WCW("White Collar Worker", "白领工人", WF.color, WF.category),
    BCW("Blue Collar Worker", "蓝领工人", WF.color, WF.category),

    KPS("Key Person", "关键人", GE.color, GE.category),
    CMN("Common Man", "普通人", GE.color, GE.category),
    PSB("Passer-by", "路人甲", GE.color, GE.category),

    MAC("Major Character", "主要角色", GU.color, GU.category),
    MIC("Minor Character", "次要角色", GU.color, GU.category),
    FIG("Figurant", "群众演员", GU.color, GU.category),

    SAM("Stable as Mountain", "稳如泰山", SU.color, SU.category),
    HAS("Hard as Stone", "坚若磐石", SU.color, SU.category),
    SIN("Seriously Injured", "伤痕累累", SU.color, SU.category),

    ANI("Advanced Ninja", "上等忍者", SG.color, SG.category),
    MNI("Mediocre Ninja", "普通忍者", SG.color, SG.category),
    LCS("Lower-class", "不入流", SG.color, SG.category),

    LKD("Lucky Dog", "幸运儿", NO.color, NO.category),
    QAP("Qualified Apprentice", "合格学徒", NO.color, NO.category),
    BGN("Beginner", "初学者", NO.color, NO.category),

    LSS("Life-saving Straw", "救命稻草", FU.color, FU.category),
    LSP("Little Spark", "点点星火", FU.color, FU.category),
    BDT("Burnt Dust", "湮灭尘埃", FU.color, FU.category),

    ;

    companion object {
        /**
         * 第一版分类计算：基于效果排名与输出排名
         */
        fun getV1(eraIndex: Double, draIndex: Double): PlayerClass {
            return when {
                eraIndex < 1.0 / 6 -> when {
                    draIndex < 1.0 / 6 -> BC
                    draIndex < 2.0 / 6 -> CA
                    draIndex < 4.0 / 6 -> MF
                    else -> SP
                }
                eraIndex < 2.0 / 6 -> when {
                    draIndex < 2.0 / 6 -> CA
                    draIndex < 4.0 / 6 -> MF
                    else -> SP
                }
                eraIndex < 4.0 / 6 -> when {
                    draIndex < 2.0 / 6 -> WF
                    draIndex < 4.0 / 6 -> GE
                    else -> GU
                }
                eraIndex < 5.0 / 6 -> when {
                    draIndex < 2.0 / 6 -> SU
                    draIndex < 4.0 / 6 -> SG
                    else -> NO
                }
                else -> when {
                    draIndex < 2.0 / 6 -> SU
                    draIndex < 4.0 / 6 -> SG
                    draIndex < 5.0 / 6 -> NO
                    else -> FU
                }
            }
        }

        /**
         * 第二版分类计算：引入胜率贡献排名 (RWS)
         */
        fun getV2(eraIndex: Double, draIndex: Double, rwsIndex: Double): PlayerClass {
            val baseClass = getV1(eraIndex, draIndex)

            return when (baseClass) {
                BC -> when {
                    rwsIndex < 1.0 / 9 -> SMA
                    rwsIndex < 3.0 / 9 -> CMA
                    else -> IMA
                }
                FU -> when {
                    rwsIndex < 6.0 / 9 -> LSS
                    rwsIndex < 8.0 / 9 -> LSP
                    else -> BDT
                }
                CA -> when {
                    rwsIndex < 2.0 / 9 -> EGE
                    rwsIndex < 4.0 / 9 -> AGE
                    else -> SGE
                }
                MF -> when {
                    rwsIndex < 2.0 / 9 -> BMF
                    rwsIndex < 5.0 / 9 -> RMF
                    else -> SMF
                }
                SP -> when {
                    rwsIndex < 3.0 / 9 -> EAS
                    rwsIndex < 6.0 / 9 -> NAS
                    else -> FAS
                }
                WF -> when {
                    rwsIndex < 2.0 / 9 -> GCW
                    rwsIndex < 5.0 / 9 -> WCW
                    else -> BCW
                }
                GE -> when {
                    rwsIndex < 3.0 / 9 -> KPS
                    rwsIndex < 6.0 / 9 -> CMN
                    else -> PSB
                }
                GU -> when {
                    rwsIndex < 4.0 / 9 -> MAC
                    rwsIndex < 7.0 / 9 -> MIC
                    else -> FIG
                }
                SU -> when {
                    rwsIndex < 3.0 / 9 -> SAM
                    rwsIndex < 6.0 / 9 -> HAS
                    else -> SIN
                }
                SG -> when {
                    rwsIndex < 4.0 / 9 -> ANI
                    rwsIndex < 7.0 / 9 -> MNI
                    else -> LCS
                }
                NO -> when {
                    rwsIndex < 5.0 / 9 -> LKD
                    rwsIndex < 7.0 / 9 -> QAP
                    else -> BGN
                }
                else -> CMN
            }
        }
    }
}