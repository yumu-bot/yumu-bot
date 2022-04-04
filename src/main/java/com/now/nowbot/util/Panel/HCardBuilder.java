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

        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,900,110,20),new Paint().setARGB(255,56,46,50));
        canvas.restore();

        //画评级指示矩形
        canvas.save();
        canvas.translate(700,0);
//                                                                                                                            这里我写了rank -> 颜色
        canvas.drawRRect(RRect.makeXYWH(0,0,200,110,0,20,20,0),new Paint().setColor(SkiaUtil.getRankColor(info.getRank())));
        canvas.restore();

        //画中间层背景图
        canvas.save();
        canvas.translate(160,0);
        canvas.drawRRect(RRect.makeXYWH(0,0,570,110,20),new Paint().setARGB(255,56,46,50));
        canvas.clipRRect(RRect.makeXYWH(0,0,570,110,20));
        //取背景
        Image HCardLightBG = SkiaImageUtil.getImage(info.getBeatmapset().getCovers().getCover2x());
        Image HCardLightBGSC = SkiaImageUtil.getScaleCenterImage(HCardLightBG,620,140); //缩放至合适大小，这里放大了一点，以应对模糊带来的负面效果
        canvas.drawImage(HCardLightBGSC,0, 0,new Paint().setAlphaf(0.2f).setImageFilter(ImageFilter.makeBlur(5, 5, FilterTileMode.REPEAT))
        );
        canvas.restore();


        //画谱面难度色标指示矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,40,110,20,0,0,20),new Paint().setColor(SkiaUtil.getStartColot(info.getBeatmap().getDifficultyRating())));
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
        Image HCardLightBG = SkiaImageUtil.getImage("https://assets.ppy.sh/beatmaps/1087774/covers/cover@2x.jpg");
        Image HCardLightBGSC = SkiaImageUtil.getScaleCenterImage(HCardLightBG,620,140); //缩放至合适大小，这里放大了一点，以应对模糊带来的负面效果
        canvas.drawImage(HCardLightBGSC,0, 0,new Paint().setAlphaf(0.2f).setImageFilter(ImageFilter.makeBlur(5, 5, FilterTileMode.REPEAT))
        );
        canvas.restore();

        //画谱面难度色标指示矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,40,110,20,0,0,20),new Paint().setColor(SkiaUtil.getStartColot(5.42f)));
        canvas.restore();

        //画主要缩略图
        canvas.save();
        canvas.translate(20,0);
        canvas.clipRRect(RRect.makeXYWH(0,0,176,110,20));
        Image HCardMainBG = SkiaImageUtil.getImage("https://assets.ppy.sh/beatmaps/1087774/covers/list@2x.jpg");
        Image HCardMainBGSC = SkiaImageUtil.getScaleCenterImage(HCardMainBG,176,110); //缩放至合适大小
        canvas.drawImage(HCardMainBGSC,0, 0,new Paint().setAlphaf(1.0f));
        canvas.restore();

        /*
        //写标题
        Typeface typeface = Typeface.makeDefault();
        Font FontH48 = new Font(typeface, 48);
        Font FontH36 = new Font(typeface, 36);
        Font FontH24 = new Font(typeface, 24);

        StringBuilder TextH1 = new StringBuilder();
        StringBuilder TextH2P1 = new StringBuilder();
        StringBuilder TextH2P2 = new StringBuilder();
        StringBuilder TextH3P1 = new StringBuilder();
        StringBuilder TextH3P2 = new StringBuilder();
        StringBuilder TextH4P1 = new StringBuilder();
        StringBuilder TextH4P2 = new StringBuilder();

        int TextH1L = SkiaUtil.getTextImage("标题行",SkiaUtil.getPuhuitiMedium(),20, new Paint()).getWidth();

        TextLine TextLH1 = TextLine.make("标题行",FontH36); //标题行
        TextLine TextLH2 = TextLine.make("M"+' '+"//"+' '+"",FontH24); //作者谱师行
        TextLine TextLH3 = TextLine.make("难度行"+"1087774",FontH24); //难度谱号Bid行
        TextLine TextLH4 = TextLine.make("162"+"PP",FontH48); //PP行

        */

        try{
            Files.write(Path.of("D:/output.png"),
            surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG).getBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
