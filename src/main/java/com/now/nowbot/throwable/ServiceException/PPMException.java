package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class PPMException extends TipsException {
    public static enum Type {
        PPM_Me_PlayTimeTooShort("你的游戏时长太短了，快去多玩几局吧！"),//查询自己_玩家号太新
        PPM_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        PPM_Player_VSNotFound("你倒是告诉我要对比谁啊"),//未搜到vs玩家
        PPM_Player_PlayTimeTooShort("他的游戏时长太短了，快去催他多玩几局吧！"),//查询他人_玩家号太新
        PPM_Default_ManiaComingSoon("等哪天 mania 社区风气变好了，或许就有 PPM-mania 了吧...\n要不你给我出出主意（色眯眯的笑容）"),//Mania模式查询未开放
        PPM_Default_PictureRenderFailed("我...我画笔坏了画不出图呃"),//图片渲染失败，或者绘图出错
        PPM_Default_PictureSendFailed("图片被麻花疼拿去祭天了"),//图片发送失败
        PPM_Default_DefaultException("我好像生病了，需要休息一会..."),//默认报错
        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public PPMException(Type type){
        super(type.message);
    }
}
