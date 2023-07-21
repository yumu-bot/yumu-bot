package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class QualifiedMapException extends TipsException {
    public static enum Type {
        Q_Player_NoBind("对方还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"), //玩家_对方未绑定
        Q_Result_NotFound("没有结果！"),//查询他人_未搜到玩家
        Q_Result_FetchFailed("获取结果失败，请重试。"), //玩家_获取失败
        Q_Parameter_Error("输入的参数范围错误！"),//榜单_时间错误
        Q_Parameter_OutOfRange("输入的参数范围超过 1-100！"),//榜单_时间错误
        Q_Send_Error("图片发送失败，请重试") //发送_发送失败

        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public QualifiedMapException(QualifiedMapException.Type type){
        super(type.message);
    }
}
