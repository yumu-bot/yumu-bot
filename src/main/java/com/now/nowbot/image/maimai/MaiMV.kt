package com.now.nowbot.image.maimai

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.service.messageServiceImpl.MaiVersionScoreService
import kotlin.math.floor
import kotlin.math.max

object MaiMV {
    private const val STEP_X = 155.0
    private const val STEP_Y = 146.0

    fun card(progress: MaiVersionScoreService.MaiPlateProgress, assets: ImageAssets): String {
        val mai = MaiAssets(assets)
        val score: MaiScore? = progress.score
        val left1Text = progress.songID.toString()
        val left2Text = Svg.number(progress.star)
        val left1Colors = if (progress.songID >= 10000) ColorArray.amber else ColorArray.deepBlue
        val left2Colors = MaiAssets.difficultyColors(progress.index.toInt(), false)
        val left2TextColor = if (progress.index >= 4) "#1C1719" else "#fff"
        val titleText = progress.title
        val titleFont = if (Texts.isAscii(titleText)) Fonts.torusBold else Fonts.puHuiTi
        val title = titleFont.textPath(
            titleFont.cutStringTail(titleText, 15.0, 130.0),
            67.5, 126.0, if (Texts.isAscii(titleText)) 16.0 else 15.0,
            "center baseline",
        )

        val left1Width = Fonts.torusBold.width(left1Text, 16.0)
        val left1RrectWidth = max(40.0, left1Width + 10.0)
        val left1 = Fonts.torusBold.textPath(
            left1Text, -5.0 + left1RrectWidth / 2.0, 10.0, 16.0, "center baseline", "#fff",
        )
        val left1Rrect = PanelDraw.linearGradientRect(
            -5.0, -5.0, left1RrectWidth, 20.0, 10.0, left1Colors,
            listOf(100, 0), listOf(80, 20), 1.0,
        )
        val left2Width = Fonts.torusBold.width(left2Text, 16.0)
        val left2RrectWidth = max(40.0, left2Width + 10.0)
        val left2 = Fonts.torusBold.textPath(
            left2Text, -5.0 + left2RrectWidth / 2.0, 35.0, 16.0, "center baseline", left2TextColor,
        )
        val left2Rrect = PanelDraw.linearGradientRect(
            -5.0, 20.0, left2RrectWidth, 20.0, 10.0, left2Colors,
            listOf(100, 0), listOf(80, 20), 1.0,
        )

        val rank = score?.rank.orEmpty()
        val rankBackground = mai.rankBackground(rank)
        val cover = mai.cover(progress.songID.toLong())
        val overlay = requiredOverlay(progress.required, rank, score?.combo.orEmpty(), score?.sync.orEmpty(), assets)
        val completedOpacity = if (progress.completed) 0.2 else 1.0

        return buildString {
            append("<defs>")
            append("<clipPath id=\"clippath-CV-1\"><rect width=\"135\" height=\"126\" rx=\"10\" ry=\"10\" style=\"fill: none;\"/></clipPath>")
            append("<clipPath id=\"clippath-CV-2\"><rect x=\"-5\" y=\"111\" width=\"145\" height=\"20\" rx=\"10\" ry=\"10\" style=\"fill: none;\"/></clipPath>")
            append("</defs>")
            append("<g id=\"Base-CV\"><rect width=\"135\" height=\"126\" rx=\"10\" ry=\"10\" style=\"fill: #382E32;\"/></g>")
            append("<g id=\"Background-CV\"><g style=\"clip-path: url(#clippath-CV-1);\">")
            append(Svg.image(0.0, -5.0, 135.0, 135.0, cover.orEmpty(), completedOpacity))
            append("</g></g>")
            append("<g id=\"Rank-CV\"><rect x=\"-5\" y=\"111\" width=\"145\" height=\"20\" rx=\"10\" ry=\"10\" style=\"fill: #46393f;\"/>")
            append("<g style=\"clip-path: url(#clippath-CV-2);\">")
            append(Svg.image(-5.0, 92.0, 145.0, 60.0, rankBackground.orEmpty(), 0.6))
            append("</g></g>")
            append("<g id=\"Overlay-CV\">")
            append(overlay)
            append("</g>")
            append("<g id=\"Text-CV\">")
            append(title)
            append(left1)
            append(left1Rrect)
            append(left2)
            append(left2Rrect)
            append("</g>")
        }
    }

