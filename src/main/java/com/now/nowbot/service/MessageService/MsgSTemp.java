package com.now.nowbot.service.MessageService;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class MsgSTemp {
    public static final Map<Pattern, String> services = new HashMap<>();
    String name;
    String info = null;
    MsgSTemp(Pattern pattern, String name){
        /***
         * 开头匹配 ^
         * 结尾匹配 $
         * [!！]//s*(?i)page         //(?i) 不区分大小写  (?![字符]) 不匹配某字符
         * osuname匹配  (?<name>[0-9a-zA-Z\[\]\-_ ]*)?
         */
        this.name = name;
        services.put(pattern,name);
    }
    public void setInfo(String s){
        info = s;
    }
    public String getInfo(){
        return info;
    }
    public String getName(){
        return name;
    }
}
