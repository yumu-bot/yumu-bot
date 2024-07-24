package com.now.nowbot.util;


import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ASyncMessageUtil {

    private static final Long      OFF_TIME = 60 * 60 * 1000L;
    private static final Set<Lock> lockList = new CopyOnWriteArraySet<>();

    /**
     * 指定群组跟发送人的锁
     *
     * @param group
     * @param send
     * @return
     */
    public static Lock getLock(long group, long send) {
        return getLock(group, send, OFF_TIME, null);
    }

    private static final Logger log = LoggerFactory.getLogger(ASyncMessageUtil.class);

    public static Lock getLock(long group, long send, long offTime, Function<MessageEvent, Boolean> check) {
        var l = new OLock(group, send, offTime, check);
        return l;
    }

    public static Lock getLock(MessageEvent event, long offTime) {
        return getLock(event, offTime, null);
    }

    public static Lock getLock(MessageEvent event) {
        if (event instanceof GroupMessageEvent g) {
            return getLock(g.getGroup().getId(), g.getSender().getId());
        }
        return getSenderLock(event.getSender().getId());
    }

    public static Lock getLock(MessageEvent event, long offTime, Function<MessageEvent, Boolean> check) {
        if (event instanceof GroupMessageEvent g) {
            return getLock(g.getGroup().getId(), g.getSender().getId(), offTime, check);
        }
        return getSenderLock(event.getSender().getId(), offTime, check);
    }

    public static Lock getSenderLock(long send, Long offTime, Function<MessageEvent, Boolean> check) {
        var l = new OLock(null, send, offTime, check);
        return l;
    }

    /**
     * 指定发送人的锁(无论哪个群)
     *
     * @param send
     * @return
     */
    public static Lock getSenderLock(long send) {
        return getSenderLock(send, OFF_TIME, null);
    }

    /**
     * 指定群的锁,无论发送人
     *
     * @param group
     * @return
     */
    public static Lock getGroupLock(long group) {
        return getGroupLock(group, OFF_TIME);
    }

    public static Lock getGroupLock(long group, Long offTime) {
        var l = new OLock(group, null, offTime, null);
        return l;
    }

    /**
     * 在event监听使用
     *
     * @param message
     */
    public static void put(MessageEvent message) {
        lockList.forEach(lock -> lock.checkAdd(message));
    }

    public interface Lock {
        void checkAdd(MessageEvent message);

        MessageEvent get();
    }

    static boolean check(MessageEvent message, Long group, Long send) {
        return (group == null && message.getSender().getId() == send) ||
                (send == null && message instanceof GroupMessageEvent && message.getSubject().getId() == group) ||
                (message instanceof GroupMessageEvent && message.getSubject().getId() == group && message.getSender().getId() == send);
    }

    static void remove(Lock lock) {
        lockList.remove(lock);
    }

    static void add(Lock lock) {
        lockList.add(lock);
    }
}
// 使用BlockingQueue实现的锁 有可能会出现内存泄漏
class BLock implements ASyncMessageUtil.Lock {
    Long                                      group;
    Long                                      send;
    long                                      off;
    Optional<Function<MessageEvent, Boolean>> checkOpt;
    BlockingQueue<MessageEvent>               queue;

    public BLock(Long group, Long send, long offTime, Function<MessageEvent, Boolean> check) {
        this.group = group;
        this.send = send;
        this.off = offTime;
        this.checkOpt = Optional.ofNullable(check);
        queue = new ArrayBlockingQueue<>(1);
    }

    @Override
    public void checkAdd(MessageEvent message) {
        if (ASyncMessageUtil.check(message, this.group, this.send) &&
                checkOpt.map(f -> f.apply(message)).orElse(true) &&
                Objects.nonNull(queue)) {
            queue.add(message);
        }
    }

    @Override
    public MessageEvent get() {
        ASyncMessageUtil.add(this);
        try {
            return queue.poll(off, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        } finally {
            ASyncMessageUtil.remove(this);
            queue = null;
        }
    }
}

// 使用LockSupport实现的锁 不会释放锁, 但是性能最好
class NLock implements ASyncMessageUtil.Lock {
    Long                                      group;
    Long                                      send;
    long                                      off    = 0;
    MessageEvent                              msg;
    Optional<Function<MessageEvent, Boolean>> checkOpt;
    Thread                                    thread = null;

    public NLock(Long group, Long send, long offTime, Function<MessageEvent, Boolean> check) {
        this.group = group;
        this.send = send;
        this.off = offTime;
        this.checkOpt = Optional.ofNullable(check);
    }

    @Override
    public void checkAdd(MessageEvent message) {
        if (ASyncMessageUtil.check(message, this.group, this.send) &&
                checkOpt.map(f -> f.apply(message)).orElse(true)) {
            this.msg = message;
            LockSupport.unpark(thread);
        }
    }

    @Override
    public MessageEvent get() {
        ASyncMessageUtil.add(this);
        try {
            thread = Thread.currentThread();
            LockSupport.parkUntil(System.currentTimeMillis() + off);
            thread = null;
            return msg;
        } finally {
            ASyncMessageUtil.remove(this);
        }
    }
}

// 使用ReentrantLock实现的锁 综合最佳
class OLock implements ASyncMessageUtil.Lock {
    private static final ReentrantLock reentrantLock = new ReentrantLock();

    Long                                      group;
    Long                                      send;
    long                                      off = 0;
    MessageEvent                              msg;
    Optional<Function<MessageEvent, Boolean>> checkOpt;
    final Condition condition;

    public OLock(Long group, Long send, long offTime, Function<MessageEvent, Boolean> check) {
        this.group = group;
        this.send = send;
        this.off = offTime;
        this.checkOpt = Optional.ofNullable(check);
        condition = reentrantLock.newCondition();
    }

    @Override
    public void checkAdd(MessageEvent message) {
        if (ASyncMessageUtil.check(message, this.group, this.send) &&
                checkOpt.map(f -> f.apply(message)).orElse(true)) {
            reentrantLock.lock();
            try {
                this.msg = message;
                condition.signalAll();
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public MessageEvent get() {
        ASyncMessageUtil.add(this);
        try {
            reentrantLock.lock();
            if (msg == null) {
                condition.await(off, TimeUnit.MILLISECONDS);
            }
            return msg;
        } catch (InterruptedException e) {
            return null;
        } finally {
            reentrantLock.unlock();
            ASyncMessageUtil.remove(this);
            msg = null;
        }
    }

}