package com.now.nowbot.util

import com.now.nowbot.util.AsyncMethodExecutor.Runnable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.InterruptedException
import java.lang.System
import java.lang.Thread
import java.time.Duration
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

    /**
     * 异步执行不需要返回的结果。
     * 这个方法不等待结果返回，直接进行下一步。如果需要等待所有异步操作完成，请使用 awaitRunnableExecute
     */
    @JvmStatic
    fun asyncRunnableExecute(works: Collection<Runnable>) {
        works.map { w: Runnable ->
            (Runnable {
                try {
                    w.run()
                } catch (e: Throwable) {
                    log.error("Async error", e)
                }
            })
        }.forEach { task: Runnable -> Thread.startVirtualThread(task) }
    }

    /**
     * 异步执行不需要返回的结果，并等待至所有操作都完成。
     * 这个方法会等待结果返回，不直接进行下一步。如果不需要等待所有异步操作完成，请使用 asyncRunnableExecute
     */
    @JvmStatic
    fun awaitRunnableExecute(works: Collection<Runnable>, timeout: Duration = Duration.ofMinutes(4)) {
        val lock = CountDownLatch(works.size)
        works.map { w: Runnable ->
            (Runnable {
                try {
                    w.run()
                } catch (e: Throwable) {
                    log.error("Async error", e)
                } finally {
                    lock.countDown()
                }
            })
        }.forEach { task: Runnable -> Thread.startVirtualThread(task) }

        lock.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }
    @JvmStatic
    fun <T> awaitSupplierExecute(works: Collection<Supplier<T>>): List<T?> {
        return awaitSupplierExecute(works, Duration.ofMinutes(4))
    }

    /**
     * 异步执行需要返回的结果，并等待至所有操作都完成。
     * 这个方法会等待结果返回，不直接进行下一步。如果不需要返回结果（void 方法），请使用 awaitRunnableExecute
     */
    @JvmStatic
    fun <T> awaitSupplierExecute(works: Collection<Supplier<T>>, timeout: Duration = Duration.ofMinutes(4)): List<T?> {
        val size = works.size
        val lock = CountDownLatch(size)
        val results: MutableList<T?> = LinkedList()
        works.map { w: Supplier<T> ->
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
            lock.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
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
