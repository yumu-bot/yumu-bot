package com.now.nowbot.qq.onebot;

import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.message.MessageReceipt;
import com.now.nowbot.qq.message.ReplyMessage;

import com.mikuac.shiro.core.Bot;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OneBotMessageReceipt extends MessageReceipt {
    private static final ScheduledExecutorService executor;
    static {
        var threadFactory = Thread.ofVirtual().name("v-OneBotMessageReceipt", 50).factory();
        executor = Executors.newScheduledThreadPool(Integer.MAX_VALUE, threadFactory);
    }
    int mid;
    Bot bot;
    com.now.nowbot.qq.onebot.contact.Contact contact;
    private OneBotMessageReceipt() {}

    public static OneBotMessageReceipt create(Bot bot, int mid, com.now.nowbot.qq.onebot.contact.Contact contact) {
        var r = new OneBotMessageReceipt();
        r.mid = mid;
        r.bot = bot;
        r.contact = contact;
        return r;
    }

    @Override
    public void recall() {
        bot.deleteMsg(mid);
    }

    @Override
    public void recallIn(long time) {
        executor.schedule(()->{
            bot.deleteMsg(mid);
        }, time, TimeUnit.MILLISECONDS);
    }

    @Override
    public Contact getTarget() {
        return contact;
    }

    @Override
    public ReplyMessage replay() {
        return new ReplyMessage(mid);
    }
}
