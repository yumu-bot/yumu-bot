package com.now.nowbot.image.panel

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.osu.Beatmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

data class PanelDeltaParam(
    val beatmap: Beatmap,
    val round: String = "Qualifier",
    val mod: String = "NM",
    val position: Short = 1,
    val hasBG: Boolean = true,
)

data class DeltaRenderResult(val svg: String, val report: RenderReport)

object PanelDelta {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_TEXT = "<g id=\"Text\">"
    private const val MARK_STAT = "<g id=\"Stat\">"
    private const val MARK_BASE = "<g id=\"Base\">"
    private const val MARK_BACKGROUND = "<g id=\"Background\">"
    private const val MARK_SR = "<g id=\"SR\" filter=\"url(#clippath-PDelta-SR)\">"
    private const val MARK_CS = "<g id=\"CS\" filter=\"url(#clippath-PDelta-CS)\">"
    private const val MARK_AR = "<g id=\"AR\" filter=\"url(#clippath-PDelta-AR)\">"
    private const val MARK_OD = "<g id=\"OD\" filter=\"url(#clippath-PDelta-OD)\">"
    private const val MARK_LENGTH = "<g id=\"LH\" clip-path=\"url(#clippath-PDelta-LH)\">"

    fun toSvg(param: PanelDeltaParam, lease: RenderResourceLease, remote: RemoteImageSource? = null): String =
        render(param, lease, remote).svg

    fun render(param: PanelDeltaParam, lease: RenderResourceLease, remote: RemoteImageSource? = null): DeltaRenderResult {
        val report = RenderReport()
        val assets = ImageAssets(RenderAssetRegistry { bytes -> lease.registerImage(bytes).url }, remote, report = report)
        return DeltaRenderResult(render(param, assets, report), report)
    }

    fun render(param: PanelDeltaParam, assets: ImageAssets, report: RenderReport = RenderReport()): String {
        val beatmap = param.beatmap
        val beatmapset = beatmap.beatmapset ?: throw IllegalArgumentException("Delta 需要 beatmapset")
        val star = beatmap.starRating
        val cs = beatmap.cs?.toDouble() ?: throw IllegalArgumentException("Delta 需要 cs")
        val ar = beatmap.ar?.toDouble() ?: throw IllegalArgumentException("Delta 需要 ar")
        val od = beatmap.od?.toDouble() ?: throw IllegalArgumentException("Delta 需要 od")
        val bpm = beatmap.bpm.toDouble()
        require(star.isFinite() && cs.isFinite() && ar.isFinite() && od.isFinite() && bpm.isFinite()) { "Delta 谱面属性必须是有效数值" }

        val mod = param.mod.uppercase()
        val position = param.position.toString()
        val modColor = Colors.getModColor(mod)
        val totalLength = beatmap.totalLength
        val length = when (mod) {
            "DT" -> totalLength / 1.5
            "EZ" -> totalLength * 1.5
            else -> totalLength.toDouble()
        }
        val minute = floor(length / 60).toInt().toString()
        val second = floor(length % 60).toInt().toString().padStart(2, '0')
        val round = Fonts.lineSeedSans.textPath(
            Fonts.lineSeedSans.cutStringTail(param.round.ifBlank { "Unknown" }, 42.0, 783.0 - 20, true),
            391.5, 538.0, 42.0, "center baseline", "#282425",
        )
        val title = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(beatmapset.title, 106.0, 880.0, true),
            1450.0, 318.0, 106.0, "center baseline", "#fff",
        )
        val artist = Fonts.poppinsBold.textPath(
            Fonts.poppinsBold.cutStringTail(beatmapset.artist, 68.0, 880.0, true),
            1450.0, 414.0, 68.0, "center baseline", "#fff",
        )
        val difficulty = Fonts.poppinsBold.textPath(
            "[" + Fonts.poppinsBold.cutStringTail(beatmap.difficultyName, 64.0, 880.0, true) + "]",
            1450.0, 500.0, 64.0, "center baseline", "#F9D4BA",
        )
        val bid = Fonts.poppinsBold.textPath("BID: ${beatmap.beatmapID}", 1450.0, 566.0, 42.0, "center baseline", "#F9D4BA")
        val bpmText = Fonts.poppinsBold.textPath(Numbers.jsRound(bpm).toLong().toString(), 1720.0, 970.0, 54.0, "center baseline", "#282425")
        val modText = Fonts.poppinsBold.textPath(mod + position, 1453.0, 166.0, 68.0, "center baseline", "#fff")
        val starText = Fonts.poppinsBold.textPath(Numbers.floor(star, 2), 1360.0, 696.0, 48.0, "left baseline", "#fff")
        val csText = Fonts.poppinsBold.textPath(Numbers.floor(cs, 1), 1360.0, 788.0, 48.0, "left baseline", "#fff")
        val arText = Fonts.poppinsBold.textPath(Numbers.floor(ar, 1), 1360.0, 880.0, 48.0, "left baseline", "#fff")
        val odText = Fonts.poppinsBold.textPath(Numbers.floor(od, 1), 1360.0, 972.0, 48.0, "left baseline", "#fff")
        val lengthText = Fonts.poppinsBold.textPath("$minute:$second", 1662.5, 820.0, 58.0, "center baseline", "#fff")

