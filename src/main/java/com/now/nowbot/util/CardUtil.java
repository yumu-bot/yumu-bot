package com.now.nowbot.util;

import org.jetbrains.skija.*;

public class CardUtil {
    public static Image Card1(){
        Image img = null;
        try (Surface surface = Surface.makeRasterN32Premul(430,210);
        ){
            Canvas canvas = surface.getCanvas();

            img = RRectout(surface, 20);
        }
        return img;
    }
    public static Image Card2(){
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

    public static void drowInfo$A_MAX(Canvas canvas, String str){
        canvas.save();
        canvas.translate(130, 20);
        Typeface typeface = Typeface.makeFromFile("D:\\Documents\\Tencent Files\\2480557535\\FileRecv\\Torus-Regular.ttf");
        Font font = new Font(typeface,48)
                .setEmboldened(true)
                .setScaleX(1.02f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);

        var line = TextLine.make(str ,font);
        canvas.drawTextLine(line,1.5f,3.5f+line.getCapHeight(),new Paint().setARGB(255,255,255,255).setStrokeWidth(10));
        canvas.restore();
    }
    public static void drowInfo$A1(Canvas canvas, String str){
        canvas.save();
        canvas.translate(20, 20);
        Typeface typeface = Typeface.makeFromFile("F:\\项目素材\\nowbot\\data\\font\\Puhuiti.ttf");
        Font font = new Font(typeface,36).setEmboldened(true)
                .setScaleX(1.011f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(str ,font);
        canvas.drawTextLine(line,0,28,new Paint().setARGB(255,255,255,255).setStrokeWidth(10));
        canvas.restore();
    }
    public static void drowInfo$A2(Canvas canvas, String str){
        canvas.save();
        canvas.translate(20, 55);
        Typeface typeface = Typeface.makeFromFile("F:\\项目素材\\nowbot\\data\\font\\Puhuiti.ttf");
        Font font = new Font(typeface,24).setEmboldened(false)
                .setScaleX(1.022f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(str ,font);
//        var text = new TextBlobBuilder().appendRun(font,str,0,0).build();
//        canvas.drawTextBlob(text,0,23,new Paint().setARGB(255,0,0,0).setStrokeWidth(10));
        canvas.drawTextLine(line,0,23,new Paint().setARGB(255,255,255,255).setStrokeWidth(10));
        canvas.restore();
    }
    public static void drowInfo$A3(Canvas canvas, String str){
        canvas.save();
        canvas.translate(20, 85);
        Typeface typeface = Typeface.makeFromFile("F:\\项目素材\\nowbot\\data\\font\\Puhuiti.ttf");
        Font font = new Font(typeface,24).setEmboldened(false)
                .setScaleX(1.022f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(str ,font);
        canvas.drawTextLine(line,0,23,new Paint().setARGB(255,255,255,255).setStrokeWidth(10));
        canvas.restore();
    }
    public static void drowInfo$B1(Canvas canvas, String str){
        canvas.save();
        canvas.translate(20, 120);
        Typeface typeface = Typeface.makeFromFile("F:\\项目素材\\nowbot\\data\\font\\Torus-Regular.ttf");
        Font font = new Font(typeface,24).setEmboldened(true)
                .setScaleX(1.022f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(str ,font);
        canvas.drawTextLine(line,0,22,new Paint().setARGB(255,255,255,255));
        canvas.restore();
    }
    public static void drowInfo$B2(Canvas canvas, String str){
        canvas.save();
        canvas.translate(20, 145);
        Typeface typeface = Typeface.makeFromFile("F:\\项目素材\\nowbot\\data\\font\\Torus-Regular.ttf");
        Font font = new Font(typeface,24).setEmboldened(true)
                .setScaleX(1.022f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(str ,font);
        canvas.drawTextLine(line,0,23,new Paint().setARGB(255,255,255,255));
        canvas.restore();
    }
    public static void drowInfo$B3(Canvas canvas, String str){
        canvas.save();
        canvas.translate(20, 170);
        Typeface typeface = Typeface.makeFromFile("F:\\项目素材\\nowbot\\data\\font\\Torus-Regular.ttf");
        Font font = new Font(typeface,24).setEmboldened(true)
                .setScaleX(1.022f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(str ,font);
        canvas.drawTextLine(line,0,23,new Paint().setARGB(255,255,255,255));
        canvas.restore();
    }
    public static void drowInfo$C1(Canvas canvas, String str){
        canvas.save();
        canvas.translate(420, 145);
        Typeface typeface = Typeface.makeFromFile("D:\\Documents\\Tencent Files\\2480557535\\FileRecv\\Torus-Regular.ttf");
        Font font = new Font(typeface,60).setEmboldened(true)
                .setScaleX(1.022f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(str ,font);
        canvas.drawTextLine(line,-line.getWidth(),49,new Paint().setARGB(255,255,255,255));
        canvas.restore();
    }
    public static void drowInfo$C2(Canvas canvas, String str){
        canvas.save();
        canvas.translate(420, 120);
        Typeface typeface = Typeface.makeFromFile("F:\\项目素材\\nowbot\\data\\font\\Torus-Regular.ttf");
        Font font = new Font(typeface,24).setEmboldened(true)
                .setScaleX(1.022f)
                .setHinting(FontHinting.NONE)
                .setBaselineSnapped(true);
        var line = TextLine.make(str ,font);
        canvas.drawTextLine(line,-line.getWidth(),22,new Paint().setARGB(255,255,255,255));
        canvas.restore();
    }
    public static void drowHead(Canvas canvas, Image head){
        canvas.save();
        canvas.translate(20,20);
        canvas.clipRRect(RRect.makeXYWH(0,0,100,100,10));
        canvas.drawImage(SkiaUtil.getScaleCenterImage(head, 100, 100),0,0);
        canvas.restore();
    }
}
