package com.now.nowbot.skiademo;

import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class a {
    public static void main(String[] args) throws IOException {

        int width = 1200;
        int height = 900;
        //创建画布
        Surface surface = Surface.makeRasterN32Premul(width, height);
        //得到画纸,也可以叫画笔
        Canvas canvas = surface.getCanvas();
        //清空/重置画面
        canvas.clear(Color.makeRGB(255,255,255));

        drawImg(canvas, 0,0);

        drawImgWithAlpha(canvas,200,0);

        //移动笔坐边
        canvas.save(); //注意,save只会记录 坐标,缩放
        canvas.translate(256,0);
        Image head = SkiaImageUtil.getImage("https://a.ppy.sh/17064371?1622380408.jpeg"); //加载网图
        canvas.drawImage(head, 0, 0); //画笔偏移 x:256
        canvas.drawImage(head, 256, 0); //在画笔偏移基础上,再偏移x:256 y:0
        canvas.restore();// 坐标,缩放 恢复到上次save

        canvas.save();
        canvas.translate(0, 256);
//        图片缩放
        Image mImage = SkiaUtil.getScaleImage(head, 100,100);
        canvas.drawImage(mImage, 0,0);

        //图片裁剪
        Image cutImage = SkiaUtil.getCutImage(head, 0, 0, 100, 100);
        canvas.drawImage(cutImage, 100,0);
        //文字
        Typeface typeface = Typeface.makeDefault();
        Font font = new Font(typeface, 30);
        TextLine line = TextLine.make("字",font);
        canvas.drawRect(Rect.makeXYWH(0,0,line.getWidth(), line.getHeight()), new Paint().setARGB(50, 0,0,0));
        canvas.drawRect(Rect.makeXYWH(line.getWidth(),0,line.getWidth(), line.getXHeight()), new Paint().setARGB(50, 255,0,0));
        canvas.drawRect(Rect.makeXYWH(line.getWidth()*2,0,line.getWidth(), line.getCapHeight()), new Paint().setARGB(50, 0,255,0));
        canvas.drawTextLine(line,0,line.getHeight()-line.getXHeight(),new Paint().setARGB(255,255,0,255));
        canvas.restore();
        //输出

        try {
            Files.write(Path.of("D:/out.png"),
                    surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG).getBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void drawImg(Canvas canvas, int x, int y) throws IOException {
        //绘制图片
        Image head = SkiaImageUtil.getImage("https://a.ppy.sh/17064371?1622380408.jpeg"); //加载网图
        canvas.drawImage(
                head,       //图
                0,     //x偏移
                0    //y
        );
    }
    private static void drawImgWithAlpha(Canvas canvas, int x, int y) throws IOException {
        //绘制图片
        Image head = SkiaImageUtil.getImage("https://a.ppy.sh/17064371?1622380408.jpeg"); //加载网图
        canvas.drawImage(head,0,0,
                new Paint().setAlpha(150) //设置透明的
        );
    }

    public static void other() {
        Surface surface = Surface.makeRasterN32Premul(500,500);
        var canvas = surface.getCanvas();
        canvas.clear(Color.makeRGB(255,255,255));
        canvas.save();

        //渐变色
        Shader shader = Shader.makeLinearGradient(0,0,0,500, new int[]{Color.makeARGB(255, 173,83,137), Color.makeARGB(170,60,16,83)});
        //矩形
        canvas.drawRect(Rect.makeWH(500,500), new Paint().setShader(shader));

        canvas.translate(160, 230);
        Paint paint = new Paint().setARGB(290,170,35,99);
        //画圆
        canvas.drawCircle(0,0,50,paint);
        canvas.restore();

        canvas.translate(200,200);
        canvas.rotate(30);  //设定旋转,角度90为直角
        shader = Shader.makeLinearGradient(0,0,100,100, new int[]{Color.makeARGB(0, 255, 255, 255), Color.makeARGB(170,24,54,154)});
        //圆角矩形
        canvas.drawRRect(RRect.makeXYWH(0,0,100,100,10,10,10,10), new Paint().setShader(shader));
        canvas.restore();


        var data = surface.makeImageSnapshot().encodeToData().getBytes();
        try {
            Files.write(Path.of("D:/jb.png"), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
