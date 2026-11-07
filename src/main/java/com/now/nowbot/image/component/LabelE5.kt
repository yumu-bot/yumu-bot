package com.now.nowbot.image.component

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Numbers
import kotlin.math.abs

data class LabelSpec(
    val iconTitle: String,
    val iconFile: String,
    val barColor: String,
    val barColors: List<String>,
    val barMin: Double,
    val barMid: Double,
    val barMax: Double,
)

data class LabelE5Data(
    val spec: LabelSpec,
    val remark: String,
    val dataMain: String,
    val dataMid: String,
    val dataTail: String,
    val barProgress: Double?,
    val icon: String?,
    val barMin: Double = spec.barMin,
    val barMid: Double = spec.barMid,
    val barMax: Double = spec.barMax,
)

object LabelCatalog {
    val bpm = LabelSpec("BPM", "object-score-beatsperminute.png", "#A864A8", ColorArray.purple, 90.0, 180.0, 270.0)
    val length = LabelSpec("Length", "object-score-length.png", "#F06EA9", ColorArray.peach, 30.0, 150.0, 270.0)
    val cs = LabelSpec("CS", "object-score-circlesize.png", "#00B7EE", ColorArray.cyan, 2.0, 4.0, 6.0)
    val key = LabelSpec("KEY", "object-score-circlesize.png", "#00B7EE", ColorArray.cyan, 2.0, 4.0, 6.0)
    val ar = LabelSpec("AR", "object-score-approachrate.png", "#7CC576", ColorArray.green, 7.5, 9.0, 10.5)
    val od = LabelSpec("OD", "object-score-overalldifficulty.png", "#FFF467", ColorArray.yellow, 6.0, 8.0, 10.0)
    val hp = LabelSpec("HP", "object-score-healthpoint.png", "#F26C4F", ColorArray.pink, 4.0, 6.0, 8.0)

    fun build(
        spec: LabelSpec,
        remark: String,
        dataMain: String,
        dataMid: String,
        dataTail: String,
        progress: Double,
        icon: String?,
    ): LabelE5Data {
        return LabelE5Data(spec, remark, dataMain, dataMid, dataTail, progress, icon)
    }

    fun stat(
        spec: LabelSpec,
        stat: Double,
        remark: String,
        progress: Double,
        original: Double,
        display: Boolean,
        icon: String?,
    ): LabelE5Data {
        if (!display) {
            return LabelE5Data(spec, "-", "-", "", "", null, icon)
        }
        val value = Numbers.roundParts(stat, 1)
        val changed = abs(original - stat) > 0.1
        return LabelE5Data(
            spec,
            remark,
            value.integer,
            value.decimal,
            if (changed) " [${Numbers.round(original, 1)}]" else "",
            progress,
            icon,
        )
    }
}

object LabelE5 {
    fun render(data: LabelE5Data): String {
        val labelWidth = 215.0
        val labelLeft = 61.0
        val progressWidth = labelWidth - labelLeft
        val numberColor = if (data.barProgress == null) "#666" else "#fff"

        val iconTitle = Fonts.poppinsBold.textPath(data.spec.iconTitle, 25.0, 65.0, 14.0, "center baseline", "#fff")
        val number = Typography.multipleTextPath(
            listOf(
                TextPart(data.dataMain, Fonts.poppinsBold, 36.0, numberColor),
                TextPart(data.dataMid + " ", Fonts.poppinsBold, 24.0, numberColor),
                TextPart(data.dataTail, Fonts.poppinsBold, 16.0, numberColor),
            ),
            labelLeft, 30.0, "left baseline",
        )
        val remark = Fonts.poppinsBold.textPath(data.remark, labelWidth, 30.0, 14.0, "right baseline", "#666")
        val barMin = Fonts.poppinsBold.textPath(Svg.number(data.barMin), labelLeft, 65.0, 14.0, "left baseline", "#666")
        val barMid = Fonts.poppinsBold.textPath(Svg.number(data.barMid), labelLeft + progressWidth / 2, 65.0, 14.0, "center baseline", "#666")
        val barMax = Fonts.poppinsBold.textPath(Svg.number(data.barMax), labelWidth, 65.0, 14.0, "right baseline", "#666")

        val progress = data.barProgress ?: 0.0
        val barWidth = Numbers.clampToInteger(progress * progressWidth, progressWidth, 10.0)
        val bar = if (data.spec.barColors.size == 2) {
            PanelDraw.linearGradientRect(labelLeft, 38.0, barWidth, 10.0, 5.0, data.spec.barColors, listOf(0, 100), listOf(0, 0), 1.0)
        } else {
            PanelDraw.rect(labelLeft, 38.0, barWidth, 10.0, 5.0, data.spec.barColor)
        }
        val barBase = if (data.spec.barColors.size == 2) {
            PanelDraw.linearGradientRect(labelLeft, 38.0, progressWidth, 10.0, 5.0, data.spec.barColors, listOf(0, 100), listOf(0, 0), 0.2)
        } else {
            PanelDraw.rect(labelLeft, 38.0, progressWidth, 10.0, 5.0, data.spec.barColor, 0.2)
        }

        val iconPath = data.icon?.let { Svg.image(0.0, 0.0, 50.0, 50.0, it) }.orEmpty()
        return iconPath + iconTitle + number + remark + barMin + barMid + barMax + barBase + bar
    }

    fun icon(assets: ImageAssets, spec: LabelSpec): String? = assets.local(spec.iconFile)
}
