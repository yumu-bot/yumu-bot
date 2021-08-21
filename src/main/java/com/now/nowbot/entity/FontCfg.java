package com.now.nowbot.entity;

import com.now.nowbot.config.NowbotConfig;
import org.jetbrains.skija.Typeface;

public class FontCfg {
    public static Typeface JP;
    public static Typeface TORUS_REGULAR;
    public static void init(){
        JP = Typeface.makeFromFile(NowbotConfig.FONT_PATH +"font.otf");
        TORUS_REGULAR = Typeface.makeFromFile(NowbotConfig.FONT_PATH +"Torus-Regular.ttf");
    }
}
