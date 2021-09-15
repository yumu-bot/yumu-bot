package com.now.nowbot.throwable;

public class LogException extends Exception{
    Throwable x;
    public LogException(String msg,Throwable throwable){
        super(msg);
        x = throwable;
    }
    public Throwable getThrowable(){
        return x;
    }
}
