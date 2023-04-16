package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class FriendException extends TipsException {
    public static enum Type {
        FRIEND_Client_ParameterOverRange("你给的参数太奇怪了，我看不懂！\n请给我两个自然数并且差值不要超过100个！"),//查询_参数错误
        FRIEND_Client_ParameterTooLarge("进不去！\n这么大的参数，怎么想都进不去吧？"),//查询_参数太大
        FRIEND_Client_FriendTooLittle("我们 Bot 是经过专业训练的\n无论多好笑，我们都不会笑，除非忍不住\n(您好友太少啦！"),//查询_好友太少
        FRIEND_Client_CardRenderFailed("有张卡片加载失败了...\n详细数据是：\n"),//查询_卡片错误

        FRIEND_Default_PictureRenderFailed("我...我画笔坏了画不出图呃"),//图片渲染失败，或者绘图出错
        FRIEND_Default_PictureSendFailed("图片被麻花疼拿去祭天了"),//图片发送失败
        FRIEND_Default_DefaultException("我好像生病了，需要休息一会..."),//默认报错
        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
        }
    public FriendException(FriendException.Type type){
        super(type.message);
    }
}