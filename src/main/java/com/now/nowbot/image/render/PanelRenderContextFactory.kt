package com.now.nowbot.image.render

import com.now.nowbot.image.draw.RemoteImageSource
import com.now.nowbot.image.draw.RenderReport
import com.now.nowbot.image.resource.RenderResourceService
import org.springframework.stereotype.Component

@Component
class PanelRenderContextFactory(
    private val renderResourceService: RenderResourceService,
    private val remoteImageSource: RemoteImageSource,
    private val renderer: SvgScreenshotRenderer,
    private val policy: RenderOutputPolicy,
) {
    fun open(report: RenderReport = RenderReport()): PanelRenderContext {
        return PanelRenderContext(
            renderer = renderer,
            policy = policy,
            lease = renderResourceService.lease(),
            remoteImageSource = remoteImageSource,
            report = report,
        )
    }

    fun <T> withContext(block: (PanelRenderContext) -> T): T {
        return open().use(block)
    }
}
