package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class BphtException extends TipsException {
    public static enum Type {
        BPS_Me_NoBind("您还没有绑定呢，请输入 !bind 点击链接登录，完成绑定吧"),//查询自己_玩家未绑定
        BPS_Me_NoAuthorization("您撤销了授权呢，请输入 !bind 点击链接登录，重新授权吧"),//查询自己_玩家撤销授权
        BPS_Me_BPNotFill("快刷满 BP100 再尝试查询吧！"),//查询自己_玩家BP未满
        BPS_Me_Banned("哼哼，你号没了"),//查询自己_玩家被封禁
        BPS_Me_Blacklisted("本 Bot 根本不想理你"),//查询自己_玩家黑名单
        BPS_Player_NoBind("他还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"),//查询他人_玩家未绑定
        BPS_Player_NoAuthorization("他撤销了授权呢，请提醒他输入 !bind 点击链接登录，重新授权吧"),//查询他人_玩家撤销授权
        BPS_Player_NoData("你查的不会是我的机器人同类吧！"),//查询他人_玩家无数据
        BPS_Player_NotFound("这是谁呀，小沐不认识哦"),//查询他人_未搜到玩家
        BPS_Player_BPNotFill("快刷满 BP100 再尝试查询吧！"),//查询他人_玩家BP未满
        BPS_Player_Banned("哼哼，他号没了"),//查询他人_玩家被封禁
        BPS_Player_Blacklisted("我不想和他一起玩！"),//查询他人_玩家黑名单
        BPS_Default_NoToken("哼，你 Token 失效啦！看在我们关系的份上，就帮你这一次吧！"),//token不存在，使用本机AccessToken
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