package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import org.jetbrains.skija.Color;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;

import java.io.IOException;
import java.util.List;

public class InfoLegacyPanelBuilder extends PanelBuilder{
    private static final Paint c2 = new Paint().setARGB(255,42,34,38);

    public InfoLegacyPanelBuilder() {
        super(1920, 1080);
        canvas.clear(Color.makeRGB(42,34,38));
    }

    public InfoLegacyPanelBuilder drawBanner(Image banner){
        drawImage(banner);
        return this;
    }

    public InfoLegacyPanelBuilder mainCard(Image image){
        canvas.drawImage(image, 40, 40);
        return this;
    }

    public Image build(OsuUser user, List<Score> bps) throws IOException {
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
            //canvas.drawImage(j4, 40, 705);
            canvas.drawImage(j5, 510, 705);
            canvas.drawImage(j6, 1450, 705);
        }
        return super.build(20, "v0.2.0 Debug // Info (!testinfo) (v3.2 Enhanced)");
    }
}
