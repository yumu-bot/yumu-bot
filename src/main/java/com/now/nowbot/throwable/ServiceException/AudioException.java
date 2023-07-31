package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class AudioException extends TipsException {
    public static enum Type {
        SONG_Parameter_NoBid("请输入想要试听的 Bid 或者 Sid！\n(!a:b / !a:s <id>，默认b)"),//查询未输入字段
        SONG_Connect_TimeOut("连接超时，请重试。"), //发送_发送失败
        SONG_Download_Error("试听音频下载失败。\n也许是谱面输入错误，或者谱面被版权限制了。"), //发送_发送失败
        SONG_Send_Error("试听音频发送失败。\n请重试，或者将信息反馈给开发者。") //发送_发送失败
        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public AudioException(AudioException.Type type){
        super(type.message);
    }
}