package com.now.nowbot.image.panel

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiRenderResult
import com.now.nowbot.image.maimai.MaiVersusRenderParam
import com.now.nowbot.image.maimai.maiImageAssets
import com.now.nowbot.image.maimai.readMaiVersusRenderParam
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg

object PanelTheta {
    private val statusTexts = mapOf(
        "DOOMED" to "完了",
        "DAMN" to "不好",
        "DRAW" to "那就来吧",
        "DOMINATE" to "找死",
        "UNKNOWN" to "好吧",
    )

    fun toSvg(
        param: MaiVersusRenderParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(param, lease, remote).svg

    fun toSvg(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): String = render(body, lease, remote).svg

    fun render(
        body: Map<String, Any?>,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult = render(readMaiVersusRenderParam(body), lease, remote)

    fun render(
        param: MaiVersusRenderParam,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
    ): MaiRenderResult {
        val report = RenderReport()
        val assets = maiImageAssets(lease, remote, report)
        return MaiRenderResult(render(param, assets, report), report)
    }

    fun render(
        param: MaiVersusRenderParam,
        assets: ImageAssets,
        report: RenderReport = RenderReport(),
    ): String {
        val mai = MaiAssets(assets)
        val background = mai.versusBackground()
        val text = Fonts.puHuiTi.textPath("我赢了的话", 550.0, 220.0, 80.0, "left baseline", "#000") +
            Fonts.puHuiTi.textPath("前辈要和我交往哦", 550.0, 300.0, 80.0, "left baseline", "#000") +
            Fonts.puHuiTi.textPath(
                (statusTexts[param.status] ?: statusTexts.getValue("UNKNOWN")) + "...",
                950.0, 840.0, 80.0, "center baseline", "#000",
            )

        val me = drawPlate(param.me?.rating ?: 16384, 680.0, 620.0, mai)
        val other = drawPlate(param.other?.rating ?: 16384, 680.0, 20.0, mai)
        val my = drawCover(param.my, 0.0, 960.0, mai)
        val others = drawCover(param.others, 0.0, 360.0, mai)

        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"1200\" ")
            append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 1200 1200\">")
            append("<g id=\"Background_PT\">")
            append(background?.let { Svg.image(0.0, 0.0, 1200.0, 1200.0, it) }.orEmpty())
            append("</g>")
            append("<g id=\"Card_PT\">")
            append(my)
            append(others)
            append("</g>")
            append("<g id=\"Plate_PT\">")
            append(me)
            append(other)
            append("</g>")
            append("<g id=\"Index_PT\">")
            append(text)
            append("</g>")
            append("</svg>")
        }
    }

    private fun drawPlate(rating: Int, x: Double, y: Double, mai: MaiAssets): String {
        val builder = StringBuilder("<g>")
        mai.ratingPlate(rating)?.let { builder.append(Svg.image(x, y, 498.0, 96.0, it)) }

        val value = rating.toString()
        var currentX = x + 386.0
        for (index in value.indices.reversed()) {
            val digit = value[index]
            mai.ratingDigit(digit)?.let {
                builder.append(Svg.image(currentX, y + 24.0, 45.0, 51.0, it))
            }
            currentX -= 39.0
        }
        return builder.append("</g>").toString()
    }

    private fun drawCover(songIDs: List<Int>, x: Double, y: Double, mai: MaiAssets): String {
        val builder = StringBuilder()
        songIDs.forEachIndexed { index, songID ->
            mai.cover(songID.toLong())?.let {
                builder.append(Svg.image(x + index * 240.0, y, 240.0, 240.0, it))
            }
        }
        return builder.toString()
    }
}
