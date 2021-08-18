package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import org.jetbrains.skija.*;
import org.jetbrains.skija.paragraph.*;
import org.jetbrains.skija.svg.*;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SkiaUtil {
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
                byte[] data = cin.readAllBytes();
                cin.close();
                Files.createFile(pt);
                Files.write(pt,data);
                System.gc();
                return Image.makeFromEncoded(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Image getScaleImage(Image image, int width, int height){
        Image img = null;
        try(Surface sms = Surface.makeRasterN32Premul(width, height)) {
            sms.getCanvas().setMatrix(Matrix33.makeScale(1f * width / image.getWidth(), 1f * height / image.getHeight())).drawImage(image, 0, 0);
            img = sms.makeImageSnapshot();
        }
        return img;
    }
    public static Canvas drowScaleImage(Canvas canvas, Image image, float x, float y, float w, float h){
        canvas.save();
        canvas.translate(x,y);
        canvas.setMatrix(Matrix33.makeScale(1f * w / image.getWidth(), 1f * h / image.getHeight())).drawImage(image, 0, 0);
        canvas.restore();
        return canvas;
    }
    public static Image getCutImage(Image image, int left, int top, int width, int height){
        Image img;
        try(Surface sms = Surface.makeRasterN32Premul(width, height)) {
            sms.getCanvas().drawImage(image, -1 * left, -1 * top);
            img =  sms.makeImageSnapshot();
        }
        return img;
    }
    public static Canvas drowCutImage(Canvas canvas, Image image, float x,  float y, int l, int t, int width, int height){
        canvas.save();
        canvas.translate(x,y);
        canvas.clipRect(Rect.makeXYWH(0, 0, width,height));
        canvas.drawImage(image, -1 * l, -1 * t);
        canvas.restore();
        return canvas;
    }
    public static Image fileToImage(String path) throws IOException {
        File f = new File(path);
        long ln = f.length();
        byte[] data = new byte[Math.toIntExact(ln)];
        FileInputStream in = new FileInputStream(f);
        in.read(data);
        return Image.makeFromEncoded(data);
    }
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
    public static Canvas drowRRectImage(Canvas canvas, Image image, float x, float y , float r){
        drowRRectImage(canvas,image,x,y,r,null);
        return canvas;
    }
    public static Canvas drowRRectImage(Canvas canvas, Image image, float x, float y , float r, Paint p){
        canvas.save();
        canvas.translate(x,y);
        canvas.clipRRect(RRect.makeNinePatchXYWH(0,0,image.getWidth(),image.getHeight(),r,r,r,r), false);
        canvas.drawImage(image, 0,0,p);
        canvas.restore();
        return canvas;
    }
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
    public static Canvas cutImage(Canvas canvas, Image img, float dx, float dy, float fx, float fy, float width, float height){
        canvas.save();
        canvas.translate(dx,dy);
        canvas.clipRect(Rect.makeXYWH(0, 0, width,height));
        canvas.drawImage(img,-fx, -fy);
        canvas.restore();

        canvas.drawRect(Rect.makeLTRB(10,10,10,10),new Paint().setARGB(255,255,255,255));
        return canvas;
    }
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
    public static Canvas drowSvg(Canvas canvas, SVGDOM svg, float x, float y, float width, float height){
        return drowSvg(canvas, svg, x, y, width, height, SVGPreserveAspectRatioAlign.XMIN_YMIN, SVGPreserveAspectRatioScale.SLICE);
    }
    public static SVGDOM lodeNetWorkSVGDOM(String path) throws IOException {
        URL url = new URL(path);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.connect();
        InputStream cin = httpConn.getInputStream();
        byte[] svgbytes = cin.readAllBytes();
        cin.close();
        return new SVGDOM(Data.makeFromBytes(svgbytes));
    }
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
    public static void drowTextStyel(Canvas canvas, int x, int y, String s, TextStyle ts){
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
    public static String getFlagUrl(String ct){
        int A =  0x1f1e6;
        char x1 = ct.charAt(0);
        char x2 = ct.charAt(1);
        int s1 = A + x1-'A';
        int s2 = A + x2-'A';
        return "https://osu.ppy.sh/assets/images/flags/"+Integer.toHexString(s1)+"-"+Integer.toHexString(s2)+".svg";
    }
    public static Path creat6(float size, float lineWidth, float p1, float p2, float p3, float p4, float p5, float p6){
        var p = new Path();
        if(p1>1) p1 = 1;
        if(p2>1) p2 = 1;
        if(p3>1) p3 = 1;
        if(p4>1) p4 = 1;
        if(p5>1) p5 = 1;
        if(p6>1) p6 = 1;
        if(p6<0) p6 = 0;
        if(p5<0) p5 = 0;
        if(p4<0) p4 = 0;
        if(p3<0) p3 = 0;
        if(p2<0) p2 = 0;
        if(p1<0) p1 = 0;
        float[]ponX = new float[6];
        float[]ponY = new float[6];
        ponX[0] = -size*p1*0.5f;
        ponY[0] = -size*p1*0.866f;
        ponX[1] = size*p2*0.5f;
        ponY[1] = -size*p2*0.866f;
        ponX[2] = size*p3;
        ponY[2] = 0 ;
        ponX[3] = size*p4*0.5f;
        ponY[3] = size*p4*0.866f;
        ponX[4] = -size*p5*0.5f;
        ponY[4] = size*p5*0.866f;
        ponX[5] = -size*p6;
        ponY[5] = 0 ;
        p.moveTo(ponX[0],ponY[0]);
        p.lineTo(ponX[1],ponY[1]);
        p.lineTo(ponX[2],ponY[2]);
        p.lineTo(ponX[3],ponY[3]);
        p.lineTo(ponX[4],ponY[4]);
        p.lineTo(ponX[5],ponY[5]);
        p.closePath();
        p.addCircle(ponX[0],ponY[0],lineWidth);
        p.addCircle(ponX[1],ponY[1],lineWidth);
        p.addCircle(ponX[2],ponY[2],lineWidth);
        p.addCircle(ponX[3],ponY[3],lineWidth);
        p.addCircle(ponX[4],ponY[4],lineWidth);
        p.addCircle(ponX[5],ponY[5],lineWidth);

        return p;
    }
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
}
