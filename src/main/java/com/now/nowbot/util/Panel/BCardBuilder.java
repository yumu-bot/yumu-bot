package com.now.nowbot.util.Panel;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCardBuilder extends CardBuilder{
    static private Logger log = LoggerFactory.getLogger(BCardBuilder.class);
    private static int MODE_OFFSET = 8;

    int modeOffset = 0;
    boolean down = true;
    public BCardBuilder(Image bg) {
        super(bg);
    }
    /***
     * 铺面状态 只能调用一次
     * @param flag
     * @return
     */
    public BCardBuilder drawD5(Image flag) {
        if (down) {
            down = false;
            canvas.save();
            canvas.translate(370, 10);
            canvas.drawImage(flag, 0, 0);
            canvas.restore();
        }
        return this;
    }

    /***
     * 曲名
     * @param text
     * @return
     */
    public BCardBuilder drawD4(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getPUHUITIMedium();
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

    public BCardBuilder drawD3(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getPUHUITIMedium();
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


    public BCardBuilder drawD2(String mode, int color) {
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
            canvas.translate(20 + modeOffset, 94);
            canvas.drawTextLine(line, 0, line.getCapHeight(), new Paint().setColor(color));
            modeOffset += line.getWidth() + MODE_OFFSET;
        }
        canvas.restore();
        return this;
    }
    /***
     * 
     * @param mode mod
     * @param color 颜色
     * @return
     */
    public BCardBuilder drawD2(OsuMode mode, int color) {
        switch (mode){
            case OSU -> drawD2(PanelUtil.MODE_OSU, color);
            case CATCH -> drawD2(PanelUtil.MODE_CATCH, color);
            case TAIKO -> drawD2(PanelUtil.MODE_TAIKO, color);
            case MANIA -> drawD2(PanelUtil.MODE_MANIA, color);
            default -> { throw new TipsRuntimeException("mode错误");}
        }
        return this;
    }

    public BCardBuilder drawD1(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getPUHUITIMedium();
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
