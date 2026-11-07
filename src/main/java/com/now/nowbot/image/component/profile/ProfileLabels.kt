package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.font.TextPart
import com.now.nowbot.image.font.Typography
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Texts

data class ProfileLabelData(
    val icon: String? = null,
    val iconTitle: String = "",
    val abbr: String = "",
    val dataMain: String = "",
    val dataMid: String = "",
    val remark: String = "",
    val colorRemark: String = "#aaa",
    val barColor: String? = null,
)

object ProfileLabels {
    fun d1(data: ProfileLabelData): String {
        var svg = "<g id=\"Icon_LD1\"></g><g id=\"Text_LD1\"></g>"
        val iconTitle = Fonts.torus.textPath(data.iconTitle, 145.0, 68.88, 18.0, "right baseline", "#ccc")
        val abbr = Fonts.torus.textPath(data.abbr, 180.0, 110.0, 36.0, "center baseline", "#fff")
        val number = Fonts.torus.get2SizeTextPath(data.dataMain, data.dataMid, 48.0, 36.0, 177.5, 154.88, "center baseline", "#fff")
        if (data.remark.isNotEmpty()) {
            svg = Svg.setText(svg, Fonts.torus.textPath(data.remark, 210.0, 68.88, 18.0, "left baseline", data.colorRemark), "<g id=\"Text_LD1\">")
        }
        svg = Svg.setTexts(svg, listOf(abbr, iconTitle, number), "<g id=\"Text_LD1\">")
        svg = Svg.setText(svg, data.icon?.let { Svg.image(142.5, 0.0, 74.0, 74.0, it) }, "<g id=\"Icon_LD1\">")
        return svg
    }

    fun d2(data: ProfileLabelData): String {
        var svg = "<g id=\"Icon_LD2\"></g><g id=\"Text_LD2\"></g>"
        val iconTitle = Fonts.torus.textPath(data.iconTitle, 56.0, 14.88, 18.0, "left baseline", "#aaa")
        val abbr = Fonts.torus.textPath(data.abbr, 420.0, 44.75, 24.0, "right baseline", "#aaa")
        val number = Fonts.torus.get2SizeTextPath(data.dataMain, data.dataMid, 36.0, 24.0, 56.0, 44.75, "left baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(abbr, iconTitle, number), "<g id=\"Text_LD2\">")
        if (data.remark.isNotEmpty()) {
            svg = Svg.setText(svg, Fonts.torus.textPath(data.remark, 420.0, 14.88, 18.0, "right baseline", data.colorRemark), "<g id=\"Text_LD2\">")
        }
        svg = Svg.setText(svg, data.icon?.let { Svg.image(0.0, 0.0, 50.0, 50.0, it) }, "<g id=\"Icon_LD2\">")
        return svg
    }

    fun d3(data: ProfileLabelData, delta: String = "", deltaColor: String = "none"): String {
        var svg = "<g id=\"Icon_LD3\"></g><g id=\"Text_LD3\"></g>"
        val text = Fonts.poppinsBold.textPath(data.dataMain, 42.0, 32.0, 18.0, "left baseline", "#fff")
        val deltaPath = Fonts.poppinsBold.textPath(delta, 42.0, 14.0, 14.0, "left baseline", deltaColor)
        svg = Svg.setTexts(svg, listOf(text, deltaPath), "<g id=\"Text_LD3\">")
        svg = Svg.setText(svg, data.icon?.let { Svg.image(5.0, 5.0, 40.0, 40.0, it) }, "<g id=\"Icon_LD3\">")
        return svg
    }

