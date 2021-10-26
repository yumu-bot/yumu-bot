package com.now.nowbot.util;

import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
class messageT<T>{
    Thread inThread;
    boolean isLock = false;
    LinkedList<T> objs = new LinkedList<>();

    T get(){
        if (objs.size() == 0) {
            if(isLock) throw new RuntimeException("不可重复读取");
            inThread = Thread.currentThread();
            isLock = true;
            LockSupport.park();
        }
        return objs.pollFirst();
    }

    void push(T obj){
        objs.offerLast(obj);
        if (isLock){
            isLock = false;
            LockSupport.unpark(inThread);
        }
    }

}
class lock{
    Long groupId;
    Long sendId;
    long time;
    boolean isClose = true;
    lock(long id){
        sendId = id;
        time = System.currentTimeMillis();
    }
    lock(long group, long send){
        groupId = group;
        sendId = send;
        time = System.currentTimeMillis();
    }

    public long getTime() {
        return time;
    }

    public void close() {
        isClose = false;
    }

    public boolean isClose(long off) {
        return isClose || time + off < System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof lock)) return false;
        lock lock = (lock) o;
        return Objects.equals(groupId, lock.groupId) && Objects.equals(sendId, lock.sendId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, sendId);
    }
}
public class ASyncMessageUtil {
    private static final Map<lock, messageT<MessageChain>> getList = new HashMap<>();
    private static final int OFF_TIME = 90*1000;
    private static final Logger log = LoggerFactory.getLogger(ASyncMessageUtil.class);
    public static @Nullable lock getLock(long group, long send){
        var lock = new lock(group, send);
        if(getList.containsKey(lock)) return null;
        var mess = new messageT<MessageChain>();
        getList.put(lock, mess);
        return lock;
    }
    public static MessageChain get(lock lock){
        if (getList.containsKey(lock)) {
            return getList.get(lock).get();
        }
        log.error("锁已失效");
        return null;
    }

    public static void put(MessageEvent message){
        getList.entrySet().removeIf(e ->{
            if (e.getKey().isClose(OFF_TIME)){
                return true;
            }else {
                if (e.getKey().groupId == null && e.getKey().sendId.equals(message.getSender().getId())){
                    e.getValue().push(message.getMessage());
                } else if (message instanceof GroupMessageEvent
                        && e.getKey().groupId.equals(((GroupMessageEvent) message).getGroup().getId())
                        && e.getKey().sendId.equals(message.getSender().getId())){
                    e.getValue().push(message.getMessage());
                }
                return false;
            }
        });
    }

    public static void closeLock(lock l){
        if (l != null && getList.containsKey(l))
            l.close();
    }
}
