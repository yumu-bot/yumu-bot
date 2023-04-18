package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class TodayBPException extends TipsException {
    public static enum Type {
        TBP_Player_NoBind ("对方还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"), //玩家_对方未绑定
        TBP_BP_NoBP ("无法获取该玩家的 BP..."), //榜单_没有榜单
        TBP_BP_No24H ("今天之内没有新增的 BP 呢...\n尝试扩大搜索天数吧"), //榜单_今天没有
        TBP_BP_NoPeriod ("这段时间之内都没有新增的 BP 呢...\n尝试扩大搜索天数吧"), //榜单_一段时间没有
        TBP_Send_Error ("图片发送失败，请重试") //发送_发送失败

        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public TodayBPException(TodayBPException.Type type){
        super(type.message);
    }
}