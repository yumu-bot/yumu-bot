package com.now.nowbot.throwable;

public class TipsException extends Exception{
    String message;
    public TipsException(String message) {
        setMessage(message);
    }
    public TipsException(){}
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        if (message != null) {
            return message;
        }
        return super.getMessage();
    }
}
