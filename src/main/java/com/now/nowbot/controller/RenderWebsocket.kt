package com.now.nowbot.controller

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import com.now.nowbot.util.JacksonUtil
import jakarta.annotation.PreDestroy
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class RenderWebSocketHandler : TextWebSocketHandler() {
    private val scheduler = Executors.newScheduledThreadPool(2)
    private val log = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = JacksonUtil.mapper
    private val roundRobinCounter = AtomicInteger(0)

    private val activeSessions = ConcurrentHashMap<Int, WebSocketSession>()

    private val sessionLocks = ConcurrentHashMap<String, ReentrantLock>()

    // 存储挂起的请求：MessageId -> Future
    private val pendingRequests = ConcurrentHashMap<String, CompletableFuture<ByteArray>>()

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val response = objectMapper.readTree(message.payload)

            if (response.has("type") && response.get("type").asString() == "HEARTBEAT") {
                return
            }

            if (response.has("type") && response.get("type").asString() == "AUTH") {
                val pid = response.get("pid").asInt()

                if (session.attributes["PID"] == null) {
                    anonymousConnectionCount.decrementAndGet()
                }

                // 记录 PID
                session.attributes["PID"] = pid

                activeSessions.compute(pid) { _, existingSession ->
                    if (existingSession != null && existingSession.id != session.id) {
                        log.info("渲染服务器：检测到重复连接 [PID: $pid]，正在关闭旧连接 ${existingSession.id}")
                        try {
                            if (existingSession.isOpen) {
                                existingSession.close(CloseStatus(4001, "Replaced by new process connection"))
                            }
                        } catch (e: Exception) {
                            log.error("关闭旧 Session 失败", e)
                        }
                    }
                    session
                }

                log.info("渲染服务器：进程验证成功 [PID: $pid, Session: ${session.id}]")
                return
            }

            val messageId = response.get("messageId")?.asString()
            val status = response.get("status")?.asString()

            if (messageId != null && status == "success") {
                val dataNode = response.get("data")

                val bytes: ByteArray = when {
                    dataNode.isString -> Base64.getDecoder().decode(dataNode.asString())
                    dataNode.isObject && dataNode.has("data") -> {
                        val dataField = dataNode.get("data")
                        when {
                            dataField.isString -> Base64.getDecoder().decode(dataField.asString())
                            dataField.isBinary -> dataField.binaryValue()
                            else -> throw IllegalArgumentException("无法识别的 data 内部格式")
                        }
                    }
                    else -> throw IllegalArgumentException("无法识别的 data 结构")
                }

                pendingRequests.remove(messageId)?.complete(bytes)
            }
        } catch (e: Exception) {
            log.error("渲染服务器：解析 JS 返回消息失败", e)
        }
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val payload = message.payload
        val bytes = ByteArray(payload.remaining())
        payload.get(bytes)

        // 1. 分离头部 (前 36 字节是 UUID)
        val idLength = 36
        if (bytes.size <= idLength) return

        val messageId = String(bytes.copyOfRange(0, idLength), Charsets.UTF_8)

        // 2. 提取剩余的 PNG 数据
        val imageData = bytes.copyOfRange(idLength, bytes.size)

        // 3. 完成请求
        pendingRequests.remove(messageId)?.complete(imageData)
    }

    fun sendTask(path: String, payload: Any?, timeoutSeconds: Long = 30): CompletableFuture<ByteArray> {
        val available = activeSessions.values.filter { it.isOpen }
        if (available.isEmpty()) {
            throw IllegalStateException("渲染服务器：当前没有活跃的 JS 渲染进程")
        }

        // 优化：轮询 (Round-Robin) 负载均衡
        // 使用 and 0x7FFFFFFF 防止计数器溢出变为负数导致的数组越界
        val index = (roundRobinCounter.getAndIncrement() and 0x7FFFFFFF) % available.size
        val session = available.getOrNull(index) ?: throw IllegalStateException("渲染服务器：没有可用的渲染进程连接")

        val messageId = UUID.randomUUID().toString()
        val future = CompletableFuture<ByteArray>()
        pendingRequests[messageId] = future

        // 引入超时定时任务
        val timeoutTask = scheduler.schedule({
            if (pendingRequests.remove(messageId) != null) {
                log.warn("渲染服务器：请求超时 [ID: $messageId]，已从等待队列清理")
                future.completeExceptionally(TimeoutException("渲染服务器：任务超时：$messageId"))
            }
        }, timeoutSeconds, TimeUnit.SECONDS)

        // 当任务成功完成时，取消定时器（避免资源浪费）
        future.whenComplete { _: ByteArray?, _: Throwable? -> timeoutTask.cancel(false) }

        try {
            val requestMap = mapOf(
                "path" to path,
                "messageId" to messageId,
                "payload" to payload
            )
            val jsonString = objectMapper.writeValueAsString(requestMap)

            val lock = sessionLocks.computeIfAbsent(session.id) { ReentrantLock() }

            lock.withLock {
                if (session.isOpen) {
                    session.sendMessage(TextMessage(jsonString))
                } else {
                    throw IllegalStateException("Session 在发送前已关闭")
                }
            }
        } catch (e: Exception) {
            pendingRequests.remove(messageId)
            future.completeExceptionally(e)
        }

        return future
    }

    private val anonymousConnectionCount = AtomicInteger(0)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        // 如果还没认证的连接超过 10 个，直接拒绝新连接，保护内存
        if (anonymousConnectionCount.incrementAndGet() > 10) {
            anonymousConnectionCount.decrementAndGet()
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        // 清理该 Session 对应的锁
        sessionLocks.remove(session.id)

        val pid = session.attributes["PID"] as? Int

        if (pid != null) {
            activeSessions.remove(pid, session)
            log.info("渲染服务器：连接已关闭 [PID: $pid, Session: ${session.id}]")
        } else {
            // 如果是匿名连接关闭，减少计数
            anonymousConnectionCount.decrementAndGet()
        }
    }

    @PreDestroy
    fun shutdown() {
        scheduler.shutdown()
    }
}