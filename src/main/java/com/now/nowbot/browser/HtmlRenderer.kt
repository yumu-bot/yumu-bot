package com.now.nowbot.browser

import com.now.nowbot.config.BrowserConfig
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@Component
class HtmlRenderer(config: BrowserConfig) : AutoCloseable {
    private val pool = RenderPool(config)
    private val closeStarted = AtomicBoolean(false)
    private val closed = CountDownLatch(1)

    @Volatile
    private var closeFailure: RenderException? = null

    fun render(html: String, options: ScreenshotOptions = ScreenshotOptions()): ByteArray =
        pool.render(html, options)

    @PreDestroy
    override fun close() {
        if (!closeStarted.compareAndSet(false, true)) {
            awaitUninterruptibly(closed)
            closeFailure?.let { throw it }
            return
        }
        var failure: RenderException? = null
        try {
            if (!pool.close()) {
                failure = RenderException("HtmlRenderer 关闭不完整：有渲染任务或浏览器资源未能在超时内释放")
            }
        } catch (e: Throwable) {
            failure = if (e is RenderException) {
                e
            } else {
                RenderException("HtmlRenderer 关闭失败: ${e.message}", e)
            }
        } finally {
            closeFailure = failure
            closed.countDown()
        }
        failure?.let { throw it }
    }

    private fun awaitUninterruptibly(latch: CountDownLatch) {
        var interrupted = false
        while (true) {
            try {
                latch.await()
                break
            } catch (_: InterruptedException) {
                interrupted = true
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt()
        }
    }
}
