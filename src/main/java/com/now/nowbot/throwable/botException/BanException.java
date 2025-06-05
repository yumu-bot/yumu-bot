package com.now.nowbot.throwable.botException;

import com.now.nowbot.throwable.TipsException;

public class BanException extends TipsException {
    public enum Type {
        SUPER_Instruction("""
                请输入 super 操作！超管可用的操作有：
                whitelist：查询白名单
                blacklist：查询黑名单
                add：添加用户至白名单
                remove：移除用户出白名单
                ban：添加用户至黑名单
                unban：移除用户出黑名单"""),
        SUPER_Receive_NoQQ("%s 操作必须输入 qq！\n格式：!sp %s qq=114514 / group=1919810"),
        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BanException(BanException.Type type){
        super(type.message);
    }

    public BanException(BanException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
