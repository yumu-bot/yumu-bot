package com.now.nowbot.service.MessageService;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class MsgSTemp {
    public static final Map<Pattern, String> services = new HashMap<>();
    String info = null;
    MsgSTemp(Pattern pattern, String name){
        /***
         * name  (?<name>[0-9a-zA-Z\[\]\-_ ]*)?
         */
        services.put(pattern,name);
    }
    public void setInfo(String s){
        info = s;
    }
    public String getInfo(){
        return info;
    }
}
