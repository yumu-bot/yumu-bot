package com.now.nowbot.util.Panel;


import com.now.nowbot.model.enums.OsuMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

import java.util.List;

public class TBPPanelBuilder extends PanelBuilder{
    private static final Paint c2 = new Paint().setARGB(255,42,34,38);
    public TBPPanelBuilder(int size){
        super(1920, 330 + 150 * Math.round(size/2f));
    }



    public TBPPanelBuilder drawBanner(Image banner){
        drawImage(banner);
        return this;
    }
    public TBPPanelBuilder mainCrawCard(Image main){
        drawImage(main, 40, 40);
        return this;
    }

    public TBPPanelBuilder drawBp(List<Image> hCards){
        canvas.drawRRect(RRect.makeXYWH(0,290,1920,150 * Math.round(hCards.size()/2f) + 80, 30), c2);
        int evenNum = (hCards.size()%2 == 0) ? hCards.size() : hCards.size()-1;
        canvas.save();
        canvas.translate(40, 180);
        for (int i = 0; i < evenNum; i+=2) {
            canvas.translate(0,150);
            canvas.drawImage(hCards.get(i), 0, 0);
        }
        canvas.restore();
        canvas.save();
        canvas.translate(980, 180);
        for (int i = 1; i < evenNum; i+=2) {
            canvas.translate(0,150);
            canvas.drawImage(hCards.get(i), 0, 0);
        }
        canvas.restore();
        if (evenNum < hCards.size()){
            canvas.drawImage(hCards.get(evenNum),510,330+75*evenNum);
        }

        return this;
    }

    public Image build(OsuMode mode) {
        String modeStr;
        switch (mode){
            case OSU -> modeStr = ":O";
            case TAIKO -> modeStr = ":T";
            case CATCH -> modeStr = ":C";
            case MANIA -> modeStr = ":M";
            default -> modeStr =  "";
        }
        drawName("TBP"+modeStr);
        return super.build(20, "v0.2.0 Debug // TodayBP (!tbp)");
    }
}
