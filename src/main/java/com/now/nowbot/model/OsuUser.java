package com.now.nowbot.model;

import com.now.nowbot.entity.OsuUserLite;
import com.now.nowbot.entity.OsuUserModeLite;

import java.util.List;


public class OsuUser {
//    获得玩家信息,包含:个人基础信息,某模式的信息,注意区分四个模式
    OsuUserLite user;
    OsuUserModeLite play;
    public OsuUser(){}

    /***
     * 拿到bp列表,这个可以放在OsuGet里,让他返回一个列表包含成绩就行
     * @return
     */
    List<ScoreOsu> getBP(){
        return null;
    }
}
