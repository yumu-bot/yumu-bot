package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPAnalysisException extends TipsException {
    public enum Type {
        BPA_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)\n除此之外，还可以试试 !ba [username]"),//查询自己_绑定失效
        BPA_Me_FetchFailed("获取你的 BPA 失败，请重试。\n或者，重新绑定也是一种解决方法呢。"), //玩家_获取失败
        BPA_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"), //玩家_对方未绑定
        BPA_Player_NotFound("这是谁呀，小沐找不到他哦"),//查询他人_未搜到玩家
        BPA_Player_FetchFailed("获取对方的 BPA 失败，请重试。\n或者，让他重新绑定也是一种解决方法呢。"), //玩家_获取失败
        BPA_Me_NotEnoughBP("你的 BP 不足 5 个呢...\n灼热分析 EX"), //玩家_最好成绩范围错误
        BPA_Player_NotEnoughBP("对方的 BP 不足 5 个呢...\n灼热分析 EX"), //玩家_最好成绩范围错误
        BPA_BPHT_NotSupported("bpht 已移至 uuba。\n您也可以使用 !ba 来体验丰富版本。"), //不受支持
        BPA_Send_Error("BPA 发送失败。\n请耐心等待问题修复。或者可以使用 !bpht。") //发送_发送失败

        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BPAnalysisException(BPAnalysisException.Type type){
        super(type.message);
    }
}
