package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MapMinusException extends TipsException {
    public enum Type {
        MM_Bid_Error("请输入正确的 BID！"),
        MM_Function_NotSupported("抱歉，本功能暂不支持除 Mania 模式以外的谱面！"),
        MM_Map_NotFound("找不到谱面。"),
        MM_Map_FetchFailed("谱面下载失败，请尝试重新获取！"),
        MM_Map_ModeError("谱面模式获取失败！"),
        MM_Send_Error("MM 发送失败。请重试。") //发送_发送失败
        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MapMinusException(MapMinusException.Type type){
        super(type.message);
    }
}