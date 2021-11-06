package com.now.nowbot.util.Panel;

import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

public class CardBuilder extends PanelBuilder{
    CardBuilder(Image bg) {
        super(430, 210, bg);
    }

    public CardBuilder drowB1(String text) {
        return drowB(text, 177);
    }

    public CardBuilder drowB2(String text) {
        return drowB(text, 151);
    }

    public CardBuilder drowB3(String text) {
        return drowB(text, 125);
    }

    CardBuilder drowB(String text, int y) {
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

    public CardBuilder drowC1(String text) {
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

    public CardBuilder drowC2(String text) {
        return drowC(text, surface.getWidth() - 10, 126);
    }

    public CardBuilder drowC3(String text) {
        return drowC(text, surface.getWidth() - 10, 99);
    }

    /***
     * 小字标识
     * @param text
     * @return
     */
    public CardBuilder drowC4(String text) {
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

    CardBuilder drowC(String text, int x, int y) {
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
}
