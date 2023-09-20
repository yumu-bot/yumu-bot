package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPException extends TipsException {
    public static enum Type {
        BP_Me_NoBind("您还没有绑定呢，请输入 !ymbind 点击链接登录，完成绑定吧。"),//查询自己_玩家未绑定
        BP_Me_LoseBind("您的绑定状态已经失效！\n请输入 !ymbind 重新绑定。"),//查询自己_绑定失效
        BP_Map_NoRank ("请输入 BP 编号！"),
        BP_Map_RankError ("请输入正确的 BP 编号！"),
        BP_Player_NotFound("这是谁呀，小沐找不到他哦"),//查询他人_未搜到玩家
        BP_Player_FetchFailed("获取 BP 失败，请重试。"), //玩家_获取失败
        BP_Player_NoBP("该玩家基本上没玩过，建议多练。"), //玩家_没有最好成绩
        BP_Send_Error("BP 发送失败。\n或者可以使用 !BPL (BP Legacy)。") //发送_发送失败
        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BPException(BPException.Type type){
        super(type.message);
    }
}