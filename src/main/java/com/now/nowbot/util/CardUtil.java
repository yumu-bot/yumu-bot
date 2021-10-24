package com.now.nowbot.util;

import org.jetbrains.skija.*;

public class CardUtil {
    public static class cardBuilder {
        int width;
        int hight;
        final Surface surface;
        Canvas canvas;
        boolean isClose = false;
        cardBuilder(int w, int h){
            width = w;
            hight = h;
            surface = Surface.makeRasterN32Premul(w, h);
            canvas = surface.getCanvas();
            canvas.clear(Color.makeARGB(100,0,0,0));
        }

        public Image build(int r) {
            try(surface){
                isClose = true;
                return CardUtil.RRectout(surface,r);
            }
        }
        public boolean isClose(){ return isClose; }
    }
    public static class ABuilder extends cardBuilder {
        final Paint p_white = new Paint().setARGB(255,255,255,255).setStrokeWidth(10);
        ABuilder() {
            super(430, 210);
        }
        public ABuilder drowB1(String text){
            return drowB(text, 177);
        }
        public ABuilder drowB2(String text){
            return drowB(text, 151);
        }
        public ABuilder drowB3(String text){
            return drowB(text, 125);
        }
        ABuilder drowB(String text, int y){
            canvas.save();
            Typeface typeface = SkiaUtil.getTorusSemiBold();
            Font font = new Font(typeface,24)
                    .setHinting(FontHinting.NONE)
                    .setBaselineSnapped(true);
            var line = TextLine.make(text ,font);
            try(font; line) {
                canvas.translate(20, y);
                canvas.drawTextLine(line, 0, line.getCapHeight(), p_white);
            }
            canvas.restore();
            return this;
        }
        public ABuilder drowC1(String text){
            return drowC(text, surface.getWidth() - 10, 153);
        }
        public ABuilder drowC2(String text){
            return drowC(text, surface.getWidth() - 10, 126);
        }
        public ABuilder drowC3(String text){
            return drowC(text, surface.getWidth() - 10, 99);
        }

        /***
         * 小字标识
         * @param text
         * @return
         */
        public ABuilder drowC4(String text){
            canvas.save();
            Typeface typeface = SkiaUtil.getTorusSemiBold();
            Font font = new Font(typeface,24)
                    .setHinting(FontHinting.NONE)
                    .setBaselineSnapped(true);
            var line = TextLine.make(text ,font);
            try(font; line) {
                canvas.translate(215, 152);
                canvas.drawTextLine(line, 0, line.getCapHeight(), p_white);
            }
            canvas.restore();
            return this;
        }
        public ABuilder drowC(String text, int x, int y){
            canvas.save();
            Typeface typeface = SkiaUtil.getTorusSemiBold();
            Font font = new Font(typeface,24)
                    .setHinting(FontHinting.NONE)
                    .setBaselineSnapped(true);
            var line = TextLine.make(text ,font);
            try(font; line) {
                canvas.translate(x, y);
                canvas.drawTextLine(line, -line.getWidth(), line.getCapHeight(), p_white);
            }
            canvas.restore();
            return this;
        }
        public Image build(){
            return build(20);
        }
    }
    public static class A1Builder extends ABuilder{
        /***
         * 头像
         * @param head_url
         * @return
         */
        public A1Builder drowA1(String head_url){
            canvas.save();
            final Image head = SkiaUtil.lodeNetWorkImage(head_url);
            try (head){
                canvas.translate(20,20);
                canvas.clipRRect(RRect.makeXYWH(0,0,100,100,10));
                canvas.drawImage(SkiaUtil.getScaleCenterImage(head, 100, 100),0,0);
            }
            canvas.restore();
            return this;
        }

        /***
         * 国旗,撒泼特,好友状态
         * @return
         */
        public A1Builder drowA2(Object ...loge){
            //todo
            return this;
        }

        /***
         * 名字
         * @param text
         * @return
         */
        public A1Builder drowA3(String text){
            canvas.save();
            Typeface typeface = SkiaUtil.getTorusSemiBold();
            final Font font = new Font(typeface,48)
                    .setHinting(FontHinting.NONE)
                    .setBaselineSnapped(true);
            final var line = TextLine.make(text ,font);
            try(font; line){
                canvas.translate(130, 23);
                canvas.drawTextLine(line,0,line.getCapHeight(), p_white);
            }
            canvas.restore();
            return this;
        }
    }
    public static class A2Builder extends ABuilder{
        /***
         * 铺面状态
         * @param flag
         * @return
         */
        public A2Builder drowD5(Object flag){
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
        public A2Builder drowD4(String text){
            canvas.save();
            Typeface typeface = SkiaUtil.getPuhuitiMedium();
            final Font font = new Font(typeface,36)
                    .setHinting(FontHinting.NONE)
                    .setBaselineSnapped(true);
            final var line = TextLine.make(text ,font);
            try(font; line){
                canvas.translate(20, 24);
                canvas.drawTextLine(line,0,line.getCapHeight(), p_white);
            }
            canvas.restore();
            return this;
        }
        public A2Builder drowD3(String text){
            canvas.save();
            Typeface typeface = SkiaUtil.getPuhuitiMedium();
            final Font font = new Font(typeface,24)
                    .setHinting(FontHinting.NONE)
                    .setBaselineSnapped(true);
            final var line = TextLine.make(text ,font);
            try(font; line){
                canvas.translate(20, 62);
                canvas.drawTextLine(line,0,line.getCapHeight(), p_white);
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
        public A2Builder drowD2(String mode, int color){
            canvas.save();
            Typeface typeface = SkiaUtil.getEXTRA();
            final Font font = new Font(typeface,24)
                    .setHinting(FontHinting.NONE)
                    .setBaselineSnapped(true);
            final var line = TextLine.make(mode ,font);
            try(font; line){
                canvas.translate(20, 94);
                canvas.drawTextLine(line,0,line.getCapHeight(), new Paint().setColor(color));
            }
            canvas.restore();
            return this;
        }
        public A2Builder drowD1(String text){
            canvas.save();
            Typeface typeface = SkiaUtil.getPuhuitiMedium();
            final Font font = new Font(typeface,24)
                    .setHinting(FontHinting.NONE)
                    .setBaselineSnapped(true);
            final var line = TextLine.make(text ,font);
            try(font; line){
                canvas.translate(45, 94);
                canvas.drawTextLine(line,0,line.getCapHeight(), p_white);
            }
            canvas.restore();
            return this;
        }
    }

    /***
     * 玩家面板
     * @return
     */
    public static A1Builder getA1Builder(){
        return new A1Builder();
    }

    /***
     * 谱面/成绩面板
     * @return
     */
    public static A2Builder getA2Builder(){
        return new A2Builder();
    }
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
}