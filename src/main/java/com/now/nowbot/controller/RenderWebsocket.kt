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

    // 记录正在进行 AUTH 鉴权的未认证连接数
    private val anonymousConnectionCount = AtomicInteger(0)

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val response = objectMapper.readTree(message.payload)

            if (response.has("type") && response.get("type").asString() == "HEARTBEAT") {
                return
            }

            if (response.has("type") && response.get("type").asString() == "AUTH") {
                val pid = response.get("pid").asInt()

                // 标记该 session 已通过验证，防止后续释放重复扣减计数器
                if (session.attributes.putIfAbsent("AUTHENTICATED", true) == null) {
                    anonymousConnectionCount.decrementAndGet()
                }

                session.attributes["PID"] = pid

                // 修复：先剔除旧 Session，在 ConcurrentHashMap 锁外部执行 close() 避免死锁
                val oldSession = activeSessions.put(pid, session)
                if (oldSession != null && oldSession.id != session.id) {
                    log.info("渲染服务器：检测到重复连接 [PID: $pid]，正在关闭旧连接 ${oldSession.id}")
                    CompletableFuture.runAsync {
                        try {
                            if (oldSession.isOpen) {
                                oldSession.close(CloseStatus(4001, "Replaced by new process connection"))
                            }
                        } catch (e: Exception) {
                            log.error("关闭旧 Session 失败", e)
                        }
                    }
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

        val idLength = 36
        if (bytes.size <= idLength) return

        val messageId = String(bytes.copyOfRange(0, idLength), Charsets.UTF_8)
        val imageData = bytes.copyOfRange(idLength, bytes.size)

        pendingRequests.remove(messageId)?.complete(imageData)
    }

    fun sendTask(path: String, payload: Any?, timeoutSeconds: Long = 30): CompletableFuture<ByteArray> {
        val available = activeSessions.values.filter { it.isOpen }
        if (available.isEmpty()) {
            throw IllegalStateException("渲染服务器：当前没有活跃的 JS 渲染进程")
        }

        val session = available.minByOrNull { s ->
            sessionActiveTasks.computeIfAbsent(s.id) { AtomicInteger(0) }.get()
        } ?: throw IllegalStateException("渲染服务器：没有可用的渲染进程连接")

        val messageId = UUID.randomUUID().toString()
        val future = CompletableFuture<ByteArray>()

        val taskCounter = sessionActiveTasks.computeIfAbsent(session.id) { AtomicInteger(0) }
        taskCounter.incrementAndGet()

        pendingRequests[messageId] = future

        val timeoutTask = scheduler.schedule({
            if (pendingRequests.remove(messageId) != null) {
                log.warn("渲染服务器：请求超时 [ID: $messageId, Session: ${session.id}]，已从等待队列清理")
                future.completeExceptionally(TimeoutException("渲染服务器：任务超时：$messageId"))
            }
        }, timeoutSeconds, TimeUnit.SECONDS)

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
            future.completeExceptionally(e)
        }

        return future
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        if (anonymousConnectionCount.incrementAndGet() > 10) {
            anonymousConnectionCount.decrementAndGet()
            log.warn("渲染服务器：未认证连接过多，拒绝新连接 ${session.id}")
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }

        session.binaryMessageSizeLimit = 6.MB.bytesInt
        session.textMessageSizeLimit = 128.KB.bytesInt
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        // 清理锁和计数器
        sessionLocks.remove(session.id)
        sessionActiveTasks.remove(session.id)

        // 如果连接关闭时仍未 AUTH 成功，扣减未认证计数
        if (session.attributes.remove("AUTHENTICATED") != null) {
            // 已验证过，不做 anonymous 扣减
        } else {
            anonymousConnectionCount.decrementAndGet()
        }

        val pid = session.attributes["PID"] as? Int
        if (pid != null) {
            activeSessions.remove(pid, session)
            log.info("渲染服务器：连接已关闭 [PID: $pid, Session: ${session.id}]")
        }
    }

    @PreDestroy
    fun shutdown() {
        scheduler.shutdown()
    }
}