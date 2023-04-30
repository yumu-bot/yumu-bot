package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaImageUtil;
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
            drawImage(SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/panel-ppmodule.png"));
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
        drawLeftTitleName("Overall.B");
        drawRightTitleName("Overall.R");
        return this;
    }
    public PPMVSPanelBuilder switchLeftRank(int i, double date){
        if (date==1.20){
            drawLeftRankN(i, "X+", PanelUtil.COLOR_X_PLUS);
        }
        else if(date>=1.00){
            drawLeftRankN(i, "SS", PanelUtil.COLOR_SS);
        }
        else if(date>=0.95){
            drawLeftRankN(i, "S+", PanelUtil.COLOR_S_PLUS);
        }
        else if(date>=0.90){
            drawLeftRankN(i, "S", PanelUtil.COLOR_S);
        }
        else if(date>=0.85){
            drawLeftRankN(i, "A+", PanelUtil.COLOR_A_PLUS);
        }
        else if(date>=0.80){
            drawLeftRankN(i, "A", PanelUtil.COLOR_A);
        }
        else if(date>=0.70){
            drawLeftRankN(i, "B", PanelUtil.COLOR_B);
        }
        else if(date>=0.60){
            drawLeftRankN(i, "C", PanelUtil.COLOR_C);
        }
        else if(date>0){
            drawLeftRankN(i, "D", PanelUtil.COLOR_D);
        }
        else {
            drawLeftRankN(i, "F", PanelUtil.COLOR_F);
        }
        return this;
    }
    public PPMVSPanelBuilder switchRightRank(int i, double date){
        if (date==1.20){
            drawRightRankN(i, "X+", PanelUtil.COLOR_X_PLUS);
        }
        else if(date>=1.00){
            drawRightRankN(i, "SS", PanelUtil.COLOR_SS);
        }
        else if(date>=0.95){
            drawRightRankN(i, "S+", PanelUtil.COLOR_S_PLUS);
        }
        else if(date>=0.90){
            drawRightRankN(i, "S", PanelUtil.COLOR_S);
        }
        else if(date>=0.85){
            drawRightRankN(i, "A+", PanelUtil.COLOR_A_PLUS);
        }
        else if(date>=0.80){
            drawRightRankN(i, "A", PanelUtil.COLOR_A);
        }
        else if(date>=0.70){
            drawRightRankN(i, "B", PanelUtil.COLOR_B);
        }
        else if(date>=0.60){
            drawRightRankN(i, "C", PanelUtil.COLOR_C);
        }
        else if(date>0){
            drawRightRankN(i, "D", PanelUtil.COLOR_D);
        }
        else {
            drawRightRankN(i, "F", PanelUtil.COLOR_F);
        }
        return this;
    }

    @Override
    public Image build() {
        return super.build("PPMinus Panel v3.0");
    }
}
