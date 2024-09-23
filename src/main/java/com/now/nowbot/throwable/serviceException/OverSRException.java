package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class OverSRException extends TipsException {
    public enum Type {
        OV_Send_Success("未超星。"),
        OV_Parameter_Null("请输入正确的星数！"),
        OV_Parameter_Error("捞翔，恁发嘞是个啥玩应啊？"),
        OV_Parameter_OutOfRange("对方真的糊了那么高星的图吗？还是说你在滥用功能..."),
        OV_Send_Error("超星计时发送失败，请耐心等待问题修复。")

        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public OverSRException(OverSRException.Type type){
        super(type.message);
    }
}
