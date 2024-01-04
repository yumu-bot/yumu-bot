package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MiniPanelException extends TipsException {
    public enum Type {
        MINI_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_绑定失效
        MINI_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        MINI_Recent_NotFound("找不到 24 小时内的成绩。"), //分数_找不到分数
        MINI_Classification_Error("迷你面板分类失败。\n请重试，或者将信息反馈给开发者。"), //发送_发送失败
        MINI_Fetch_Error("谱面获取数据失败。"),
        MINI_Send_Error("迷你面板发送失败。\n请耐心等待问题修复。"), //发送_发送失败

        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MiniPanelException(MiniPanelException.Type type){
        super(type.message);
    }
}