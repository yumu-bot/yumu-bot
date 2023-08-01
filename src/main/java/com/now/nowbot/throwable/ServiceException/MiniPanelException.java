package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MiniPanelException extends TipsException {
    public static enum Type {
        MINI_Me_LoseBind("您的绑定状态已经失效！\n请输入 !ymbind 重新绑定。"),//查询自己_绑定失效
        MINI_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        MINI_Recent_NotFound("找不到 24 小时内的成绩。"), //分数_找不到分数
        MINI_Classification_Error("迷你面板分类失败。\n请重试，或者将信息反馈给开发者。"), //发送_发送失败
        MINI_Send_Error("迷你面板发送失败。\n请重试，或者将信息反馈给开发者。"), //发送_发送失败

        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MiniPanelException(MiniPanelException.Type type){
        super(type.message);
    }
}