    fun label(group: MaiVersionScoreService.MaiPlateList): String {
        val levelText = group.star.ifBlank { "??" }
        val level = Fonts.berlinBold.textPath(levelText, 65.0, 51.0, 48.0, "center baseline", "#1C1719") +
            Fonts.berlinBold.textPath(levelText, 64.0, 50.0, 48.0, "center baseline", "#fff")
        val ratioText = "${group.finished} / ${group.count}"
        val ratio = Fonts.poppinsBold.textPath(ratioText, 65.0, 81.0, 16.0, "center baseline", "#1C1719") +
            Fonts.poppinsBold.textPath(ratioText, 64.0, 80.0, 16.0, "center baseline", "#fff")
        val percentValue = if (group.count > 0) group.finished.toDouble() / group.count else 0.0
        val percentText = Math.round(100 * percentValue).toString() + "%"
        val percent = Fonts.poppinsBold.textPath(percentText, 65.0, 105.0, 16.0, "center baseline", "#1C1719") +
            Fonts.poppinsBold.textPath(percentText, 64.0, 104.0, 16.0, "center baseline", "#fff")
        val percentColors = progressColor(percentValue)
        val percentRrect = PanelDraw.linearGradientRect(
            108.0, 20.0 + 85.0 - percentValue * 85.0, 10.0, percentValue * 85.0, 5.0,
            percentColors, listOf(50, 50), listOf(100, 0), 1.0,
        )
        val background = PanelDraw.linearGradientRect(
            10.0, 10.0, 115.0, 106.0, 10.0, levelColor(group.star),
            listOf(100, 0), listOf(80, 20), 0.8,
        )
        return buildString {
            append("<g id=\"Base-LV\"><rect x=\"10\" y=\"10\" width=\"115\" height=\"106\" rx=\"10\" ry=\"10\" style=\"fill: #46393F;\"/></g>")
            append("<g id=\"Background-LV\"><rect x=\"108\" y=\"20\" width=\"10\" height=\"85\" rx=\"5\" ry=\"5\" style=\"fill: #54454C;\"/></g>")
            append("<g id=\"Text-LV\">")
            append(background)
            append(level)
            append(ratio)
            append(percent)
            append(percentRrect)
            append("</g>")
        }
    }

    fun component(data: MaiVersionScoreService.MaiVersionResponse, assets: ImageAssets): String {
        val plate = MaiAssets(assets).plate(data.plateID)
        val progress12 = if (data.count12 > 0) data.finished12.toDouble() / data.count12 else 0.0
        val progress15 = if (data.count15 > 0) data.finished15.toDouble() / data.count15 else 0.0
        val width12 = if (progress12 <= 0) 0.0 else max(Math.round(progress12 * 540).toDouble(), 30.0)
        val width15 = if (progress15 <= 0) 0.0 else max(Math.round(progress15 * 540).toDouble(), 30.0)
        val color12 = progressColor(progress12, ColorArray.blue)
        val color15 = progressColor(progress15)

        val finished12Color = if (progress12 >= 0.7) "#382E32" else "#FFF"
        val finished15Color = if (progress15 >= 0.7) "#382E32" else "#FFF"

        val progress12Rect = PanelDraw.linearGradientRect(20.0, 118.0, width12, 30.0, 15.0, color12)
        val progress15Rect = PanelDraw.linearGradientRect(20.0, 118.0, width15, 30.0, 15.0, color15)

        val title12 = Fonts.poppinsBold.textPath("0-12:", 20.0, 172.0, 22.0, "left baseline")
        val percent12 = Fonts.poppinsBold.textPath(
            Numbers.round(progress12 * 100, 1) + "% [${data.count12}]",
            20.0, 198.0, 22.0, "left baseline",
        )
        val finished12Width = Fonts.poppinsBold.width(data.finished12.toString(), 22.0)
        val finished15Width = Fonts.poppinsBold.width(data.finished15.toString(), 22.0)

        val finished15Offset = if (width15 - width12 >= finished15Width + 20) 20.0 + width15 - 10.0 else 550.0
        val finished12Offset = if (width12 >= finished12Width + 20 && width15 - width12 >= finished15Width + 20) {
            20.0 + width12 - 10.0 to "right baseline"
        } else {
            30.0 to "left baseline"
        }
        val finished12 = Fonts.poppinsBold.textPath(
            data.finished12.toString(), finished12Offset.first, 140.0, 22.0, finished12Offset.second, finished12Color,
        )

        val main15Text = Numbers.round(progress15 * 100, 1)
        val percent15Text = "% [${data.count15}]"
        val main15Width = Math.round(
            Fonts.poppinsBold.width(main15Text, 48.0) + Fonts.poppinsBold.width(percent15Text, 36.0),
        )
        val title15 = Fonts.poppinsBold.textPath(
            "12+:", 580.0 - 20.0 - main15Width - 10.0, 172.0, 22.0, "right baseline",
        )
        val main15 = Fonts.poppinsBold.get2SizeTextPath(
            main15Text, percent15Text, 48.0, 36.0, 580.0 - 20.0 - main15Width, 194.0, "left baseline",
        )
        val finished15 = Fonts.poppinsBold.textPath(
            data.finished15.toString(), finished15Offset, 140.0, 22.0, "right baseline", finished15Color,
        )

        return buildString {
            append("<defs>")
            append("<clipPath id=\"clippath-OV-1\"><rect x=\"20\" y=\"118\" width=\"540\" height=\"30\" rx=\"15\" ry=\"15\" style=\"fill: none;\"/></clipPath>")
            append("<clipPath id=\"clippath-OV-2\"><rect x=\"20\" y=\"20\" width=\"540\" height=\"87\" rx=\"5\" ry=\"5\" style=\"fill: none;\"/></clipPath>")
            append("</defs>")
            append("<g id=\"Base_OV\"><rect x=\"0\" y=\"0\" width=\"580\" height=\"210\" rx=\"20\" ry=\"20\" style=\"fill: #382e32;\"/>")
            append("<rect x=\"20\" y=\"118\" width=\"540\" height=\"30\" rx=\"15\" ry=\"15\" style=\"fill: #4facfe; fill-opacity: 0.2\"/></g>")
            append("<g id=\"Rect_OV\">")
            append("<g style=\"clip-path: url(#clippath-OV-1);\">$progress12Rect</g>")
            append("<g style=\"clip-path: url(#clippath-OV-1);\">$progress15Rect</g>")
            append("</g>")
            append("<g id=\"Plate_OV\" style=\"clip-path: url(#clippath-OV-2);\">")
            append(Svg.image(20.0, 20.0, 540.0, 87.0, plate.orEmpty()))
            append("</g>")
            append("<g id=\"Text_OV\">")
            append(title12)
            append(percent12)
            append(finished12)
            append(title15)
            append(main15)
            append(finished15)
            append("</g>")
        }
    }

