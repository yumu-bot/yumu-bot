package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPAException extends TipsException {
    public static enum Type {
        BP_Player_NoBind("对方还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"), //玩家_对方未绑定
        BP_Other_NotFound("获取对方的 BP 失败，请重试"), //分数_找不到分数
        BP_Send_Error("BP 分析发送失败，请重试") //发送_发送失败

        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BPAException(BPAException.Type type){
        super(type.message);
    }
}
