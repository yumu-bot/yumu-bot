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
            return getLock(event.group.id, event.sender.id)
        }
        return getSenderLock(event.sender.id)
    }

    fun getLock(event: MessageEvent, offTime: Long, check: ((MessageEvent?) -> Boolean)? = null): Lock {
        if (event is GroupMessageEvent) {
            return getLock(event.group.id, event.sender.id, offTime, check)
        }
        return getSenderLock(event.sender.id, offTime, check)
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
                    message.subject.id == group
                } else {
                    message.subject.id == group && message.sender.id == send
                }
            }

            null -> false

            else -> (group == null && message.sender.id == send)
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
internal class OLock(var group: Long?, var send: Long?, var offTime: Long = 0, var check: ((MessageEvent?) -> Boolean)? = null) :
    ASyncMessageUtil.Lock {
    var event: MessageEvent? = null
    val condition: Condition = reentrantLock.newCondition()

    override fun checkAdd(message: MessageEvent?) {
        if (ASyncMessageUtil.check(message, this.group, this.send) && check?.invoke(message) == true) {
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
        try {
            reentrantLock.lock()
            if (event == null) {
                condition.await(offTime, TimeUnit.MILLISECONDS)
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
        private val reentrantLock = ReentrantLock()
    }
}