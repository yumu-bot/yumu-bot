package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class TodayBPException extends TipsException {
    public enum Type {
        TBP_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        TBP_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"),
        TBP_QQ_NotFound("找不到 %s 所绑定的玩家！"),
        TBP_Player_NotFound("找不到玩家 %s，请检查。"),
        TBP_Player_FetchFailed("获取对方的今日最好成绩失败，请重试。\n或者，也可以让他重新绑定。"),
        TBP_BP_NoBP("无法获取玩家 %s 的 BP...\n如果这不是你想查询的玩家，请在末尾添加 #1。"),
        TBP_BP_Inactive("玩家 %s 最近不活跃...\n如果这不是你想查询的玩家，请在末尾添加 #1。"),
        TBP_BP_No24H("玩家 %s 今天之内没有新增的 BP 呢...\n尝试扩大搜索天数吧"),
        TBP_BP_NoPeriod("玩家 %s 这段时间之内都没有新增的 BP 呢...\n尝试扩大搜索天数吧"),
        TBP_List_FetchError("获取最好成绩列表失败！"),
        TBP_BP_TooLongAgo("你输入的天数范围太久远了！"),
        TBP_API_Unavailable("ppy API 状态异常！"),
        TBP_Fetch_Error("今日最好成绩图片渲染失败。"),
        TBP_Send_Error("今日最好成绩发送失败，请耐心等待问题修复。"),

        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public TodayBPException(TodayBPException.Type type){
        super(type.message);
    }
    public TodayBPException(TodayBPException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
