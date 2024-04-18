package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class NominationException extends TipsException {
    public enum Type {
        N_Instructions("""
                欢迎使用 Yumu Nomination 功能！食用方法：
                !ymnomination / !ymn (:mode) [sid]
                
                - mode: 模式。如果输入 b，则可以输入谱面的 bid。默认 s。
                - id：编号。可以输入 sid 或 bid。默认 sid。"""),
        N_Map_NotFound("找不到这张谱面，请检查。"),
        N_Map_FetchFailed("提名信息：谱面获取失败。\n请重试，或者将信息反馈给开发者。"),
        N_Discussion_FetchFailed("提名信息：讨论区获取失败。\n请重试，或者将信息反馈给开发者。"),
        N_API_Unavailable("ppy API 状态异常！"),
        N_Render_Failed("提名信息：渲染失败。"),
        N_Send_Error("提名信息：发送失败。\n请重试，或者将信息反馈给开发者。"),

        ;
        public final String message;

        Type(String message) {
            this.message = message;
        }
    }

    public NominationException(NominationException.Type type){
        super(type.message);
    }

    public NominationException(NominationException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}