package com.now.nowbot.util;

import net.mamoe.mirai.event.events.MessagePreSendEvent;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SendmsgUtil {
    static int max = 5;
    static int stime = 15;
    static Map<Long, LinkedList<Message>> MessageList = new ConcurrentHashMap<>();
    static Map<Long, Long> SleepTime = new ConcurrentHashMap<>();

    public static synchronized void check(MessagePreSendEvent event) {
        //得到发送者
        var target = event.getTarget().getId();
        //拿到发送的消息
        var e = event.getMessage();
        //判断是否为禁言对象
        if (SleepTime.containsKey(target)) {
            if (System.currentTimeMillis() < SleepTime.get(target)) {
                //是禁言对象且在禁言期内
                event.cancel();
                return;
            } else {
                //不在禁言期则移除
                SleepTime.remove(target);
            }
        }
        //判断是否已经发送过消息
        if (MessageList.containsKey(target)) {
            //拿到消息列表
            var list = MessageList.get(target);
            //判断重复
            for (int i = 0; i < list.size(); i++) {
                if (!e.equals(list.get(i))) break;
                if (i == max - 1) {
                    //加入禁言套餐(
                    SleepTime.put(target, System.currentTimeMillis() + 60 * stime * 1000);
                    event.setMessage(new PlainText("触发复读预警,bot即将禁用" + stime + "分钟"));
                    //消息队列清空
                    list.clear();
                }
            }
            //插入消息
            list.addFirst(e);
            if (list.size() > max) list.removeLast();
        } else {
            var list = new LinkedList<Message>();
            list.addFirst(e);
            MessageList.put(target, list);
        }
    }

    public static void setMax(int max) {
        SendmsgUtil.max = max;
    }

    public static void setStime(int stime) {
        SendmsgUtil.stime = stime;
    }

    /***
     * 支持单独添加禁言
     * @param id
     * @param time
     */
    public static void addSleep(long id, int time) {
        SleepTime.put(id, System.currentTimeMillis() + 1000 * time);
    }
    public static void wakeUp(long id){
        SleepTime.remove(id);
    }
}
