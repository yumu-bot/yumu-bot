package com.now.nowbot.controller

import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.KB
import com.now.nowbot.util.MB
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

@Component
class RenderWebSocketHandler : TextWebSocketHandler() {
    private val scheduler = Executors.newScheduledThreadPool(2)
    private val log = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = JacksonUtil.mapper

    private val activeSessions = ConcurrentHashMap<Int, WebSocketSession>()
    private val sessionLocks = ConcurrentHashMap<String, ReentrantLock>()

    private val sessionActiveTasks = ConcurrentHashMap<String, AtomicInteger>()

    private val pendingRequests = ConcurrentHashMap<String, CompletableFuture<ByteArray>>()

    fun sendTask(path: String, payload: Any?, timeoutSeconds: Long = 30): CompletableFuture<ByteArray> {
        // 1. 过滤出依然开启的 Session
        val available = activeSessions.values.filter { it.isOpen }
        if (available.isEmpty()) {
            throw IllegalStateException("渲染服务器：当前没有活跃的 JS 渲染进程")
        }

        // 2. 改进负载均衡：优先挑选当前“挂起任务最少”的 Session (Least Connections)
        val session = available.minByOrNull { session ->
            sessionActiveTasks.computeIfAbsent(session.id) { AtomicInteger(0) }.get()
        } ?: throw IllegalStateException("渲染服务器：没有可用的渲染进程连接")

        val messageId = UUID.randomUUID().toString()
        val future = CompletableFuture<ByteArray>()

        val taskCounter = sessionActiveTasks.computeIfAbsent(session.id) { AtomicInteger(0) }
        taskCounter.incrementAndGet()

        pendingRequests[messageId] = future

        // 3. 超时定时任务
        val timeoutTask = scheduler.schedule({
            if (pendingRequests.remove(messageId) != null) {
                log.warn("渲染服务器：请求超时 [ID: $messageId, Session: ${session.id}]，已从等待队列清理")
                future.completeExceptionally(TimeoutException("渲染服务器：任务超时：$messageId"))
            }
        }, timeoutSeconds, TimeUnit.SECONDS)

        // 4. 完成时计数器减一 & 取消定时器
        future.whenComplete { _: ByteArray, _: Throwable ->
            timeoutTask.cancel(false)
            taskCounter.decrementAndGet()
        }

        try {
            val requestMap = mapOf(
                "path" to path,
                "messageId" to messageId,
                "payload" to payload
            )
            val jsonString = objectMapper.writeValueAsString(requestMap)

            val lock = sessionLocks.computeIfAbsent(session.id) { ReentrantLock() }

            // 尝试非阻塞拿锁，若拿不到或发送异常，迅速跳过并标记失败
            val acquired = lock.tryLock(5, TimeUnit.SECONDS)
            if (acquired) {
                try {
                    if (session.isOpen) {
                        session.sendMessage(TextMessage(jsonString))
                    } else {
                        throw IllegalStateException("Session 在发送前已关闭")
                    }
                } finally {
                    lock.unlock()
                }
            } else {
                throw IllegalStateException("渲染服务器：获取 Session 锁超时，网络可能存在拥堵")
            }
        } catch (e: Exception) {
            pendingRequests.remove(messageId)
            taskCounter.decrementAndGet()
            future.completeExceptionally(e)
        }

        return future
    }

    private val anonymousConnectionCount = AtomicInteger(0)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        if (anonymousConnectionCount.incrementAndGet() > 10) {
            anonymousConnectionCount.decrementAndGet()
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }

        session.binaryMessageSizeLimit = 6.MB.bytesInt
        session.textMessageSizeLimit = 128.KB.bytesInt
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessionLocks.remove(session.id)
        sessionActiveTasks.remove(session.id)

        val pid = session.attributes["PID"] as? Int
        if (pid != null) {
            activeSessions.remove(pid, session)
            log.info("渲染服务器：连接已关闭 [PID: $pid, Session: ${session.id}]")
        } else {
            anonymousConnectionCount.decrementAndGet()
        }
    }

    @PreDestroy
    fun shutdown() {
        scheduler.shutdown()
    }
}