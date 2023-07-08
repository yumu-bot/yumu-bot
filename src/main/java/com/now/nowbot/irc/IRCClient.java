package com.now.nowbot.irc;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.net.Socket;
import java.util.Map;

public class IRCClient implements Runnable {
    private static final int DEFAULT_DELAY = 200;

    private final String m_address;
    private final int    m_port;
    private final String m_user;
    private final String m_password;

    private Socket      m_socket;
    private PrintStream m_outStream;

    private PipedInputStream  pipedInputStream;
    private PipedOutputStream pipedOutputStream;

    private RateLimitedFlusher   m_flusher;
    private Map<String, Channel> m_channels;
    private int                  m_delay;

    private boolean m_disconnected;

    public IRCClient(String address, int port, String user, String password) {
        m_address = address;
        m_port = port;
        m_user = user;
        m_password = password;

        m_channels = new HashMap<>();
        m_delay = DEFAULT_DELAY;

        m_disconnected = true;

        try {
            pipedOutputStream = new PipedOutputStream();
            pipedInputStream = new PipedInputStream(pipedOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDisconnected() {
        return m_disconnected;
    }

    public void setDelay(int delay) {
        m_delay = delay;
    }

    public Channel getChannel(String channel) {
        return m_channels.get(channel);
    }

    public InputStream getInputStream() throws IOException {
        return pipedInputStream;
    }

    public String getUser() {
        return m_user;
    }

    Map<String, Channel> getChannels() {
        return m_channels;
    }

    public void connect() throws IOException {
        if (!m_disconnected) {
            System.out.println("Attempt to connect the IRCClient without first disconnecting.");
            return;
        }

        m_socket = new Socket(m_address, m_port);
        m_outStream = new PrintStream(m_socket.getOutputStream());

        m_flusher = new RateLimitedFlusher(this, m_delay);
        m_flusher.start();

        m_disconnected = false;

        register();
    }

    @Override
    public void run() {
        try (InputStream in = m_socket.getInputStream()) {
            String line = null;
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader breader = new BufferedReader(inputStreamReader);
            while (!m_disconnected && (line = breader.readLine()) != null) {
                pipedOutputStream.write(line.getBytes());
                pipedOutputStream.flush();

                int nameIndex;
                if ((nameIndex = line.indexOf("!cho@ppy.sh")) != -1) {

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void disconnect() throws IOException {
        if (m_disconnected) {
            System.out.println("Attempt to disconnect without first connecting.");
            return;
        }
        m_flusher.interrupt();
        m_outStream.close();
        m_socket.close();
        m_disconnected = true;
    }

    public void write(String message) {
        write(message, false);
    }

    private void write(String message, boolean censor) {
        if (!censor && !message.startsWith("PING") && !message.startsWith("PONG")) {
            System.out.println("SEND(" + new Date(System.currentTimeMillis()) + "): " + message);
        }
        m_outStream.println(message);
    }

    private void register() {
        write("PASS" + " " + m_password, true);
        write("NICK" + " " + m_user);
        write("USER" + " " + m_user + " no irc.ppy.sh :" + m_user);
    }

    public Channel joinChannel(String channel) {
        write("JOIN " + channel);
        return m_channels.put(channel, new Channel(channel, this));
    }

    public void sendMessage(String channel, String message) {
        synchronized (m_channels) {
            if (!m_channels.containsKey(channel)) {
                m_channels.put(channel, new Channel(channel, this));
            }

//            m_channels.get(channel).addMessage(message);
        }
    }
}