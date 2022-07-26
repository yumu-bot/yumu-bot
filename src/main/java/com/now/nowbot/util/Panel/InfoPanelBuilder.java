package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import org.jetbrains.skija.Color;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.RRect;

import java.io.IOException;
import java.util.List;

public class InfoPanelBuilder extends PanelBuilder{
    private static final Paint c2 = new Paint().setARGB(255,42,34,38);

    public InfoPanelBuilder () {
        super(1920, 1080);
        canvas.clear(Color.makeRGB(42,34,38));
    }

    public InfoPanelBuilder drawBanner(Image banner){
        drawImage(banner);
        canvas.drawRRect(RRect.makeXYWH(0, 290, 1920, 40,30, 30, 0, 0), c2);
        return this;
    }

    public InfoPanelBuilder mainCard(Image image){
        canvas.drawImage(image, 40, 40);
        return this;
    }

    public Image build(OsuUser user, List<BpInfo> bps) throws IOException {
        drawName("Info");
        var card = CardBuilder.getUserCard(user).build();

        var j1 = new J1CardBuilder(user, bps).build();
        var j2 = new J2CardBuilder(user).build();
        var j3 = new J3CardBuilder(user).build();
        var j4 = new J4CardBuilder(user).build();
        var j5 = new J5CardBuilder(user).build();
        var j6 = new J6CardBuilder(bps).build();

        try (card;j1;j2;j3;j4;j5;j6){
            mainCard(card);
            canvas.drawImage(j1, 40, 330);
            canvas.drawImage(j2, 510, 330);
            canvas.drawImage(j3, 1450, 330);
            canvas.drawImage(j4, 40, 705);
            canvas.drawImage(j5, 510, 705);
            canvas.drawImage(j6, 1450, 705);
        }
        return super.build(20, " Info Panel v3.2 Enhanced");
    }
}