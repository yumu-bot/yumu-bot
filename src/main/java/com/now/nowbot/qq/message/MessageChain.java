package com.now.nowbot.qq.message;

import com.mikuac.shiro.common.utils.MsgUtils;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class MessageChain {
    public static class MessageChainBuilder {
        private final MessageChain msgChain = new MessageChain();

        public MessageChainBuilder addImage(String path) {
            msgChain.addMessage(new ImageMessage(path));
            return this;
        }
        public MessageChainBuilder addImage(URL path) {
            msgChain.addMessage(new ImageMessage(path));
            return this;
        }
        public MessageChainBuilder addImage(byte[] data) {
            msgChain.addMessage(new ImageMessage(data));
            return this;
        }
        public MessageChainBuilder addText(String msg) {
            msgChain.addMessage(new TextMessage(msg));
            return this;
        }
        public MessageChainBuilder addAt(long qq) {
            msgChain.addMessage(new AtMessage(qq));
            return this;
        }
        public MessageChainBuilder addAtAll() {
            msgChain.addMessage(new AtMessage());
            return this;
        }



        public MessageChain build() {
            return msgChain;
        }
    }

    MessageChain() {
    }

    protected LinkedList<Message> messageList = new LinkedList<>();

    MessageChain(String msg) {
        messageList.add(new TextMessage(msg));
    }

    MessageChain addMessage(Message msg) {
        return this;
    }

    public LinkedList<Message> getMessageList() {
        return messageList;
    }
}
