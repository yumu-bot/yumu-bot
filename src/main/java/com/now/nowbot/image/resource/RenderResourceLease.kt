package com.now.nowbot.image.resource

import org.springframework.http.MediaType

internal class RenderResourceLeaseState {
    val ids = LinkedHashSet<String>()
    var closed = false
}

class RenderResourceLease internal constructor(
    private val service: RenderResourceService,
    internal val state: RenderResourceLeaseState,
) : AutoCloseable {
    fun register(bytes: ByteArray, mediaType: MediaType): RenderResource =
        service.registerInLease(this, bytes, mediaType)

    fun register(bytes: ByteArray, mediaType: String): RenderResource =
        service.registerInLease(this, bytes, mediaType)

    fun registerImage(bytes: ByteArray): RenderResource =
        service.registerImageInLease(this, bytes)

    fun resourceUrls(): Set<String> = service.resourceUrls(this)

    /**
     * 关闭 lease 后，本 lease 的资源才从关闭时刻开始计算安全 TTL。
     */
    override fun close() {
        service.closeLease(this, immediate = false)
    }

    /**
     * 仅在确认截图任务已实际结束、页面不再引用资源后调用。
     */
    fun releaseNow() {
        service.closeLease(this, immediate = true)
    }
}
