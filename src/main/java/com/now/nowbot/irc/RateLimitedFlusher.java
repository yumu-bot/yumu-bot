package com.now.nowbot.irc;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class RateLimitedFlusher extends Thread{
    private IRCClient m_client;
    private int m_delay;

    RateLimitedFlusher(IRCClient client, int delay) {
        m_client = client;
        m_delay = delay;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Iterator<Channel> it = m_client.getChannels().values().iterator();
                synchronized (it) {
                    while (it.hasNext()) {
                        Channel limiter = it.next();
                        String line = limiter.poll();
                        if (line != null)
                            m_client.write(line);
                    }
                }
                it = null;
                Thread.sleep(m_delay);
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            } catch (InterruptedException ignore) {
            }
        }
    }
}
