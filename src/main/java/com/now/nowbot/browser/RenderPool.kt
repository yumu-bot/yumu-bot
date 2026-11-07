package com.now.nowbot.browser

import com.now.nowbot.config.BrowserConfig
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock

internal class RenderPool(private val config: BrowserConfig) {
    private val lock = ReentrantLock()
    private val available = lock.newCondition()
    private val slots: List<RenderSlot> = List(config.poolSize) { RenderSlot(it, config) }
    private val idle = ArrayDeque(slots)
    private var borrowed = 0
    private var closed = false

    fun render(html: String, options: ScreenshotOptions): ByteArray {
        val slot = acquire()
        val future: Future<ByteArray> = try {
            slot.executor.submit(
                Callable {
                    try {
                        slot.execute(html, options)
                    } finally {
                        release(slot)
                    }
                },
            )
        } catch (e: RejectedExecutionException) {
            release(slot)
            throw RenderException("渲染线程拒绝任务", e)
        }

        val resultWaitMillis = config.renderTimeoutMillis + config.resultWaitGraceMillis
        try {
            return future.get(resultWaitMillis, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            throw RenderException(
                "等待渲染结果超时（${resultWaitMillis} ms）；任务可能仍在执行，槽位会在任务真正结束后归还",
                e,
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RenderException("等待渲染结果时被中断；截图任务会继续完成并归还槽位", e)
        } catch (e: ExecutionException) {
            val cause = e.cause
            if (cause is RenderException) {
                throw cause
            }
            throw RenderException(cause?.message ?: "渲染任务异常", cause)
        }
    }

    fun close(): Boolean {
        var interrupted = false
        var clean = true
        val borrowDeadlineNanos =
            System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(config.closeWaitTimeoutMillis)
        lock.lock()
        try {
            if (!closed) {
                closed = true
                available.signalAll()
            }
            while (borrowed > 0) {
                val remaining = borrowDeadlineNanos - System.nanoTime()
                if (remaining <= 0L) {
                    clean = false
                    break
                }
                try {
                    available.awaitNanos(remaining)
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
        } finally {
            lock.unlock()
        }
        slots.forEach { slot ->
            if (!slot.close(config.closeCleanupTimeoutMillis)) {
                clean = false
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt()
        }
        return clean
    }

    private fun acquire(): RenderSlot {
        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(config.acquireTimeoutMillis)
        lock.lock()
        try {
            while (true) {
                if (closed) {
                    throw IllegalStateException("HtmlRenderer 已关闭")
                }
                val slot = idle.pollFirst()
                if (slot != null) {
                    borrowed += 1
                    return slot
                }
                val remaining = deadlineNanos - System.nanoTime()
                if (remaining <= 0L) {
                    throw RenderException(
                        "等待空闲渲染槽位超时（${config.acquireTimeoutMillis} ms，poolSize=${config.poolSize}）",
                    )
                }
                try {
                    available.awaitNanos(remaining)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw RenderException("等待空闲渲染槽位时被中断", e)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    private fun release(slot: RenderSlot) {
        lock.lock()
        try {
            borrowed -= 1
            if (!closed) {
                idle.addLast(slot)
            }
            available.signalAll()
        } finally {
            lock.unlock()
        }
    }
}
