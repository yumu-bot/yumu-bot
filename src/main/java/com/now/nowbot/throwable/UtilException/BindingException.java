package com.now.nowbot.throwable.UtilException;

import com.now.nowbot.throwable.TipsException;

public class BindingException extends TipsException {
    public enum Type{
        no_bind("没有绑定啊!"),
        ;
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BindingException(BindingException.Type type){
        super(type.message);
    }
}
