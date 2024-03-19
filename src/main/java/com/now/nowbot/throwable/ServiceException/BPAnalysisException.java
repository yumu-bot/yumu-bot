package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPAnalysisException extends TipsException {
    public enum Type {
        BPA_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)\n除此之外，还可以试试 !ba [username]"),//查询自己_绑定失效
        BPA_Attr_FetchFailed("获取变化的谱面数据超时（太多了），如果你是第一次见到这条消息，请重试。"),
        BPA_Me_FetchFailed("获取你的 BPA 失败，请重试。\n如果一直出现错误，请提醒开发者。"), //玩家_获取失败
        BPA_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"), //玩家_对方未绑定
        BPA_Player_NotFound("这是谁呀，小沐找不到他哦。"),//查询他人_未搜到玩家
        BPA_Player_FetchFailed("获取对方的 BPA 失败，请重试。\n如果一直错误请反馈开发者。"), //玩家_获取失败
        BPA_Me_NotEnoughBP("你在 %s 模式上的 BP 不足 10 个呢...\n灼热分析 EX"), //玩家_最好成绩范围错误
        BPA_Player_NotEnoughBP("对方在 %s 模式上的 BP 不足 10 个呢...\n灼热分析 EX"), //玩家_最好成绩范围错误
        BPA_Instruction_Deprecated("bpht 已移至 uuba。\n您也可以使用 !ba 来体验丰富版本。\n猫猫 Bot 的 bpht 需要输入 !get bpht。"), //不受支持
        BPA_Send_Error("最好成绩分析发送失败。\n请耐心等待问题修复。或者可以使用 !uuba。") //发送_发送失败

        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BPAnalysisException(BPAnalysisException.Type type){
        super(type.message);
    }
    public BPAnalysisException(BPAnalysisException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
