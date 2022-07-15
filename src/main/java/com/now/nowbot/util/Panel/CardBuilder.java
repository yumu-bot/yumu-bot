package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;

public class CardBuilder extends PanelBuilder{
    CardBuilder(Image bg) {
        super(430, 210, bg);
    }

    public CardBuilder drawB1(String text) {
        return drawB(text, 177);
    }

    public CardBuilder drawB2(String text) {
        return drawB(text, 151);
    }

    public CardBuilder drawB3(String text) {
        return drawB(text, 125);
    }

    CardBuilder drawB(String text, int y) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        Font font = new Font(typeface, 24)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(text, font);
        try (font; line) {
            canvas.translate(20, y);
            canvas.drawTextLine(line, 0, line.getCapHeight(), p_white);
        }
        canvas.restore();
        return this;
    }

    public CardBuilder drawC1(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        Font font = new Font(typeface, 60)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(text, font);
        try (font; line) {
            canvas.translate(surface.getWidth() - 10, 153);
            canvas.drawTextLine(line, -line.getWidth(), line.getCapHeight(), p_white);
        }
        canvas.restore();
        return this;
    }

    public CardBuilder drawC2(String text) {
        return drawC(text, surface.getWidth() - 10, 126);
    }

    public CardBuilder drawC3(String text) {
        return drawC(text, surface.getWidth() - 10, 99);
    }

    /***
     * 小字标识
     * @param text
     * @return
     */
    public CardBuilder drawC4(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        Font font = new Font(typeface, 24)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(text, font);
        try (font; line) {
            canvas.translate(215, 152);
            canvas.drawTextLine(line, 0, line.getCapHeight(), p_white);
        }
        canvas.restore();
        return this;
    }

    CardBuilder drawC(String text, int x, int y) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        Font font = new Font(typeface, 24)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(text, font);
        try (font; line) {
            canvas.translate(x, y);
            canvas.drawTextLine(line, -line.getWidth(), line.getCapHeight(), p_white);
        }
        canvas.restore();
        return this;
    }

    public Image build() {
        return build(20);
    }

    public static ACardBuilder getUserCard(OsuUser user) throws IOException {
        Image uBg = PanelUtil.getBgUrl("用户自定义路径", user.getCoverUrl(), true);
        var card = PanelUtil.getA1Builder(uBg)
                .drawA1(user.getAvatarUrl())
                .drawA2(PanelUtil.getFlag(user.getCountry().countryCode()))
                .drawA3(user.getUsername());
        card.drawB2("#" + user.getStatistics().getGlobalRank())
                .drawB1(user.getCountry().countryCode() + "#" + user.getStatistics().getCountryRank())
                .drawC2(String.format("%.2f",user.getStatistics().getAccuracy()) + "% Lv." +
                        user.getStatistics().getLevelCurrent() +
                        "(" + user.getStatistics().getLevelProgress() + "%)")
                .drawC1(user.getStatistics().getPP().intValue() + "PP");
        if (user.getSupportLeve()>0) {
            card.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }
        return card;
    }
}
