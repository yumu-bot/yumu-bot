package com.now.nowbot.util.Panel;

import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.skija.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PPPanelBuilder extends PanelBuilder {
    protected static final Logger log = LoggerFactory.getLogger(PPPanelBuilder.class);

    PPPanelBuilder() {
        super(1920, 1080);
    }
    //l80 r430 m510 m790 m960 m1130 m1410 l1490 r1840
    /**
     * 左值名称 左对齐
     */
    private static final int X_L1 = 80;
    /**
     * 左数值 右对齐
     */
    private static final int X_L2 = 430;
    /**
     * 第一层文字高度
     */
    private static final int Y_T1 = 370 + 12;
    /**
     * 每层文字偏移
     */
    private static final int Y_T1_OFF = 115;
    /**
     * 左评价 居中对齐
     */
    private static final int X_L3 = 510;
    /**
     * 偏左中下 总和 居中对齐
     */
    private static final int X_L4 = 790;
    /**
     * 数字高度
     */
    private static final int Y_T2 = 945 + 12;
    /**
     * 偏右中下 总和
     */
    private static final int X_R4 = 1130;
    /**
     * 右评价
     */
    private static final int X_R3 = 1410;
    /**
     * 右值名称
     */
    private static final int X_R1 = 1490;
    /**
     * 右数值
     */
    private static final int X_R2 = 1840;
    /**
     * 大字体size
     */
    private static final float FONT_SIZE_BIG = 60;
    /**
     * 小字体尺寸
     */
    private static final float FONT_SIZE_SIM = 36;
    /**
     * 大小字体高度差
     */
    private static final float FONT_OFFSET = 16;

    public PPPanelBuilder drawBanner(Image bg) {
        var temp = SkiaImageUtil.getScaleCenterImage(bg, 1920, 320);
        try (temp) {
            canvas.drawImage(temp, 0, 0);
        }
        return this;
    }

    /**
     * 绘制六边形
     *
     * @param point 输入数值,范围[0-1];
     * @param color 颜色预设 true:蓝色 | false:红色
     * @return return
     */
    public PPPanelBuilder drawHexagon(float[] point, boolean color) {
        if (point.length != 6) {
            throw new RuntimeException("输入参数长度错误");
        }
        canvas.save();
        Path[] paths = SkiaUtil.creat6(230, 10, point);
        if (paths == null || paths.length != 2) {
            throw new RuntimeException("创建形状错误");
        }
        final var pt1 = paths[0];
        final var pt2 = paths[1];
        int c = color ? PanelUtil.COLOR_HEX_ME : PanelUtil.COLOR_HEX_OTHER;
        try (pt1; pt2) {
            canvas.translate(960, 600);
            //填充,半透
            canvas.drawPath(pt1, new Paint().setColor(c).setAlphaf(0.2f).setStroke(false));
            canvas.drawPath(pt1, new Paint().setColor(c).setStrokeWidth(5).setStroke(true));
            canvas.drawPath(pt2, new Paint().setColor(c).setStroke(false));
        }
        canvas.restore();
        return this;
    }

    /**
     * 填Panel类型
     * @param name
     * @return
     */
    public PPPanelBuilder drawPanelName(String name){
        canvas.save();
        var typeface = SkiaUtil.getTorusSemiBold();
        var font = new Font(typeface, 48);
        var text = TextLine.make(name,font);
        try(font;text){
            canvas.translate(607.5f, 50+40*0.2f);
            canvas.drawTextLine(text,-0.5f*text.getWidth(),text.getCapHeight(),p_white);
        }
        canvas.restore();
        return this;
    }
    /***
     * 绘制左边的value值  [name  value rank/sign]
     * @param n 行数下标,从零开始
     * @param text 文字
     * @return
     */
    public PPPanelBuilder drawLeftValueN(int n, String text) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        return drawRightText(text, null, X_L2, Y_T1 + Y_T1_OFF * n, p_white);
    }

    /***
     * 绘制左边的name值  [name  value rank/sign]
     * @param n 行数下标,从零开始
     * @param text 文字
     * @return
     */
    public PPPanelBuilder drawLeftNameN(int n, String text) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        return drawLeftText(text, null, X_L1, Y_T1 + Y_T1_OFF * n, p_white);
    }

    public PPPanelBuilder drawLeftNameN(int n, String bigText, String simText,@Nullable Paint color) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        if (color == null) color = p_white;
        return drawLeftText(bigText, simText, X_L1, Y_T1 + Y_T1_OFF * n, color);
    }

    /***
     * 绘制左边的rank/sing值  [name  value rank/sign]
     * @param n 行数下标,从零开始
     * @param text 文字
     * @return
     */
    public PPPanelBuilder drawLeftRankN(int n, String text, int color) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        return drawCenterText(text, null, X_L3, Y_T1 + Y_T1_OFF * n, new Paint().setColor(color));
    }

    /***
     * 绘制右边的value值  [name  value rank/sign]
     * @param n 行数下标,从零开始
     * @param text 文字
     * @return
     */
    public PPPanelBuilder drawRightValueN(int n, String text) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        return drawRightText(text, null, X_R2, Y_T1 + Y_T1_OFF * n, p_white);
    }

    /***
     * 绘制右边的name值  [name  value rank/sign]
     * @param n 行数下标,从零开始
     * @param text 文字
     * @return
     */
    public PPPanelBuilder drawRightNameN(int n, String text) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        return drawLeftText(text, null, X_R1, Y_T1 + Y_T1_OFF * n, p_white);
    }

    public PPPanelBuilder drawRightNameN(int n, String bigText, String simText,@Nullable Paint color) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        if (color == null) color = p_white;
        return drawLeftText(bigText, simText, X_R1, Y_T1 + Y_T1_OFF * n, color);
    }


    /***
     * 绘制右边的rank/sing值  [name  value rank/sign]
     * @param n 行数下标,从零开始
     * @param text 文字
     * @return
     */
    public PPPanelBuilder drawRightRankN(int n, String text, int color) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        return drawCenterText(text, null, X_R3, Y_T1 + Y_T1_OFF * n, new Paint().setColor(color));
    }

    /**
     * 左侧value大小文字渲染
     *
     * @param n       层数
     * @param bigText 大文字
     * @param simText 小文字
     * @return
     */
    public PPPanelBuilder drawLeftValueN(int n, String bigText, String simText) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        return drawRightText(bigText, simText, X_L2, Y_T1 + Y_T1_OFF * n, p_white);
    }

    /**
     * 右侧value大小文字渲染
     *
     * @param n       层数
     * @param bigText 大文字
     * @param simText 小文字
     * @return
     */
    public PPPanelBuilder drawRightValueN(int n, String bigText, String simText) {
        if (n < 0 || n > 5) throw new RuntimeException("超出范围");
        return drawRightText(bigText, simText, X_R2, Y_T1 + Y_T1_OFF * n, p_white);
    }

    /**
     * 总值 左
     *
     * @param text
     * @return
     */
    public PPPanelBuilder drawLeftTotal(String text) {
        return drawCenterText(text, null, X_L4, Y_T2, p_white);
    }

    /**
     * 总值 右
     *
     * @param text
     * @return
     */
    public PPPanelBuilder drawRightTotal(String text) {
        return drawCenterText(text, null, X_R4, Y_T2, p_white);
    }

    /**
     * 中间的值(类似于总和) 左
     *
     * @return
     */
    public PPPanelBuilder drawLeftTotal(String bigText, String simText) {
        return drawCenterText(bigText, simText, X_L4, Y_T2, p_white);
    }

    /**
     * 中间的值(类似于总和) 右
     *
     * @return
     */
    public PPPanelBuilder drawRightTotal(String bigText, String simText) {
        return drawCenterText(bigText, simText, X_R4, Y_T2, p_white);
    }

    /**
     * 中间值的名称 左
     * 颜色写死了,有需要可以改
     * @param name
     * @return
     */
    public PPPanelBuilder drawLeftTotleName(String name) {
        drawCenterText(name,null, X_L4,860, new Paint().setARGB(255,161,161,161));
        return this;
    }

    /**
     * 中间值的名称 右
     * 颜色写死了,到时候有需要了可以改
     * @param name
     * @return
     */
    public PPPanelBuilder drawRightTotleName(String name) {
        drawCenterText(name,null, X_R4,860, new Paint().setARGB(255,161,161,161));
        return this;
    }

    //文字渲染 分别对应着 左 中 右 文字对齐方式

    /***
     * 左对齐渲染
     */
    protected PPPanelBuilder drawLeftText(String bigText, @Nullable String simText, int left, int top, Paint color) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        final Font fontB = new Font(typeface, FONT_SIZE_BIG);
        final var lineB = TextLine.make(bigText, fontB);
        if (simText != null) {
            final Font fontS = new Font(typeface, FONT_SIZE_SIM);
            final var lineS = TextLine.make(simText, fontS);
            try (typeface; fontB; fontS; lineB; lineS) {
                canvas.translate(left, top);
                canvas.drawTextLine(lineB, 0, lineB.getCapHeight(), color);
                canvas.translate(lineB.getWidth(), FONT_OFFSET);
                canvas.drawTextLine(lineS, 0, lineS.getCapHeight(), color);
            }
        } else {
            try (typeface; fontB; lineB) {
                canvas.translate(left, top);
                canvas.drawTextLine(lineB, 0, lineB.getCapHeight(), color);
            }
        }
        canvas.restore();
        return this;
    }

    /***
     * 居中对齐渲染 带颜色
     * @param bigText 大数
     * @param simText 小数
     * @param center
     * @param top
     * @return
     */
    protected PPPanelBuilder drawCenterText(String bigText, @Nullable String simText, int center, int top, Paint color) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        final Font fontB = new Font(typeface, 60);
        final var lineB = TextLine.make(bigText, fontB);
        if (simText != null) {
            final Font fontS = new Font(typeface, 36);
            final var lineS = TextLine.make(simText, fontS);
            try (typeface; fontB; fontS; lineB; lineS) {
                canvas.translate((center - 0.5f * (lineB.getWidth() + lineS.getWidth())), top);
                canvas.drawTextLine(lineB, 0, lineB.getCapHeight(), color);
                canvas.translate(lineB.getWidth(), FONT_OFFSET);
                canvas.drawTextLine(lineS, 0, lineS.getCapHeight(), color);
            }
        } else {
            try (typeface; fontB; lineB) {
                canvas.translate((center - 0.5f * lineB.getWidth()), top);
                canvas.drawTextLine(lineB, 0, lineB.getCapHeight(), color);
            }
        }
        canvas.restore();
        return this;
    }

    /**
     * 设置居中字体大小的调用
     * @param size 大小
     */
    protected PPPanelBuilder drawCenterText(String bigText, int center, int top, int size, Paint color) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        final Font fontB = new Font(typeface, size);
        final var lineB = TextLine.make(bigText, fontB);
        try (typeface; fontB; lineB) {
            canvas.translate((center - 0.5f * lineB.getWidth()), top);
            canvas.drawTextLine(lineB, 0, lineB.getCapHeight()+size*0.2f, color);
        }

        canvas.restore();
        return this;
    }

    /***
     * 右对齐渲染
     * @param bigText
     * @param simText
     * @param right
     * @param top
     * @return
     */
    protected PPPanelBuilder drawRightText(String bigText, @Nullable String simText, int right, int top, Paint color) {
        canvas.save();
        Typeface typeface = SkiaUtil.getTorusSemiBold();
        final Font fontB = new Font(typeface, 60);
        final var lineB = TextLine.make(bigText, fontB);
        if (simText != null) {
            final Font fontS = new Font(typeface, 36);
            final var lineS = TextLine.make(simText, fontS);
            try (typeface; fontB; fontS; lineB; lineS) {
                canvas.translate(right - (lineS.getWidth() + lineB.getWidth()), top);
                canvas.drawTextLine(lineB, 0, lineB.getCapHeight(), color);
                canvas.translate(lineB.getWidth(), FONT_OFFSET);
                canvas.drawTextLine(lineS, 0, lineS.getCapHeight(), color);
            }
        } else {
            try (typeface; fontB; lineB) {
                canvas.translate(right, top);
                canvas.drawTextLine(lineB, -lineB.getWidth(), lineB.getCapHeight(), color);
            }
        }
        canvas.restore();
        return this;
    }

    /***
     * 左侧card
     * @param card
     * @return
     */
    public PPPanelBuilder drawLeftCard(Image card) {
        canvas.save();
        canvas.translate(40, 40);
        canvas.drawImage(card, 0, 0);
        canvas.restore();
        return this;
    }

    /***
     * 右侧card
     * @param card
     * @return
     */
    public PPPanelBuilder drawRightCard(Image card) {
        canvas.save();
        canvas.translate(width - card.getWidth() - 40, 40);
        canvas.drawImage(card, 0, 0);
        canvas.restore();
        return this;
    }

    @Override
    public PPPanelBuilder drawImage(Image add) {
        super.drawImage(add);
        return this;
    }

    public Image build() {
        return super.build(15);
    }

    public Image build(String text) {
        return super.build(15, text);
    }
}
