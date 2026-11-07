package com.now.nowbot.image.userlist

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderAssetRegistry
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceLease

data class UserListRenderResult(val svg: String, val report: RenderReport)

internal fun renderUserList(
    lease: RenderResourceLease,
    remote: RemoteImageSource? = null,
    report: RenderReport = RenderReport(),
    render: (ImageAssets) -> String,
): UserListRenderResult {
    val assets = ImageAssets(
        RenderAssetRegistry { bytes -> lease.registerImage(bytes).url },
        remote,
        report = report,
    )
    return UserListRenderResult(render(assets), report)
}
