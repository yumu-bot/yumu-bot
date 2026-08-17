package com.now.nowbot.controller

import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.KB
import com.now.nowbot.util.MB
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.BinaryMessage
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

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val response = objectMapper.readTree(message.payload)

            if (response.has("type") && response.get("type").asString() == "HEARTBEAT") {
                return
            }

            if (response.has("type") && response.get("type").asString() == "AUTH") {
                // ... 保持你原有的 AUTH 处理逻辑不变 ...
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

            if (messageId != null) {
                if (status == "success") {
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

                } else if (status == "error") {
                    val errorMessage = response.get("error")?.asString() ?: "Node.js 端发生未知异常"
                    log.error("渲染服务器：收到 JS 进程错误响应 [ID: {}]: {}", messageId, errorMessage)

                    pendingRequests.remove(messageId)?.completeExceptionally(NetworkException.RenderModuleException.InternalServerError())
                }
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