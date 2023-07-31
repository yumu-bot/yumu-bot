package com.now.nowbot.qq.contact;

import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.message.MessageReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public interface Contact {
    Logger log = LoggerFactory.getLogger(Contact.class);
    long getId();

    String getName();


    MessageReceipt sendMessage(MessageChain msg);

    default MessageReceipt sendMessage(String msg) {
        var m = new MessageChain.MessageChainBuilder().addText(msg).build();
        return sendMessage(m);
    }

    default MessageReceipt sendText(String msg) {
        return sendMessage(new MessageChain.MessageChainBuilder().addText(msg).build());
    }

    default MessageReceipt sendImage(String path) {
        return sendMessage(new MessageChain.MessageChainBuilder().addImage(path).build());
    }

    default MessageReceipt sendImage(URL url) {
        return sendMessage(new MessageChain.MessageChainBuilder().addImage(url).build());
    }

    default MessageReceipt sendImage(byte[] data) {
        return sendMessage(new MessageChain.MessageChainBuilder().addImage(data).build());
    }

    default MessageReceipt sendVoice(byte[] data) {
        return sendMessage(new MessageChain.MessageChainBuilder().addVoice(data).build());
    }

    default void recall(MessageReceipt msg){
        msg.recall();
    }
    default void recallIn(MessageReceipt msg , long time){
        msg.recallIn(time);
    }
}
