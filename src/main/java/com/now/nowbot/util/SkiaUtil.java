package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import org.jetbrains.skija.*;
import org.jetbrains.skija.paragraph.*;
import org.jetbrains.skija.svg.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SkiaUtil {
    static final Logger log = LoggerFactory.getLogger(SkiaUtil.class);
    //字体文件
    public static Typeface TORUS_REGULAR;
    public static Typeface getTorusRegular(){
        if(TORUS_REGULAR == null || TORUS_REGULAR.isClosed()){
            try {
                TORUS_REGULAR = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Torus-Regular.ttf");
            } catch (Exception e) {
                log.error("未读取到目标字体:Puhuiti.ttf",e);
                PUHUITI = Typeface.makeDefault();
            }
        }
        return TORUS_REGULAR;
    }
    public static Typeface PUHUITI;
    public static Typeface getPUHUITI(){
        if(PUHUITI == null || PUHUITI.isClosed()){
            try {
                PUHUITI = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Puhuiti.ttf");
            } catch (Exception e) {
                log.error("未读取到目标字体:Puhuiti.ttf",e);
                PUHUITI = Typeface.makeDefault();
            }
        }
        return PUHUITI;
    }
    /***
     * 网络加载图片
     * @param path
     * @return
     */
    public static Image lodeNetWorkImage(String path){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(path.getBytes());
        md.getAlgorithm();
        java.nio.file.Path pt = java.nio.file.Path.of(NowbotConfig.IMGBUFFER_PATH+new BigInteger(1, md.digest()).toString(16));
        try {
            if (Files.isRegularFile(pt)){
                md.reset();
                return Image.makeFromEncoded(Files.readAllBytes(pt));
            }else {
                URL url = new URL(path);
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.connect();
                InputStream cin = httpConn.getInputStream();
                byte[] date = cin.readAllBytes();
                cin.close();
                Files.createFile(pt);
                Files.write(pt,date);
                System.gc();
                return Image.makeFromEncoded(date);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
     * 缩放图形 直接绘制建议使用 drowScaleImage
     * @param image
     * @param width
     * @param height
     * @return
     */
    public static Image getScaleImage(Image image, int width, int height){
        Image img = null;
        try(Surface sms = Surface.makeRasterN32Premul(width, height)) {
            sms.getCanvas().setMatrix(Matrix33.makeScale(1f * width / image.getWidth(), 1f * height / image.getHeight())).drawImage(image, 0, 0);
            img = sms.makeImageSnapshot();
        }
        return img;
    }

    /***
     * 绘制缩放图形
     * @param canvas
     * @param image
     * @param x 被绘制的坐标
     * @param y
     * @param w 缩放后的高宽
     * @param h
     * @return
     */
    public static Canvas drowScaleImage(Canvas canvas, Image image, float x, float y, float w, float h){
        canvas.save();
        canvas.translate(x,y);
        canvas.setMatrix(Matrix33.makeScale(1f * w / image.getWidth(), 1f * h / image.getHeight())).drawImage(image, 0, 0);
        canvas.restore();
        return canvas;
    }

    /***
     * 剪切矩形 如果直接绘制建议使用 drowCutImage
     * @param image
     * @param left
     * @param top
     * @param width
     * @param height
     * @return
     */
    public static Image getCutImage(Image image, int left, int top, int width, int height){
        Image img;
        try(Surface sms = Surface.makeRasterN32Premul(width, height)) {
            sms.getCanvas().drawImage(image, -1 * left, -1 * top);
            img =  sms.makeImageSnapshot();
        }
        return img;
    }

    /***
     * 绘制裁切图形
     * @param canvas
     * @param image
     * @param x 被绘制的位置(底图)
     * @param y
     * @param l 要绘制的图片(上层图)
     * @param t
     * @param width 宽
     * @param height 高
     * @return
     */
    public static Canvas drowCutImage(Canvas canvas, Image image, float x,  float y, int l, int t, int width, int height){
        canvas.save();
        canvas.translate(x,y);
        canvas.clipRect(Rect.makeXYWH(0, 0, width,height));
        canvas.drawImage(image, -1 * l, -1 * t);
        canvas.restore();
        return canvas;
    }

    /***
     * 读取本地图片文件
     * @param path
     * @return
     * @throws IOException
     */
    public static Image fileToImage(String path) throws IOException {
        File f = new File(path);
        long ln = f.length();
        byte[] date = new byte[Math.toIntExact(ln)];
        FileInputStream in = new FileInputStream(f);
        in.read(date);
        return Image.makeFromEncoded(date);
    }

    /***
     * 剪切圆角矩形 如果直接绘制建议使用 drowRRectImage
     * @param image
     * @param w
     * @param h
     * @param r
     * @return
     */
    public static Image getRRectImage( Image image, float w, float h, float r){
        Image img;
        try(Surface surface = Surface.makeRasterN32Premul(((int) w), ((int) h))) {
            var canvas = surface.getCanvas();
            canvas.clipRRect(RRect.makeNinePatchXYWH(0, 0, w, h, r, r, r, r), true);
            canvas.drawImage(image, 0, 0);
            img = surface.makeImageSnapshot();
        }
        return img;
    }

    /***
     * 绘制为圆角矩形(默认画笔)
     * @param canvas
     * @param image
     * @param x
     * @param y
     * @param r
     * @return
     */
    public static Canvas drowRRectImage(Canvas canvas, Image image, float x, float y , float r){
        drowRRectImage(canvas,image,x,y,r,null);
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
    public static Canvas drowRRectImage(Canvas canvas, Image image, float x, float y , float r, Paint p){
        canvas.save();
        canvas.translate(x,y);
        canvas.clipRRect(RRect.makeNinePatchXYWH(0,0,image.getWidth(),image.getHeight(),r,r,r,r), false);
        canvas.drawImage(image, 0,0,p);
        canvas.restore();
        return canvas;
    }

    /***
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
    public static Canvas drowCutRRectImage(Canvas canvas, Image image, float dx, float dy , float fx, float fy, float w, float h, float r){
        drowCutRRectImage(canvas, image, dx, dy, fx, fy, w, h, r,null);
        return canvas;
    }
    public static Canvas drowCutRRectImage(Canvas canvas, Image image, float dx, float dy , float fx, float fy, float w, float h, float r, Paint p){
        canvas.save();
        canvas.translate(dx,dy);
        canvas.clipRRect(RRect.makeNinePatchXYWH(0,0,w,h,r,r,r,r), true);
        canvas.drawImage(image, -fx,-fy, p);
        canvas.restore();
        return canvas;
    }

    /***
     * 裁剪绘制图形
     * @param canvas
     * @param img
     * @param dx 针对底图位置偏移x,y(被绘制的对象上)
     * @param dy
     * @param fx 上层图偏移x,y(要绘制的)
     * @param fy
     * @param width
     * @param height
     * @return
     */
    public static Canvas cutImage(Canvas canvas, Image img, float dx, float dy, float fx, float fy, float width, float height){
        canvas.save();
        canvas.translate(dx,dy);
        canvas.clipRect(Rect.makeXYWH(0, 0, width,height));
        canvas.drawImage(img,-fx, -fy);
        canvas.restore();

        canvas.drawRect(Rect.makeLTRB(10,10,10,10),new Paint().setARGB(255,255,255,255));
        return canvas;
    }

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
    public static Canvas drowSvg(Canvas canvas, SVGDOM svg, float x, float y, float width, float height, SVGPreserveAspectRatioAlign svgPreserveAspectRatioAlign, SVGPreserveAspectRatioScale svgPreserveAspectRatioScale){
        canvas.save();
        canvas.translate(x,y);
        canvas.clipRect(Rect.makeXYWH(0, 0, width,height));
        if(x == 0 && y == 0)
                canvas.clear(Color.makeARGB(100,255,0,0));
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
     * svg(默认右上角),保持横纵比缩放保留空白 绘制
     * @param canvas
     * @param svg
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    public static Canvas drowSvg(Canvas canvas, SVGDOM svg, float x, float y, float width, float height){
        return drowSvg(canvas, svg, x, y, width, height, SVGPreserveAspectRatioAlign.XMIN_YMIN, SVGPreserveAspectRatioScale.SLICE);
    }

    /***
     * 下载svg 大概不用再这里
     * @param path 下载url
     * @return svg对象
     * @throws IOException
     */
    public static SVGDOM lodeNetWorkSVGDOM(String path) throws IOException {
        URL url = new URL(path);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.connect();
        InputStream cin = httpConn.getInputStream();
        byte[] svgbytes = cin.readAllBytes();
        cin.close();
        return new SVGDOM(Data.makeFromBytes(svgbytes));
    }

    /***
     * 绘制模糊效果
     * @param canvas
     * @param x
     * @param y
     * @param w
     * @param h
     * @param radius 模糊程度 推荐10-20之间
     */
    public static void drawBlur(Canvas canvas, int x, int y, int w, int h,  int radius) {
        try (Bitmap bitmap = new Bitmap()) {
            bitmap.allocPixels(ImageInfo.makeS32(x+w,y+h, ColorAlphaType.OPAQUE));
            canvas.readPixels(bitmap, x,y);
            if (true) {

                try (var shader = bitmap.makeShader();
                     var blur   = ImageFilter.makeBlur(radius, radius, FilterTileMode.REPEAT);
                     var fill   = new Paint().setShader(shader).setImageFilter(blur))
                {
                    canvas.save();
                    canvas.translate(x,y);
                    canvas.drawRect(Rect.makeXYWH(0, 0,w, h), fill);
                    canvas.restore();
                }
            }
        }
    }

    /***
     * 绘制圆角模糊效果
     * @param canvas
     * @param x
     * @param y
     * @param w
     * @param h
     * @param radius 模糊程度 推荐10-20之间
     * @param r 圆角半径
     */
    public static void drawRBlur(Canvas canvas, int x, int y, int w, int h,  int radius,int r) {
        try (Bitmap bitmap = new Bitmap()) {
            bitmap.allocPixels(ImageInfo.makeS32(x+w,y+h, ColorAlphaType.OPAQUE));
            canvas.readPixels(bitmap, x,y);
            if (true) {

                try (var shader = bitmap.makeShader();
                     var blur   = ImageFilter.makeBlur(radius, radius, FilterTileMode.CLAMP);
                     var fill   = new Paint().setShader(shader).setImageFilter(blur))
                {
                    canvas.save();
                    canvas.translate(x,y);
                    canvas.drawRRect(RRect.makeNinePatchXYWH(0,0,w,h,r,r,r,r), fill);
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
    public static void drowTextStyel(Canvas canvas, int x, int y, String s, TextStyle ts){
        TextStyle f = new TextStyle();
        try (ParagraphStyle ps   = new ParagraphStyle();
             ParagraphBuilder pb = new ParagraphBuilder(ps, new FontCollection().setDefaultFontManager(FontMgr.getDefault()));)
        {
            pb.pushStyle(ts);
            pb.addText(s);
            try (Paragraph p = pb.build();) {
                p.layout(Float.POSITIVE_INFINITY);
                p.paint(canvas, x, y);
            }
        }
    }

    /***
     * 获取ppy 国旗svg的接口,
     * @param ct 国家缩写
     * @return 国旗url
     */
    public static String getFlagUrl(String ct){
        int A =  0x1f1e6;
        char x1 = ct.charAt(0);
        char x2 = ct.charAt(1);
        int s1 = A + x1-'A';
        int s2 = A + x2-'A';
        return "https://osu.ppy.sh/assets/images/flags/"+Integer.toHexString(s1)+"-"+Integer.toHexString(s2)+".svg";
    }

    /***
     * 生成六边形路径
     * @param size 基准大小
     * @param circle_width 转折点大小
     * @param point 六点数据 长度必须为6 范围[0,1]
     * @return path[0]六边形路径   path[1]转折点路径
     */
    public static Path[] creat6(float size, float circle_width, float... point){
        if (point.length != 6)return null;
        var path = new org.jetbrains.skija.Path();
        for (int i = 0; i < 6; i++) {
            if (point[i]<0) point[i] = 0;
            if (point[i]>1) point[i] = 1;
        }
        float[]ponX = new float[6];
        float[]ponY = new float[6];
        ponX[0] = -size*point[0]*0.5f;
        ponY[0] = -size*point[0]*0.866f;
        ponX[1] = size*point[1]*0.5f;
        ponY[1] = -size*point[1]*0.866f;
        ponX[2] = size*point[2];
        ponY[2] = 0 ;
        ponX[3] = size*point[3]*0.5f;
        ponY[3] = size*point[3]*0.866f;
        ponX[4] = -size*point[4]*0.5f;
        ponY[4] = size*point[4]*0.866f;
        ponX[5] = -size*point[5];
        ponY[5] = 0 ;
        path.moveTo(ponX[0],ponY[0]);
        path.lineTo(ponX[1],ponY[1]);
        path.lineTo(ponX[2],ponY[2]);
        path.lineTo(ponX[3],ponY[3]);
        path.lineTo(ponX[4],ponY[4]);
        path.lineTo(ponX[5],ponY[5]);
        path.closePath();
        if (circle_width == 0){
            return new Path[]{path};
        }
        Path path1 = new Path();
        path1.addCircle(ponX[0],ponY[0],circle_width);
        path1.addCircle(ponX[1],ponY[1],circle_width);
        path1.addCircle(ponX[2],ponY[2],circle_width);
        path1.addCircle(ponX[3],ponY[3],circle_width);
        path1.addCircle(ponX[4],ponY[4],circle_width);
        path1.addCircle(ponX[5],ponY[5],circle_width);
        return new Path[]{path,path1};
    }

    /***
     * ppy星数->色彩 算法
     * @param star 星数
     * @return 颜色rgb的int按位表示值,
     */
    public static int getStartColot(float star) {
        var starts = new float[]{1.5f,2f,2.5f,3.375f,4.625f,5.875f,7,8};
        var colorgroup = new int[][]{
                {79,192,255},
                {79,255,213},
                {124,255,79},
                {246,240,92},
                {255,128,104},
                {255,60,113},
                {101,99,222},
                {24,21,142},
        };
        int imax = starts.length-1,imin = 0;
        if(star<=starts[imin]) return (0xFF) << 24|(colorgroup[imin][0]<<16)|(colorgroup[imin][1]<<8)|(colorgroup[imin][2]);
        if(star>=starts[imax]) return (0xFF) << 24|(0<<16)|(0<<8)|(0);
        while(imax - imin>1){
            int t = (imax+imin)/2;
            if(starts[t]>star){
                imax = t;
            }else if(starts[t]<star){
                imin = t;
            }else {
                return (0xFF) << 24|(colorgroup[t][0]<<16)|(colorgroup[t][1]<<8)|(colorgroup[t][2]);
            }
        }
        float dy = (star - starts[imin])/(starts[imax] - starts[imin]);
        int[] caa = {
                (int)(dy*(colorgroup[imax][0] - colorgroup[imin][0])+colorgroup[imin][0]),
                (int)(dy*(colorgroup[imax][1] - colorgroup[imin][1])+colorgroup[imin][1]),
                (int)(dy*(colorgroup[imax][2] - colorgroup[imin][2])+colorgroup[imin][2]),
        };

        return (0xFF) << 24|(caa[0]<<16)|(caa[1]<<8)|(caa[2]);
    }

    /***
     * 图片颜色主色提取 未完成
     * @param image 输入图片
     * @return 色组
     */
    public static Color[] getMainColor(Image image) {
        Bitmap bitmap = Bitmap.makeFromImage(image);
        int x_length = bitmap.getWidth();
        int y_length = bitmap.getHeight();
        if (x_length * y_length >= Integer.MAX_VALUE) return null;
        int r,g,b;
        for (int x_index = 0; x_index < x_length; x_index++) {
            for (int y_index = 0; y_index < y_index; y_index++) {
                r = Color.getR(bitmap.getColor(x_index,y_index));
                g = Color.getG(bitmap.getColor(x_index,y_index));
                b = Color.getB(bitmap.getColor(x_index,y_index));
            }
        }
        return new Color[3];
    }

}
