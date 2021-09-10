package com.now.nowbot.model;

import com.now.nowbot.config.NowbotConfig;
import org.jetbrains.skija.Typeface;

//FIXME 大概不应该放在这里
public class SkiaConfig {
    public static Typeface JP;
    public static Typeface TORUS_REGULAR;

    public static Typeface getJP(){
        if(JP == null || JP.isClosed()){
            JP = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "font.otf");
        }
        return JP;
    }
    public static Typeface getTorusRegular(){
        if(TORUS_REGULAR == null || TORUS_REGULAR.isClosed()){
            TORUS_REGULAR = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Torus-Regular.ttf");
        }
        return TORUS_REGULAR;
    }
}