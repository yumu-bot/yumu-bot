package com.now.nowbot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MapPoolUtil {
    private static final Logger log = LoggerFactory.getLogger(MapPoolUtil.class);

    public static boolean check(String name, Object obj){
        try {
            var m = MapPoolUtil.class.getDeclaredMethod(name, Object.class);
            return (boolean) m.invoke(null, obj);
        } catch (Exception e) {
            log.error("MapPoolUtil check error", e);
            return false;
        }
    }

    static boolean a1(Object s){
        System.out.println("a1:"+s);
        return true;
    }
}
