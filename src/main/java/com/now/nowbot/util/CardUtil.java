package com.now.nowbot.util;

import org.jetbrains.skija.*;

public class CardUtil {
    public static Image CardUser1(){
        Image img = null;
        try (Surface surface = Surface.makeRasterN32Premul(430,210);
        ){
            Canvas canvas = surface.getCanvas();

            img = RRectout(surface, 20);
        }
        return img;
    }
    public static Image RRectout(Surface surface, int r){
        var img = surface.makeImageSnapshot();
        var canvas = surface.getCanvas();
        canvas.clear(0);
        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(0,0, surface.getWidth(), surface.getHeight(),r));
        canvas.drawImage(img, 0, 0, new Paint().setAntiAlias(true).setMode(PaintMode.FILL));
        canvas.restore();
        var rImg = surface.makeImageSnapshot();
        canvas.clear(0);
        canvas.drawImage(img,0,0);
        return rImg;
    }

    public static void drowHead(Canvas canvas, Image head){
        canvas.save();
        canvas.translate(20,20);
        canvas.clipRRect(RRect.makeXYWH(0,0,100,100,10));
        canvas.drawImage(SkiaUtil.getScaleCenterImage(head, 100, 100),0,0);
        canvas.restore();
    }
}
