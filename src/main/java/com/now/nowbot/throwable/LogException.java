package com.now.nowbot.throwable;

public class LogException extends RuntimeException{
    Throwable x;
    public LogException(String msg,Throwable throwable){
        super(msg);
        x = throwable;
    }

    public LogException(String msg) {
        super(msg);
    }
    public Throwable getThrowable(){
        return x;
    }
}
