package com.now.nowbot.image.panel

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.image.scorelist.PanelA5Param
import com.now.nowbot.image.scorelist.ScoreListFormat
import com.now.nowbot.image.scorelist.ScoreListImageRequest
import com.now.nowbot.image.scorelist.ScoreListOutputFormat
import com.now.nowbot.image.scorelist.ScoreListPanelRenderer
import com.now.nowbot.image.scorelist.ScoreListRenderResult
import com.now.nowbot.image.scorelist.renderScoreList
import com.now.nowbot.image.util.TimeFormat

object PanelA5 {
    fun outputFormat(compact: Boolean): ScoreListOutputFormat = ScoreListFormat.imageRequest(compact).format

    fun imageRequest(param: PanelA5Param): ScoreListImageRequest<PanelA5Param> {
        val plan = ScoreListFormat.imageRequest(param.compact)
        return ScoreListImageRequest(param.copy(compact = plan.dataCompact), plan.format)
    }

    /**
     * [compact] 为 true 时保持源 `router_svg` 的紧凑 I4 布局。
     * 图片请求的格式/数据策略见 [com.now.nowbot.image.scorelist.ScoreListFormat.imageRequest]。
     */
    fun toSvg(param: PanelA5Param, assets: ImageAssets): String {
        val requestTime = "request time: ${TimeFormat.nowTimestamp()}"
        val panelName: String
        val sign: String
        val panelRequestTime: String
        when (param.panel) {
            "PS" -> {
                panelName = "Passed Scores (!ymps)"
                sign = "PS"
                panelRequestTime = requestTime
            }
            "RS" -> {
                panelName = "Recent Scores (!ymrs)"
                sign = "RS"
                panelRequestTime = requestTime
            }
            "MR" -> {
                panelName = "Match Recent Scores (!ymmr)"
                sign = "MR"
                panelRequestTime = "matchID: ${param.matchId ?: 0} // $requestTime"
            }
            "SS" -> {
                panelName = "Beatmap Scores (!ymss)"
                sign = "SS"
                panelRequestTime = requestTime
            }
            else -> {
                panelName = "Multi Scores (!ymps / !ymrs)"
                sign = "SS"
                panelRequestTime = requestTime
            }
        }
        return ScoreListPanelRenderer.render(
            param.user,
            param.history,
            param.scores,
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
        param: PanelA5Param,
        lease: RenderResourceLease,
        remote: RemoteImageSource? = null,
        report: RenderReport = RenderReport(),
    ): ScoreListRenderResult {
        return renderScoreList(lease, remote, report) { assets -> toSvg(param, assets) }
    }
}
