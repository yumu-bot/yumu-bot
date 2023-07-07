package com.now.nowbot.qq.contact;

import com.now.nowbot.qq.message.MessageChain;

import java.net.URL;

public interface User {
    long getId();

    String getName();


    int sendMessage(MessageChain msg);

    default int sendText(String msg) {
        return sendMessage(new MessageChain.MessageChainBuilder().addText(msg).build());
    }

    default int sendImage(String path) {
        return sendMessage(new MessageChain.MessageChainBuilder().addImage(path).build());
    }

    default int sendImage(URL url) {
        return sendMessage(new MessageChain.MessageChainBuilder().addImage(url).build());
    }

    default int sendImage(byte[] data) {
        return sendMessage(new MessageChain.MessageChainBuilder().addImage(data).build());
    }
}
