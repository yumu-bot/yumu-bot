package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.util.SkiaImageUtil;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;

import java.io.IOException;

public class PPPlusPanelBuilder extends PPPanelBuilder{
    /**banner*/
    public PPPlusPanelBuilder drawBanner(Image bg){
        super.drawBanner(bg);
        return this;
    }
    /**叠加层*/
    public PPPlusPanelBuilder drawOverImage(){
        try {
            drawImage(SkiaImageUtil.getImage(NowbotConfig.BG_PATH+"ExportFileV3/panel-ppmodule.png"));
        } catch (IOException e) {
            log.error("PPMPanelBuilder->ppm 叠加层素材加载失败", e);
        }
        return this;
    }
    /**名字*/
    public PPPlusPanelBuilder drawValueName(){
        var p = new Paint().setARGB(255,161,161,161);
        drawLeftNameN(0, "Jump",null, p);
        drawLeftNameN(1, "Flow",null, p);
        drawLeftNameN(2, "Acc",null, p);
        drawLeftNameN(3, "Sta",null, p);
        drawLeftNameN(4, "Spd",null, p);
        drawLeftNameN(5, "Pre",null, p);
        drawLeftTitleName("Total");
        drawRightTitleName("PP");
        return this;
    }
    /***
     * 左侧card
     * @param card
     * @return
     */
    @Override
    public PPPlusPanelBuilder drawLeftCard(Image card) {
        super.drawLeftCard(card);
        return this;
    }

    public PPPlusPanelBuilder drawLeftValueN(int n, String bigText, String simText){
        super.drawLeftValueN(n, bigText, simText);
        return this;
    }

    @Override
    public Image build() {
        drawPanelName("PP+");
        return super.build();
    }
}
