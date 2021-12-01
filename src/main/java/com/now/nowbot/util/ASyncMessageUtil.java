package com.now.nowbot.util;

import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ASyncMessageUtil{

    private static final Long OFF_TIME = 90*1000L;
    static class Lock{

        Long group;
        Long send;
        long time = System.currentTimeMillis();
        long off = 0;
        MessageEvent msg;
        // 线程同步锁
        private final ReentrantLock reentrantLock = new ReentrantLock();
        private final Condition getCondition = reentrantLock.newCondition();

        boolean isClose(){
            if (System.currentTimeMillis()-time > OFF_TIME){
                this.reentrantLock.lock();
                try {
                    getCondition.signal();
                } finally {
                    reentrantLock.unlock();
                }
                return true;
            }
            return false;
        }

        void checkAdd(MessageEvent message){
            if (
                    (this.group == null && message.getSender().getId() == this.send) ||
                    (this.send == null && message instanceof GroupMessageEvent && message.getSubject().getId() == this.group) ||
                    (message instanceof GroupMessageEvent && message.getSubject().getId() == this.group && message.getSender().getId() == this.send)
            ){
                this.reentrantLock.lock();
                try {
                    this.msg = message;
                    getCondition.signal();
                } finally {
                    reentrantLock.unlock();
                }
            }
        }

    }
    private static final CopyOnWriteArrayList<Lock> lockList = new CopyOnWriteArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(ASyncMessageUtil.class);

    /**
     * 指定群组跟发送人的锁
     * @param group
     * @param send
     * @return
     */
    public static Object getLock(long group, long send){
        return getLock(group, send, OFF_TIME);
    }
    public static Object getLock(long group, long send, Long offTime){
        var l = new Lock();
        l.group = group;
        l.send = send;
        l.off = offTime;
        lockList.add(l);
        return l;
    }

    /**
     * 指定发送人的锁(无论哪个群)
     * @param send
     * @return
     */
    public static Object getSenderLock(long send){
        return getSenderLock(send, OFF_TIME);
    }
    public static Object getSenderLock(long send, Long offTime){
        var l = new Lock();
        l.send = send;
        l.off = offTime;
        lockList.add(l);
        return l;
    }

    /**
     * 指定群的锁,无论发送人
     * @param group
     * @return
     */
    public static Object getGroupLock(long group){
        return getGroupLock(group, OFF_TIME);
    }
    public static Object getGroupLock(long group, Long offTime){
        var l = new Lock();
        l.group = group;
        l.off = offTime;
        lockList.add(l);
        return l;
    }

    /**
     * 在event监听使用
     * @param message
     */
    public static void put(MessageEvent message){
        close();
        lockList.forEach(lock -> {
            lock.checkAdd(message);
        });
    }

    public static @Nullable MessageEvent getEvent (Object lock) throws InterruptedException{
        if(lock instanceof Lock t && !t.isClose()) {
            t.time = System.currentTimeMillis();
            t.reentrantLock.lock();
            try {
                while (t.msg == null) {
                    t.getCondition.await();
                }
                return t.msg;
            } finally {
                t.reentrantLock.unlock();
                t.msg = null;
            }

        }
        return null;
    }

    public static void close(){
        lockList.removeIf(Lock::isClose);
    }

    /**
     * 尽量手动关闭监听
     * @param lock
     */
    public static void close(Object lock){
        lockList.remove(lock);
    }
}
