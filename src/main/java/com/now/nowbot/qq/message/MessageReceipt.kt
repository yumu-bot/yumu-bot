package com.now.nowbot.qq.message;

import com.now.nowbot.qq.contact.Contact;

public abstract class MessageReceipt {
    public abstract void recall();
    public abstract void recallIn(long time);

    public abstract Contact getTarget();
    public abstract ReplyMessage reply();
}