        var svg = Templates.read("Panel_Delta.svg")
        svg = Svg.setTexts(svg, listOf(round, title, artist, difficulty, bid, bpmText, modText, starText, csText, arText, odText, lengthText), MARK_TEXT)

        val modRect = PanelDraw.rect(1360.0, 100.0, 186.0, 82.0, 0.0, modColor)
        val srRect = PanelDraw.rect(1010.0, 674.0, Numbers.clamp(star, 11.0, 0.0) * 336, 8.0, 4.0, "#FBCF5B")
        val csRect = PanelDraw.rect(1010.0, 766.0, Numbers.clamp(cs, 11.0, 0.0) * 336, 8.0, 4.0, "#C31542")
        val arRect = PanelDraw.rect(1010.0, 858.0, Numbers.clamp(ar, 11.0, 0.0) * 336, 8.0, 4.0, "#40ABF7")
        val odRect = PanelDraw.rect(1010.0, 950.0, Numbers.clamp(od, 11.0, 0.0) * 336, 8.0, 4.0, "#FF82A0")
        val pie = pie(1662.5, 797.5, 200.0, min(length / 600.0, 1.0), 0.0, modColor)
        svg = Svg.setText(svg, modRect, MARK_STAT)
        svg = Svg.setText(svg, srRect, MARK_SR)
        svg = Svg.setText(svg, csRect, MARK_CS)
        svg = Svg.setText(svg, arRect, MARK_AR)
        svg = Svg.setText(svg, odRect, MARK_OD)
        svg = Svg.setText(svg, pie, MARK_LENGTH)

        if (param.hasBG) {
            val background = assets.mapBackground(beatmap, "cover")
            svg = Svg.setText(svg, background?.let { Svg.image(-30.0, 0.0, 1080.0, 1080.0, it) }, MARK_BACKGROUND)
        }
        svg = Svg.setText(svg, assets.local("panel-kita.webp")?.let { Svg.image(0.0, 0.0, 1920.0, 1080.0, it) }, MARK_BASE)
        svg = Svg.setText(svg, assets.local("panel-kita-center.webp")?.let { Svg.image(1586.0, 721.0, 153.0, 153.0, it) }, MARK_INDEX)
        return svg
    }

    private fun pie(cx: Double, cy: Double, radius: Double, current: Double, previous: Double, color: String): String {
        var radCurr = 2 * PI * current
        var radPrev = 2 * PI * previous
        if (radCurr < radPrev) {
            radPrev += radCurr
            radCurr = radPrev - radCurr
            radPrev -= radCurr
        }
        if (radCurr - radPrev > 2 * PI - 1e-7) return PanelDraw.circle(cx, cy, radius, color)
        val x1 = cx + radius * sin(radPrev)
        val y1 = cy - radius * cos(radPrev)
        val x2 = cx + radius * sin(radCurr)
        val y2 = cy - radius * cos(radCurr)
        val large = if (abs(current - previous) >= 0.5) 1 else 0
        return "<path d=\"M ${Svg.number(x1)} ${Svg.number(y1)} A ${Svg.number(radius)} ${Svg.number(radius)} 0 $large 1 ${Svg.number(x2)} ${Svg.number(y2)} L ${Svg.number(cx)} ${Svg.number(cy)} L ${Svg.number(x1)} ${Svg.number(y1)} Z\" style=\"fill: ${Svg.escape(color)}; fill-opacity: 1\"/>"
    }
}
