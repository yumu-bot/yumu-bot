package com.now.nowbot.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random

object AsyncMethodExecutor {
    val log: Logger = LoggerFactory.getLogger(AsyncMethodExecutor::class.java)
    val random = Random(System.currentTimeMillis())

    @Throws(Exception::class)
    @Suppress("UNCHECKED_CAST")
    private fun <T> waitForResult(lock: Condition, key: Any, defaultValue: T): T {
        var countDownLock: CountDownLatch? = null
        try {
            reentrantLock.lock()
            Util.add(lock)
            lock.await()
            reentrantLock.unlock()
            countDownLock = countDownLocks[key]
            val result = results[key]
            if (result is Exception) {
                throw result
            }

            return result as T
        } catch (ignore: InterruptedException) {
            return defaultValue
        } finally {
            countDownLock?.countDown()
        }
    }

    private val reentrantLock = ReentrantLock()
    private val countDownLocks = ConcurrentHashMap<Any, CountDownLatch>()
    private val locks = ConcurrentHashMap<Any, Condition>()
    private val results = ConcurrentHashMap<Any, Any>()

    @JvmStatic
    @Suppress("unused")
    @Throws(Exception::class)
    fun <T : Any> execute(supplier: Supplier<T>, key: Any, defaultValue: T?): T? {
        reentrantLock.lock()
        val hasLock = locks.containsKey(key)
        val lock = locks.computeIfAbsent(key) { s: Any? -> reentrantLock.newCondition() }
        reentrantLock.unlock()
        return if (hasLock) {
            waitForResult(lock, key, defaultValue)
        } else {
            getResult(lock, key, supplier)
        }
    }

    @Throws(Exception::class)
    fun <T : Any> execute(supplier: Supplier<T>, key: Any, getDefault: Supplier<T?>): T? {
        reentrantLock.lock()
        val hasLock = locks.containsKey(key)
        val lock = locks.computeIfAbsent(key) { s: Any? -> reentrantLock.newCondition() }
        reentrantLock.unlock()
        return if (hasLock) {
            waitForResult(lock, key, getDefault)
        } else {
            getResult(lock, key, supplier)
        }
    }

    @Throws(Exception::class)
    @Suppress("UNCHECKED_CAST")
    private fun <T> waitForResult(lock: Condition, key: Any, getDefault: Supplier<T>): T {
        var countDownLock: CountDownLatch? = null
        try {
            reentrantLock.lock()
            Util.add(lock)
            lock.await()
            reentrantLock.unlock()
            countDownLock = countDownLocks[key]
            val result = results[key]
            if (result is Exception) {
                throw result
            }
            return result as T
        } catch (ignore: InterruptedException) {
            return getDefault.get()
        } finally {
            countDownLock?.countDown()
        }
    }

    @Throws(Exception::class)
    private fun <T : Any> getResult(lock: Condition, key: Any, supplier: Supplier<T>): T {
        try {
            val result = supplier.get()
            results[key] = result
            return result
        } catch (e: Exception) {
            results[key] = e
            throw e
        } finally {
            reentrantLock.lock()
            val locksSum = Util.getAndRemove(lock)
            log.info("sum : {}", locksSum)
            val count = countDownLocks.computeIfAbsent(key) { k: Any? -> CountDownLatch(locksSum) }
            lock.signalAll()
            reentrantLock.unlock()
            if (!count.await(5, TimeUnit.SECONDS)) {
                if (locksSum > 0) log.warn("wait to long")
            }
            results.remove(key)
            locks.remove(key)
            countDownLocks.remove(key)
        }
    }

    @Suppress("unused")
    @Throws(Exception::class)
    fun execute(work: Runnable, key: Any) {
        reentrantLock.lock()
        val hasLock = locks.containsKey(key)
        val lock = locks.computeIfAbsent(key) { s: Any? -> reentrantLock.newCondition() }
        reentrantLock.unlock()

        if (hasLock) {
            try {
                reentrantLock.lock()
                lock.await()
                reentrantLock.unlock()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            return
        }
        try {
            work.run()
        } finally {
            reentrantLock.lock()
            lock.signalAll()
            reentrantLock.unlock()
            locks.remove(key)
        }
    }

    fun AsyncRunnable(works: Collection<Runnable>) {
        works.stream().map { w: Runnable ->
            (Runnable {
                try {
                    w.run()
                } catch (e: Throwable) {
                    log.error("Async error", e)
                }
            })
        }.forEach { task: Runnable -> Thread.startVirtualThread(task) }
    }

    @JvmStatic
    fun <T> AsyncSupplier(works: Collection<Supplier<T>>): List<T?> {
        val size = works.size
        val lock = CountDownLatch(size)
        val results: MutableList<T?> = LinkedList()
        works.stream().map { w: Supplier<T> ->
            Runnable {
                try {
                    val result = w.get()
                    results.add(result)
                } catch (e: Exception) {
                    results.add(null)
                    log.error("AsyncSupplier error", e)
                } finally {
                    lock.countDown()
                }
            }
        }.forEach { task: Runnable -> Thread.startVirtualThread(task) }
        try {
            lock.await(120, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            log.error("lock error", e)
        }
        return results
    }

    fun interface Supplier<T> : java.util.function.Supplier<T> {
        override fun get(): T
    }

    fun interface Runnable : java.lang.Runnable {
        @Throws(Exception::class)
        override fun run()
    }

    @Throws(Exception::class)
    inline fun <T> doRetry(retry: Int, crossinline supplier: () -> T): T {
        var result: T
        if (retry <= 0) {
            throw IllegalArgumentException("retry must be greater than 0")
        }
        var err: Exception? = null
        for (i in 0 until retry) {
            try {
                result = supplier()
                return result
            } catch (e: Exception) {
                err = e
                log.error("retry $retry error", e)
                Thread.sleep(random.nextLong(800, 1000) * (1 shl i))
            }
        }
        throw err!!
    }

    private object Util {
        var conditionCount: ConcurrentHashMap<Condition, Int> = ConcurrentHashMap()

        fun add(lock: Condition) {
            conditionCount.putIfAbsent(lock, 0)
            conditionCount.computeIfPresent(lock) { k: Condition?, v: Int -> v + 1 }
        }

        fun getAndRemove(lock: Condition): Int {
            val count = conditionCount.remove(lock)
            return if (Objects.nonNull(count)) count!! else 0
        }
    }
}
