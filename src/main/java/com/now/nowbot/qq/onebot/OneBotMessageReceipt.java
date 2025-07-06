package com.now.nowbot.qq.onebot;

import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.message.MessageReceipt;
import com.now.nowbot.qq.message.ReplyMessage;

import java.util.Objects;
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
    long botId;
    com.now.nowbot.qq.onebot.contact.Contact contact;

    private OneBotMessageReceipt() {
    }

    public static OneBotMessageReceipt create(long botId, int mid, com.now.nowbot.qq.onebot.contact.Contact contact) {
        var r = new OneBotMessageReceipt();
        r.mid = mid;
        r.botId = botId;
        r.contact = contact;
        return r;
    }

    public static OneBotMessageReceipt create() {
        return new OneBotMessageReceipt();
    }

    @Override
    public void recall() {
        var bot = BotManager.Companion.getBot(botId);
        if (Objects.isNull(bot)) return;
        bot.deleteMsg(mid);
    }

    @Override
    public void recallIn(long time) {
        executor.schedule(() -> {
            var bot = BotManager.Companion.getBot(botId);
            if (Objects.isNull(bot)) return;
            bot.deleteMsg(mid);
        }, time, TimeUnit.MILLISECONDS);
    }

    @Override
    public Contact getTarget() {
        return contact;
    }

    @Override
    public ReplyMessage reply() {
        return new ReplyMessage(mid);
    }
}
