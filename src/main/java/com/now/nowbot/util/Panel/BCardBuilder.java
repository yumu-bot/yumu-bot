package com.now.nowbot.util.Panel;

import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCardBuilder extends CardBuilder{
    static private Logger log = LoggerFactory.getLogger(BCardBuilder.class);
    public BCardBuilder(Image bg) {
        super(bg);
    }
    /***
     * 铺面状态
     * @param flag
     * @return
     */
    public BCardBuilder drowD5(Object flag) {
        canvas.save();
        //todo 没有图标。。。
        canvas.restore();
        return this;
    }

    /***
     * 曲名
     * @param text
     * @return
     */
    public BCardBuilder drowD4(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getPuhuitiMedium();
        final Font font = new Font(typeface, 36)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        final var line = TextLine.make(text, font);
        try (font; line) {
            canvas.translate(20, 24);
            canvas.drawTextLine(line, 0, line.getCapHeight(), p_white);
        }
        canvas.restore();
        return this;
    }

    public BCardBuilder drowD3(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getPuhuitiMedium();
        final Font font = new Font(typeface, 24)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        final var line = TextLine.make(text, font);
        try (font; line) {
            canvas.translate(20, 62);
            canvas.drawTextLine(line, 0, line.getCapHeight(), p_white);
        }
        canvas.restore();
        return this;
    }

    /***
     *      到时候写个enum?
     * @param mode mod
     * @param color 颜色
     * @return
     */
    public BCardBuilder drowD2(String mode, int color) {
        canvas.save();
        Typeface typeface = null;
        try {
            typeface = SkiaUtil.getEXTRA();
        } catch (Exception e) {
            log.error("字体加载异常", e);
            return this;
        }
        final Font font = new Font(typeface, 24)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        final var line = TextLine.make(mode, font);
        try (font; line) {
            canvas.translate(20, 94);
            canvas.drawTextLine(line, 0, line.getCapHeight(), new Paint().setColor(color));
        }
        canvas.restore();
        return this;
    }

    public BCardBuilder drowD1(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getPuhuitiMedium();
        final Font font = new Font(typeface, 24)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        final var line = TextLine.make(text, font);
        try (font; line) {
            canvas.translate(45, 94);
            canvas.drawTextLine(line, 0, line.getCapHeight(), p_white);
        }
        canvas.restore();
        return this;
    }
}
