package com.now.nowbot.model.match

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class PlayerClass(
    val english: String,
    val chinese: String,
    val color: String,
    /**
     * 渐变色组，顺序为左 -> 右。
     */
    val colors: List<String>,
    val category: String = ""
) {
    BC("Big Carry", "大爹", "#FFF100", listOf("#FFFF00", "#FFC86B"), "BC"),
    CA("Carry", "大哥", "#FF9800", listOf("#FF9800", "#EC6841"), "CA"),
    MF("Main Force", "主力", "#22AC38", listOf("#12B4B1", "#31B16C"), "MF"),
    SP("Specialized", "专精", "#B3D465", listOf("#CAF881", "#29B126"), "SP"),
    WR("Worker", "打工", "#0068B7", listOf("#4FACFE", "#1D2088"), "WR"),
    OR("Ordinary", "普通", "#BDBDBD", listOf("#DDD", "#888"), "OR"),
    CO("Cameo", "客串", "#00A0E9", listOf("#4FACFE", "#7776FF"), "CO"),
    SU("Support", "抗压", "#9922EE", listOf("#F772D1", "#9922EE"), "SU"),
    LI("Liability", "累赘", "#E400A5", listOf("#EF66C9", "#890063"), "LI"),
    NO("Noob", "小弟", "#F48FB1", listOf("#F6A5C0", "#D84374"), "NO"),
    EB("Ember", "炮灰", "#D32F2F", listOf("#FF6868", "#B71C1C"), "EB"),

    SMA("Strongest Marshal", "最强元帅", BC.color, BC.colors, BC.category),
    CMA("Competent Marshal", "称职元帅", BC.color, BC.colors, BC.category),
    IMA("Indomitable Marshal", "不屈元帅", BC.color, BC.colors, BC.category),

    EGE("Ever-Victorious General", "常胜将军", CA.color, CA.colors, CA.category),
    AGE("Assiduous General", "勤奋将军", CA.color, CA.colors, CA.category),
    DGE("Dutiful General", "尽职将军", CA.color, CA.colors, CA.category),

    BMF("Breakthrough Main Force", "突破主力", MF.color, MF.colors, MF.category),
    RMF("Reliable Main Force", "可靠主力", MF.color, MF.colors, MF.category),
    SMF("Staunch Main Force", "坚守主力", MF.color, MF.colors, MF.category),

    EAS("Elite Assassin", "精锐刺客", SP.color, SP.colors, SP.category),
    AAS("Adept Assassin", "熟练刺客", SP.color, SP.colors, SP.category),
    FAS("Fake Assassin", "冒牌刺客", SP.color, SP.colors, SP.category),

    GCW("Gold Collar Worker", "金领工人", WR.color, WR.colors, WR.category),
    WCW("White Collar Worker", "白领工人", WR.color, WR.colors, WR.category),
    BCW("Blue Collar Worker", "蓝领工人", WR.color, WR.colors, WR.category),

    KPS("Key Person", "关键人", OR.color, OR.colors, OR.category),
    CMN("Common Man", "普通人", OR.color, OR.colors, OR.category),
    PSB("Passer-by", "路人甲", OR.color, OR.colors, OR.category),

    MAC("Major Character", "主要角色", CO.color, CO.colors, CO.category),
    MIC("Minor Character", "次要角色", CO.color, CO.colors, CO.category),
    CRA("Crowd Actor", "群众演员", CO.color, CO.colors, CO.category),

    SAM("Stable as Mountain", "稳如泰山", SU.color, SU.colors, SU.category),
    HAS("Hard as Stone", "坚若磐石", SU.color, SU.colors, SU.category),
    SES("Seriously Scarred", "伤痕累累", SU.color, SU.colors, SU.category),

    PCR("Pacemaker", "陪跑者", LI.color, LI.colors, LI.category),
    FRD("Free Rider", "搭便车", LI.color, LI.colors, LI.category),
    DWT("Deadweight", "拖油瓶", LI.color, LI.colors, LI.category),

    LKD("Lucky Dog", "幸运儿", NO.color, NO.colors, NO.category),
    SKK("Sidekick", "小跟班", NO.color, NO.colors, NO.category),
    GRH("Greenhorn", "愣头青", NO.color, NO.colors, NO.category),

    LSH("Last Hope", "救命稻草", EB.color, EB.colors, EB.category),
    LTS("Little Spark", "点点星火", EB.color, EB.colors, EB.category),
    SCD("Scorched Dust", "湮灭尘埃", EB.color, EB.colors, EB.category),

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
                    draIndex < 2.0 / 6 -> WR
                    draIndex < 4.0 / 6 -> OR
                    else -> CO
                }
                eraIndex < 5.0 / 6 -> when {
                    draIndex < 2.0 / 6 -> SU
                    draIndex < 4.0 / 6 -> LI
                    else -> NO
                }
                else -> when {
                    draIndex < 2.0 / 6 -> SU
                    draIndex < 4.0 / 6 -> LI
                    draIndex < 5.0 / 6 -> NO
                    else -> EB
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
                EB -> when {
                    rwsIndex < 6.0 / 9 -> LSH
                    rwsIndex < 8.0 / 9 -> LTS
                    else -> SCD
                }
                CA -> when {
                    rwsIndex < 2.0 / 9 -> EGE
                    rwsIndex < 4.0 / 9 -> AGE
                    else -> DGE
                }
                MF -> when {
                    rwsIndex < 2.0 / 9 -> BMF
                    rwsIndex < 5.0 / 9 -> RMF
                    else -> SMF
                }
                SP -> when {
                    rwsIndex < 3.0 / 9 -> EAS
                    rwsIndex < 6.0 / 9 -> AAS
                    else -> FAS
                }
                WR -> when {
                    rwsIndex < 2.0 / 9 -> GCW
                    rwsIndex < 5.0 / 9 -> WCW
                    else -> BCW
                }
                OR -> when {
                    rwsIndex < 3.0 / 9 -> KPS
                    rwsIndex < 6.0 / 9 -> CMN
                    else -> PSB
                }
                CO -> when {
                    rwsIndex < 4.0 / 9 -> MAC
                    rwsIndex < 7.0 / 9 -> MIC
                    else -> CRA
                }
                SU -> when {
                    rwsIndex < 3.0 / 9 -> SAM
                    rwsIndex < 6.0 / 9 -> HAS
                    else -> SES
                }
                LI -> when {
                    rwsIndex < 4.0 / 9 -> PCR
                    rwsIndex < 7.0 / 9 -> FRD
                    else -> DWT
                }
                NO -> when {
                    rwsIndex < 5.0 / 9 -> LKD
                    rwsIndex < 7.0 / 9 -> SKK
                    else -> GRH
                }
                else -> CMN
            }
        }
    }
}
