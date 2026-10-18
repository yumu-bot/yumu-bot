package com.now.nowbot.util

import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageReceipt
import com.now.nowbot.throwable.TipsRuntimeException
import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object AsyncMessageUtil {
    private const val DEFAULT_TIMEOUT = 60 * 60 * 1000L
    private val lockManager = LockManager()

    // doubleCheck 正在等待回答的会话，按和 lockManager 相同的 key 保存。
    // 单独存放是为了让新指令只取消这些会话，不影响 AsyncLock。
    private val promptFutures = ConcurrentHashMap<String, CompletableFuture<MessageEvent>>()

    // ========== 高层 API ==========

    /**
     * @param keywords 命中其中任意一个关键字都算「确认」。传入空列表则任何回复都会走 onSuccess、忽视 onFailure。
     * @param onFailure 收到不匹配任何 [keywords] 的回复时调用，参数就是这条回复事件
     */
    fun doubleCheck(
        event: MessageEvent,
        keywords: List<String> = listOf("OK"),
        onCheck: () -> MessageReceipt? = { event.reply("是否要执行操作？回复 ${keywords.joinToString("、")} 确认。") },
        onOverTime: () -> Unit = { throw TipsRuntimeException("超时了。") },
        onFailure: (MessageEvent) -> Unit = { throw TipsRuntimeException("已取消操作。") },
        onSuccess: (MessageEvent) -> Unit = {},
        timeout: Duration = 60.seconds,
        anyoneCanResponse: Boolean = false,
    ) {
        val lockKey = if (anyoneCanResponse) {
            if (event.subject is Group) {
                generateKey(group = event.subject.contactID)
            } else {
                generateKey(sender = event.sender.contactID)
            }
        } else {
            generateKey(event)
        }

        val lock = lockManager.getOrCreateLock(lockKey)

        if (!lock.tryLock()) {
            throw TipsRuntimeException("操作正在进行中，请勿重复发起")
        }

        // 发送提示消息
        val receipt: MessageReceipt? = onCheck()

        // doubleCheck 的 future 单独保存，和 AsyncLock 的 future 分开：
        // 这样新指令可以静默取消正在等待回答的会话，而不会误伤其它等待者。
        val future = CompletableFuture<MessageEvent>()
        promptFutures[lockKey] = future

        try {
            // 等待响应
            val result = try {
                future.get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                null
            } catch (e: ExecutionException) {
                // 被新指令取消：提示消息已经没用了，撤回后静默结束
                if (e.cause is PendingCancelledException) {
                    receipt.recallAndLog()
                    return
                }
                throw e
            }

            if (result == null) {
                receipt.recallAndLog()

                onOverTime()
                return
            }

            // 尝试撤回提示消息
            receipt.recallAndLog()

            // 处理响应：空列表代表「任何回复都算确认」，否则任意一个关键字命中都算确认
            val confirmed = keywords.isEmpty() ||
                    keywords.any { result.rawMessage.contains(it, ignoreCase = true) }

            if (confirmed) {
                onSuccess(result)
            } else {
                onFailure(result)
            }

        } catch (_: TimeoutException) {
            receipt.recallAndLog()
            onOverTime()
        } finally {
            promptFutures.remove(lockKey, future)
            lockManager.cleanup(lockKey)
            lock.unlock()
        }
    }

    /**
     * 「反向」的一问一答：回复 [keywords] 或超时都算取消，回复其他内容才执行操作。
     *
     * 和 [doubleCheck] 的区别只是语义颠倒：[doubleCheck] 里 [keywords] 代表「确认」，
     * 这里 [keywords] 代表「取消」。适合「先把内容展示给用户，用户不想要就回复一个固定词退出，
     * 想要就把回复内容本身当成参数」的场景。
     *
     * 内部直接委托给 [doubleCheck]，所以锁、撤回提示消息、超时等行为完全一致。
     *
     * @param keywords 取消操作的关键字，回复中包含其中任意一个即视为取消。传入空列表时任何回复都会取消。
     * @param onCancel 命中 [keywords] 或超时时调用，默认取消。
     * @param onInput 收到非取消回复时调用，参数就是这条回复事件（内容是原文，未做任何裁剪）。
     */
    fun doubleCancel(
        event: MessageEvent,
        keywords: List<String> = listOf("OK"),
        onCheck: () -> MessageReceipt? = { event.reply("回复 ${keywords.joinToString("、")} 取消操作，回复其他内容则继续。") },
        onCancel: () -> Unit = { throw TipsRuntimeException("已取消操作。") },
        onInput: (MessageEvent) -> Unit = {},
        onOverTime: () -> Unit = onCancel,
        timeout: Duration = 60.seconds,
        anyoneCanResponse: Boolean = false,
    ) {
        doubleCheck(
            event = event,
            keywords = keywords,
            onCheck = onCheck,
            onOverTime = onOverTime,
            onSuccess = { onCancel() },
            onFailure = onInput,
            timeout = timeout,
            anyoneCanResponse = anyoneCanResponse,
        )
    }

    fun MessageReceipt?.recallAndLog() {
        try {
            this?.recall()
        } catch (e: Exception) {
            log.warn("撤回消息失败: ${e.message}")
        }
    }

    fun <T> doubleCheckSync(
        event: MessageEvent,
        keywords: List<String> = listOf("OK"),
        onCheck: () -> MessageReceipt? = { event.reply("是否要执行操作？回复 ${keywords.joinToString("、")} 确认。") },
        onSuccess: (MessageEvent) -> T,
        defaultValue: T,
        timeout: Duration = 60.seconds,
        anyoneCanResponse: Boolean = false,
    ): T {
        val latch = CountDownLatch(1)
        var result: T = defaultValue
        var completed = false

        doubleCheck(
            event = event,
            keywords = keywords,
            onCheck = onCheck,
            onSuccess = {
                result = onSuccess(it)
                completed = true
                latch.countDown()
            },
            onOverTime = { latch.countDown() },
            onFailure = { latch.countDown() },
            timeout = timeout,
            anyoneCanResponse = anyoneCanResponse
        )

        return try {
            latch.await(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            if (completed) result else defaultValue
        } catch (_: InterruptedException) {
            defaultValue
        }
    }

    // ========== getLock 方法族 ==========

    /**
     * 获取锁（使用默认超时时间）
     */
    fun getLock(event: MessageEvent): AsyncLock {
        val key = generateKey(event)
        return AsyncLockImpl(key, lockManager.getOrCreateLock(key), DEFAULT_TIMEOUT)
    }

    /**
     * 获取锁（指定超时时间，毫秒）
     */
    fun getLock(event: MessageEvent, offTime: Long): AsyncLock {
        val key = generateKey(event)
        return AsyncLockImpl(key, lockManager.getOrCreateLock(key), offTime)
    }

    /**
     * 获取锁（指定超时时间，带检查函数）
     */
    fun getLock(event: MessageEvent, offTime: Long, check: ((MessageEvent?) -> Boolean)? = null): AsyncLock {
        val key = generateKey(event)
        return lockManager.getOrCreateLock(key).let { lock ->
            AsyncLockImpl(key, lock, offTime).apply {
                if (check != null) {
                    // 存储检查函数，在 notify 时使用
                    lockManager.setCheck(key, check)
                }
            }
        }
    }

    /**
     * 获取群组锁（指定群和发送人）
     */
    fun getLock(group: Long, sender: Long): AsyncLock {
        val key = generateKey(group, sender)
        return AsyncLockImpl(key, lockManager.getOrCreateLock(key), DEFAULT_TIMEOUT)
    }

    fun getLock(group: Long, sender: Long, offTime: Long): AsyncLock {
        val key = generateKey(group, sender)
        return AsyncLockImpl(key, lockManager.getOrCreateLock(key), offTime)
    }

    fun getLock(group: Long, sender: Long, offTime: Long, check: ((MessageEvent?) -> Boolean)? = null): AsyncLock {
        val key = generateKey(group, sender)
        return lockManager.getOrCreateLock(key).let { lock ->
            AsyncLockImpl(key, lock, offTime).apply {
                if (check != null) {
                    lockManager.setCheck(key, check)
                }
            }
        }
    }

    /**
     * 获取发送人锁（所有群）
     */
    fun getSenderLock(sender: Long): AsyncLock {
        val key = generateKey(sender = sender)
        return AsyncLockImpl(key, lockManager.getOrCreateLock(key), DEFAULT_TIMEOUT)
    }

    fun getSenderLock(sender: Long, offTime: Long): AsyncLock {
        val key = generateKey(sender = sender)
        return AsyncLockImpl(key, lockManager.getOrCreateLock(key), offTime)
    }

    fun getSenderLock(sender: Long, offTime: Long, check: ((MessageEvent?) -> Boolean)? = null): AsyncLock {
        val key = generateKey(sender = sender)
        return lockManager.getOrCreateLock(key).let { lock ->
            AsyncLockImpl(key, lock, offTime).apply {
                if (check != null) {
                    lockManager.setCheck(key, check)
                }
            }
        }
    }

    /**
     * 获取群组锁（所有发送人）
     */
    fun getGroupLock(group: Long): AsyncLock {
        val key = generateKey(group = group)
        return AsyncLockImpl(key, lockManager.getOrCreateLock(key), DEFAULT_TIMEOUT)
    }

    fun getGroupLock(group: Long, offTime: Long): AsyncLock {
        val key = generateKey(group = group)
        return AsyncLockImpl(key, lockManager.getOrCreateLock(key), offTime)
    }

    // ========== 底层 API ==========

    /**
     * 通知匹配的锁（用于消息分发）
     */
    fun notify(message: MessageEvent): Boolean {
        val key = generateKey(message)
        // 检查是否有自定义的检查函数
        val check = lockManager.getCheck(key)
        if (check != null && !check(message)) return false

        val lockCompleted = lockManager.completeFuture(key, message)
        val promptCompleted = promptFutures[key]?.complete(message) ?: false
        return lockCompleted || promptCompleted
    }

    /**
     * 手动 put 消息到所有等待者。
     *
     * 普通回复会同时唤醒 doubleCheck 和 AsyncLock；
     * 指令消息不应调用这里，见 [cancelPrompts] 和 [putLocks]。
     */
    fun put(message: MessageEvent?) {
        putLocks(message)
        completePromptFutures(message)
    }

    /**
     * 只唤醒 AsyncLock（getLock 系列）的等待者，保持新指令到达时的原有行为。
     */
    fun putLocks(message: MessageEvent?) {
        if (message == null) return
        lockManager.getAllKeys().forEach { key ->
            if (matchKey(key, message)) {
                lockManager.completeFuture(key, message)
            }
        }
    }

    /**
     * 静默取消正在等待回复的 doubleCheck 会话。
     *
     * 新指令到达时调用：指令不应该被旧会话当成回答，
     * 也不能让旧会话在超时后再补一条「操作超时」。
     */
    fun cancelPrompts(message: MessageEvent) {
        promptFutures.forEach { (key, future) ->
            if (matchKey(key, message)) {
                future.completeExceptionally(PendingCancelledException())
            }
        }
    }

    private fun completePromptFutures(message: MessageEvent?) {
        if (message == null) return
        promptFutures.forEach { (key, future) ->
            if (matchKey(key, message)) {
                future.complete(message)
            }
        }
    }

    private fun matchKey(key: String, message: MessageEvent): Boolean {
        // 解析 key 并匹配
        val parts = key.split(":")
        return when (parts.size) {
            3 if parts[0] == "group" -> {
                val groupId = parts[1].toLongOrNull()
                val senderId = parts[2].toLongOrNull()
                message is GroupMessageEvent &&
                        (groupId == null || message.group.contactID == groupId) &&
                        (senderId == null || message.sender.contactID == senderId)
            }
            2 if parts[0] == "sender" -> {
                val senderId = parts[1].toLongOrNull()
                senderId == null || message.sender.contactID == senderId
            }
            else -> false
        }
    }

    // ========== Key 生成 ==========

    private fun generateKey(event: MessageEvent): String {
        return when (event) {
            is GroupMessageEvent -> "group:${event.group.contactID}:${event.sender.contactID}"
            else -> "sender:${event.sender.contactID}"
        }
    }

    private fun generateKey(group: Long? = null, sender: Long? = null): String {
        return when {
            group != null && sender != null -> "group:$group:$sender"
            group != null -> "group:$group:*"
            sender != null -> "sender:$sender"
            else -> throw IllegalArgumentException("至少需要指定群或发送人")
        }
    }

    // ========== 锁实现 ==========

    interface AsyncLock {
        fun tryLock(): Boolean
        fun unlock()
        fun isLocked(): Boolean
        fun await(): MessageEvent?  // 等待通知
        fun await(timeout: Long): MessageEvent?  // 指定超时
        fun complete(message: MessageEvent): Boolean  // 手动完成
    }

    private class AsyncLockImpl(
        private val key: String,
        private val lock: ReentrantLock,
        private val timeout: Long
    ) : AsyncLock {
        private val future = CompletableFuture<MessageEvent>()

        init {
            lockManager.registerFuture(key, future, timeout)
        }

        override fun tryLock(): Boolean = lock.tryLock()

        override fun unlock() {
            lock.unlock()
            // 解锁时检查是否需要清理
            if (!lock.isLocked) {
                lockManager.cleanupIfExpired(key)
            }
        }

        override fun isLocked(): Boolean = lock.isLocked

        override fun await(): MessageEvent? {
            return try {
                future.get(timeout, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                null
            }
        }

        override fun await(timeout: Long): MessageEvent? {
            return try {
                future.get(timeout, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                null
            }
        }

        override fun complete(message: MessageEvent): Boolean {
            return future.complete(message)
        }
    }

    // ========== 锁管理器 ==========

    private class LockManager {
        private val locks = ConcurrentHashMap<String, ReentrantLock>()
        private val futures = ConcurrentHashMap<String, CompletableFuture<MessageEvent>>()
        private val checks = ConcurrentHashMap<String, (MessageEvent?) -> Boolean>()
        private val timeouts = ConcurrentHashMap<String, Long>()

        fun getOrCreateLock(key: String): ReentrantLock {
            return locks.computeIfAbsent(key) { ReentrantLock() }
        }

        fun registerFuture(key: String, future: CompletableFuture<MessageEvent>, timeout: Long) {
            futures[key] = future
            timeouts[key] = System.currentTimeMillis() + timeout
        }

        fun getOrCreateFuture(key: String, timeout: Long): CompletableFuture<MessageEvent> {
            return futures.computeIfAbsent(key) {
                CompletableFuture<MessageEvent>()
            }.also {
                timeouts[key] = System.currentTimeMillis() + timeout
            }
        }

        fun completeFuture(key: String, message: MessageEvent): Boolean {
            val future = futures[key] ?: return false
            return !future.isDone && future.complete(message)
        }

        fun setCheck(key: String, check: (MessageEvent?) -> Boolean) {
            checks[key] = check
        }

        fun getCheck(key: String): ((MessageEvent?) -> Boolean)? {
            return checks[key]
        }

        fun cleanup(key: String) {
            futures.remove(key)
            timeouts.remove(key)
            checks.remove(key)
        }

        fun cleanupIfExpired(key: String) {
            val timeout = timeouts[key] ?: return
            if (System.currentTimeMillis() > timeout) {
                cleanup(key)
                locks.remove(key)
            }
        }

        fun getAllKeys(): Set<String> = futures.keys

        fun cleanExpiredLocks() {
            val now = System.currentTimeMillis()
            timeouts.entries.removeIf { (key, timeout) ->
                if (timeout < now) {
                    futures.remove(key)
                    checks.remove(key)
                    locks.remove(key)
                    true
                } else {
                    false
                }
            }
        }
    }

    /**
     * 等待中的 doubleCheck 被新指令静默取消。
     *
     * 用异常而不是普通返回值，是为了和「用户发来回复」「等待超时」区分开：
     * 取消时既不执行 onSuccess，也不执行 onFailure / onOverTime，也不发提示。
     */
    private class PendingCancelledException : CancellationException("等待中的会话已被新指令取消")

    private val log = LoggerFactory.getLogger(AsyncMessageUtil::class.java)
}