package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;

import java.io.IOException;

public class PPMVSPanelBuilder extends PPPanelBuilder{
    /**banner*/
    public PPMVSPanelBuilder drawBanner(Image bg){
        super.drawBanner(bg);
        return this;
    }
    /**叠加层*/
    public PPMVSPanelBuilder drawOverImage(){
        try {
            drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/panel-ppmodule.png"));
        } catch (IOException e) {
            log.error("PPMPanelBuilder->ppm 叠加层素材加载失败", e);
        }
        return this;
    }
    public PPMVSPanelBuilder drawValueName(){

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
