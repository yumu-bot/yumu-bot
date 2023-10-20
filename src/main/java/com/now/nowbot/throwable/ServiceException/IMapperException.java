package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class IMapperException extends TipsException {
    public enum Type {
        IM_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_绑定失效
        IM_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        IM_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        IM_Send_Error ("IM 发送失败，请耐心等待问题修复。")

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
