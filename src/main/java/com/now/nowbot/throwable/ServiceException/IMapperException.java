package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class IMapperException extends TipsException {
    public enum Type {
        IM_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        IM_Player_TokenExpired("此玩家的令牌已过期或从未绑定，请提醒他重新授权。(!ymbind)"),
        IM_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        IM_Player_NotFound("找不到此玩家，请检查。"),
        IM_Send_Error("谱师信息发送失败，请耐心等待问题修复。"),
        IM_Fetch_Error("谱师信息图片获取失败。")

        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public IMapperException(IMapperException.Type type){
        super(type.message);
    }
}
