package com.now.nowbot.throwable.UtilException;

import com.now.nowbot.throwable.TipsException;

public class BindingException extends TipsException {
    public enum Type{
        no_bind(),
        ;
    }
    public BindingException(BindingException.Type type){
        switch (type){
            case no_bind:{
                setMessage("没有绑定啊!");
            }
        }
    }
}
