package com.now.nowbot.util.Panel;

import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;
import org.jetbrains.skija.svg.SVGDOM;

import java.io.IOException;

public class ACardBuilder extends CardBuilder{
    private static int LOG_OFFSET = 10;

    int logOffset = 0;
    public ACardBuilder(Image bg){
        super(bg);
    }

    /***
     * 头像
     * @param head_url
     * @return
     */
    public ACardBuilder drawA1(String head_url) throws IOException {
        canvas.save();
        final Image head = SkiaImageUtil.getImage(head_url);
        try (head) {
            canvas.translate(20, 20);
            canvas.clipRRect(RRect.makeXYWH(0, 0, 100, 100, 10));
            canvas.drawImage(SkiaImageUtil.getScaleCenterImage(head, 100, 100), 0, 0);
        }
        canvas.restore();
        return this;
    }

    /***
     * 撒泼特,好友状态
     * @return
     */
    public ACardBuilder drawA2(Image... loge) {
        for (var i : loge){
            canvas.save();
            canvas.translate(130 + logOffset, 70);
            canvas.drawImage(i,0,0);
            logOffset += i.getWidth() + LOG_OFFSET;
            canvas.restore();
        }
        return this;
    }
    /**
     * 国旗单独绘制
     */
    public ACardBuilder drawA2(SVGDOM svg){
        canvas.save();
        canvas.translate(130 + logOffset, 62);
        SkiaUtil.drawSvg(canvas,svg,0,0,60,60);
        logOffset += 60 + LOG_OFFSET;
        canvas.restore();
        return this;
    }

    /***
     * 名字
     * @param text
     * @return
     */
    public ACardBuilder drawA3(String text) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        final Font font = new Font(typeface, 48)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(text, font);
        int i = 0;
        i = line.getLeftOffsetAtCoord(285);
        final TextLine lineX;
        if (i == text.length()){
            lineX = line;
        }else {
            lineX = TextLine.make(text.substring(0,i-1)+"...",font);
        }
        try (font; lineX) {
            canvas.translate(130, 23);
            canvas.drawTextLine(lineX, 0, lineX.getCapHeight(), p_white);
        }
        canvas.restore();
        return this;
    }
}
