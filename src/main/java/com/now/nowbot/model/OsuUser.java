package com.now.nowbot.model;

import com.now.nowbot.entity.OsuUserLite;
import com.now.nowbot.entity.OsuUserModeLite;

public class OsuUser {
    OsuUserLite user;
    OsuUserModeLite play;
    public OsuUser(){}
    public OsuUser(OsuUserLite user){
        this.user = user;
    }

}
