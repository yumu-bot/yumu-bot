package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class InfoException extends TipsException {
    public enum Type {
        I_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        I_Me_NotFound("找不到您绑定的 osu! 玩家！"),
        I_QQ_NotFound("找不到 %s 所绑定的玩家！"),
        I_Player_NotFound("找不到此玩家，请检查。"),
        I_Player_NoBP("找不到 BP！\n该玩家可能在 %s 模式上基本上没玩过。"),
        I_Player_FetchFailed("获取玩家信息失败。\n请耐心等待问题修复。"),
        I_BP_FetchFailed("获取玩家最好成绩失败。\n请耐心等待问题修复。"),
        I_API_Unavailable("ppy API 状态异常！"),
        I_Send_Error("玩家信息发送失败。\n请耐心等待问题修复，或者可以尝试使用 !uui。"),
        I_Fetch_Error("玩家信息图片渲染失败。")
        ;

        public final String message;

        Type(String message) {
            this.message = message;
        }
    }
    public InfoException(InfoException.Type type){
        super(type.message);
    }

    public InfoException(InfoException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}