package com.now.nowbot.image.maimai

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceLease
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiScore

data class MaiRenderResult(
    val svg: String,
    val report: RenderReport = RenderReport(),
)

/**
 * Theta 的绘图输入。原 toVersusResult 的 my/others 是歌曲 ID（取模后的 5 个），
 * me/other 是当前 MaiBestScore.User，status 是 MaiVersusStatus.name。
 */
data class MaiVersusRenderParam(
    val me: MaiBestScore.User? = null,
    val other: MaiBestScore.User? = null,
    val myDist: Int = 0,
    val otherDist: Int = 0,
    val my: List<Int> = emptyList(),
    val others: List<Int> = emptyList(),
    val status: String = "UNKNOWN",
)

internal fun maiImageAssets(
    lease: RenderResourceLease,
    remote: RemoteImageSource?,
    report: RenderReport,
): ImageAssets = ImageAssets(
    RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
    remote,
    report = report,
)

/**
 * MI 的绘图输入。map 入口的 scores 已按原 toMap 截取为当前页，
 * typed 入口则由 MaiFilterParam 转换时截取。
 */
data class MaiFilterRenderParam(
    val user: MaiBestScore.User = MaiBestScore.User(),
    val scores: List<MaiScore> = emptyList(),
    val page: Int = 1,
    val maxPage: Int = 1,
    val type: String = "",
)

data class MaiScoreStatsView(
    val count: Int = 0,
    val pageRating: Int = 0,
    val totalRating: Int = 0,
    val averageRating: Double = 0.0,
    val averageAchievement: Double = 0.0,
    val averageStar: Double = 0.0,
)

data class MaiMultipleRenderParam(
    val user: MaiBestScore.User? = null,
    val scores: List<MaiScore> = emptyList(),
    val scoresLatest: List<MaiScore> = emptyList(),
    val panel: String = "MA",
    val page: Int = 1,
    val maxPage: Int = 1,
    val statistics: MaiScoreStatsView? = null,
)

data class ChuMultipleRenderParam(
    val user: com.now.nowbot.model.maimai.ChuBestScore.User? = null,
    val scores: List<com.now.nowbot.model.maimai.ChuScore> = emptyList(),
    val scoresLatest: List<com.now.nowbot.model.maimai.ChuScore> = emptyList(),
    val scoresSelection: List<com.now.nowbot.model.maimai.ChuScore> = emptyList(),
    val panel: String = "CB",
)
