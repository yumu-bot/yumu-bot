package com.now.nowbot.image.panel

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.scorelist.PanelA4Param
import com.now.nowbot.image.scorelist.ScoreListPanelRenderer
import com.now.nowbot.image.scorelist.ScoreListFormat
import com.now.nowbot.image.scorelist.ScoreListImageRequest
import com.now.nowbot.image.scorelist.ScoreListOutputFormat
import com.now.nowbot.image.scorelist.ScoreListRenderResult
import com.now.nowbot.image.scorelist.renderScoreList
import com.now.nowbot.image.util.TimeFormat

object PanelA4 {
    fun outputFormat(compact: Boolean): ScoreListOutputFormat = ScoreListFormat.imageRequest(compact).format

    fun imageRequest(param: PanelA4Param): ScoreListImageRequest<PanelA4Param> {
        val plan = ScoreListFormat.imageRequest(param.compact)
        return ScoreListImageRequest(param.copy(compact = plan.dataCompact), plan.format)
    }

    /**
     * [compact] 为 true 时保持源 `router_svg` 的紧凑 I4 布局。
     * 图片请求的格式/数据策略见 [com.now.nowbot.image.scorelist.ScoreListFormat.imageRequest]：
     * compact 为 true 时输出 ADVANCED 且传给面板的 compact 为 false，调用方状态不应被修改。
     */
    fun toSvg(param: PanelA4Param, assets: ImageAssets): String {
        val scores = param.scores
        val requestTime = "request time: ${TimeFormat.nowTimestamp()}"
        val panelName: String
        val sign: String
        var panelRequestTime = "scores count: ${scores.size} // $requestTime"
        when (param.panel) {
            "T" -> {
                if (param.filtered > 0) {
                    panelRequestTime = "filtered count: ${param.filtered} // $requestTime"
                }
                panelName = "Today Bests (!ymt)"
                sign = "T"
            }
            "BS" -> {
                panelName = "Best Scores (!ymbs)"
                sign = "BS"
            }
            "RB" -> {
                panelRequestTime = requestTime
                panelName = "Recent Bests (!ymrb)"
                sign = "RB"
            }
            "BH" -> {
                val snapshot = TimeFormat.formatted(param.createdAt)
                panelRequestTime = "snapshot time: $snapshot // $requestTime"
                panelName = "Best Snapshot (!ymbh)"
                sign = "BH"
            }
            else -> {
                panelName = "Today BP / BP (!ymt / !ymb)"
                sign = "B"
            }
        }
        return ScoreListPanelRenderer.render(
            param.user,
            param.history,
            scores,
            param.ranks,
            param.compact,
            panelName,
            sign,
            panelRequestTime,
            param.bannerIndex,
            assets,
        )
    }

    fun render(
        param: PanelA4Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): ScoreListRenderResult {
        return renderScoreList(lease, remote, report) { assets -> toSvg(param, assets) }
    }
}
