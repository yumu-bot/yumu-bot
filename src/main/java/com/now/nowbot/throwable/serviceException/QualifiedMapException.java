package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class QualifiedMapException extends TipsException {
    public enum Type {
        Q_Result_NotFound("没有结果！"),//查询他人_未搜到玩家
        Q_Result_FetchFailed("获取结果失败，请重试。"), //玩家_获取失败
        Q_Parameter_Error("输入的参数范围错误！"),//榜单_时间错误
        Q_Parameter_OutOfRange("输入的参数范围超过 1-50！"),//榜单_时间错误
        Q_Send_Error("图片发送失败，请耐心等待问题修复。") //发送_发送失败

        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public QualifiedMapException(QualifiedMapException.Type type){
        super(type.message);
    }
}
