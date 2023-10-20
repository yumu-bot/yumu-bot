package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class PPPlusException extends TipsException {
    public enum Type {
        PPP_Player_VSNotFound("哩个瓜娃子到底要 VS 哪个哦！"),//未搜到vs玩家
        PPP_Default_APIConnectFailed("PP+ 网站连不上！你们俩还是听 interbot 的话去床上解决吧！"),//PP+API访问失败
        PPP_Send_Error("PPP 发送失败，请重试。") //发送_发送失败
        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
        }
    public PPPlusException(PPPlusException.Type type){
        super(type.message);
    }
}