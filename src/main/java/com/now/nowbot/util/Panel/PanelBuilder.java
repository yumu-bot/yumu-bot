package com.now.nowbot.util.Panel;

import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PanelBuilder {
    final Paint p_white = new Paint().setARGB(255, 255, 255, 255).setStrokeWidth(10);
    final float TopTipeFontSize = 24;
    protected int width;
    protected int hight;
    protected final Surface surface;
    protected Canvas canvas;
    protected boolean isClose = false;
    protected Image outImage;

    PanelBuilder(int w, int h) {
        width = w;
        hight = h;
        surface = Surface.makeRasterN32Premul(w, h);
        canvas = surface.getCanvas();
//            canvas.clear(Color.makeARGB(100, 0, 0, 0));
    }

    PanelBuilder(int w, int h, Image bg) {
        width = w;
        hight = h;
        surface = Surface.makeRasterN32Premul(w, h);
        canvas = surface.getCanvas();
        if (bg.getWidth() != w || bg.getHeight() != h) bg = SkiaUtil.getScaleCenterImage(bg, w, h);
        canvas.drawImage(bg, 0, 0);
    }

    /**
     * 绘制叠加层
     * @param add
     * @return
     */
    PanelBuilder drawImage(Image add){
        canvas.drawImage(add,0,0);
        return this;
    }

    /**
     * 指定位置
     * @param add
     * @param x
     * @param y
     * @return
     */
    PanelBuilder drawImage(Image add, int x, int y){
        canvas.save();
        canvas.translate(x, y);
        canvas.drawImage(add, 0, 0);
        canvas.restore();
        return this;
    }
    Image build(int r) {
        if (isClose) return outImage;
        try (surface) {
            isClose = true;
            outImage = RRectout(surface, r);
            return outImage;
        }
    }

    Image build(int r, String text) {
        if (isClose) return outImage;
        drawPanelInfo(surface, r, text);
        return build(r);
    }

    void drawPanelInfo(Surface surface, int r, String text){
        Canvas canvas = surface.getCanvas();
        String leftText = "powered by Yumubot" + text;
        Font font = new Font(SkiaUtil.getTorusRegular(), TopTipeFontSize);
        TextLine leftLine = TextLine.make(leftText, font);
        TextLine rightLine = TextLine.make(DateTimeFormatter.ofPattern("'time: 'yyyy-MM-dd HH:mm:ss' UTC-8'").format(LocalDateTime.now()), font);
        Paint p = new Paint().setARGB(100, 0, 0, 0);
        try (font; leftLine; rightLine;p) {
//                    canvas.drawRRect(RRect.makeXYWH(0, 0, leftLine.getWidth() + 2*r, leftLine.getHeight(), r), p);
            canvas.drawTextLine(leftLine, r, leftLine.getCapHeight()+0.2f* TopTipeFontSize, p_white);
//                    canvas.drawRRect(RRect.makeXYWH(surface.getWidth() - rightLine.getWidth() - 2*r, 0, rightLine.getWidth() + r, leftLine.getHeight(), r), p);
            canvas.drawTextLine(rightLine, surface.getWidth() - r - rightLine.getWidth(), rightLine.getCapHeight()+0.2f* TopTipeFontSize, p_white);
        }
    }

    public boolean isClose() {
        return isClose;
    }

    static Image RRectout(Surface surface, int r) {
        var img = surface.makeImageSnapshot();
        var canvas = surface.getCanvas();
        canvas.clear(0);
        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(0, 0, surface.getWidth(), surface.getHeight(), r));
        canvas.drawImage(img, 0, 0, new Paint().setAntiAlias(true).setMode(PaintMode.FILL));
        canvas.restore();
        var rImg = surface.makeImageSnapshot();
        canvas.clear(0);
        canvas.drawImage(img, 0, 0);
        return rImg;
    }
}
