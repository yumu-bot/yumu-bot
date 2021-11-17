package com.now.nowbot.util;

import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ASyncMessageUtil <T extends MessageEvent>{
    private static final int OFF_TIME = 90*1000;
    static class lock {
        Long group;
        Long send;
        Long time = System.currentTimeMillis();

        boolean isClose(){
            return System.currentTimeMillis()-time > OFF_TIME;
        }
    }
    private static final Map<lock, BlockingQueue<MessageEvent>> map = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(ASyncMessageUtil.class);

    /**
     * 指定群组跟发送人的锁
     * @param group
     * @param send
     * @return
     */
    public static Object getLock(long group, long send){
        var l = new lock();
        l.group = group;
        l.send = send;
        var v = new LinkedBlockingQueue<MessageEvent>();
        map.put(l, v);
        return l;
    }

    /**
     * 指定发送人的锁(无论哪个群)
     * @param send
     * @return
     */
    public static Object getSenderLock(long send){
        var l = new lock();
        l.send = send;
        var v = new LinkedBlockingQueue<MessageEvent>();
        map.put(l, v);
        return l;
    }

    /**
     * 指定群的锁,无论发送人
     * @param group
     * @return
     */
    public static Object getGroupLock(long group){
        var l = new lock();
        l.group = group;
        var v = new LinkedBlockingQueue<MessageEvent>();
        map.put(l, v);
        return l;
    }

    /**
     * 在event监听使用
     * @param message
     */
    public static void put(MessageEvent message){
        close();
        map.forEach((lock, v)->{
            if (!lock.isClose()){
                //锁在生效阶段
                try {
                    if (lock.group == null && message.getSender().getId() == lock.send){
                        //只监听发送人
                        v.put(message);
                    }else if (lock.send == null && message instanceof GroupMessageEvent && message.getSubject().getId() == lock.group){
                        //只监听群组
                        v.put(message);
                    }else if( message instanceof GroupMessageEvent && message.getSubject().getId() == lock.group && message.getSender().getId() == lock.send){
                        //指定群组与发送人的
                        v.put(message);
                    }
                } catch (InterruptedException e) {
                    //do nothing
                }
            }
        });
    }

    public static @Nullable MessageEvent getEvent (Object lock) throws InterruptedException{
        if(lock instanceof ASyncMessageUtil.lock && !((ASyncMessageUtil.lock) lock).isClose()) {
            ((ASyncMessageUtil.lock) lock).time = System.currentTimeMillis();
            var l = map.get(lock);
            if(l != null) {
                return l.poll(OFF_TIME, TimeUnit.MILLISECONDS);
            }
        }
        return null;
    }

    public static void close(){
        map.keySet().removeIf(lock::isClose);
    }

    /**
     * 尽量手动关闭监听
     * @param lock
     */
    public static void close(Object lock){
        map.remove(lock);
    }
}
