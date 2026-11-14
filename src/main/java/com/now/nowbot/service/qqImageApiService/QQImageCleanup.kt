package com.now.nowbot.service.qqImageApiService

import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * QQ 图片素材的清理单例。
 *
 * 只有在消息发送成功后平台才会转存图片副本，所以素材不能在发送前删除；
 * 而 Yumu 桥接层没有发送回执，因此统一在这里延后 [DELETE_DELAY_SECONDS] 秒删除，
 * 既给桥接层留出发送和平台转存的时间，也避免提前删除导致 Markdown 引用失效。
 */
object QQImageCleanup {
    private val log = LoggerFactory.getLogger(QQImageCleanup::class.java)

    /** 发送完成后延迟删除的秒数, 从 30 秒放宽到 120 秒, 避免桥接层排队时被提前删掉 */
    private const val DELETE_DELAY_SECONDS = 120L

    private val scheduler = Executors.newSingleThreadScheduledExecutor(
        Thread.ofVirtual().name("qq-image-cleanup-", 0).factory()
    )

    @Volatile
    private var api: QQImageApiService? = null

    /** 由 [QQImageApiService] 在初始化时绑定 */
    fun bind(service: QQImageApiService) {
        api = service
    }

    /**
     * 在消息交给桥接层之后调用, 延迟删除这条消息引用的素材, 释放 100 张配额。
     */
    fun scheduleDelete(imageIds: Collection<String>, delaySeconds: Long = DELETE_DELAY_SECONDS) {
        if (imageIds.isEmpty()) return

        val service = api ?: run {
            log.warn("QQImageApiService 尚未绑定, 跳过素材清理: {}", imageIds)
            return
        }

        if (!service.isAvailable) {
            log.debug("QQ 图片素材服务不可用, 跳过素材清理: {}", imageIds)
            return
        }

        val ids = imageIds.toList()
        scheduler.schedule({
            for (id in ids) {
                runCatching { service.delete(id) }
                    .onSuccess { log.debug("已删除 QQ 图片素材 {}", id) }
                    .onFailure { log.warn("删除 QQ 图片素材失败 {}: {}", id, it.message) }
            }
        }, delaySeconds, TimeUnit.SECONDS)
    }
}
