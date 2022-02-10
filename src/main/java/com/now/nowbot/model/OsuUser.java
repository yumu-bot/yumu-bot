package com.now.nowbot.model;

import com.now.nowbot.entity.OsuUserLite;
import com.now.nowbot.entity.OsuUserModeScoreLite;
import com.now.nowbot.model.enums.OsuMode;

import java.util.List;


public class OsuUser {
//    获得玩家信息,包含:个人基础信息,某模式的信息,注意区分四个模式
    OsuUserLite user;
    OsuMode mode;
    OsuUserModeScoreLite play;
    public OsuUser(){}

    /***
     * 拿到bp列表,这个可以放在OsuGet里,让他返回一个列表包含成绩就行 这个可以写成取得时候再去数据库里查,然后需要一个最新数据
     * @return
     */
    List<ScoreOsu> getBP(){
        return null;
    }
}
