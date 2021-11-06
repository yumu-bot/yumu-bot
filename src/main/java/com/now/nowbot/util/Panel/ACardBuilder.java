package com.now.nowbot.util.Panel;

import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

public class ACardBuilder extends CardBuilder{
    public ACardBuilder(Image bg){
        super(bg);
    }
    /***
     * 头像
     * @param head_url
     * @return
     */
    public ACardBuilder drowA1(String head_url) {
        canvas.save();
        final Image head = SkiaUtil.lodeNetWorkImage(head_url);
        try (head) {
            canvas.translate(20, 20);
            canvas.clipRRect(RRect.makeXYWH(0, 0, 100, 100, 10));
            canvas.drawImage(SkiaUtil.getScaleCenterImage(head, 100, 100), 0, 0);
        }
        canvas.restore();
        return this;
    }

    /***
     * 国旗,撒泼特,好友状态
     * @return
     */
    public ACardBuilder drowA2(Object... loge) {
        //todo
        return this;
    }

    /***
     * 名字
     * @param text
     * @return
     */
    public ACardBuilder drowA3(String text) {
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
