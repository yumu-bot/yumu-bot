package com.now.nowbot.entity;

import com.now.nowbot.config.NowbotConfig;
import org.jetbrains.skija.Typeface;

public class FontCfg {
    public static Typeface face;
    public static void init(){
        face = Typeface.makeFromFile(NowbotConfig.FONT_PATH +"font.otf");
    }
}
