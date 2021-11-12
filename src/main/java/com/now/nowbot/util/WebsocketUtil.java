package com.now.nowbot.util;

public class WebsocketUtil {
    public static boolean login(String key, String token){
        return token.length() == 10 && key.length() - 4 == token.charAt(7);
    }
}
