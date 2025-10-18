package com.now.nowbot.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.StructuredTaskScope.ShutdownOnFailure
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
        val lock = locks.computeIfAbsent(key) { reentrantLock.newCondition() }
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
        val lock = locks.computeIfAbsent(key) { reentrantLock.newCondition() }
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
            log.debug("异步操作：${supplier.javaClass.simpleName} 剩余锁数量：${locksSum}")
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
        val lock = locks.computeIfAbsent(key) { reentrantLock.newCondition() }
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
            Runnable {
                try {
                    w.run()
                } catch (e: Throwable) {
                    log.error("Async error", e)
                }
            }
        }.forEach { task: Runnable -> Thread.startVirtualThread(task) }
    }

    /**
     * 异步执行不需要返回的结果。
     * 这个方法不等待结果返回，直接进行下一步。如果需要等待所有异步操作完成，请使用 awaitRunnableExecute
     */
    @JvmStatic
    fun asyncRunnableExecute(work: Runnable) {
        val task = Runnable {
            try {
                work.run()
            } catch (e: Throwable) {
                log.error("Async error", e)
            }
        }

        Thread.startVirtualThread(task)
    }

    /**
     * 异步执行不需要返回的结果，并等待至所有操作都完成。
     * 这个方法会等待结果返回，不直接进行下一步。如果不需要等待所有异步操作完成，请使用 asyncRunnableExecute
     */
    @JvmStatic
    fun awaitRunnableExecute(works: Collection<Runnable>, timeout: Duration = Duration.ofSeconds(30)) {
        val lock = CountDownLatch(works.size)
        works.map { w: Runnable ->
            Runnable {
                try {
                    w.run()
                } catch (e: Throwable) {
                    log.error("Async error", e)
                } finally {
                    lock.countDown()
                }
            }
        }.forEach { task: Runnable -> Thread.startVirtualThread(task) }

        lock.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }

    /**
     * 异步执行不需要返回的结果，并等待至所有操作都完成。
     * 这个方法会等待结果返回，不直接进行下一步。如果不需要等待所有异步操作完成，请使用 asyncRunnableExecute
     */
    fun awaitRunnableExecute(work: Runnable, timeout: Duration = Duration.ofSeconds(30)) {
        val lock = CountDownLatch(1)

        val task = Runnable {
            try {
                work.run()
            } catch (e: Throwable) {
                log.error("Async error", e)
            } finally {
                lock.countDown()
            }
        }

        Thread.startVirtualThread(task)

        lock.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }

    fun <T> awaitSupplierExecute(works: Collection<Callable<T>>): List<T> {
        val size = works.size
        val lock = CountDownLatch(size)
        val results: MutableMap<Int, T?> = ConcurrentHashMap(size)
        works.mapIndexed { i: Int, w: Callable<T> ->
            Runnable {
                try {
                    val result = w.call()
                    results[i] = result
                } catch (e: Exception) {
                    results[i] = null
                    log.error("AsyncSupplier error", e)
                } finally {
                    lock.countDown()
                }
            }
        }.forEach { task: Runnable -> Thread.startVirtualThread(task) }
        try {
            lock.await(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            log.error("lock error", e)
        }

        return results.toSortedMap().mapNotNull { it.value }
    }

    fun <T> awaitSupplierExecute(work: Supplier<T>): T {
        return awaitSupplierExecute(listOf(work), Duration.ofSeconds(30)).first()
    }

    /**
     * 异步执行需要返回的结果，并等待至所有操作都完成。
     * 这个方法会等待结果返回，不直接进行下一步。如果不需要返回结果（void 方法），请使用 awaitRunnableExecute
     * 返回结果严格按照传入的 works 顺序
     */
    @JvmStatic
    fun <T> awaitSupplierExecute(works: Collection<Supplier<T>>, timeout: Duration = Duration.ofSeconds(30)): List<T> {
        val size = works.size
        val lock = CountDownLatch(size)
        val results: MutableMap<Int, T?> = ConcurrentHashMap(size)
        works.mapIndexed { i: Int, w: Supplier<T> ->
            Runnable {
                try {
                    val result = w.get()
                    results[i] = result
                } catch (e: Exception) {
                    results[i] = null
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

        return results.toSortedMap().mapNotNull { it.value }
    }

    fun <T> awaitCallableExecute(
        work: Callable<T>,
        timeout: Duration = Duration.ofSeconds(30)
    ): T {
        ShutdownOnFailure().use { virtualPool ->
            val r = virtualPool.fork(work)
            virtualPool.joinUntil(Instant.now().plus(timeout))
            virtualPool.throwIfFailed()
            return r.get()
        }
    }

    fun <T> awaitCallableExecute(
        works: List<Callable<T>>,
        timeout: Duration = Duration.ofSeconds(30)
    ): List<T> {
        ShutdownOnFailure().use { virtualPool ->
            val results = works.map { virtualPool.fork(it) }
            virtualPool.joinUntil(Instant.now().plus(timeout))
            virtualPool.throwIfFailed()
            return results.map { it.get() }
        }
    }

    fun <T, U> awaitPairCallableExecute(
        work: Callable<T>,
        work2: Callable<U>,
        timeout: Duration = Duration.ofSeconds(30)
    ): Pair<T, U> {
        ShutdownOnFailure().use { virtualPool ->
            val r1 = virtualPool.fork(work)
            val r2 = virtualPool.fork(work2)
            virtualPool.joinUntil(Instant.now().plus(timeout))
            virtualPool.throwIfFailed()
            return Pair(r1.get(), r2.get())
        }
    }

    fun <T, U, V> awaitTripleCallableExecute(
        work: Callable<T>,
        work2: Callable<U>,
        work3: Callable<V>,
        timeout: Duration = Duration.ofSeconds(30)
    ): Triple<T, U, V> {
        ShutdownOnFailure().use { virtualPool ->
            val r1 = virtualPool.fork(work)
            val r2 = virtualPool.fork(work2)
            val r3 = virtualPool.fork(work3)
            virtualPool.joinUntil(Instant.now().plus(timeout))
            virtualPool.throwIfFailed()
            return Triple(r1.get(), r2.get(), r3.get())
        }
    }

    fun <T, U, V, W> awaitQuadSupplierExecute(
        work: Callable<T>,
        work2: Callable<U>,
        work3: Callable<V>,
        work4: Callable<W>,
        timeout: Duration = Duration.ofSeconds(30)
    ): Pair<Pair<T, U>, Pair<V, W>> {
        ShutdownOnFailure().use { virtualPool ->
            val r1 = virtualPool.fork(work)
            val r2 = virtualPool.fork(work2)
            val r3 = virtualPool.fork(work3)
            val r4 = virtualPool.fork(work4)
            virtualPool.joinUntil(Instant.now().plus(timeout))
            virtualPool.throwIfFailed()
            return (r1.get() to r2.get()) to (r3.get() to r4.get())
        }
    }

    /**
     * 出现异常时直接抛出错误不进行等待
     * 返回结果严格按照传入的 works 顺序
     */
    @JvmStatic
    @Throws(Exception::class)
    fun <T> awaitSupplierExecuteThrows(
        works: Collection<Supplier<T>>,
        timeout: Duration = Duration.ofSeconds(30)
    ): List<T> {
        val size = works.size
        val phaser = Phaser(1)
        val results: MutableMap<Int, T> = ConcurrentHashMap(size)
        val taskThreads: MutableList<Thread> = CopyOnWriteArrayList()
        var exception: Exception? = null

        works.mapIndexed { i: Int, w: Supplier<T> ->
            phaser.register()
            Runnable {
                try {
                    if (!phaser.isTerminated) {
                        val result = w.get()
                        results[i] = result
                    }
                } catch (e: Exception) {
                    exception = e
                    taskThreads.forEach {
                        try {
                            it.interrupt()
                        } catch (ignore: Exception) {
                        }
                    }
                    phaser.forceTermination()
                } finally {
                    if (!phaser.isTerminated) {
                        phaser.arriveAndDeregister()
                    }
                }
            }
        }.forEach { task: Runnable -> taskThreads.add(Thread.startVirtualThread(task)) }

        try {
            val currentPhase = phaser.phase
            phaser.arrive()
            phaser.awaitAdvanceInterruptibly(
                currentPhase,
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
            )
        } catch (e: InterruptedException) {
            log.error("lock error", e)
        }

        exception?.let { throw it }

        return results.values.toList()
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
            conditionCount.computeIfPresent(lock) { _: Condition?, v: Int -> v + 1 }
        }

        fun getAndRemove(lock: Condition): Int {
            val count = conditionCount.remove(lock)
            return if (Objects.nonNull(count)) count!! else 0
        }
    }
}
