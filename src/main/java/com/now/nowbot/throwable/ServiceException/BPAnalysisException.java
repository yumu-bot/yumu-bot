package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPAnalysisException extends TipsException {
    public static enum Type {
        BPA_Me_NoBind("您还没有绑定呢，请输入 !ymbind 点击链接登录，完成绑定吧。"),//查询自己_玩家未绑定
        BPA_Me_LoseBind("您的绑定状态已经失效！\n请输入 !ymbind 重新绑定。\n除此之外，还可以试试 !ba [username]"),//查询自己_绑定失效
        BPA_Player_NotFound("这是谁呀，小沐找不到他哦"),//查询他人_未搜到玩家
        BPA_Player_FetchFailed("获取对方的 BPA 失败，请重试。\n或者，让他来绑定也是一种解决方法呢。"), //玩家_获取失败
        BPA_Player_NotEnoughBP("该玩家的 BP 都不足 5 个呢，\n灼热分析 EX"), //玩家_最好成绩范围错误
        BPA_Send_Error("BPA 发送失败。\n或者可以使用 !bpht。") //发送_发送失败

        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BPAnalysisException(BPAnalysisException.Type type){
        super(type.message);
    }
}
