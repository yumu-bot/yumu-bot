package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import org.jetbrains.skija.*;
import org.jetbrains.skija.svg.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SkiaUtil {
    static final int[] COLOR_SUGER = new int[]{
            Color.makeRGB(73, 250, 255),
            Color.makeRGB(255, 253, 73),
            Color.makeRGB(255, 73, 73),
            Color.makeRGB(205, 255, 183),
            Color.makeRGB(201, 146, 255),
            Color.makeRGB(200, 143, 110),
    };

    static final int[][] COLOR_GRAdDIENT = new int[][]{
            {hexToRGBInt("#4e54c8"), hexToRGBInt("#8f94fb")},
            {hexToRGBInt("#11998e"), hexToRGBInt("#11998e")},
            {hexToRGBInt("#FC5C7D"), hexToRGBInt("#FC5C7D")},
            {hexToRGBInt("#74ebd5"), hexToRGBInt("#ACB6E5")},
            {hexToRGBInt("#7F00FF"), hexToRGBInt("#7F00FF")},
    };

    public static int hexToRGBInt(String colorstr) {
        if (colorstr.startsWith("#")) {
            colorstr = colorstr.substring(1);
        }

        if (colorstr.length() == 8) {
            try {
                return Integer.parseInt(colorstr, 16);
            } catch (NumberFormatException e) {
                throw new Error("color error");
            }
        } else if (colorstr.length() == 6) {
            try {
                return Integer.parseInt(colorstr, 16) | 0xff000000;
            } catch (NumberFormatException e) {
                throw new Error("color error");
            }
        } else throw new Error("color error");
    }

    public static int[] hexToRGB(String colorstr) {
        if (colorstr.startsWith("#")) {
            colorstr = colorstr.substring(1);
        }
        int color;
        if (colorstr.length() == 8) {
            try {
                color = Integer.parseInt(colorstr, 16);
            } catch (NumberFormatException e) {
                throw new Error("color error");
            }
        } else if (colorstr.length() == 6) {
            try {
                color = Integer.parseInt(colorstr, 16) | 0xff000000;
            } catch (NumberFormatException e) {
                throw new Error("color error");
            }
        } else throw new Error("color error");

        return new int[]{Color.getR(color), Color.getG(color), Color.getB(color)};
    }

    public static int[] hexToARGB(String colorstr) {
        if (colorstr.startsWith("#")) {
            colorstr = colorstr.substring(1);
        }
        int color;
        if (colorstr.length() == 8) {
            try {
                color = Integer.parseInt(colorstr, 16);
            } catch (NumberFormatException e) {
                throw new Error("color error");
            }
        } else if (colorstr.length() == 6) {
            try {
                color = Integer.parseInt(colorstr, 16) | 0xff000000;
            } catch (NumberFormatException e) {
                throw new Error("color error");
            }
        } else throw new Error("color error");

        return new int[]{Color.getA(color), Color.getR(color), Color.getG(color), Color.getB(color)};
    }

    static final Logger log = LoggerFactory.getLogger(SkiaUtil.class);
    //字体文件
    static Typeface TORUS_REGULAR;

    public static Typeface getTorusRegular() {
        if (TORUS_REGULAR == null || TORUS_REGULAR.isClosed()) {
            try {
//                InputStream in = SkiaUtil.class.getClassLoader().getResourceAsStream("static/font/Torus-Regular.ttf");
//                TORUS_REGULAR = Typeface.makeFromData(Data.makeFromBytes(in.readAllBytes()));
                TORUS_REGULAR = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Torus-Regular.ttf");
            } catch (Exception e) {
                log.error("未读取到目标字体:Torus-Regular.ttf", e);
                TORUS_REGULAR = Typeface.makeDefault();
            }
        }
        return TORUS_REGULAR;
    }

    static Typeface TORUS_SEMIBOLD;

    public static Typeface getTorusSemiBold() {
        if (TORUS_SEMIBOLD == null || TORUS_SEMIBOLD.isClosed()) {
            try {
                TORUS_SEMIBOLD = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Torus-SemiBold.ttf");
            } catch (Exception e) {
                log.error("未读取到目标字体:Torus-SemiBold.ttf", e);
                TORUS_SEMIBOLD = Typeface.makeDefault();
            }
        }
        return TORUS_SEMIBOLD;
    }

    static Typeface PUHUITI;

    public static Typeface getPUHUITI() {
        if (PUHUITI == null || PUHUITI.isClosed()) {
            try {
                PUHUITI = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Puhuiti.ttf");
            } catch (Exception e) {
                log.error("Alibaba-PuHuiTi-Medium.ttf", e);
                PUHUITI = Typeface.makeDefault();
            }
        }
        return PUHUITI;
    }

    static Typeface PUHUITI_MEDIUM;

    public static Typeface getPUHUITIMedium() {
        if (PUHUITI_MEDIUM == null || PUHUITI_MEDIUM.isClosed()) {
            try {
                PUHUITI_MEDIUM = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Alibaba-PuHuiTi-Medium.ttf");
            } catch (Exception e) {
                log.error("Alibaba-PuHuiTi-Medium.ttf", e);
                PUHUITI_MEDIUM = Typeface.makeDefault();
            }
        }
        return PUHUITI_MEDIUM;
    }

    static Typeface EXTRA;

    public static Typeface getEXTRA() throws Exception {
        if (EXTRA == null || EXTRA.isClosed()) {
            try {
                EXTRA = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "extra.ttf");
            } catch (Exception e) {
                log.error("未读取到目标字体:extra.ttf", e);
                throw e;
            }
        }
        return EXTRA;
    }

    /*
     * 直接绘制为剪切的圆角矩形
     * @param canvas
     * @param image
     * @param dx
     * @param dy
     * @param fx
     * @param fy
     * @param w
     * @param h
     * @param r 圆角半径
     * @return
     */
