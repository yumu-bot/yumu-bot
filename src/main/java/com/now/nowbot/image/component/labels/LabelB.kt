package com.now.nowbot.image.component.labels

import com.now.nowbot.image.util.ColorArray

data class LabelB(
    val iconFile: String?,
    val title: String,
    val left1: String = "",
    val left2: String = "",
    val min: Double = 0.0,
    val limit: Double = 10.0,
    val max: Double = 20.0,
    val colors: List<String> = ColorArray.gray,
)

object LabelPPM6 {
    val ACC = LabelB("Icons/arrows-to-dot-solid-full.svg", "ACC", "Accuracy", "准度", 60.0, 100.0, 101.0, ColorArray.green)
    val PTT = LabelB("Icons/arrow-trend-up-solid-full.svg", "PTT", "Potential", "潜力", 60.0, 100.0, 101.0, ColorArray.blue)
    val STA = LabelB("Icons/heart-circle-bolt-solid-full.svg", "STA", "Stamina", "耐力", 60.0, 100.0, 101.0, ColorArray.yellow)
    val STB = LabelB("Icons/anchor-solid-full.svg", "STB", "Stability", "稳定", 60.0, 100.0, 101.0, ColorArray.purple)
    val PRE = LabelB("Icons/bullseye-solid-full.svg", "PRE", "Precision", "彩率", 60.0, 100.0, 101.0, ColorArray.purple)
    val EFT = LabelB("Icons/street-view-solid-full.svg", "EFT", "Effort", "肝力", 60.0, 100.0, 101.0, ColorArray.red)
    val STH = LabelB("Icons/weight-hanging-solid-full.svg", "STH", "Strength", "强度", 60.0, 100.0, 101.0, ColorArray.amber)
    val OVA = LabelB("Icons/clipboard-list-solid-full.svg", "ALL", "Overall", "综合", 60.0, 100.0, 101.0, ColorArray.sakura)
    val OII = LabelB("Icons/dna-solid-full.svg", "OII", "O. Improvement I.", "成长指数", 0.0, 3.0, 6.0, ColorArray.pink)
    val SAN = LabelB("Icons/dna-solid-full.svg", "SAN", "Sanity", "理智", 0.0, 100.0, 120.0, ColorArray.pink)

    private val byAbbr = mapOf(
        "ACC" to ACC, "PTT" to PTT, "STA" to STA, "STB" to STB,
        "PRE" to PRE, "EFT" to EFT, "STH" to STH, "OVA" to OVA,
        "OII" to OII, "SAN" to SAN,
    )

    fun of(abbr: String): LabelB = byAbbr[abbr] ?: LabelB(null, "")
}

object LabelMM {
    val RC = LabelB("Icons/bowl-rice-solid-full.svg", "RC", "Rice", "米", 0.0, 10.0, 20.0, ColorArray.green)
    val LN = LabelB("Icons/chart-simple-solid-full.svg", "LN", "Long Note", "面", 0.0, 10.0, 20.0, ColorArray.blue)
    val CO = LabelB("Icons/hand-spock-solid-full.svg", "CO", "Coordinate", "协调", 0.0, 10.0, 20.0, ColorArray.yellow)
    val ST = LabelB("Icons/heart-circle-bolt-solid-full.svg", "ST", "Stamina", "耐力", 0.0, 10.0, 20.0, ColorArray.amber)
    val SP = LabelB("Icons/gauge-high-solid-full.svg", "SP", "Speed", "速度", 0.0, 10.0, 20.0, ColorArray.red)
    val PR = LabelB("Icons/bullseye-solid-full.svg", "PR", "Precision", "彩率", 0.0, 10.0, 20.0, ColorArray.purple)
    val SV = LabelB("Icons/spiral-solid-full.svg", "SV", "S.Variation", "变速", 0.0, 10.0, 20.0, ColorArray.pink)
    val DN = LabelB("Icons/school-solid-full.svg", "DN", "Dan Level", "段位", 0.0, 10.0, 20.0, ColorArray.sakura)
    val RF = LabelB("Icons/school-solid-full.svg", "RF", "Reform Dan", "常规段", 0.0, 10.0, 18.0, ColorArray.rhodium)
    val UJ = LabelB("Icons/face-grin-squint-regular-full.svg", "UJ", "Underjoy Dan", "长键段", 0.0, 10.0, 13.0, ColorArray.platinum)
    val RG = LabelB("Icons/water-solid-full.svg", "RG", "J.'s Regular Dan", "常规段", 0.0, 10.0, 14.0, ColorArray.rhodium)
    val JL = LabelB("Icons/water-ladder-solid-full.svg", "JL", "Jinjin's Ln Dan", "长键段", 0.0, 10.0, 14.0, ColorArray.platinum)

