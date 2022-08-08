package com.now.nowbot.model.live;

public enum LiveStatus {
    OPEN,CLOSE,PASS;
    public static LiveStatus get(int i){

        switch (i){
            case 1 : return OPEN;
            case 3 : return PASS;
            default : return CLOSE;
        }
    }
}
