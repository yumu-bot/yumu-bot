package com.now.nowbot.util;

import org.jetbrains.skija.*;
import org.jetbrains.skija.paragraph.*;

public class SkiaCanvasUtil {

    /**
     * 基础方法
     */
    private static void drawText(Canvas canvas, TextLine text, float x, float y, Paint paint) {
        canvas.save();
        canvas.translate(x, y);
        canvas.drawTextLine(text, 0, 0, paint);
        canvas.restore();
    }

    /**
     * 左对齐文字
     *
     * @param text  文字
     * @param font  字体
     * @param paint paint
     */
    public static void drawTextLeft(Canvas canvas, String text, Font font, Paint paint) {
        TextLine textLine = TextLine.make(text, font);
        drawText(canvas, textLine, 0, (textLine.getCapHeight() + 0.1f * font.getSize()), paint);
    }

    /**
     * 左对齐文字
     *
     * @param text  文字
     * @param font  字体
     * @param x     坐标
     * @param y     坐标
     * @param paint paint
     */
    public static void drawTextLeft(Canvas canvas, String text, Font font, int x, int y, Paint paint) {
        TextLine textLine = TextLine.make(text, font);
        drawText(canvas, textLine, x, y + (textLine.getCapHeight() + 0.1f * font.getSize()), paint);
    }

    /**
     * 右对齐文字
     *
     * @param text  文字
     * @param font  字体
     * @param x     坐标
     * @param y     坐标
     * @param paint paint
     */
    public static void drawTextRight(Canvas canvas, String text, Font font, int x, int y, Paint paint) {
        TextLine textLine = TextLine.make(text, font);
        drawText(canvas, textLine, x - textLine.getWidth(), y + (textLine.getCapHeight() + 0.1f * font.getSize()), paint);
    }

    /**
     * 居中对齐文字
     *
     * @param text  文字
     * @param font  字体
     * @param x     坐标
     * @param y     坐标
     * @param paint paint
     */
    public static void drawTextCenter(Canvas canvas, String text, Font font, int x, int y, Paint paint) {
        TextLine textLine = TextLine.make(text, font);
        drawText(canvas, textLine, x - (textLine.getWidth() / 2), y + (textLine.getCapHeight() + 0.1f * font.getSize()), paint);
    }


    /***
     * 给指定区域绘制模糊效果
     * @param canvas 画笔
     * @param x 左上顶点x
     * @param y 左上顶点y
     * @param w 宽度
     * @param h 高度
     * radius 模糊程度 推荐10-20之间
     */
    public static void drawBlur(Canvas canvas, int x, int y, int w, int h) {
        try (Bitmap bitmap = new Bitmap()) {
            bitmap.allocPixels(ImageInfo.makeS32(x + w, y + h, ColorAlphaType.OPAQUE));
            canvas.readPixels(bitmap, x, y);
            try (var shader = bitmap.makeShader();
                 var blur = ImageFilter.makeBlur(/*radius*/10, 10, FilterTileMode.REPEAT);
                 var fill = new Paint().setShader(shader).setImageFilter(blur)) {
                canvas.save();
                canvas.translate(x, y);
                canvas.drawRect(Rect.makeXYWH(0, 0, w, h), fill);
                canvas.restore();
            }
        }
    }

    /***
     * 绘制圆角模糊效果
     * @param canvas
     * @param canvas 画笔
     * @param x 左上顶点x
     * @param y 左上顶点y
     * @param w 宽度
     * @param h 高度
     * @param r 圆角半径
     */
    public static void drawRBlur(Canvas canvas, int x, int y, int w, int h, int r) {
        try (Bitmap bitmap = new Bitmap()) {
            bitmap.allocPixels(ImageInfo.makeS32(x + w, y + h, ColorAlphaType.OPAQUE));
            canvas.readPixels(bitmap, x, y);
            if (true) {

                try (var shader = bitmap.makeShader();
                     var blur = ImageFilter.makeBlur(10, 10, FilterTileMode.CLAMP);
                     var fill = new Paint().setShader(shader).setImageFilter(blur)) {
                    canvas.save();
                    canvas.translate(x, y);
                    canvas.drawRRect(RRect.makeNinePatchXYWH(0, 0, w, h, r, r, r, r), fill);
                    canvas.restore();
                }
            }
        }
    }

    /***
     * 绘制字体阴影,也可以实现其他效果
     * @param canvas
     * @param x 位置
     * @param y
     * @param s 文字
     * @param ts 效果
     */
    public static void drawTextStyle(Canvas canvas, int x, int y, String s, TextStyle ts) {
        TextStyle f = new TextStyle();
        try (ParagraphStyle ps = new ParagraphStyle();
             ParagraphBuilder pb = new ParagraphBuilder(ps, new FontCollection().setDefaultFontManager(FontMgr.getDefault()));) {
            pb.pushStyle(ts);
            pb.addText(s);
            try (Paragraph p = pb.build();) {
                p.layout(Float.POSITIVE_INFINITY);
                p.paint(canvas, x, y);
            }
        }
    }

    /***
     * 绘制缩放图形
     * @param canvas 画笔
     * @param image 要画的图
     * @param x 被绘制的x坐标,取左上顶点
     * @param y y
     * @param w 缩放后的宽
     * @param h 高
     * @return canvas
     */
    public static Canvas drawScaleImage(Canvas canvas, Image image, float x, float y, float w, float h) {
        canvas.setMatrix(Matrix33.makeScale(w / image.getWidth(), h / image.getHeight()))
                .setMatrix(Matrix33.makeTranslate(x, y))
                .drawImage(image, 0, 0)
                .resetMatrix();
        return canvas;
    }

    /***
     * 绘制裁切图形
     * @param canvas
     * @param image
     * @param x,y 被绘制的位置(底图)
     * @param l,t 要绘制的图片(上层图)
     * @param width 要绘制的图片的宽
     * @param height 要绘制的图片的高
     * @return
     */
    public static Canvas drawCutImage(Canvas canvas, Image image, float x, float y, int l, int t, int width, int height) {
        canvas.save();
        canvas.translate(x, y);
        canvas.clipRect(Rect.makeXYWH(0, 0, width, height));
        canvas.drawImage(image, -1 * l, -1 * t);
        canvas.restore();
        return canvas;
    }

    /***
     * 绘制为圆角矩形
     * @param canvas
     * @param image
     * @param x
     * @param y
     * @param r
     * @param p 指定效果(画笔
     * @return
     */
    public static Canvas drawRRectImage(Canvas canvas, Image image, float x, float y, float r, Paint p) {
        canvas.save();
        canvas.translate(x, y);
        canvas.clipRRect(RRect.makeNinePatchXYWH(0, 0, image.getWidth(), image.getHeight(), r, r, r, r), false);
        canvas.drawImage(image, 0, 0, p);
        canvas.restore();
        return canvas;
    }
}
