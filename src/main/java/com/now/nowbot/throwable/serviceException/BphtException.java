package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class BphtException extends TipsException {
    public static enum Type {
        BPS_Me_BPNotFill("快刷满 BP100 再尝试查询吧！"),//查询自己_玩家BP未满
        BPS_Player_BPNotFill("快刷满 BP100 再尝试查询吧！"),//查询他人_玩家BP未满
        BPS_Default_PictureRenderFailed("我...我画笔坏了画不出图呃"),//图片渲染失败，或者绘图出错
        BPS_Default_PictureSendFailed("图片被麻花疼拿去祭天了"),//图片发送失败
        BPS_Default_DefaultException("我好像生病了，需要休息一会..."),//默认报错
        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
        }
    public BphtException(BphtException.Type type){
        super(type.message);
    }
}