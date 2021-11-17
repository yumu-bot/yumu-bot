package com.now.nowbot.util;

import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ASyncMessageUtil {
    private static final int OFF_TIME = 90*1000;
    static class l {
        Long group;
        Long send;
        Long time = System.currentTimeMillis();

        boolean isClose(){
            return System.currentTimeMillis()-time > OFF_TIME;
        }
    }
    private static final Map<l, LinkedBlockingQueue<MessageEvent>> map = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(ASyncMessageUtil.class);
    public static Object getLock(long group, long send){
        var l = new l();
        l.group = group;
        l.send = send;
        var v = new LinkedBlockingQueue<MessageEvent>();
        map.put(l, v);
        return l;
    }

    public static Object getSenderLock(long send){
        var l = new l();
        l.send = send;
        var v = new LinkedBlockingQueue<MessageEvent>();
        map.put(l, v);
        return l;
    }

    public static Object getGroupLock(long group){
        var l = new l();
        l.group = group;
        var v = new LinkedBlockingQueue<MessageEvent>();
        map.put(l, v);
        return l;
    }

    public static void put(MessageEvent message){
        close();
        map.forEach((l, v)->{
            if (!l.isClose()){
                try {
                    if (l.group == null && message.getSender().getId() == l.send){
                        v.put(message);
                    }else if (l.send == null && message instanceof GroupMessageEvent && message.getSubject().getId() == l.group){
                        v.put(message);
                    }else if( message instanceof GroupMessageEvent && message.getSubject().getId() == l.group && message.getSender().getId() == l.send){
                        v.put(message);
                    }
                } catch (InterruptedException e) {
                    //do not
                }
            }
        });
    }

    public static MessageEvent getEvent (Object lock) throws InterruptedException{
        if(lock instanceof l && !((l) lock).isClose()) {
            ((l) lock).time = System.currentTimeMillis();
            var l = map.get(lock);
            if(l != null) {
                return l.poll(OFF_TIME, TimeUnit.MILLISECONDS);
            }
        }
        return null;
    }

    public static void close(){
        map.keySet().removeIf(l::isClose);
    }

    public static void close(Object lock){
        map.remove(lock);
    }
}
