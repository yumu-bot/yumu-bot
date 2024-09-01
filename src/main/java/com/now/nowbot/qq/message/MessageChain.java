package com.now.nowbot.qq.message;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class MessageChain {
    public static class MessageChainBuilder {
        private final MessageChain msgChain = new MessageChain();

        public MessageChainBuilder addImage(URL path) {
            msgChain.addMessage(new ImageMessage(path));
            return this;
        }

        public MessageChainBuilder addImage(byte[] data) {
            msgChain.addMessage(new ImageMessage(data));
            return this;
        }

        public MessageChainBuilder addVoice(byte[] data) {
            msgChain.addMessage(new VoiceMessage(data));
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

        public boolean isEmpty() {
            return msgChain.isEmpty();
        }

        public boolean isNotEmpty() {
            return !isEmpty();
        }

        public MessageChain build() {
            return msgChain;
        }
    }

    MessageChain() {
        messageList = new LinkedList<>();
    }

    public MessageChain(List<Message> messages) {
        messageList = new LinkedList<>(messages);
    }

    protected LinkedList<Message> messageList;

    public MessageChain(String msg) {
        messageList = new LinkedList<>();
        addMessage(new TextMessage(msg));
    }

    MessageChain addMessage(Message msg) {
        this.messageList.add(msg);
        return this;
    }

    public boolean isEmpty() {
        return messageList.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }
    public String getRawMessage() {
        var sb = new StringBuilder();
        messageList.forEach(m -> sb.append(m.toString()));
        return sb.toString();
    }

    public LinkedList<Message> getMessageList() {
        return messageList;
    }
}
