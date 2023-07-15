package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPAException extends TipsException {
    public static enum Type {
        BPA_Me_NoBind("您还没有绑定呢，请输入 !ymbind 点击链接登录，完成绑定吧"),//查询自己_玩家未绑定
        BPA_Player_NoBind("对方还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"), //玩家_对方未绑定
        BPA_Other_NotFound("获取对方的 BPA 失败，请重试。\n或者，让他来绑定也是一种解决方法呢。"), //分数_找不到分数
        BPA_Send_Error("BPA 发送失败，请重试。\n这个功能需要的查询任务很重，请尽量错峰查询。\n或者使用 !bpht 的旧版查询。") //发送_发送失败

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