    fun d4(data: ProfileLabelData, delta: String = "", deltaColor: String = "none"): String {
        var svg = "<g id=\"Icon_LD4\"></g><g id=\"Text_LD4\"></g>"
        val title = Typography.multipleTextPath(
            listOf(
                TextPart(data.iconTitle + " ", if (Texts.isAscii(data.iconTitle)) Fonts.poppinsBold else Fonts.puHuiTi, if (Texts.isAscii(data.iconTitle)) 18.0 else 16.0, "#bbb"),
                TextPart(data.remark, if (Texts.isAscii(data.remark)) Fonts.poppinsBold else Fonts.puHuiTi, if (Texts.isAscii(data.remark)) 18.0 else 16.0, "#bbb"),
            ),
            450.0, 50.0, "right baseline",
        )
        val abbr = (if (Texts.isAscii(data.abbr)) Fonts.poppinsBold else Fonts.puHuiTi)
            .textPath(data.abbr, 75.0, 50.0, if (Texts.isAscii(data.abbr)) 18.0 else 16.0, "left baseline", "#bbb")
        val number = Fonts.poppinsBold.get2SizeTextPath(data.dataMain, data.dataMid, 40.0, 30.0, 75.0, 30.0, "left baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(abbr, title, number), "<g id=\"Text_LD4\">")
        if (data.barColor != null) {
            svg = Svg.setText(svg, PanelDraw.rect(60.0, 0.0, 4.0, 50.0, 2.0, data.barColor), "<g id=\"Icon_LD4\">")
        }
        if (delta.isNotEmpty()) {
            svg = Svg.setText(svg, Fonts.poppinsBold.textPath(delta, 75.0, -4.0, 18.0, "left baseline", deltaColor, 1.0), "<g id=\"Text_LD4\">")
        }
        svg = Svg.setText(svg, data.icon?.let { Svg.image(0.0, 0.0, 50.0, 50.0, it) }, "<g id=\"Icon_LD4\">")
        return svg
    }

    fun d5(data: ProfileLabelData, delta: String = "", deltaColor: String = "none"): String {
        var svg = "<g id=\"Icon_LD5\"></g><g id=\"Text_LD5\"></g>"
        val remark = (if (Texts.isAscii(data.remark)) Fonts.poppinsBold else Fonts.puHuiTi)
            .textPath(data.remark, 215.0, 50.0, if (Texts.isAscii(data.remark)) 18.0 else 16.0, "right baseline", "#bbb")
        val abbr = (if (Texts.isAscii(data.abbr)) Fonts.poppinsBold else Fonts.puHuiTi)
            .textPath(data.abbr, 75.0, 50.0, if (Texts.isAscii(data.abbr)) 18.0 else 16.0, "left baseline", "#bbb")
        val number = Fonts.poppinsBold.get2SizeTextPath(data.dataMain, data.dataMid, 40.0, 30.0, 75.0, 30.0, "left baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(abbr, remark, number), "<g id=\"Text_LD5\">")
        if (data.barColor != null) {
            svg = Svg.setText(svg, PanelDraw.rect(60.0, 0.0, 4.0, 50.0, 2.0, data.barColor), "<g id=\"Icon_LD5\">")
        }
        if (delta.isNotEmpty()) {
            svg = Svg.setText(svg, Fonts.poppinsBold.textPath(delta, 75.0, -4.0, 18.0, "left baseline", deltaColor, 1.0), "<g id=\"Text_LD5\">")
        }
        svg = Svg.setText(svg, data.icon?.let { Svg.image(0.0, 0.0, 50.0, 50.0, it) }, "<g id=\"Icon_LD5\">")
        return svg
    }

    fun e(data: ProfileLabelData): String {
        var svg = "<g id=\"Icon_LE\"></g><g id=\"Text_LE\"></g>"
        val iconTitle = Fonts.torus.textPath(data.iconTitle, 56.0, 14.88, 18.0, "left baseline", "#aaa")
        val number = Fonts.torus.get2SizeTextPath(data.dataMain, data.dataMid, 36.0, 24.0, 56.0, 46.0, "left baseline", "#fff")
        svg = Svg.setTexts(svg, listOf(iconTitle, number), "<g id=\"Text_LE\">")
        if (data.remark.isNotEmpty()) {
            svg = Svg.setText(svg, Fonts.torus.textPath(data.remark, 200.0, 14.88, 18.0, "right baseline", data.colorRemark), "<g id=\"Text_LE\">")
        }
        svg = Svg.setText(svg, data.icon?.let { Svg.image(0.0, 0.0, 50.0, 50.0, it) }, "<g id=\"Icon_LE\">")
        return svg
    }
}
