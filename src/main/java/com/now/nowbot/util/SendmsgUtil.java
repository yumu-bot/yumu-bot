package com.now.nowbot.util;

import com.now.nowbot.entity.ServiceThrowError;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SendmsgUtil {
    static int max = 5;
    static Map<Contact, List<String>> MessageList = new ConcurrentHashMap<>();
    static Map<Contact, Long> SleepTime = new ConcurrentHashMap<>();
    public static MessageReceipt send(Contact sender, Message e) throws Exception {
        if(MessageList.containsKey(sender)){
            var list = MessageList.get(sender);
            list.add(0,e.contentToString());
            if (list.size()>max) list.remove(max);
            String limt = list.get(0);
            for (int i = 0; i < list.size(); i++) {
                if(!limt.equals(list.get(i)))break;
                if(i == max-1) {
                    sender.sendMessage("触发复读预警,防止扰乱本群,即将禁用15分钟");
                    SleepTime.put(sender, System.currentTimeMillis());
                    throw new Exception(sender.getId()+"触发复读预警");
                }
            }
        }else {
            MessageList.put(sender, List.of(e.contentToString()));
        }
        if (SleepTime.containsKey(sender)){
            if (System.currentTimeMillis()<SleepTime.get(sender)+60*15*1000){
                throw new Exception(sender.getId()+"触发复读预警");
            }else {
                SleepTime.remove(sender);
            }
        }
        return sender.sendMessage(e);
    }
    public static MessageReceipt send(Contact sender, String e) throws Exception {
        if(MessageList.containsKey(sender)){
            var list = MessageList.get(sender);
            list.add(0,e);
            if (list.size()>max) list.remove(max);
            String limt = list.get(0);
            for (int i = 0; i < list.size(); i++) {
                if(!limt.equals(list.get(i)))break;
                if(i == max-1) {
                    sender.sendMessage("触发复读预警,防止扰乱本群,即将禁用15分钟");
                    SleepTime.put(sender, System.currentTimeMillis());
                    throw new Exception(sender.getId()+"触发复读预警");
                }
            }
        }else {
            MessageList.put(sender, List.of(e));
        }
        if (SleepTime.containsKey(sender)){
            if (System.currentTimeMillis()<SleepTime.get(sender)+60*15*1000){
                throw new Exception(sender.getId()+"触发复读预警");
            }else {
                SleepTime.remove(sender);
            }
        }
        return sender.sendMessage(e);
    }
}
