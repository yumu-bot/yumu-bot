package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class PppException extends TipsException {
    public static enum Type {
        PPP_Player_VSNotFound("哩个瓜娃子到底要 VS 哪个哦！"),//未搜到vs玩家-name == null
        PPP_Default_APIConnectFailed("PP+ 网站连不上！你们俩还是听 interbot 的话去床上解决吧！"),//PP+API访问失败
        PPP_Default_PictureRenderFailed("我...我画笔坏了画不出图呃"),//图片渲染失败，或者绘图出错
        PPP_Default_PictureSendFailed("图片被麻花疼拿去祭天了"),//图片发送失败
        PPP_Default_DefaultException("我好像生病了，需要休息一会..."),//默认报错
        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
        }
    public PppException(PppException.Type type){
        super(type.message);
    }
}