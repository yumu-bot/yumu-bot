package com.now.nowbot.qq.local;

import com.now.nowbot.qq.Bot;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.local.contact.LocalGroup;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.message.TextMessage;

import java.util.List;

public class Event implements com.now.nowbot.qq.event.Event {
    private final com.now.nowbot.qq.local.Bot bot;

    Event(com.now.nowbot.qq.local.Bot b) {
        bot = b;
    }

    @Override
    public Bot getBot() {
        return bot;
    }

    static public class MessageEvent extends Event implements com.now.nowbot.qq.event.MessageEvent {
        private final LocalGroup group;
        private final String     message;

        MessageEvent(com.now.nowbot.qq.local.Bot b, LocalGroup group, String message) {
            super(b);
            this.group = group;
            this.message = message;
        }

        @Override
        public Contact getSubject() {
            return group;
        }

        @Override
        public Contact getSender() {
            return group;
        }

        @Override
        public MessageChain getMessage() {
            return new MessageChain(List.of(new TextMessage(message)));
        }

        @Override
        public String getRawMessage() {
            return message;
        }

        @Override
        public String getTextMessage() {
            return message;
        }
    }

    static public class GroupMessageEvent extends MessageEvent implements com.now.nowbot.qq.event.GroupMessageEvent {

        public GroupMessageEvent(com.now.nowbot.qq.local.Bot b, LocalGroup group, String message) {
            super(b, group, message);
        }

        @Override
        public Group getGroup() {
            return super.group;
        }

        public Group getSubject() {
            return super.group;
        }
    }
}