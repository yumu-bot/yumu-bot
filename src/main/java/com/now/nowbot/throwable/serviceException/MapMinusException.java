package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class MapMinusException extends TipsException {
    public enum Type {
        MM_Rate_TooSmall("这是什么奇怪的倍速？"),
        MM_Rate_Error("请输入正确的倍速，或是输入 + DT/HT 修改倍速！"),
        MM_Rate_TooLarge("我觉得吧，你要是能按这个倍速去打，早就进入天空之城了。"),
        MM_Bid_Error("请输入正确的 BID！"),
        MM_Function_NotSupported("抱歉，本功能暂不支持除 Mania 模式以外的谱面！"),
        MM_Map_NotFound("找不到谱面，或是 ppy API 无法访问。"),
        MM_Map_FetchFailed("谱面文件获取失败，请重试！"),
        MM_Map_ModeError("谱面模式获取失败！"),
        MM_Render_Error("谱面 Minus 信息渲染失败。请耐心等待问题修复，或稍后重试。"),
        MM_Send_Error("谱面 Minus 信息发送失败。请耐心等待问题修复，或稍后重试。")
        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MapMinusException(MapMinusException.Type type){
        super(type.message);
    }
    public MapMinusException(MapMinusException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}