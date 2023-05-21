package com.now.nowbot.throwable;

public class PermissionException extends RuntimeException{
    public PermissionException(String s){
        super(s);
    }
    public PermissionException(String s, String s2){
        super(s + s2);
    }

}
