package com.now.nowbot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncMethodExecutor {
    private static final Logger log = LoggerFactory.getLogger(AsyncMethodExecutor.class);

    public interface Supplier<T> {
        T get() throws Exception;
    }

    public interface Runnable {
        void run() throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static <T> T waitForResult(Condition lock, Object key, T defaultValue) throws Exception {
        CountDownLatch countDownLock = null;
        try {
            reentrantLock.lock();
            Util.add(lock);
            lock.await();
            reentrantLock.unlock();
            countDownLock = countDownLocks.get(key);
            Object result = results.get(key);
            if (result instanceof Exception e) {
                throw e;
            }
            return (T) result;
        } catch (InterruptedException ignore) {
            return defaultValue;
        } finally {
            if (countDownLock != null) countDownLock.countDown();
        }
    }

    private static final ReentrantLock reentrantLock = new ReentrantLock();
    private static final ConcurrentHashMap<Object, CountDownLatch> countDownLocks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Object, Condition> locks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Object, Object> results = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    public static <T> T execute(Supplier<T> supplier, Object key, T defaultValue) throws Exception {
        boolean hasLock;
        Condition lock;
        reentrantLock.lock();
        hasLock = locks.containsKey(key);
        lock = locks.computeIfAbsent(key, s -> reentrantLock.newCondition());
        reentrantLock.unlock();
        if (hasLock) {
            return waitForResult(lock, key, defaultValue);
        } else {
            return getResult(lock, key, supplier);
        }
    }

    public static <T> T execute(Supplier<T> supplier, Object key, Supplier<T> getDefault) throws Exception {
        boolean hasLock;
        Condition lock;
        reentrantLock.lock();
        hasLock = locks.containsKey(key);
        lock = locks.computeIfAbsent(key, s -> reentrantLock.newCondition());
        reentrantLock.unlock();
        if (hasLock) {
            return waitForResult(lock, key, getDefault);
        } else {
            return getResult(lock, key, supplier);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T waitForResult(Condition lock, Object key, Supplier<T> getDefault) throws Exception {
        CountDownLatch countDownLock = null;
        try {
            reentrantLock.lock();
            Util.add(lock);
            lock.await();
            reentrantLock.unlock();
            countDownLock = countDownLocks.get(key);
            Object result = results.get(key);
            if (result instanceof Exception e) {
                throw e;
            }
            return (T) result;
        } catch (InterruptedException ignore) {
            return getDefault.get();
        } finally {
            if (countDownLock != null) countDownLock.countDown();
        }
    }

    private static <T> T getResult(Condition lock, Object key, Supplier<T> supplier) throws Exception {

        try {
            T result = supplier.get();
            results.put(key, result);
            return result;
        } catch (Exception e) {
            results.put(key, e);
            throw e;
        } finally {
            reentrantLock.lock();
            int locksSum = Util.getAndRemove(lock);
            log.info("sum : {}", locksSum);
            CountDownLatch count = countDownLocks.computeIfAbsent(key, k -> new CountDownLatch(locksSum));
            lock.signalAll();
            reentrantLock.unlock();
            if (!count.await(5, TimeUnit.SECONDS)) {
                if (locksSum > 0) log.warn("wait to long");
            }
            results.remove(key);
            locks.remove(key);
            countDownLocks.remove(key);
        }

    }

    @SuppressWarnings("unused")
    public static void execute(Runnable work, Object key) throws Exception {
        boolean hasLock;
        Condition lock;
        reentrantLock.lock();
        hasLock = locks.containsKey(key);
        lock = locks.computeIfAbsent(key, s -> reentrantLock.newCondition());
        reentrantLock.unlock();

        if (hasLock) {
            try {
                reentrantLock.lock();
                lock.await();
                reentrantLock.unlock();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        try {
            work.run();
        } finally {
            reentrantLock.lock();
            lock.signalAll();
            reentrantLock.unlock();
            locks.remove(key);
        }
    }

    private static class Util {
        static ConcurrentHashMap<Condition, Integer> conditionCount = new ConcurrentHashMap<>();

        static void add(Condition lock) {
            conditionCount.putIfAbsent(lock, 0);
            conditionCount.computeIfPresent(lock, (k, v) -> v + 1);
        }

        static int getAndRemove(Condition lock) {
            Integer count = conditionCount.remove(lock);
            return Objects.nonNull(count) ? count : 0;
        }
    }
}
