package com.now.nowbot.image.maimai

import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.TimeFormat
import kotlin.math.ceil

data class MaiLabel(
    val width: Double,
    val svg: String,
)

object MaiDraw {
    private const val VERSION = "v0.8.5"
    private const val VERSION_CODE = "VS"
    private const val POWERED_BY = "Yumubot"

    /**
     * 原 card_A3.label_A8。imageSlot 用于区分“没有图片”和“图片缺失”：
     * 原版图片路径即使不存在也是非空字符串，因此仍保留 22px 的图标位。
     */
    fun labelA8(
        text: Any?,
        image: String?,
        hasText: Boolean?,
        color: String?,
        isHighlight: Boolean = true,
        imageSlot: Boolean = false,
    ): MaiLabel {
        var width = Fonts.torusBold.width(text, 20.0) + 16.0
        var textMiddle = width / 2.0

        val opacity = if (isHighlight) 1.0 else 0.2
        val textOpacity = if (isHighlight) 1.0 else 0.4
        val baseOpacity = if (isHighlight) 0.6 else 0.2
        val baseColor = color?.takeIf { it.isNotEmpty() } ?: "none"

        val icon = when {
            imageSlot && image != null && image.isNotBlank() -> {
                width += 22.0
                textMiddle += 22.0
                Svg.image(6.0, 3.5, 18.0, 18.0, image, 1.0)
            }
            imageSlot -> {
                width += 22.0
                textMiddle += 22.0
                ""
            }
            hasText == true -> {
                width += 12.0
                textMiddle += 12.0
                Svg.rect(0.0, 0.0, 14.0, 25.0, 7.0, baseColor, opacity)
            }
            hasText == false -> {
                val rect = Svg.rect(0.0, 0.0, 14.0, 25.0, 7.0, baseColor, opacity)
                return MaiLabel(14.0, labelBody("", "", rect))
            }
            else -> ""
        }

        val textPath = Fonts.torusBold.textPath(
            text,
            textMiddle,
            19.0,
            20.0,
            "center baseline",
            "#fff",
            textOpacity,
        )
        val base = Svg.rect(0.0, 0.0, width, 25.0, 7.0, baseColor, baseOpacity)
        return MaiLabel(width, labelBody(base, textPath, icon))
    }

    fun panelName(
        name: String,
        index: String,
        requestTime: String = "request time: ${TimeFormat.nowTimestamp()}",
    ): String {
        val powered = Fonts.torus.textPath(
            "powered by $POWERED_BY $VERSION $VERSION_CODE // $name",
            20.0, 26.84, 24.0, "left baseline", "#fff",
        )
        val request = Fonts.torus.textPath(
            requestTime,
            1900.0, 26.84, 24.0, "right baseline", "#fff",
        )
        val sign = Fonts.torus.textPath(
            index,
            607.5, 83.67, 48.0, "center baseline", "#fff",
        )
        return powered + "\n" + request + "\n" + sign
    }

    fun panelHeight(
        cardCount: Int,
        cardHeight: Double = 210.0,
        cardPerRow: Int = 4,
        bannerHeight: Double = 290.0,
        interval: Double = 20.0,
        spacing: Double = 40.0,
    ): Double {
        if (cardPerRow == 0) return bannerHeight + spacing
        val row = ceil(cardCount.toDouble() / cardPerRow).toInt()
        if (row < 0) return 1080.0
        return (bannerHeight + 2 * spacing) + (cardHeight + interval) * row - interval
    }

    private fun labelBody(base: String, text: String, icon: String): String {
        return "<g id=\"Base_LA8\">$base</g>" +
            "<g id=\"Text_LA8\">$text</g>" +
            "<g id=\"Icon_LA8\">$icon</g>"
    }
}
