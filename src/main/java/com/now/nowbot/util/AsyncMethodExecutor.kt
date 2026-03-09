package com.now.nowbot.util

import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.StructuredTaskScope.ShutdownOnFailure
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
        } catch (_: InterruptedException) {
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
    @Throws(Exception::class)
    fun <T : Any> execute(supplier: Supplier<T>, key: Any, defaultValue: T?): T? {
        reentrantLock.lock()
        val hasLock = locks.containsKey(key)
        val lock = locks.computeIfAbsent(key) {
            reentrantLock.newCondition()
        }
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
        } catch (_: InterruptedException) {
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
            val count = countDownLocks.computeIfAbsent(key) {
                CountDownLatch(locksSum)
            }
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
    fun awaitRunnableExecute(works: Collection<Runnable>, timeout: Duration = 30.seconds) {
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

        lock.await(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    /**
     * 异步执行不需要返回的结果，并等待至所有操作都完成。
     * 这个方法会等待结果返回，不直接进行下一步。如果不需要等待所有异步操作完成，请使用 asyncRunnableExecute
     */
    fun awaitRunnableExecute(work: Runnable, timeout: Duration = 30.seconds) {
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

        lock.await(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }


    /**
     * 异步执行需要返回的结果，并等待至所有操作都完成。
     * 这个方法会等待结果返回，不直接进行下一步。如果不需要返回结果（void 方法），请使用 awaitRunnableExecute
     * 返回结果严格按照传入的 works 顺序
     */
    /*
    fun <T> awaitCallableExecute(
        works: Collection<Callable<out T>>,
        timeout: Duration = 30.seconds
    ): List<T> {
        val size = works.size
        val lock = CountDownLatch(size)

        val results: MutableMap<Int, T> = ConcurrentHashMap<Int, T>(size)
        val failure: MutableMap<Int, Exception> = ConcurrentHashMap<Int, Exception>(size)

        works.mapIndexed { i: Int, w: Callable<out T> ->
            Runnable {
                try {
                    val result = w.call()
                    results[i] = result
                } catch (e: Exception) {
                    failure[i] = e
                } finally {
                    lock.countDown()
                }
            }
        }.forEach {
                task: Runnable -> Thread.startVirtualThread(task)
        }

        try {
            lock.await(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            log.error("lock error", e)
        }

        if (failure.isNotEmpty()) {
            failure.forEach { e ->
                log.error("AsyncSupplier error", e.value)
            }
        }

        return results.toSortedMap().mapNotNull { it.value }
    }

     */

    /*
    fun <T> awaitSupplierExecute(work: Supplier<T>): T {
        return awaitSupplierExecute(listOf(work), 30.seconds).first()
    }

    /**
     * 异步执行需要返回的结果，并等待至所有操作都完成。
     * 这个方法会等待结果返回，不直接进行下一步。如果不需要返回结果（void 方法），请使用 awaitRunnableExecute
     * 返回结果严格按照传入的 works 顺序
     */
    fun <T> awaitSupplierExecute(works: Collection<Supplier<T>>, timeout: Duration = 30.seconds): List<T> {
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
            lock.await(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            log.error("lock error", e)
        }

        return results.toSortedMap().mapNotNull { it.value }
    }

     */

    fun <T> awaitCallableExecute(
        work: Callable<out T>,
        timeout: Duration = 30.seconds
    ): T {
        ShutdownOnFailure().use { virtualPool ->
            val r = virtualPool.fork(work)
            virtualPool.joinUntil(Instant.now().plusMillis(timeout.inWholeMilliseconds))
            virtualPool.throwIfFailed()
            return r.get()
        }
    }

    /**
     * 分批执行，避免 ppy API 限制
     */
    fun <T> awaitBatchCallableExecute(
        works: List<Callable<out T>>,
        batchSize: Int = 60,
        latency: Duration = 60.seconds,
        timeout: Duration = 30.seconds
    ): List<T> {
        val batches = works.chunked(batchSize)
        val allResults = mutableListOf<T>()

        ShutdownOnFailure().use { scope ->
            val batchFutures = batches.mapIndexed { batchIndex, batch ->
                scope.fork {
                    // 批次间延迟（第一批不延迟）
                    if (batchIndex > 0) {
                        Thread.sleep(latency.inWholeMilliseconds)
                    }

                    // 处理当前批次
                    val batchResults = processBatch(batch, timeout)
                    batchResults
                }
            }

            scope.join() // 等待所有批次完成
            scope.throwIfFailed() // 如果有异常则抛出

            // 收集所有结果
            batchFutures.forEach { future ->
                allResults.addAll(future.get())
            }
        }

        return allResults
    }

    private fun <T> processBatch(
        batch: List<Callable<out T>>,
        timeout: Duration
    ): List<T> {
        return ShutdownOnFailure().use { innerScope ->
            val futures = batch.map { work ->
                innerScope.fork(work)
            }

            innerScope.joinUntil(Instant.now().plusMillis(timeout.inWholeMilliseconds))
            innerScope.throwIfFailed()

            futures.map { it.get() }
        }
    }

    /* 以下是协程代码 */

    val vDispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

    fun <T> await(work: Callable<out T>, timeout: Duration = 30.seconds): T {
        return runBlocking(vDispatcher) {
            suspend(work, timeout)
        }
    }

    suspend fun <T> suspend(work: Callable<out T>, timeout: Duration = 30.seconds): T = withTimeout(timeout) {
        val r = async(vDispatcher) { work.call() }

        r.await()
    }

    fun <T, U> awaitPair(
        work: Callable<out T>,
        work2: Callable<out U>,
        timeout: Duration = 30.seconds
    ): Pair<T, U> {
        return runBlocking(vDispatcher) {
            suspendPair(work, work2, timeout)
        }
    }

    suspend fun <T, U> suspendPair(
        work: Callable<out T>,
        work2: Callable<out U>,
        timeout: Duration = 30.seconds
    ): Pair<T, U> = withTimeout(timeout) {
        val r1 = async(vDispatcher) { work.call() }
        val r2 = async(vDispatcher) { work2.call() }

        r1.await() to r2.await()
    }

    fun <T, U, V> awaitTriple(
        work: Callable<out T>,
        work2: Callable<out U>,
        work3: Callable<out V>,
        timeout: Duration = 30.seconds
    ): Triple<T, U, V> {
        return runBlocking(vDispatcher) {
            suspendTriple(work, work2, work3, timeout)
        }
    }

    suspend fun <T, U, V> suspendTriple(
        work: Callable<out T>,
        work2: Callable<out U>,
        work3: Callable<out V>,
        timeout: Duration = 30.seconds
    ): Triple<T, U, V> = withTimeout(timeout) {
        val r1 = async(vDispatcher) { work.call() }
        val r2 = async(vDispatcher) { work2.call() }
        val r3 = async(vDispatcher) { work3.call() }

        Triple(r1.await(), r2.await(), r3.await())
    }



    fun <T, U, V, W> awaitQuad(
        work: Callable<out T>,
        work2: Callable<out U>,
        work3: Callable<out V>,
        work4: Callable<out W>,
        timeout: Duration = 30.seconds
    ): Pair<Pair<T, U>, Pair<V, W>> {
        return runBlocking(vDispatcher) {
            suspendQuad(work, work2, work3, work4, timeout)
        }
    }

    suspend fun <T, U, V, W> suspendQuad(
        work: Callable<out T>,
        work2: Callable<out U>,
        work3: Callable<out V>,
        work4: Callable<out W>,
        timeout: Duration = 30.seconds
    ): Pair<Pair<T, U>, Pair<V, W>> = withTimeout(timeout) {
        val r1 = async(vDispatcher) { work.call() }
        val r2 = async(vDispatcher) { work2.call() }
        val r3 = async(vDispatcher) { work3.call() }
        val r4 = async(vDispatcher) { work4.call() }

        (r1.await() to r2.await()) to (r3.await() to r4.await())
    }

    @CanIgnoreReturnValue
    fun <T> awaitList(
        works: List<Callable<out T>>,
        timeout: Duration = 30.seconds
    ): List<T> = runBlocking(vDispatcher) {
        suspendList(works, timeout)
    }

    suspend fun <T> suspendList(
        works: List<Callable<out T>>,
        timeout: Duration = 30.seconds
    ): List<T> = withTimeout(timeout) {
        works.map { async(vDispatcher) { it.call() } }.awaitAll()
    }

    fun <T> awaitBatch(
        works: List<Callable<out T>>,
        batchSize: Int = 60,
        latency: Duration = 60.seconds,
        timeout: Duration = 30.seconds
    ): List<T> = runBlocking(vDispatcher) {
        suspendBatch(works, batchSize, latency, timeout)
    }

    /**
     * 协程原生版：处理分批异步逻辑
     */
    suspend fun <T> suspendBatch(
        works: List<Callable<out T>>,
        batchSize: Int = 60,
        latency: Duration = 60.seconds,
        timeout: Duration = 30.seconds
    ): List<T> = coroutineScope {
        val batches = works.chunked(batchSize)

        // 将每一批次映射为一个异步 Job
        val batchDeferred = batches.mapIndexed { index, batch ->
            async(vDispatcher) {
                // 批次间延迟：使用非阻塞的 delay 替代 Thread.sleep
                if (index > 0) {
                    delay(latency)
                }

                suspendList(batch, timeout)
            }
        }

        batchDeferred.awaitAll().flatten()
    }

    /* 以下是 java 原生的 ShutdownOnFailure */

    fun <T, U> awaitPairCallableExecute(
        work: Callable<out T>,
        work2: Callable<out U>,
        timeout: Duration = 30.seconds
    ): Pair<T, U> {
        ShutdownOnFailure().use { virtualPool ->
            val r1 = virtualPool.fork(work)
            val r2 = virtualPool.fork(work2)
            virtualPool.joinUntil(Instant.now().plusMillis(timeout.inWholeMilliseconds))
            virtualPool.throwIfFailed()
            return Pair(r1.get(), r2.get())
        }
    }

    fun <T, U, V> awaitTripleCallableExecute(
        work: Callable<out T>,
        work2: Callable<out U>,
        work3: Callable<out V>,
        timeout: Duration = 30.seconds
    ): Triple<T, U, V> {
        ShutdownOnFailure().use { virtualPool ->
            val r1 = virtualPool.fork(work)
            val r2 = virtualPool.fork(work2)
            val r3 = virtualPool.fork(work3)
            virtualPool.joinUntil(Instant.now().plusMillis(timeout.inWholeMilliseconds))
            virtualPool.throwIfFailed()
            return Triple(r1.get(), r2.get(), r3.get())
        }
    }

    fun <T, U, V, W> awaitQuadCallableExecute(
        work: Callable<out T>,
        work2: Callable<out U>,
        work3: Callable<out V>,
        work4: Callable<out W>,
        timeout: Duration = 30.seconds
    ): Pair<Pair<T, U>, Pair<V, W>> {
        ShutdownOnFailure().use { virtualPool ->
            val r1 = virtualPool.fork(work)
            val r2 = virtualPool.fork(work2)
            val r3 = virtualPool.fork(work3)
            val r4 = virtualPool.fork(work4)
            virtualPool.joinUntil(Instant.now().plusMillis(timeout.inWholeMilliseconds))
            virtualPool.throwIfFailed()
            return (r1.get() to r2.get()) to (r3.get() to r4.get())
        }
    }

    fun <X> awaitCallableExecute(works: List<Callable<out X>>, timeout: Duration = 30.seconds): List<X> {
        if (works.isEmpty()) return emptyList()

        ShutdownOnFailure().use { virtualPool ->
            val forks = works.map { work ->
                virtualPool.fork(work)
            }

            virtualPool.joinUntil(Instant.now().plusMillis(timeout.inWholeMilliseconds))
            virtualPool.throwIfFailed()

            return forks.map { task -> task.get() }
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
        timeout: Duration = 30.seconds
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
                        } catch (_: Exception) {}
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
                timeout.inWholeMilliseconds,
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
