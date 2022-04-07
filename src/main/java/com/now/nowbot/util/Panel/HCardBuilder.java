package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HCardBuilder extends PanelBuilder{

    public HCardBuilder(BpInfo info) throws IOException {
        super(900,100);

        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,900,110,20),new Paint().setARGB(255,56,46,50));
        canvas.restore();


        //画评级指示矩形
        canvas.save();
        canvas.translate(700,0);
//                                                                                                                            这里我写了rank -> 颜色
//        canvas.drawRRect(RRect.makeXYWH(0,0,200,110,0,20,20,0),new Paint().setColor(SkiaUtil.getRankColor(info.getRank())));
        canvas.drawRRect(
                RRect.makeXYWH(0,0,200,110,0,20,20,0),
                new Paint().setShader(
                        Shader.makeLinearGradient(0,0,900,0,new int[]{SkiaUtil.getRankColor(info.getRank()),SkiaUtil.getRankColor(info.getRank() + 1)})
                )
        );
        canvas.restore();

        //画中间层背景图
        canvas.save();
        canvas.translate(160,0);
        canvas.drawRRect(RRect.makeXYWH(0,0,570,110,20),new Paint().setARGB(255,56,46,50));
        canvas.clipRRect(RRect.makeXYWH(0,0,570,110,20));

        //取背景
        Image HCardLightBG = SkiaImageUtil.getImage(info.getBeatmapset().getCovers().getCover2x());
        Image HCardLightBGSC = SkiaImageUtil.getScaleCenterImage(HCardLightBG,620,140); //缩放至合适大小，这里放大了一点，以应对模糊带来的负面效果
        canvas.drawImage(HCardLightBGSC,0, 0,new Paint().setAlphaf(0.2f).setImageFilter(ImageFilter.makeBlur(5, 5, FilterTileMode.REPEAT)));
        canvas.restore();

        //画谱面难度色标指示矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,40,110,20,0,0,20),new Paint().setColor(SkiaUtil.getStartColor(info.getBeatmap().getDifficultyRating())));
        canvas.restore();

        //画主要缩略图
        canvas.save();
        canvas.translate(20,0);
        canvas.clipRRect(RRect.makeXYWH(0,0,176,110,20));

        //去背景
        Image HCardMainBG = SkiaImageUtil.getImage(info.getBeatmapset().getCovers().getList2x());
        Image HCardMainBGSC = SkiaImageUtil.getScaleCenterImage(HCardMainBG,176,110); //缩放至合适大小
        canvas.drawImage(HCardMainBGSC,0, 0,new Paint().setAlphaf(1.0f));
        canvas.restore();
    }



    public static void main(String[] args) throws IOException {
        int HCardWidth = 900;
        int HCardHeight = 110;
        Surface surface = Surface.makeRasterN32Premul(HCardWidth, HCardHeight);
        Canvas canvas = surface.getCanvas();

        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,900,110,20),new Paint().setARGB(255,56,46,50));
        canvas.restore();

        //画评级指示矩形
        canvas.save();
        canvas.translate(700,0);
        canvas.drawRRect(RRect.makeXYWH(0,0,200,110,0,20,20,0),new Paint().setColor(SkiaUtil.getRandomColor()));
        canvas.restore();

        //画中间层背景图
        canvas.save();
        canvas.translate(160,0);
        canvas.drawRRect(RRect.makeXYWH(0,0,570,110,20),new Paint().setARGB(255,56,46,50));
        canvas.clipRRect(RRect.makeXYWH(0,0,570,110,20));
        Image HCardLightBG = SkiaImageUtil.getImage("https://assets.ppy.sh/beatmaps/1166831/covers/cover@2x.jpg");
        Image HCardLightBGSC = SkiaImageUtil.getScaleCenterImage(HCardLightBG,620,140); //缩放至合适大小，这里放大了一点，以应对模糊带来的负面效果
        canvas.drawImage(HCardLightBGSC,0, 0,new Paint().setAlphaf(0.2f).setImageFilter(ImageFilter.makeBlur(5, 5, FilterTileMode.REPEAT)));
        canvas.restore();

        //画谱面难度色标指示矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,40,110,20,0,0,20),new Paint().setColor(SkiaUtil.getStartColor(5.42f)));// 星数
        canvas.restore();

        //画主要缩略图
        canvas.save();
        canvas.translate(20,0);
        canvas.clipRRect(RRect.makeXYWH(0,0,176,110,20));
        Image HCardMainBG = SkiaImageUtil.getImage("https://assets.ppy.sh/beatmaps/1166831/covers/list@2x.jpg");
        Image HCardMainBGSC = SkiaImageUtil.getScaleCenterImage(HCardMainBG,176,110); //缩放至合适大小
        canvas.drawImage(HCardMainBGSC,0, 0,new Paint());
        canvas.restore();

        //写标题
        Typeface TorusSB = Typeface.makeFromFile("F:/【osu! 文件大全】/【BOT相关】/nowbot/data/font/Torus-SemiBold.ttf");
        Font FontS24 = new Font(TorusSB, 24);
        Font FontS36 = new Font(TorusSB, 36);
        Font FontS48 = new Font(TorusSB, 48);

        TextLine TextLineH1 = TextLine.make("Fushimi Rio Drunk",FontS36); //标题行
        TextLine TextLineH2 = TextLine.make("Fushimi Rio"+' '+"//"+' '+"YourDad",FontS24); //作者谱师行
        TextLine TextLineH3 = TextLine.make("[Phirida's Insane]"+" - b"+"1146831",FontS24); //难度谱号Bid行
        TextLine TextLineH4L = TextLine.make("727",FontS48); //PP行
        TextLine TextLineH4R = TextLine.make("PP",FontS24); //固定的PP

        int TextLineH4LW = SkiaUtil.getTextImage("727",Typeface.makeFromFile("F:/【osu! 文件大全】/【BOT相关】/nowbot/data/font/Torus-SemiBold.ttf"),48, new Paint()).getWidth();
        int TextLineH4RW = SkiaUtil.getTextImage("PP",Typeface.makeFromFile("F:/【osu! 文件大全】/【BOT相关】/nowbot/data/font/Torus-SemiBold.ttf"),24, new Paint()).getWidth();

        canvas.save();
        canvas.translate(210,10);
        canvas.drawTextLine(TextLineH1,0,TextLineH1.getHeight()-TextLineH1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(0,40);
        canvas.drawTextLine(TextLineH2,0,TextLineH2.getHeight()-TextLineH2.getXHeight(),new Paint().setARGB(255,177,177,177));
        canvas.translate(0,30);
        canvas.drawTextLine(TextLineH3,0,TextLineH3.getHeight()-TextLineH3.getXHeight(),new Paint().setARGB(255,177,177,177));
        canvas.translate(520,-40);
        canvas.translate((170-TextLineH4LW-TextLineH4RW)/2f,0);//居中处理，170减大减小除以2
        canvas.drawTextLine(TextLineH4L,0,TextLineH4L.getHeight()-TextLineH4L.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(TextLineH4LW,0);
        canvas.drawTextLine(TextLineH4R,0,TextLineH4L.getHeight()-TextLineH4L.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();

        //加 mod 图标
        Image DT = SkiaImageUtil.getImage("F:/【osu! 文件大全】/【BOT相关】/nowbot/data/bg/ExportFileV3/Mods/DT.png");
        Image HD = SkiaImageUtil.getImage("F:/【osu! 文件大全】/【BOT相关】/nowbot/data/bg/ExportFileV3/Mods/HD.png");
        canvas.save();
        canvas.translate(620,23);//初始位置
        canvas.drawImage(DT,0,0);
        canvas.drawImage(HD,-40,0);//后续每个 mod 都与前面相隔 40px

        try{
            Files.write(Path.of("D:/output.png"),
            surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG).getBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Image build(){
        return super.build(0);
    }
}
