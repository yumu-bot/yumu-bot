package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class SetuException extends TipsException {
    public static enum Type {
        SETU_Read_Error("图片读取失败。"), //读取_读取失败
        SETU_Download_Error("图片下载失败。"), //读取_读取失败
        SETU_Send_TooManyRequests("休息一下好不好"), //发送_太频繁
        SETU_Send_Error("图片发送失败。") //发送_发送失败

        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public SetuException(SetuException.Type type){
        super(type.message);
    }
}
