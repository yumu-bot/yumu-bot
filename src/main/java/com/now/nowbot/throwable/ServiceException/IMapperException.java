package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class IMapperException extends TipsException {
    public enum Type {
        IM_Me_LoseBind("您的绑定状态已经失效！\n请输入 !ymbind 重新绑定。"),//查询自己_绑定失效
        IM_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        IM_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        IM_Send_Error ("IM 发送失败，请重试。")

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
