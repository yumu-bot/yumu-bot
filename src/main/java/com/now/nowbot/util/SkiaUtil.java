package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import org.jetbrains.skija.*;
import org.jetbrains.skija.svg.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static Typeface getPuhuitiMedium() {
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

    public static Image getTextImage(String text, Typeface typeface, float textsize, Paint paint){
        Image image;
        var font = new Font(typeface,textsize);
        var textLine = TextLine.make(text, font);
        var surface = Surface.makeRasterN32Premul((int)textLine.getWidth(), (int)textLine.getHeight());
        System.out.println(textLine.getHeight());
        System.out.println(textLine.getXHeight());
        System.out.println(textLine.getCapHeight());
        try (font;textLine;surface){
            var canvas = surface.getCanvas();
            canvas.clear(0);
            canvas.translate(0,1.2f*textLine.getCapHeight());
            canvas.drawTextLine(textLine,0,0,paint);
            image = surface.makeImageSnapshot();
        }
        return image;
    }

    public static Path get(int width, int height, float... t){
        if (t.length < 2) return new Path();
        float step = 1f * width / (t.length-1);
        float max = t[0],min = t[0];

        for (var temp : t){
            if (max < temp) max = temp;
            if (min > temp) min = temp;
        }
        float stepY = height / (min - max);
        Path out = new Path();
        out.moveTo(0,height + stepY * (t[0]-min));

        for (int i = 1; i < t.length-1; i++) {
//            out.lineTo(i*step, height + stepY * (t[i]-min));
//            out.quadTo(i*step, height + stepY * (t[i]-min), (i+1)*step, height + stepY * (t[(i+1)]-min));// 二次贝塞尔曲线
//            out.cubicTo()// 三次贝塞尔曲线
            out.cubicTo(
                    (i-0.5f)*step,  height + stepY * ((t[i-1]+t[i])/2-min),
                    i*step, height + stepY * (t[i]-min),
                    (i+0.5f)*step,  height + stepY * ((t[i+1]+t[i])/2-min)
            );
        }
        out.quadTo((t.length-2)*step, height + stepY * (t[t.length-2]-min),(t.length-1)*step, height + stepY * (t[t.length-1]-min));
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
                {170,170,170},
                {66,144,251},
                {79,192,255},
                {79,255,213},
                {124,255,79},
                {246,240,92},
                {255,104,104},
                {255,78,111},
                {198,69,184},
                {101,99,222},
                {24,21,142},
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
            int[] colorCount = new int[1<<15];
            for (int x_index = 0; x_index < x_length; x_index++) {
                for (int y_index = 0; y_index < y_length; y_index++) {
                    int i = bitmap.getColor(x_index, y_index);
                    //压缩 RGB555 颜色空间
                    int r = ((i>>19)&((1<<5)-1));
                    int g = ((i>>11)&((1<<5)-1));
                    int b = ((i>>3)&((1<<5)-1));
                    colorCount[(r<<10)|(g<<5)|b]++;
                }
            }
            List<Integer> colors = new ArrayList<>();
            for (int i = 0; i < colorCount.length; i++) {
                if (colorCount[i] != 0 && true/* 接近白色、黑色和红色的颜色。 why? */){
                    colors.add(i);
                }
            }
            if (colors.size() < len){
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
        if (len>0)return colors_int;
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


    public static int[] getRandomColors() {
        return COLOR_GRAdDIENT[new Random().nextInt(COLOR_GRAdDIENT.length)];
    }

    public static int getRandomColor() {
        return COLOR_SUGER[new Random().nextInt(COLOR_SUGER.length)];
    }

    public static int getRankColor(String rank) {
        if (rank == null) rank = "F";
        return switch (rank.trim().toUpperCase()){
            case "S" -> Color.makeRGB(240,148,80);
            case "SH" -> Color.makeRGB(180,180,180);
            case "X" -> Color.makeRGB(254,246,103);
            case "XH" -> Color.makeRGB(248,248,248);
            case "A" -> Color.makeRGB(121,196,113);
            case "B" -> Color.makeRGB(62,188,239);
            case "C" -> Color.makeRGB(151,129,183);
            case "D" -> Color.makeRGB(234,107,72);
            default ->  Color.makeRGB(32,32,32);
        }; //注意，这里有R+3，G-5，B+3的操作，保证不要溢出
    }

    public static int getPlayerJudgeColor(String Judge){
        if (Judge == null) Judge = "F";
        return switch (Judge.toUpperCase()){
            case "BC" -> Color.makeRGB(254,246,103);
            case "CA" -> Color.makeRGB(240,148,80);
            case "MF" -> Color.makeRGB(48,181,115);
            case "SP" -> Color.makeRGB(170,212,110);
            case "WF" -> Color.makeRGB(49,68,150);
            case "GE" -> Color.makeRGB(180,180,180);
            case "GU" -> Color.makeRGB(62,188,239);
            case "SU" -> Color.makeRGB(106,80,154);
            case "SG" -> Color.makeRGB(236,107,158);
            case "NO" -> Color.makeRGB(234,107,72);
            case "FU" -> Color.makeRGB(150,0,20);
            default -> Color.makeRGB(32,32,32);
        }; //是这么写的吗 我不会 还得写个判断judge的
    }

    public static void main(String[] args) {

    }
}
