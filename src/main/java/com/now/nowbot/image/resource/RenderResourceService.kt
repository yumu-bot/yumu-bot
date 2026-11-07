package com.now.nowbot.image.resource

import com.now.nowbot.config.BrowserConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class RenderResourceService @Autowired constructor(
    private val config: RenderResourceConfig,
    browserConfig: BrowserConfig,
    private val urlProvider: RenderResourceUrlProvider,
) {
    private var clock: Clock = Clock.systemUTC()

    constructor(
        config: RenderResourceConfig,
        browserConfig: BrowserConfig,
        urlProvider: RenderResourceUrlProvider,
        clock: Clock,
    ) : this(config, browserConfig, urlProvider) {
        this.clock = clock
    }

    private class Entry(
        val url: String,
        val bytes: ByteArray,
        val mediaType: MediaType,
        var expiresAt: Instant,
        val lease: RenderResourceLeaseState?,
    )

    private val lock = Any()
    private val resources = LinkedHashMap<String, Entry>()
    private var totalBytes: Long = 0L

    val ttlMillis: Long = maxOf(
        config.ttlMillis,
        browserConfig.acquireTimeoutMillis +
            browserConfig.renderTimeoutMillis +
            browserConfig.resultWaitGraceMillis +
            config.browserTimeoutMarginMillis,
    )

    fun register(bytes: ByteArray, mediaType: MediaType): RenderResource =
        putStandalone(bytes, RenderResourceMediaTypes.normalize(mediaType))

    fun register(bytes: ByteArray, mediaType: String): RenderResource =
        register(bytes, RenderResourceMediaTypes.parse(mediaType))

    fun registerImage(bytes: ByteArray): RenderResource =
        register(bytes, RenderResourceMediaTypes.detect(bytes) ?: throw IllegalArgumentException("无法识别图片类型"))

    fun lease(): RenderResourceLease =
        RenderResourceLease(this, RenderResourceLeaseState())

    fun open(id: String): RenderResourceContent? {
        if (!RenderResourceId.isValid(id)) return null
        val content = synchronized(lock) {
            val now = clock.instant()
            val entry = resources[id] ?: return@synchronized null
            if (!entry.expiresAt.isAfter(now)) {
                removeLocked(id, entry)
                return@synchronized null
            }
            RenderResourceContent(entry.bytes.copyOf(), entry.mediaType, entry.expiresAt)
        }
        return content
    }

    @Scheduled(fixedDelayString = "\${render.resource.cleanup-interval-millis:30000}")
    fun cleanUp() {
        synchronized(lock) {
            cleanupExpiredLocked(clock.instant())
        }
    }

    internal fun registerInLease(
        lease: RenderResourceLease,
        bytes: ByteArray,
        mediaType: MediaType,
    ): RenderResource = synchronized(lock) {
        putLocked(bytes, RenderResourceMediaTypes.normalize(mediaType), lease.state)
    }

    internal fun registerInLease(
        lease: RenderResourceLease,
        bytes: ByteArray,
        mediaType: String,
    ): RenderResource = synchronized(lock) {
        putLocked(bytes, RenderResourceMediaTypes.parse(mediaType), lease.state)
    }

    internal fun registerImageInLease(
        lease: RenderResourceLease,
        bytes: ByteArray,
    ): RenderResource = synchronized(lock) {
        putLocked(
            bytes,
            RenderResourceMediaTypes.detect(bytes) ?: throw IllegalArgumentException("无法识别图片类型"),
            lease.state,
        )
    }

    internal fun resourceUrls(lease: RenderResourceLease): Set<String> = synchronized(lock) {
        lease.state.ids.mapNotNullTo(LinkedHashSet<String>()) { id -> resources[id]?.url }
    }

    internal fun closeLease(lease: RenderResourceLease, immediate: Boolean) {
        synchronized(lock) {
            val state = lease.state
            if (immediate) {
                removeLeaseLocked(state)
                state.closed = true
                return
            }
            if (state.closed) return
            state.closed = true
            val expiresAt = clock.instant().plusMillis(ttlMillis)
            state.ids.forEach { id -> resources[id]?.expiresAt = expiresAt }
        }
    }

    private fun putStandalone(bytes: ByteArray, mediaType: MediaType): RenderResource =
        synchronized(lock) {
            putLocked(bytes, mediaType, null)
        }

    private fun putLocked(
        bytes: ByteArray,
        mediaType: MediaType,
        lease: RenderResourceLeaseState?,
    ): RenderResource {
        require(bytes.isNotEmpty()) { "渲染资源不能为空" }
        require(bytes.size.toLong() <= config.maxResourceBytes) {
            "渲染资源超过大小限制: ${bytes.size} > ${config.maxResourceBytes}"
        }
        if (lease != null) {
            check(!lease.closed) { "渲染资源 lease 已关闭" }
        }

        val now = clock.instant()
        cleanupExpiredLocked(now)
        val size = bytes.size.toLong()
        check(resources.size.toLong() < config.maxResources) {
            "渲染资源数量已达上限: ${config.maxResources}"
        }
        check(totalBytes + size <= config.maxBytes) {
            "渲染资源容量已满: ${totalBytes + size} > ${config.maxBytes}"
        }

        val id = generateIdLocked()
        val url = urlProvider.urlFor(id)
        val payload = bytes.copyOf()
        val expiresAt = if (lease == null) now.plusMillis(ttlMillis) else Instant.MAX
        val entry = Entry(url, payload, mediaType, expiresAt, lease)
        resources[id] = entry
        totalBytes += size
        lease?.ids?.add(id)
        return RenderResource(id, url, mediaType, payload.size, expiresAt)
    }

    private fun cleanupExpiredLocked(now: Instant) {
        if (resources.isEmpty()) return
        val iterator = resources.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.expiresAt.isAfter(now)) {
                iterator.remove()
                totalBytes -= entry.value.bytes.size.toLong()
                entry.value.lease?.ids?.remove(entry.key)
            }
        }
    }

    private fun removeLeaseLocked(state: RenderResourceLeaseState) {
        state.ids.forEach { id ->
            resources.remove(id)?.let { totalBytes -= it.bytes.size.toLong() }
        }
        state.ids.clear()
    }

    private fun removeLocked(id: String, entry: Entry) {
        resources.remove(id)
        totalBytes -= entry.bytes.size.toLong()
        entry.lease?.ids?.remove(id)
    }

    private fun generateIdLocked(): String {
        while (true) {
            val id = RenderResourceId.next()
            if (!resources.containsKey(id)) return id
        }
    }
}
