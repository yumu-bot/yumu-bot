package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

/**
 * ppm可能发生的异常
 */
public class PpmException extends TipsException {
    public static enum Type {
        PPM_Me_NoBind("您还没有绑定呢，请输入 !bind 点击链接登录，完成绑定吧"),//查询自己_玩家未绑定
        PPM_Me_NoAuthorization("您撤销了授权呢，请输入 !bind 点击链接登录，重新授权吧"),//查询自己_玩家撤销授权
        PPM_Me_PlayTimeTooShort("你的游戏时长太短了，快去多玩几局吧！"),//查询自己_玩家号太新
        PPM_Me_Banned("哼哼，你号没了"),//查询自己_玩家被封禁
        PPM_Me_Blacklisted("本 Bot 根本不想理你"),//查询自己_玩家黑名单
        PPM_Player_NoBind("他还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"),//查询他人_玩家未绑定
        PPM_Player_NoAuthorization("他撤销了授权呢，请提醒他输入 !bind 点击链接登录，重新授权吧"),//查询他人_玩家撤销授权
        PPM_Player_NoData("你查的不会是我的机器人同类吧！"),//查询他人_玩家无数据
        PPM_Player_NotFound("这是谁呀，小沐不认识哦"),//查询他人_未搜到玩家
        PPM_Player_VSNotFound("你倒是告诉我要对比谁啊"),//未搜到vs玩家
        PPM_Player_PlayTimeTooShort("他的游戏时长太短了，快去催他多玩几局吧！"),//查询他人_玩家号太新
        PPM_Player_Banned("哼哼，他号没了"),//查询他人_玩家被封禁
        PPM_Player_Blacklisted("我不想和他一起玩！"),//查询他人_玩家黑名单
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
    public PpmException(Type type){
        super(type.message);
    }
}
