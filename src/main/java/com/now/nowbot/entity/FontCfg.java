package com.now.nowbot.entity;

import com.now.nowbot.config.NowbotConfig;
import org.jetbrains.skija.Typeface;

public class FontCfg {
    public static Typeface JP;
    public static Typeface TORUS_REGULAR;
    public static void init(){
        if (JP == null || TORUS_REGULAR == null) {
            JP = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "font.otf");
            TORUS_REGULAR = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Torus-Regular.ttf");
        }
    }
    public static Typeface getTorusRegular(){
        if(TORUS_REGULAR == null || TORUS_REGULAR.isClosed()){
            TORUS_REGULAR = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Torus-Regular.ttf");
        }
        return TORUS_REGULAR;
    }
}