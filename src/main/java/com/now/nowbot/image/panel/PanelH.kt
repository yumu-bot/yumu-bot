package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardH
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.mappool.old.MapPoolDto
import com.now.nowbot.model.mappool.old.ModPool
import com.now.nowbot.model.osu.Beatmap
import kotlin.math.floor

object PanelH {
    private const val MARK_MAIN = "<g id=\"MainCard\">"
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PH-1);\">"
    private const val MARK_BODY = "<g id=\"BodyCard\">"

    fun render(pool: MapPoolDto, mode: OsuMode, assets: ImageAssets): String {
        var svg = Templates.read("Panel_H.svg")
        svg = Svg.setText(svg, PanelBatchSupport.panelName("Get Mappool (!ymgp)", "GP"), MARK_INDEX)

        val pools = pool.modPools
        val cards = mutableListOf<String>()
        var row = 1
        var mapCount = 0
        for (modPool in pools) {
            cards += drawModPool(modPool, mode, row, assets)
            row += rowCount(modPool.beatmaps.size)
            mapCount += modPool.beatmaps.size
        }
        svg = Svg.setTexts(svg, cards, MARK_BODY)

        val poolInfo = CardA2.render(
            CardA2.fromPool(pool, mode, mapCount, pools.size, assets, PanelBatchSupport.randomBanner(assets)),
            assets,
        )
        val panelHeight = 330.0 + 150.0 * (row - 1)
        val cardHeight = 40.0 + 150.0 * (row - 1)
        svg = PanelBatchSupport.applyHeights(svg, panelHeight, cardHeight)
        svg = Svg.setText(svg, PanelBatchSupport.randomBanner(assets)?.let {
            Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7)
        }, MARK_BANNER)
        return Svg.setText(svg, poolInfo, MARK_MAIN)
    }

    private fun drawModPool(pool: ModPool, mode: OsuMode, rowStart: Int, assets: ImageAssets): List<String> {
        val cards = mutableListOf<String>()
        val count = pool.beatmaps.size
        for (j in 0 until fullRowCount(count)) {
            for (k in 0 until 3) {
                cards += drawCardD(
                    pool.beatmaps[j * 3 + k], pool, mode, rowStart + j, k + 1, 3, assets,
                )
            }
        }
        if (hasRemain(count)) {
            for (o in 0 until remain(count)) {
                cards += drawCardD(
                    pool.beatmaps[fullRowCount(count) * 3 + o], pool, mode,
                    rowStart + fullRowCount(count), o + 1, remain(count), assets,
                )
            }
        }
        return cards
    }

    private fun drawCardD(
        beatmap: Beatmap,
        pool: ModPool,
        mode: OsuMode,
        row: Int,
        column: Int,
        maxColumn: Int,
        assets: ImageAssets,
    ): String {
        val x = (3 - maxColumn) * 300.0 + 80.0 + 600.0 * (column - 1)
        val y = 330.0 + 150.0 * (row - 1)
        return Svg.group(x, y, CardH.render(CardH.fromBeatmap(beatmap, pool, assets, mode), assets))
    }

    private fun rowCount(count: Int): Int {
        var rows = fullRowCount(count)
        if (hasRemain(count)) rows++
        return rows
    }

    private fun fullRowCount(count: Int): Int = floor(count / 3.0).toInt()

    private fun remain(count: Int): Int = count % 3

    private fun hasRemain(count: Int): Boolean = remain(count) != 0
}
