package com.now.nowbot.image.panel

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.util.TimeFormat
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.ceil

internal object PanelBatchSupport {
    private const val VERSION = "v0.8.5"
    private const val VERSION_CODE = "VS"
    private const val POWERED_BY = "Yumubot"

    fun panelHeight(
        cardCount: Int,
        cardHeight: Double = 110.0,
        cardPerRow: Int = 2,
        bannerHeight: Double = 290.0,
        interval: Double = 40.0,
        spacing: Double = 40.0,
    ): Double {
        if (cardPerRow == 0) return bannerHeight + spacing
        val row = ceil(cardCount.toDouble() / cardPerRow)
        return bannerHeight + 2 * spacing + (cardHeight + interval) * row - interval
    }

    fun applyHeights(svg: String, panelHeight: Double, cardHeight: Double, viewBoxHeight: Double = panelHeight): String {
        return svg
            .replace("\${panelheight}", com.now.nowbot.image.svg.Svg.number(panelHeight))
            .replace("\${cardheight}", com.now.nowbot.image.svg.Svg.number(cardHeight))
            .replace("\${height}", com.now.nowbot.image.svg.Svg.number(viewBoxHeight))
    }

    fun panelName(name: String, index: String, requestTime: String = "request time: ${TimeFormat.nowTimestamp()}"): String {
        val powered = Fonts.torus.textPath(
            "powered by $POWERED_BY $VERSION $VERSION_CODE // $name",
            20.0, 26.84, 24.0, "left baseline", "#fff",
        )
        val request = Fonts.torus.textPath(requestTime, 1900.0, 26.84, 24.0, "right baseline", "#fff")
        val sign = Fonts.torus.textPath(index, 607.5, 83.67, 48.0, "center baseline", "#fff")
        return powered + "\n" + request + "\n" + sign
    }

    fun page(page: Int, maxPage: Int, panelHeight: Double): String {
        return Fonts.torusBold.textPath(
            "page: $page of $maxPage", 960.0, panelHeight - 15.0, 20.0, "center baseline", "#fff", 0.6,
        )
    }

    fun randomBanner(assets: ImageAssets, today: LocalDate = LocalDate.now(ZoneOffset.ofHours(8))): String? {
        return assets.randomBanner(today = today)
    }
}
