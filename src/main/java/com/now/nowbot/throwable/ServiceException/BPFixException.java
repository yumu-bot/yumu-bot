package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPFixException extends TipsException {
    public enum Type {
        BF_BP_Empty("没有最好成绩！"),
        BF_Fix_Empty("您的最好成绩已经全是理论值了！"),
        BF_Render_Error("理论最好成绩渲染失败。请耐心等待问题修复，或稍后重试。"),
        BF_Send_Error("理论最好成绩发送失败。请耐心等待问题修复，或稍后重试。")
        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BPFixException(BPFixException.Type type){
        super(type.message);
    }
    public BPFixException(BPFixException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
