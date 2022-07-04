package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HCardBuilder extends PanelBuilder{
    public static final ArrayList<Float> F24L = new ArrayList<>();
    public static final ArrayList<Float> F36L = new ArrayList<>();

    public HCardBuilder(BpInfo info, int bpN) throws IOException {
        super(900,110);

        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,900,110,20),new Paint().setARGB(255,56,46,50));
        canvas.restore();


        //画评级指示矩形
        canvas.save();
        canvas.translate(700,0);
//                                                                                                                            这里我写了rank -> 颜色
//        canvas.drawRRect(RRect.makeXYWH(0,0,200,110,0,20,20,0),new Paint().setColor(SkiaUtil.getRankColor(info.getRank())));
        var colorA = SkiaUtil.getRankColor(info.getRank());
        var colorB = Color.makeRGB(Color.getR(colorA)-3, Color.getG(colorA)+5, Color.getB(colorA)-3);
        canvas.drawRRect(
                RRect.makeXYWH(0,0,200,110,0,20,20,0),
                new Paint().setShader(
                        Shader.makeLinearGradient(0,0,110,110,new int[]{colorA, colorB})
                )
        );
        canvas.restore();

        //画中间层背景图
        canvas.save();
        canvas.translate(160,0);
        canvas.drawRRect(RRect.makeXYWH(0,0,570,110,20),new Paint().setARGB(255,56,46,50));
        canvas.clipRRect(RRect.makeXYWH(0,0,570,110,20));
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
        Image HCardMainBG = SkiaImageUtil.getImage(info.getBeatmapset().getCovers().getList2x());
        Image HCardMainBGSC = SkiaImageUtil.getScaleCenterImage(HCardMainBG,176,110); //缩放至合适大小
        canvas.drawImage(HCardMainBGSC,0, 0,new Paint());
        canvas.restore();

        // mods
        int textMaxW = drawMods(info.getMods());
        //写文字
        drawText(textMaxW, info, bpN);

    }


