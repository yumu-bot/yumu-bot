package com.now.nowbot.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.BufferedWriter;
import java.util.concurrent.TimeUnit;

public class WebsocketUtil {
    public static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)//设置读取超时时间
            .writeTimeout(5, TimeUnit.SECONDS)//设置写的超时时间
            .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
            .build();

    public static boolean login(String key, String token) {
        return true;
    }

    public static WebSocket getWebsocket(String url, String token, WebSocketListener listener) {
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


}
