package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class InfoException extends TipsException {
    public enum Type {
        INFO_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_绑定失效
        INFO_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        INFO_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        INFO_Player_NoBP("找不到 BP！\n该玩家可能基本上没玩过，建议多练。"), //玩家_没有最好成绩
        INFO_Player_FetchFailed("获取玩家信息失败。\n请耐心等待问题修复。"), //发送_获取失败
        INFO_Send_Error("玩家信息发送失败。\n请耐心等待问题修复，或者可以尝试使用 !uui。") //发送_发送失败
        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public InfoException(InfoException.Type type){
        super(type.message);
    }
}