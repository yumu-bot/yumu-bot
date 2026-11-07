package com.now.nowbot.image.component.t

import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.PanelColor

object PanelTComponents {
    fun labelT(iconTitle: String, abbr: String, dataB: String, barProgress: Double, barColor: String, maxWidth: Double, dark: Boolean = false): String {
        var svg = "<g id=\"Text_T\"></g>"
        val tooShort = maxWidth - Fonts.poppinsBold.width(iconTitle, 16.0) < 30.0
        val title = Fonts.poppinsBold.cutStringTail(if (tooShort) abbr else iconTitle, 16.0, maxWidth - 20.0 - 10.0)
        val circle = PanelDraw.circle(18.0, 15.0, 8.0, barColor)
        val icon = Fonts.poppinsBold.textPath(title, 36.0, 20.0, 16.0, "left baseline", "#fff")
        val number = Fonts.poppinsBold.textPath(dataB, maxWidth - 10.0, 20.0, 16.0, "right baseline", barColor)
        val width = Numbers.clampToInteger(barProgress * maxWidth, maxWidth, 30.0)
        val bar = PanelDraw.rect(0.0, 0.0, width, 30.0, 15.0, barColor, 0.2)
        val base = PanelDraw.rect(0.0, 0.0, maxWidth, 30.0, 15.0, if (dark) PanelColor.bright() else PanelColor.top(), 1.0)
        svg = Svg.setTexts(svg, listOf(circle, icon, number, bar, base), "<g id=\"Text_T\">")
        return svg
    }

    fun pie(cx: Double, cy: Double, radius: Double, start: Double, end: Double, color: String): String {
        if (end <= start) return ""
        val a0 = start * 2 * Math.PI - Math.PI / 2
        val a1 = end * 2 * Math.PI - Math.PI / 2
        val x0 = cx + radius * Math.cos(a0)
        val y0 = cy + radius * Math.sin(a0)
        val x1 = cx + radius * Math.cos(a1)
        val y1 = cy + radius * Math.sin(a1)
        val large = if (end - start > 0.5) 1 else 0
        return "<path d=\"M ${Svg.number(cx)},${Svg.number(cy)} L ${Svg.number(x0)},${Svg.number(y0)} A ${Svg.number(radius)},${Svg.number(radius)} 0 $large 1 ${Svg.number(x1)},${Svg.number(y1)} Z\" style=\"fill:${Svg.escape(color)};\"/>"
    }
}
