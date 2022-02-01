package com.now.nowbot.model.live;

public enum LiveStatus {
    OPEN,CLOSE,PASS;
    public static LiveStatus get(int i){
        return switch (i){
            case 1 -> OPEN;
            case 3 -> PASS;
            default -> CLOSE;
        };
    }
}
