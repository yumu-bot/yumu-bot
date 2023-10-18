package com.now.nowbot.util;


import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ASyncMessageUtil{

    private static final Long OFF_TIME = 120*1000L;
    private static final ReentrantLock reentrantLock = new ReentrantLock();
    public static class Lock{

        Long group;
        Long send;
        long time = System.currentTimeMillis();
        long         off = 0;
        MessageEvent msg;
        // 线程同步锁

        private final Condition getCondition = reentrantLock.newCondition();

        boolean isClose(){
            return false;
        }

        void checkAdd(MessageEvent message){
            if (
                    (this.group == null && message.getSender().getId() == this.send) ||
                    (this.send == null && message instanceof GroupMessageEvent && message.getSubject().getId() == this.group) ||
                    (message instanceof GroupMessageEvent && message.getSubject().getId() == this.group && message.getSender().getId() == this.send)
            ){
                reentrantLock.lock();
                try {
                    this.msg = message;
                    getCondition.signalAll();
                } finally {
                    reentrantLock.unlock();
                }
            }
        }

        @SuppressWarnings({"ResultOfMethodCallIgnored"})
        public MessageEvent get() {
            ASyncMessageUtil.lockList.add(this);
            try {
                reentrantLock.lock();
                if (msg == null) {
                    getCondition.await(off, TimeUnit.MILLISECONDS);
                }
                return msg;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                reentrantLock.unlock();
                ASyncMessageUtil.lockList.remove(this);
                msg = null;
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
    public static Lock getLock(long group, long send){
        return getLock(group, send, OFF_TIME);
    }
    public static Lock getLock(long group, long send, Long offTime){
        var l = new Lock();
        l.group = group;
        l.send = send;
        l.off = offTime;
        lockList.add(l);
        return l;
    }
    public static Lock getLock(MessageEvent event){
        if (event instanceof GroupMessageEvent g){
            return getLock(g.getGroup().getId(), g.getSender().getId());
        }
        return getSenderLock(event.getSender().getId());
    }
    public static Lock getLock(MessageEvent event, long offTime){
        if (event instanceof GroupMessageEvent g){
            return getLock(g.getGroup().getId(), g.getSender().getId(), offTime);
        }
        return getSenderLock(event.getSender().getId(), offTime);
    }

    /**
     * 指定发送人的锁(无论哪个群)
     * @param send
     * @return
     */
    public static Lock getSenderLock(long send){
        return getSenderLock(send, OFF_TIME);
    }
    public static Lock getSenderLock(long send, Long offTime){
        var l = new Lock();
        l.send = send;
        l.off = offTime;
        return l;
    }

    /**
     * 指定群的锁,无论发送人
     * @param group
     * @return
     */
    public static Lock getGroupLock(long group){
        return getGroupLock(group, OFF_TIME);
    }
    public static Lock getGroupLock(long group, Long offTime){
        var l = new Lock();
        l.group = group;
        l.off = offTime;
        return l;
    }

    /**
     * 在event监听使用
     * @param message
     */
    public static void put(MessageEvent message){
        lockList.forEach(lock -> lock.checkAdd(message));
    }
}
