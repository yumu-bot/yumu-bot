package com.now.nowbot.util;

import com.now.nowbot.irc.IRCClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.net.Socket;

public class WebsocketUtil {
    public static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.SECONDS)//设置读取超时时间
            .writeTimeout(3, TimeUnit.SECONDS)//设置写的超时时间
            .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
            .build();

    public static boolean login(String key, String token) {
        return true;
    }

    public static WebSocket getWebsocker(String url, String token, WebSocketListener listener) {
        Request request = new Request
                .Builder()
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .url(url)
                .build();
        return CLIENT.newWebSocket(request, listener);
    }

    static void sendString(BufferedWriter bw, String str) {
        try {
            bw.write(str + "\r\n");
            bw.flush();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }

    public static void main(String[] args) {

        try {

            String server = "irc.ppy.sh";
            int port = 6667;
            String password = "cc83c588";
            String nickname = "-Spring_Night-";
            String channel = "#Chinese";
            String message = "hi, all";

            IRCClient ircClient = new IRCClient(server, port, nickname, password);
            ircClient.setDelay(500);
            ircClient.connect();

            new Thread(() -> {
                InputStreamReader inputStreamReader = null;
                try {
                    inputStreamReader = new InputStreamReader(ircClient.getInputStream());
                    BufferedReader breader = new BufferedReader(inputStreamReader);
                    String line = null;
                    while ((line = breader.readLine()) != null) {
                        if (!line.contains("cho@ppy.sh QUIT")) {
                            if (line.contains("001 AutoHost")) {
                                System.out.println("Logged in");
                                System.out.println("Line: " + line);
                            } else if (line.startsWith("PING")) {
                                String pingResponse = line.replace("PING", "PONG");
                                ircClient.write(pingResponse);
                            } else if (line.startsWith("PONG")) {
                            } else {
                                System.out.println(">> " + line);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }).start();

            var b = ircClient.getChannel("BanchoBot");
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                String line = null;
                String c;
                String m;
                while ((line = scanner.nextLine()) != null) {
                    c = line.trim();
                    int n = c.indexOf(' ');
                    if (n < 0) continue;
                    m = c.substring(n+1);
                    c = c.substring(0,n);
                    if (c.equals("write")) {
                        ircClient.write(m);
                    } else {
                        ircClient.sendMessage(c, m);
                    }
                }
            }).start();
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                try {
                    ircClient.disconnect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            ));

            Thread.sleep(1000);
            ircClient.write("/who");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
