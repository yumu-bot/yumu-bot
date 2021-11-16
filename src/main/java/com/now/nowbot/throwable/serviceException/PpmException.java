package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

/**
 * ppm可能发生的异常
 */
public class PpmException extends TipsException {
    public static enum Type {
        异常名1("提示内容"),
        异常名2("提示内容"),
        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public PpmException(Type type){
        super(type.message);
    }
}
