package com.now.nowbot.util.Panel;

import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;

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

    public Image build() {
        drawName("Info");
        return super.build(20, " Info Panel v3.2 Enhanced");
    }
}