    private val byAbbr = mapOf(
        "RC" to RC, "LN" to LN, "CO" to CO, "ST" to ST,
        "SP" to SP, "PR" to PR, "SV" to SV, "DN" to DN,
        "RF" to RF, "UJ" to UJ, "RG" to RG, "JL" to JL,
    )

    fun of(abbr: String): LabelB = byAbbr[abbr] ?: LabelB(null, "")
}

object LabelPPP {
    val JUMP = LabelB("Icons/road-barrier-solid-full.svg", "JP", "Jump", "跳", 0.0, 5800.0, 5800.0 * 1.5, ColorArray.green)
    val FLOW = LabelB("Icons/wind-solid-full.svg", "FL", "Flow", "串", 0.0, 1400.0, 1400.0 * 1.5, ColorArray.blue)
    val ACC = LabelB("Icons/arrows-to-dot-solid-full.svg", "AC", "Accuracy", "准度", 0.0, 3200.0, 3200.0 * 1.5, ColorArray.purple)
    val STA = LabelB("Icons/heart-circle-bolt-solid-full.svg", "ST", "Stamina", "耐力", 0.0, 2800.0, 2800.0 * 1.5, ColorArray.red)
    val SPD = LabelB("Icons/gauge-high-solid-full.svg", "SP", "Speed", "速度", 0.0, 3800.0, 3800.0 * 1.5, ColorArray.amber)
    val PRE = LabelB("Icons/crosshairs-solid-full.svg", "PR", "Precision", "小圈", 0.0, 1200.0, 1200.0 * 1.5, ColorArray.yellow)
    val OVA = LabelB("Icons/clipboard-list-solid-full.svg", "ALL", "Overall", "综合", 0.0, 10000.0, 15000.0, ColorArray.sakura)
    val AIM = LabelB("Icons/marker-solid-full.svg", "AIM", "Aim", "移动", 0.0, 5800.0, 5800.0 * 1.5, ColorArray.pink)

    private val byAbbr = mapOf(
        "JUMP" to JUMP, "FLOW" to FLOW, "ACC" to ACC, "STA" to STA,
        "SPD" to SPD, "PRE" to PRE, "OVA" to OVA, "AIM" to AIM,
    )

    fun of(abbr: String): LabelB = byAbbr[abbr] ?: LabelB(null, "")
}

object LabelETX {
    val SR = LabelB("Mods/td.png", "DR", "Duel Rating", "星数", 4.0, 7.5, 9.0, ColorArray.orange)
    val NM = LabelB("Mods/nm.png", "NM", "No mod", "无模", 4.0, 7.5, 9.0, ColorArray.green)
    val HD = LabelB("Mods/hd.png", "HD", "Hidden", "隐藏", 4.0, 7.5, 9.0, ColorArray.yellow)
    val HR = LabelB("Mods/hr.png", "HR", "Hard Rock", "硬摇", 4.0, 7.5, 9.0, ColorArray.red)
    val DT = LabelB("Mods/dt.png", "DT", "Double Time", "倍速", 4.0, 7.5, 9.0, ColorArray.blue)
    val FM = LabelB("Mods/fi.png", "FM", "Force Mod", "强模", 4.0, 7.5, 9.0, ColorArray.amber)
    val EF = LabelB("Mods/cs.png", "EF", "Effective", "有效", 0.0, 1.0, 1.0, ColorArray.pink)
    val PE = LabelB("Mods/cl.png", "PE", "Permanent", "永久", 0.0, 1.0, 1.0, ColorArray.platinum)

    private val byAbbr = mapOf(
        "SR" to SR, "NM" to NM, "HD" to HD, "HR" to HR,
        "DT" to DT, "FM" to FM, "EF" to EF, "PE" to PE,
    )

    fun of(abbr: String): LabelB = byAbbr[abbr] ?: LabelB(null, "")
}