/*
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
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);
        Font fontS48 = new Font(TorusSB, 48);

        TextLine TextLineH1 = TextLine.make("Fushimi Rio Drunk",fontS36); //标题行
        TextLine TextLineH2 = TextLine.make("Fushimi Rio"+' '+"//"+' '+"YourDad",fontS24); //作者谱师行
        TextLine TextLineH3 = TextLine.make("[Phirida's Insane]"+" - b"+"1146831",fontS24); //难度谱号Bid行
        TextLine TextLineH4L = TextLine.make("727",fontS48); //PP行
        TextLine TextLineH4R = TextLine.make("PP",fontS24); //固定的PP

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

 */

    private int drawMods(List<String> mods){
        if (mods.size() == 0) return 500;
        canvas.save();
        canvas.translate(620,23);
        int mx = 60;
        for(var mod : mods){
            try {
                var modImg = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/Mods/" + mod + ".png");
                canvas.drawImage(modImg,0,0);
                canvas.translate(-40,0);
                mx += 40;
            } catch (IOException ignored) {

            }
        }
        canvas.restore();
        return 500 - mx;
    }

    private void drawText(int maxWidth, BpInfo info, int bpN){
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);
        Font fontS48 = new Font(TorusSB, 48);
        StringBuilder sb = new StringBuilder();
        float allWidth = 0;

        //写标题 第一行
        TextLine h1;
        int backL = 0;
        float pointW32 = 3*F36L.get('.');
        var titleChar = info.getBeatmapset().getTitle().toCharArray();
        //计算字符长度
        for (var thisChar : titleChar) {
            if (allWidth > maxWidth){
                break;
            }
            sb.append(thisChar);
            allWidth += F36L.get(thisChar);
            if ((allWidth + pointW32) < maxWidth){
                backL++;
            }
        }
        if (allWidth > maxWidth){
            sb.delete(backL,sb.length());
            sb.append("...");
        }
        h1 = TextLine.make(sb.toString(), fontS36);

        sb.delete(0,sb.length());
        allWidth = 0;
        backL=0;
        float pointW24 = 3*F24L.get('.');
        //标题行
        TextLine h2;
        {
            sb.append(info.getBeatmapset().getCreator());
            h2 = TextLine.make(info.getBeatmapset().getCreator(), fontS24);
            allWidth = (h2.getWidth() + 2*F24L.get(' ') + 2 * F24L.get('/') + pointW24);
            if (allWidth < maxWidth) {
                sb.insert(0, " // ");
                float tmax = maxWidth - (h2.getWidth() + 2 * F24L.get('/'));
                allWidth = 0;
                StringBuilder sbt = new StringBuilder();
                var artistChar = info.getBeatmapset().getArtist().toCharArray();
                for (var thisChar : artistChar) {
                    if (allWidth > tmax) {
                        break;
                    }
                    allWidth += F24L.get(thisChar);
                    sbt.append(thisChar);
                    if ((pointW24 + allWidth) < tmax) {
                        backL++;
                    }
                }
                if ((pointW24 + allWidth) > tmax) {
                    sbt.delete(backL, sbt.length());
                    sbt.append("...");
                }
                h2.close();
                h2 = TextLine.make(sbt.append(sb).toString(), fontS24);
            }
        }

        //难度谱号Bid行
        TextLine h3;
        {
            sb.delete(0, sb.length());
            h3 = TextLine.make("[] - bp" + bpN, fontS24);
            allWidth = 0;
            backL = 0;
            float tmax = maxWidth - h3.getWidth();
            var versionChar = info.getBeatmap().getVersion().toCharArray();
            for (var thisChar : versionChar){
                if (allWidth > tmax) {
                    break;
                }
                allWidth += F24L.get(thisChar);
                sb.append(thisChar);
                if ((pointW24 + allWidth) < tmax) {
                    backL++;
                }
            }
            if ((pointW24 + allWidth) > tmax) {
                sb.delete(backL, sb.length());
                sb.append("...");
            }
            sb.insert(0,'[').append("] - bp").append(bpN);
            h3.close();
            h3 = TextLine.make(sb.toString(), fontS24);
        }

        //pp
        TextLine h4l = TextLine.make(String.valueOf(info.getPp().intValue()),fontS48); //PP行
        TextLine h4r = TextLine.make("PP",fontS24); //固定的PP

        float TextLineH4LW = h4l.getWidth();
        float TextLineH4RW = h4r.getWidth();

        canvas.save();
        canvas.translate(210,10);
        canvas.drawTextLine(h1,0,h1.getHeight()-h1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(0,40);
        canvas.drawTextLine(h2,0,h2.getHeight()-h2.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(0,30);
        canvas.drawTextLine(h3,0,h3.getHeight()-h3.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(520,-40);
        canvas.translate((170-TextLineH4LW-TextLineH4RW)/2f,0);//居中处理，170减大减小除以2

        if (Objects.equals(info.getRank(), "XH") || Objects.equals(info.getRank(), "X")) {
            canvas.drawTextLine(h4l, 0, h4l.getHeight() - h4l.getXHeight(), new Paint().setARGB(255, 43,34,39));
            canvas.translate(TextLineH4LW, 0);
            canvas.drawTextLine(h4r, 0, h4l.getHeight() - h4l.getXHeight(), new Paint().setARGB(255, 43,34,39));
        } else {
                canvas.drawTextLine(h4l,0,h4l.getHeight()-h4l.getXHeight(),new Paint().setARGB(255,255,255,255));
                canvas.translate(TextLineH4LW,0);
                canvas.drawTextLine(h4r,0,h4l.getHeight()-h4l.getXHeight(),new Paint().setARGB(255,255,255,255));
        }
        canvas.restore();
    }

    public Image build(){
        return super.build(0);
    }
}
