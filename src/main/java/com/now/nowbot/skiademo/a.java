package com.now.nowbot.skiademo;

import com.now.nowbot.util.SkiaCanvasUtil;
import com.now.nowbot.util.SkiaImageUtil;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class a {

    Paint t = new Paint().setAntiAlias(true); // 设置抗锯齿
    public static void main(String[] args) throws IOException {

        int width = 1200;
        int height = 900;
        var info = ImageInfo.makeN32Premul(width, height);
        //创建画布
        Surface surface = Surface.makeRaster(info);
        //得到画纸,也可以叫画笔 canvas
        Canvas canvas = surface.getCanvas();

        //canvas常用操作
        canvas.clear(Color.makeRGB(255,255,255));  //清空/重置画面
        canvas.save(); //保存状态 注意,save只会记录设定 坐标,缩放  并不会保存实际画面,draw上去的无法回滚
        canvas.restore(); //坐标,缩放等 恢复至上次保存的状态
        //也可以记录save的值,回滚至指定save的状态
        var saveId = canvas.save();
        canvas.save();
        canvas.save();
        canvas.save();
        canvas.restoreToCount(saveId);//恢复到saveId记录的那一个的状态

        canvas.translate(0,0); //调整坐标,可restore()回滚
        canvas.scale(0,0);//设定缩放 可restore()回滚
        canvas.rotate(0);//设定旋转,以当前canvas的坐标为中心 可restore()回滚
        canvas.skew(0,0);//拉伸,效果很鬼畜,不建议使用 可restore()回滚
        //注:设定缩放或者旋转后,draw方法的坐标偏移也会等比例变化,但是如果先canvas.translate()设定好偏移,再设置scale/rotate则不受影响,对精确的坐标偏移强烈建议使用canvas.translate()

        //Matrix增强canvas的功能,通过调整参数Matrix33,能实现translate,scale,rotate,rotate所有的效果
        canvas.setMatrix(Matrix33.makeTranslate(1f,1f)); //设置偏移效果
        canvas.setMatrix(Matrix33.makeScale(1f,1f)); //设置缩放效果
        canvas.setMatrix(Matrix33.makeRotate(1f,new Point(0,0))); //设置旋转效果 可以额外设定中心点
        canvas.setMatrix(Matrix33.makeSkew(1f,1f)); //没用过
        canvas.resetMatrix(); // 取消所有的setMatrix效果

        //剩下的是draw各种组件,常用的 绘制
//        canvas.drawImage() 画图
//        canvas.drawTextLine() 写字
//        canvas.drawRect() 画框框
//        canvas.drawRRect() 画圆角框框

        //canvas.clipXXX 设定范围,类似与ps的选区,所有的draw只在选区内生效
        canvas.clipRRect(RRect.makeXYWH(0,0,1920,1080,10));
        canvas.clipRect(Rect.makeWH(1920,1080));

        if (testcanvas()) return;
        /********************下面的选看***********************/
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
        Image mImage = SkiaImageUtil.getScaleImage(head, 100,100);
        canvas.drawImage(mImage, 0,0);

        //图片裁剪
        Image cutImage = SkiaImageUtil.getCutImage(head, 0, 0, 100, 100);
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

        //划弧型
        canvas.save();
        canvas.translate(200,200);
        canvas.drawArc(
                0,0,  //所在弧形"补全(椭)圆"的外接矩形 左上顶点坐标
                50,50, //所在弧形"补全(椭)圆"的外接矩形 尺寸
                10,70,// 起始角度 从圆心往右顺时针 起始/结束 角度制,允许负数,0-360一圈
                true, //是否填充三角区域
                new Paint().setARGB(255,15,34,74)
                );
        canvas.restore();

        var data = surface.makeImageSnapshot().encodeToData().getBytes();
        try {
            Files.write(Path.of("D:/jb.png"), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static boolean testcanvas() throws IOException {
        var img = SkiaImageUtil.getImage("https://a.ppy.sh/17064371?1622380408.jpeg");
        img = SkiaImageUtil.getScaleCenterImage(img, 100,100);

        Surface surface = Surface.makeRasterN32Premul(900,600);
        var canvas = surface.getCanvas();
        var paint = new Paint();
        canvas.clear(Color.makeRGB(0,0,0));
        canvas.save();
        canvas.drawImage(img,0,0);
        canvas.rotate(45);
        canvas.drawImage(img,100,0);
        canvas.restore();
        canvas.save();
        canvas.translate(400,0);
        canvas.rotate(45);
        canvas.scale(2,2);
        canvas.drawImage(img,0,0);
        canvas.restore();
        canvas.save();
        canvas.translate(400,100);
        canvas.drawImage(img,0,0);
        canvas.restore();
        int size = 50;
        canvas.drawRect(Rect.makeWH(size,size),new Paint().setARGB(150,255,0,0));
        Font font = new Font(Typeface.makeDefault(),size);
        System.out.println(font.getSize());
        SkiaCanvasUtil.drawTextLeft(canvas,"abcABC#@#$你好",font,new Paint().setColor(Color.makeRGB(0,255,0)));
        Files.write(Path.of("D:/tests.png"),surface.makeImageSnapshot().encodeToData().getBytes());
        return true;
    }
}
