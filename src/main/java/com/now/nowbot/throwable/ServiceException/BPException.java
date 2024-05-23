package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPException extends TipsException {
    public enum Type {
        BP_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        BP_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"),
        BP_Me_Banned("你号没了，或是 ppy API 无法访问。"),
        BP_Map_NoRank("请输入 BP 编号或范围！"),
        BP_Param_OutOfRange("请输入正确的 BP 编号或范围！"),
        BP_QQ_NotFound("找不到 %s 所绑定的玩家！"),
        BP_Player_NotFound("找不到玩家 %s，请检查。"),
        BP_Player_FetchFailed("无法获取玩家信息，请检查。"),
        BP_List_FetchFailed("获取 BP 失败，请重试。"),
        BP_Player_NoBP("该玩家在 %s 模式内基本没玩过。"),
        BP_Render_Failed("最好成绩渲染失败。"),
        BP_Send_Failed("最好成绩发送失败。\n请耐心等待问题修复。"),

        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BPException(BPException.Type type){
        super(type.message);
    }

    public BPException(BPException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
