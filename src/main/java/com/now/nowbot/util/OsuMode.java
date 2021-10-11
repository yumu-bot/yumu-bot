package com.now.nowbot.util;
import java.util.*;

public enum OsuMode {

    OSU(null, "osu", "o", "0"),
    TAIKO("taiko", "t", "1"),
    CATCH("catch", "c", "2"),
    MANIA("mania", "m", "3");

    private Set<String> matcher;

    OsuMode(String... desc){
        this.matcher = new HashSet<>(Arrays.asList(desc));
    }

    public static OsuMode getMode(String desc){
        desc=desc.toLowerCase();
        for(var m : OsuMode.values()){
            if(m.matcher.contains(desc)){
                return m;
            }

        }
        return OSU;
    }
}
