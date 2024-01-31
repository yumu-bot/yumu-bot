package com.now.nowbot.throwable;

public class ModsException extends TipsRuntimeException{
    public enum Type {
        MOD_Receive_CharNotPaired("模组输入异常：字符数量非偶数"),
        MOD_Receive_Conflict("模组输入异常：以下模组冲突：\n%s"),

        ;

        public final String message;

        Type(String message) {
            this.message = message;
        }
    }

    public ModsException(ModsException.Type type){
        super(type.message);
    }

    public ModsException(ModsException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
