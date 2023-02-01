package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import org.jetbrains.skija.Color;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;

import java.io.IOException;

public class ScorePanelBuilder extends PanelBuilder{
    private static final Paint c2 = new Paint().setARGB(255,42,34,38);

    public ScorePanelBuilder () {
        super(1920, 1080);
        canvas.clear(Color.makeRGB(42,34,38));
    }

    public ScorePanelBuilder drawBanner(Image banner){
        drawImage(banner);
        return this;
    }

    public ScorePanelBuilder mainCard(Image image){
        canvas.drawImage(image, 40, 40);
        return this;
    }

    public Image build(OsuUser user, Score score) throws IOException {
        drawName("Score");

        var card = CardBuilder.getUserCard(user).build();

        var k0 = new K0CardBuilder(score).build();
        var k1 = new K1CardBuilder(score).build();
        var k2 = new K2CardBuilder(score).build();
        var k3 = new K3CardBuilder(score).build();

        try (card;k0;k1;k2;k3){
            mainCard(card);
            canvas.drawImage(k0, 0, 290);// 底层 Rank 图片显示
            canvas.drawImage(k1, 0, 290);// 这张卡片的左上角在可绘制部分的最左上角，没有 40 偏移
            canvas.drawImage(k2, 880, 330);
            canvas.drawImage(k3, 880, 770);
        }
        return super.build(20, "ScorePanel v3.5");
    }
}