    fun area(startIndex: Int, endIndex: Int, colors: List<String>, opacity: Double = 0.4): String {
        if (startIndex > endIndex) return ""
        val startX = startIndex % 12
        val startY = startIndex / 12
        val endX = endIndex % 12
        val endY = endIndex / 12
        val builder = StringBuilder()
        for (row in startY..endY) {
            val first = if (row == startY) startX else 0
            val last = if (row == endY) endX else 11
            val x = 40.0 + first * STEP_X - 10.0
            val y = 330.0 + row * STEP_Y - 10.0
            val width = (last - first + 1) * STEP_X
            builder.append(
                PanelDraw.linearGradientRect(
                    x, y, width, STEP_Y, 15.0, colors,
                    listOf(100, 0), listOf(80, 20), opacity,
                ),
            )
        }
        return builder.toString()
    }

    fun levelColor(level: String): List<String> = when (level) {
        "15" -> ColorArray.sakura
        "14+" -> ColorArray.peach
        "14" -> ColorArray.purple
        "13+" -> ColorArray.magenta
        "13" -> ColorArray.red
        "12+" -> ColorArray.amber
        "12-" -> ColorArray.yellow
        else -> ColorArray.green
    }

    private fun progressColor(percent: Double, default: List<String> = ColorArray.gray): List<String> {
        return when {
            percent >= 1.0 -> ColorArray.rainbow
            percent >= 0.9 -> ColorArray.lightYellow
            percent >= 0.8 -> ColorArray.lightGreen
            percent >= 0.7 -> ColorArray.cyan
            percent >= 0.6 -> ColorArray.purple
            percent >= 0.5 -> ColorArray.red
            else -> default
        }
    }

    private fun requiredOverlay(
        required: String,
        rank: String,
        combo: String,
        sync: String,
        assets: ImageAssets,
    ): String {
        val mai = MaiAssets(assets)
        return when (required) {
            "ap", "fc" -> {
                val name = when (combo) {
                    "fc" -> "fullcombo"
                    "fcp" -> "fullcomboplus"
                    "ap" -> "allperfect"
                    "app" -> "allperfectplus"
                    else -> "clear"
                }
                Svg.image(109.0, -8.0, 36.0, 40.0, mai.local("Maimai/object-icon-combo-$name.png").orEmpty())
            }
            "fsd" -> {
                val name = when (sync) {
                    "sync" -> "sync"
                    "fs" -> "fullsync"
                    "fsp" -> "fullsyncplus"
                    "fsd" -> "fullsyncdx"
                    "fsdp" -> "fullsyncdxplus"
                    else -> "solo"
                }
                Svg.image(109.0, -8.0, 36.0, 40.0, mai.local("Maimai/object-icon-sync-$name.png").orEmpty())
            }
            else -> {
                if (rank.isBlank()) "" else Svg.image(84.0, -10.0, 56.0, 30.0, mai.rankImage(rank).orEmpty())
            }
        }
    }
}
