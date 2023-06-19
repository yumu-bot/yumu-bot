package com.now.nowbot.irc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Channel {
    private       String                m_channel;
    private final BlockingQueue<String> m_messages;

    private final IRCClient client;

    Channel(String channel, IRCClient client) {
        m_channel = channel;
        m_messages = new LinkedBlockingQueue<>();
        this.client = client;
    }

    public void addMessage(String message){
        try {
            m_messages.put(message);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String poll() {
        try {
            return m_messages.take();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMessage(String message) {
        client.sendMessage(m_channel, message);
    }

    boolean hasNext() {
        return !m_messages.isEmpty();
    }
}
