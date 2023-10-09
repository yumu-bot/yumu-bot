package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class KitaException extends TipsException {
    public enum Type {
        KITA_Player_NoBind ("对方还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"),
        KITA_Parameter_NoBid("请输入 bid！"),
        KITA_Parameter_BidError("请输入正确的 bid！"),
        KITA_Parameter_ModError("请输入正确的模组及位置！(NM1)"),
        KITA_Parameter_RoundError("请输入正确的对局名称！(Qualifier)"),
        KITA_Map_FetchFailed("从官网获取谱面失败。重试一下吧。"),
        KITA_Send_NotGroup("请在群聊中使用 KITA-X！"),
        KITA_Send_Error ("KITA 发送失败，请耐心等待问题修复。")

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
