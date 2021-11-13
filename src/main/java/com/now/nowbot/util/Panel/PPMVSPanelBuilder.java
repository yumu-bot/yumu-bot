package com.now.nowbot.util.Panel;

import org.jetbrains.skija.Paint;

public class PPMVSPanelBuilder extends PPPanelBuilder{
    public PPMVSPanelBuilder drawLeftValueName(){
        var p = new Paint().setARGB(255,161,161,161);
        drawLeftNameN(0, "FAC",null, p);
        drawLeftNameN(1, "PTT",null, p);
        drawLeftNameN(2, "STA",null, p);
        drawLeftNameN(3, "STB",null, p);
        drawLeftNameN(4, "ENG",null, p);
        drawLeftNameN(5, "STH",null, p);
        drawLeftTotleName("Overall.B");
        drawRightTotleName("Overall.R");
        return this;
    }
}
