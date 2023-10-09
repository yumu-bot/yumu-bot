package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class InfoException extends TipsException {
    public enum Type {
        INFO_Me_LoseBind("您的绑定状态已经失效！\n请输入 !ymbind 重新绑定。"),//查询自己_绑定失效
        INFO_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        INFO_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        INFO_Player_NoBP("获取 BP 失败。\n也许，该玩家基本上没玩过，建议多练。"), //玩家_没有最好成绩
        INFO_Send_Error("INFO 发送失败。\n或者可以使用 !uui (Info Legacy)。") //发送_发送失败
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