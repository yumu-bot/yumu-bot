package com.now.nowbot.image.scorelist

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceLease

data class ScoreListRenderResult(val svg: String, val report: RenderReport)

internal fun renderScoreList(
    lease: RenderResourceLease,
    remote: RemoteImageSource? = null,
    report: RenderReport = RenderReport(),
    render: (ImageAssets) -> String,
): ScoreListRenderResult {
    val assets = ImageAssets(
        RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
        remote,
        report = report,
    )
    return ScoreListRenderResult(render(assets), report)
}
