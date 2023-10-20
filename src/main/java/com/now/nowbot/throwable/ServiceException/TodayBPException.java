package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class TodayBPException extends TipsException {
    public enum Type {
        TBP_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_绑定失效
        TBP_Player_TokenExpired("玩家的令牌已过期，请提醒他重新授权。(!ymbind)"), //玩家_对方未绑定
        TBP_Player_NotFound("这是谁呀，小沐找不到他哦。"),//查询他人_未搜到玩家
        TBP_Player_FetchFailed("获取对方的 TBP 失败，请重试。\n或者，也可以让他重新绑定。"), //玩家_获取失败
        TBP_BP_NoBP ("无法获取该玩家的 BP..."), //榜单_没有榜单
        TBP_BP_No24H ("今天之内没有新增的 BP 呢...\n尝试扩大搜索天数吧"), //榜单_今天没有
        TBP_BP_NoPeriod ("这段时间之内都没有新增的 BP 呢...\n尝试扩大搜索天数吧"), //榜单_一段时间没有
        TBP_BP_TooLongAgo("你输入的天数范围太久远了！"),//榜单_时间错误
        TBP_Send_Error ("TBP 发送失败，请耐心等待问题修复") //发送_发送失败

        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public TodayBPException(TodayBPException.Type type){
        super(type.message);
    }
}
