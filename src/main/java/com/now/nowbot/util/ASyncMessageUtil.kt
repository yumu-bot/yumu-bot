package com.now.nowbot.util

import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

object ASyncMessageUtil {
    private const val OFF_TIME = 60 * 60 * 1000L
    private val lockList: MutableSet<Lock?> = CopyOnWriteArraySet<Lock?>()

    /**
     * 指定群聊跟发送人的锁
     * 
     */
    fun getLock(group: Long, send: Long): Lock {
        return getLock(group, send, OFF_TIME, null)
    }

    fun getLock(group: Long?, send: Long?, offTime: Long, check: ((MessageEvent?) -> Boolean)? = null): Lock {
        return OLock(group, send, offTime, check)
    }

    fun getLock(event: MessageEvent, offTime: Long): Lock {
        return getLock(event, offTime, null)
    }

    fun getLock(event: MessageEvent): Lock {
        if (event is GroupMessageEvent) {
            return getLock(event.group.contactID, event.sender.contactID)
        }
        return getSenderLock(event.sender.contactID)
    }

    fun getLock(event: MessageEvent, offTime: Long, check: ((MessageEvent?) -> Boolean)? = null): Lock {
        if (event is GroupMessageEvent) {
            return getLock(event.group.contactID, event.sender.contactID, offTime, check)
        }
        return getSenderLock(event.sender.contactID, offTime, check)
    }

    fun getSenderLock(send: Long, offTime: Long, check: ((MessageEvent?) -> Boolean)? = null): Lock {
        return OLock(null, send, offTime, check)
    }

    /**
     * 指定发送人的锁(无论哪个群)
     * 
     */
    fun getSenderLock(send: Long): Lock {
        return getSenderLock(send, OFF_TIME, null)
    }

    /**
     * 指定群的锁,无论发送人
     * 
     */
    fun getGroupLock(group: Long): Lock {
        return getGroupLock(group, OFF_TIME)
    }

    fun getGroupLock(group: Long, offTime: Long): Lock {
        return OLock(group, null, offTime, null)
    }

    /**
     * 在event监听使用
     * 
     */
    fun put(message: MessageEvent?) {
        lockList.forEach { 
            it!!.checkAdd(message)
        }
    }

    fun check(message: MessageEvent?, group: Long?, send: Long?): Boolean {
        return when(message) {
            is GroupMessageEvent -> {
                if (send == null) {
                    message.subject.contactID == group
                } else {
                    message.subject.contactID == group && message.sender.contactID == send
                }
            }

            null -> false

            else -> (group == null && message.sender.contactID == send)
        }
    }

    fun remove(lock: Lock?) {
        lockList.remove(lock)
    }

    fun add(lock: Lock?) {
        lockList.add(lock)
    }

    interface Lock {
        fun checkAdd(message: MessageEvent?)

        fun get(): MessageEvent?
    }
}

// 使用BlockingQueue实现的锁 有可能会出现内存泄漏

// 使用LockSupport实现的锁 不会释放锁, 但是性能最好

// 使用ReentrantLock实现的锁 综合最佳
internal class OLock(
    var group: Long?,
    var send: Long?,
    var offTime: Long = 0,
    var check: ((MessageEvent?) -> Boolean)? = null
) : ASyncMessageUtil.Lock {

    private var event: MessageEvent? = null
    private val condition: Condition = reentrantLock.newCondition()

    override fun checkAdd(message: MessageEvent?) {
        val checkPassed = check?.invoke(message) ?: true

        if (ASyncMessageUtil.check(message, this.group, this.send) && checkPassed) {
            reentrantLock.lock()
            try {
                this.event = message
                condition.signalAll()
            } finally {
                reentrantLock.unlock()
            }
        }
    }

    override fun get(): MessageEvent? {
        ASyncMessageUtil.add(this)
        reentrantLock.lock()

        try {
            var remainingNanos = TimeUnit.MILLISECONDS.toNanos(offTime)

            while (event == null) {
                if (remainingNanos <= 0L) {
                    break
                }

                remainingNanos = condition.awaitNanos(remainingNanos)
            }

            return event
        } catch (_: InterruptedException) {
            return null
        } finally {
            reentrantLock.unlock()
            ASyncMessageUtil.remove(this)
            event = null
        }
    }

    companion object {
        // 对应 Java 的 static final ReentrantLock
        // 所有 OLock 实例共用这一把大锁，这与 Java 逻辑一致
        private val reentrantLock = ReentrantLock()
    }
}