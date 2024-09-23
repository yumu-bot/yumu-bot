package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class BPAnalysisException extends TipsException {
    public enum Type {
        BA_Me_TokenExpired("您的令牌已过期，或仅使用名称绑定。请重新授权。(!ymbind)\n除此之外，还可以试试 !ba [username]"),
        BA_Me_FetchFailed("获取你的 BPA 失败，请重试。\n如果一直出现错误，请提醒开发者。"), BA_Me_NotEnoughBP("你在 %s 模式上的 BP 不足 10 个呢...\n灼热分析 EX"),
        BA_Player_TokenExpired("此玩家的令牌已过期，或仅使用名称绑定。请提醒他重新授权。(!ymbind)"), BA_Player_NotFound("这是谁呀，小沐找不到他哦。"),
        BA_Player_FetchFailed("获取对方的 BPA 失败，请重试。\n如果一直错误请反馈开发者。"),
        BA_Player_NotEnoughBP("对方在 %s 模式上的 BP 不足 10 个呢...\n灼热分析 EX"),
        BA_Attr_FetchFailed("获取变化的谱面数据超时（太多了），如果你是第一次见到这条消息，第二次通常就会恢复了。"),
        BA_Instruction_Deprecated("bpht 已移至 ua。\n您也可以使用 !ba 来体验丰富版本。\n猫猫 Bot 的 bpht 需要输入 !get bpht。"),
        BA_I_Deprecated("uuba-i 已移至 ua。\n您也可以使用 !ba 来体验丰富版本。"), BA_Fetch_Failed("最好成绩获取失败。"),
        BA_Render_Error("最好成绩渲染失败。"),
        BA_Send_Error("最好成绩分析发送失败。\n请耐心等待问题修复。或者可以使用 !uuba。"),
        BA_Send_UUError("最好成绩分析（文字版）发送失败。"),

        ;
        public final String message;

        Type(String message) {
            this.message = message;
        }
    }

    public BPAnalysisException(BPAnalysisException.Type type) {
        super(type.message);
    }

    public BPAnalysisException(BPAnalysisException.Type type, Object... args) {
        super(String.format(type.message, args));
    }
}
