package com.now.nowbot.throwable.botException;

import com.now.nowbot.throwable.TipsException;

public class KitaException extends TipsException {
    public enum Type {
        KITA_Parameter_NoBid("请输入 bid！"),
        KITA_Parameter_BidError("请输入正确的 bid！"),
        KITA_Parameter_ModError("请输入正确的模组及位置！(NM1)"),
        KITA_Parameter_RoundError("请输入正确的对局名称！(Qualifier)"),
        KITA_Map_FetchFailed("从官网获取谱面失败。重试一下吧。"),
        KITA_Send_NotGroup("请在群聊中使用 KITA-X！"),
        KITA_Send_Error("喜多谱面信息发送失败，请耐心等待问题修复。"),
        KITA_Deprecated_K("!ymk 喜多谱面信息面板已移至 !kt。"),

        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public KitaException(KitaException.Type type){
        super(type.message);
    }
}