//    public static Canvas drawCutRRectImage(Canvas canvas, Image image, float dx, float dy, float fx, float fy, float w, float h, float r) {
//        drawCutRRectImage(canvas, image, dx, dy, fx, fy, w, h, r, null);
//        return canvas;
//    }
//
//    public static Canvas drawCutRRectImage(Canvas canvas, Image image, float dx, float dy, float fx, float fy, float w, float h, float r, Paint p) {
//        canvas.save();
//        canvas.translate(dx, dy);
//        canvas.clipRRect(RRect.makeNinePatchXYWH(0, 0, w, h, r, r, r, r), true);
//        canvas.drawImage(image, -fx, -fy, p);
//        canvas.restore();
//        return canvas;
//    }


    /***
     * 绘制svg
     * @param canvas
     * @param svg
     * @param x
     * @param y
     * @param width
     * @param height
     * @param svgPreserveAspectRatioAlign SVGPreserveAspectRatioAlign.X(MIN/MID/MAX)_YMIN(MIN/MID/MAX) x(左/中/右),y(上/中/下)对齐
     * @param svgPreserveAspectRatioScale SVGPreserveAspectRatioScale.MEET/SLICE 保持横纵比的缩放(会有空白)/拉伸填充(会裁切)
     * @return
     */
    public static Canvas drawSvg(Canvas canvas, SVGDOM svg, float x, float y, float width, float height, SVGPreserveAspectRatioAlign svgPreserveAspectRatioAlign, SVGPreserveAspectRatioScale svgPreserveAspectRatioScale) {
        canvas.save();
        canvas.translate(x, y);
        canvas.clipRect(Rect.makeXYWH(0, 0, width, height));
//        if(x == 0 && y == 0) /* 调试代码 */
//                canvas.clear(Color.makeARGB(100,255,0,0));
        try (var root = svg.getRoot()) {
            root.setWidth(new SVGLength(width))
                    .setHeight(new SVGLength(height))
                    .setPreserveAspectRatio(new SVGPreserveAspectRatio(svgPreserveAspectRatioAlign, svgPreserveAspectRatioScale));
        }
        svg.render(canvas);
        canvas.restore();
        return canvas;
    }

    /***
     * svg(默认左右居中,上对其),保持横纵比缩放保留空白 绘制
     * @param canvas
     * @param svg
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    public static Canvas drawSvg(Canvas canvas, SVGDOM svg, float x, float y, float width, float height) {
        return drawSvg(canvas, svg, x, y, width, height, SVGPreserveAspectRatioAlign.XMID_YMIN, SVGPreserveAspectRatioScale.SLICE);
    }

    /***
     * 获得条状渐变色
     * @param x 渐变开始坐标
     * @param x1 渐变结束的坐标
     * @param colors 色彩组
     * @return
     */
    public static Shader getLinearShader(int x, int y, int x1, int y1, int... colors) {
        return Shader.makeLinearGradient(x, y, x1, y1, colors);
    }

    /***
     * 获取ppy 国旗svg的接口,
     * @param ct 国家缩写
     * @return 国旗url
     */
    public static String getFlagUrl(String ct) {
        int A = 0x1f1e6;
        char x1 = ct.charAt(0);
        char x2 = ct.charAt(1);
        int s1 = A + x1 - 'A';
        int s2 = A + x2 - 'A';
        return "https://osu.ppy.sh/assets/images/flags/" + Integer.toHexString(s1) + "-" + Integer.toHexString(s2) + ".svg";
    }

    /***
     * 生成六边形路径
     * @param size 基准大小 半径
     * @param circle_width 转折点大小 半径
     * @param point 六点数据 长度必须为6 范围[0,1]
     * @return path[0]六边形路径   path[1]转折点路径
     *
     */
    public static Path[] creat6(float size, float circle_width, float... point) {
        if (point.length != 6) return null;
        var path = new org.jetbrains.skija.Path();
        for (int i = 0; i < 6; i++) {
            if (point[i] < 0) point[i] = 0;
            if (point[i] > 1) point[i] = 1;
        }
        float[] ponX = new float[6];
        float[] ponY = new float[6];
        ponX[0] = -size * point[0] * 0.5f;
        ponY[0] = -size * point[0] * 0.866f;
        ponX[1] = size * point[1] * 0.5f;
        ponY[1] = -size * point[1] * 0.866f;
        ponX[2] = size * point[2];
        ponY[2] = 0;
        ponX[3] = size * point[3] * 0.5f;
        ponY[3] = size * point[3] * 0.866f;
        ponX[4] = -size * point[4] * 0.5f;
        ponY[4] = size * point[4] * 0.866f;
        ponX[5] = -size * point[5];
        ponY[5] = 0;
        path.moveTo(ponX[0], ponY[0]);
        path.lineTo(ponX[1], ponY[1]);
        path.lineTo(ponX[2], ponY[2]);
        path.lineTo(ponX[3], ponY[3]);
        path.lineTo(ponX[4], ponY[4]);
        path.lineTo(ponX[5], ponY[5]);
        path.closePath();
        if (circle_width == 0) {
            return new Path[]{path};
        }
        Path path1 = new Path();
        path1.addCircle(ponX[0], ponY[0], circle_width);
        path1.addCircle(ponX[1], ponY[1], circle_width);
        path1.addCircle(ponX[2], ponY[2], circle_width);
        path1.addCircle(ponX[3], ponY[3], circle_width);
        path1.addCircle(ponX[4], ponY[4], circle_width);
        path1.addCircle(ponX[5], ponY[5], circle_width);
        return new Path[]{path, path1};
    }

    public static Image getTextImage(String text, Typeface typeface, float textsize, Paint paint) {
        Image image;
        var font = new Font(typeface, textsize);
        var textLine = TextLine.make(text, font);
        var surface = Surface.makeRasterN32Premul((int) textLine.getWidth(), (int) textLine.getHeight());
        System.out.println(textLine.getHeight());
        System.out.println(textLine.getXHeight());
        System.out.println(textLine.getCapHeight());
        try (font; textLine; surface) {
            var canvas = surface.getCanvas();
            canvas.clear(0);
            canvas.translate(0, 1.2f * textLine.getCapHeight());
            canvas.drawTextLine(textLine, 0, 0, paint);
            image = surface.makeImageSnapshot();
        }
        return image;
    }

    /**
     * 创建折线图
     *
     * @param width  宽度
     * @param height
     * @param t      数据
     * @return
     */
    public static Path getPolyline(int width, int height, float... t) {
        if (t.length < 2) return new Path();
        float step = 1f * width / (t.length - 1);
        float max = t[0], min = t[0];

        for (var temp : t) {
            if (max < temp) max = temp;
            if (min > temp) min = temp;
        }
        float stepY = height / (min - max);
        Path out = new Path();
        out.moveTo(0, height + stepY * (t[0] - min));

        for (int i = 1; i < t.length - 1; i++) {
//            out.lineTo(i*step, height + stepY * (t[i]-min));
//            out.quadTo(i*step, height + stepY * (t[i]-min), (i+1)*step, height + stepY * (t[(i+1)]-min));// 二次贝塞尔曲线
//            out.cubicTo()// 三次贝塞尔曲线
            out.cubicTo(
                    (i - 0.5f) * step, height + stepY * ((t[i - 1] + t[i]) / 2 - min),
                    i * step, height + stepY * (t[i] - min),
                    (i + 0.5f) * step, height + stepY * ((t[i + 1] + t[i]) / 2 - min)
            );
        }
        out.quadTo((t.length - 2) * step, height + stepY * (t[t.length - 2] - min), (t.length - 1) * step, height + stepY * (t[t.length - 1] - min));
        return out;
    }

    /***
     * ppy星数->色彩 算法
     * @param star 星数
     * @return 颜色rgb的int按位表示值,
     */
    public static int getStartColor(float star) {
        var starts = new float[]{0.0999f, 0.1f, 1.25f, 2, 2.5f, 3.3f, 4.2f, 4.9f, 5.8f, 6.7f, 7.7f, 9};
        var colorgroup = new int[][]{
                {170, 170, 170},
                {66, 144, 251},
                {79, 192, 255},
                {79, 255, 213},
                {124, 255, 79},
                {246, 240, 92},
                {255, 104, 104},
                {255, 78, 111},
                {198, 69, 184},
                {101, 99, 222},
                {24, 21, 142},
                {0, 0, 0},
        };
        int imax = starts.length - 1, imin = 0;
        if (star <= starts[imin])
            return (0xFF) << 24 | (colorgroup[imin][0] << 16) | (colorgroup[imin][1] << 8) | (colorgroup[imin][2]);
        if (star >= starts[imax]) return (0xFF) << 24 | (0 << 16) | (0 << 8) | (0);
        while (imax - imin > 1) {
            int t = (imax + imin) / 2;
            if (starts[t] > star) {
                imax = t;
            } else if (starts[t] < star) {
                imin = t;
            } else {
                return (0xFF) << 24 | (colorgroup[t][0] << 16) | (colorgroup[t][1] << 8) | (colorgroup[t][2]);
            }
        }
        float dy = (star - starts[imin]) / (starts[imax] - starts[imin]);
        int[] caa = {
                (int) (dy * (colorgroup[imax][0] - colorgroup[imin][0]) + colorgroup[imin][0]),
                (int) (dy * (colorgroup[imax][1] - colorgroup[imin][1]) + colorgroup[imin][1]),
                (int) (dy * (colorgroup[imax][2] - colorgroup[imin][2]) + colorgroup[imin][2]),
        };

        return (0xFF) << 24 | (caa[0] << 16) | (caa[1] << 8) | (caa[2]);
    }

    /***
     * todo 图片颜色主色提取 未完成
     * @param image 输入图片
     * @return 色组
     */
    private static final float MAIN_COLOR_IMAGE_MAX_SIZE = 500;

    public static int[] getMainColor(Image image, int len) {
        //缩放图片
//        if (Math.max(image.getWidth(), image.getHeight()) > MAIN_COLOR_IMAGE_MAX_SIZE) {
//            image = SkiaImageUtil.getScaleImage(image, MAIN_COLOR_IMAGE_MAX_SIZE / Math.max(image.getWidth(), image.getHeight()));
//        }
        Bitmap bitmap = Bitmap.makeFromImage(image);
        int x_length = bitmap.getWidth();
        int y_length = bitmap.getHeight();
        {
            int[] colorCount = new int[1 << 15];
            for (int x_index = 0; x_index < x_length; x_index++) {
                for (int y_index = 0; y_index < y_length; y_index++) {
                    int i = bitmap.getColor(x_index, y_index);
                    //压缩 RGB555 颜色空间
                    int r = ((i >> 19) & ((1 << 5) - 1));
                    int g = ((i >> 11) & ((1 << 5) - 1));
                    int b = ((i >> 3) & ((1 << 5) - 1));
                    colorCount[(r << 10) | (g << 5) | b]++;
                }
            }
            List<Integer> colors = new ArrayList<>();
            for (int i = 0; i < colorCount.length; i++) {
                if (colorCount[i] != 0 && true/* 接近白色、黑色和红色的颜色。 why? */) {
                    colors.add(i);
                }
            }
            if (colors.size() < len) {
                var data = new int[colors.size()];
                for (int i = 0; i < colors.size(); i++) {
                    data[i] = colors.get(i);
                }
                return data;
            }
        }
        //提取色彩int值
        int[] colors_int = new int[x_length * y_length];
        for (int x_index = 0; x_index < x_length; x_index++) {
            for (int y_index = 0; y_index < y_length; y_index++) {
                colors_int[x_index * y_length + y_index] = bitmap.getColor(x_index, y_index);
            }
        }
        if (len > 0) return colors_int;
        //色彩排序
        Arrays.sort(colors_int);
        //计算颜色柱数量
        int color_size = 0;
        int colorCount = 1;
        int currentColor = colors_int[0];
        for (int i = 1; i < colors_int.length; i++) {
            // If we encounter a new color, increase the population
            if (colors_int[i] != currentColor) {
                currentColor = colors_int[i];
                colorCount++;
            }
        }
        if (colors_int.length < 2) color_size = 2;
        var mColors = new int[color_size];
        var mColorCounts = new int[color_size];

        return colors_int;
    }

    /**
     * 计算bonusPP
     * 算法通过 正态分布 "估算"超过bp100的 pp，此方法不严谨
     */

    public static float getBonusPP(double[] pp, Long pc){
        double bonus = 0;
        double sumOxy = 0;
        double sumOx2 = 0;
        double avgX = 0;
        double avgY = 0;
        double sumX = 0;
        for (int i = 1; i <= pp.length; i++) {
            double weight = Math.log1p(i + 1);
            sumX += weight;
            avgX += i * weight;
            avgY += pp[i - 1] * weight;
        }
        avgX /= sumX;
        avgY /= sumX;
        for(int n = 1; n <= pp.length; n++){
            sumOxy += (n - avgX) * (pp[n - 1] - avgY) * Math.log1p(n + 1.0D);
            sumOx2 += Math.pow(n - avgX, 2.0D) * Math.log1p(n + 1.0D);
        }
        double Oxy = sumOxy / sumX;
        double Ox2 = sumOx2 / sumX;
        for(int n = 100; n <= pc; n++){
            double val = Math.pow(100.0D, (avgY - (Oxy / Ox2) * avgX) + (Oxy / Ox2) * n);
            if(val <= 0.0D){
                break;
            }
            bonus += val;
        }
        return (float) bonus;
    }

    public static int[] getRandomColors() {
        return COLOR_GRAdDIENT[new Random().nextInt(COLOR_GRAdDIENT.length)];
    }

    public static int getRandomColor() {
        return COLOR_SUGER[new Random().nextInt(COLOR_SUGER.length)];
    }

    public static int getRankColor(String rank) {
        if (rank == null) rank = "F";
        switch (rank.trim().toUpperCase()) {
            case "S" : return  Color.makeRGB(240, 148, 80);
            case "SH" : return  Color.makeRGB(180, 180, 180);
            case "X" : return  Color.makeRGB(254, 246, 103);
            case "XH" : return  Color.makeRGB(248, 248, 248);
            case "A" : return  Color.makeRGB(121, 196, 113);
            case "B" : return  Color.makeRGB(62, 188, 239);
            case "C" : return  Color.makeRGB(151, 129, 183);
            case "D" : return  Color.makeRGB(234, 107, 72);
            default : return  Color.makeRGB(32, 32, 32);
        }
    }

    public static double getV3ScoreProgress(Score score, BeatMap beatMap) { //下下策
        OsuMode mode = score.getMode();

        int s_300 = score.getStatistics().getCount300();
        int s_100 = score.getStatistics().getCount100();
        int s_50 = score.getStatistics().getCount50();
        int s_g = score.getStatistics().getCountGeki();
        int s_k = score.getStatistics().getCountKatu();
        int s_0 = score.getStatistics().getCountMiss();

        int s = beatMap.getMaxCombo();

        double progress;
        if(!score.getPassed()){
            switch (mode) {
                case OSU : progress = 1D * (s_300 + s_100 + s_50 + s_0) / s; break;
                case TAIKO: {}
                case CATCH : progress = 1D * (s_300 + s_100 + s_0) / s; break;
                case MANIA : progress = 1D * (s_g + s_300 + s_k + s_100 + s_50 + s_0) / s; break;
                default : progress = 1D;
            }
        } else {
            progress = 1D;
        }
        return progress;
    }

    public static String getV3Score(Score score, BeatMap beatmap) {
        // 算 v3 分（lazer的计分方式
        // 有个版本指出，目前 stable 的 v2 是这个算法的复杂版本，acc是10次方，转盘分数纳入mod倍率

        OsuMode mode = score.getMode();
        List<String> mods = score.getMods();

        int fc = 100_0000;
        double i = getV3ModsMultiplier(mods,mode);
        double p = getV3ScoreProgress(score,beatmap); //下下策
        int c = score.getStatistics().getMaxCombo();
        int m = beatmap.getMaxCombo();
        double ap8 = Math.pow(score.getAccuracy(), 8f);
        double v3 = 0;

        switch (score.getMode()){
            case OSU:
            case CATCH:
            case DEFAULT : v3 = fc * i * (0.7f * c / m + 0.3f * ap8) * p; break;
            case TAIKO : v3 = fc * i * ( 0.75f * c / m + 0.25f * ap8) * p; break;
            case MANIA : v3 = fc * i * ( 0.01f * c / m + 0.99f * ap8) * p; break;
        }

        return String.format("%07d",Math.round(v3)); //补 7 位达到 v3 分数的要求
    }

    public static double getV3ModsMultiplier(List<String> mod, OsuMode mode) {
        double index = 1.00D;

        if (mod.contains("EZ")) index *= 0.50D;

        if (mode == OsuMode.OSU){
            if (mod.contains("HT")) index *= 0.30D;
            if (mod.contains("HR")) index *= 1.10D;
            if (mod.contains("DT")) index *= 1.20D;
            if (mod.contains("NC")) index *= 1.20D;
            if (mod.contains("HD")) index *= 1.06D;
            if (mod.contains("FL")) index *= 1.12D;
            if (mod.contains("SO")) index *= 0.90D;
        }

        if (mode == OsuMode.TAIKO){
            if (mod.contains("HT")) index *= 0.30D;
            if (mod.contains("HR")) index *= 1.06D;
            if (mod.contains("DT")) index *= 1.12D;
            if (mod.contains("NC")) index *= 1.12D;
            if (mod.contains("HD")) index *= 1.06D;
            if (mod.contains("FL")) index *= 1.12D;

        }

        if (mode == OsuMode.CATCH){
            if (mod.contains("HT")) index *= 0.30D;
            if (mod.contains("HR")) index *= 1.12D;
            if (mod.contains("DT")) index *= 1.12D;
            if (mod.contains("NC")) index *= 1.12D;
            if (mod.contains("FL")) index *= 1.12D;

        }

        if (mode == OsuMode.MANIA){
            if (mod.contains("HT")) index *= 0.50D;
            if (mod.contains("CO")) index *= 0.90D;

        }

        return index;
    }

    public class PolylineBuilder {
        ArrayList<Float> point = new ArrayList<>();
        float width;
        float height;
        List<Integer> mark;
        int max_index = -1;
        int min_index = -1;
        boolean poly = false;

        public PolylineBuilder addPoint(int... d) {
            for (int i : d) {
                if (max_index > 0 && (i - point.get(max_index)) > 0) {
                    max_index = point.size();
                }
                if (min_index > 0 && (i - point.get(min_index)) < 0) {
                    min_index = point.size();
                }

                point.add((float) i);
            }
            return this;
        }

        public PolylineBuilder addPoint(float... d) {
            for (float i : d) {
                if (max_index > 0 && (i - point.get(max_index)) > 0) {
                    max_index = point.size();
                }
                if (min_index > 0 && (i - point.get(min_index)) < 0) {
                    min_index = point.size();
                }
                point.add(i);
            }
            return this;
        }
    }

    public static void main(String[] args) throws IOException {
        var img = SkiaImageUtil.getImage("/home/spring/cache/nowbot/bg/ExportFileV3/object-beatmap-mask.png");
        var image1 = SkiaImageUtil.getImage("/home/spring/cache/nowbot/bg/ExportFileV3/object-score-backimage-C.jpg");
        var bitmap = Bitmap.makeFromImage(img);
        var s = Surface.makeRasterN32Premul(150,150);
        var c = s.getCanvas();

//        c.clear(Color.makeARGB(255,0,0,0));
        var ems = BlendMode.values();
        var p = new Paint().setImageFilter(ImageFilter.makeImage(image1));

        for (var e: ems) {
            p.setBlendMode(e);
            c.drawImage(img,0,0, p);
            Files.write(java.nio.file.Path.of("/home/spring/ee-"+e.name()+".png"), s.makeImageSnapshot().encodeToData().getBytes());
        }
    }
}

