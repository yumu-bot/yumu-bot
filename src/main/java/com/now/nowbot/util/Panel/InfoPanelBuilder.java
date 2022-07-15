package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;

import java.io.IOException;
import java.util.List;

public class InfoPanelBuilder extends PanelBuilder{
    private static final Paint c2 = new Paint().setARGB(255,42,34,38);

    public InfoPanelBuilder () {
        super(1920, 1080);
    }

    public InfoPanelBuilder drawBanner(Image banner){
        drawImage(banner);
        return this;
    }

    public InfoPanelBuilder mainCard(Image image){
        canvas.drawImage(image, 40, 40);
        return this;
    }

    public Image build(OsuUser user, List<BpInfo> bps) throws IOException {
        drawName("Info");
        var card = CardBuilder.getUserCard(user).build();

        var j1 = new J1CardBuilder(user).build();
        var j2 = new J2CardBuilder(user).build();
        var j3 = new J3CardBuilder(user).build();
        var j4 = new J4CardBuilder(user).build();
        var j5 = new J5CardBuilder(user).build();
        var j6 = new J6CardBuilder(bps).build();

        try (card;j1;j2;j3;j4;j5;j6){
            mainCard(card);
        }
        return super.build(20, " Info Panel v3.2 Enhanced");
    }
}
