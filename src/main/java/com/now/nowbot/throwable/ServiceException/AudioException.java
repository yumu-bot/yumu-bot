package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class AudioException extends TipsException {
    public enum Type {
        SONG_Parameter_NoBid("请输入想要试听的 bid 或者 sid！\n(!a <bid> / !a:s <sid>)"),//查询未输入字段
        SONG_Parameter_BidError("请输入正确的 bid 或者 sid！"),//查询未输入字段
        SONG_Map_NotFound ("找不到这张谱面，请检查。"), //谱面
        SONG_Connect_TimeOut("连接超时，请重试。"), //发送_发送失败
        SONG_Download_Error("试听音频下载失败。\n也许是谱面输入错误，或者谱面被版权限制了。\n自己去官网听算了。"), //发送_发送失败
        SONG_Send_Error("试听音频发送失败。\n请重试，或者将信息反馈给开发者。") //发送_发送失败
        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public AudioException(AudioException.Type type){
        super(type.message);
    }
}