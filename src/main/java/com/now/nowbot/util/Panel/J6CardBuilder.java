package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.beatmap.Mod;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;

public class J6CardBuilder extends PanelBuilder {

    Paint colorRRect = new Paint().setARGB(255,56,46,50);
    Paint colorGrey = new Paint().setARGB(255,170,170,170);
    Paint colorWhite = new Paint().setARGB(255,255,255,255);

    record modSum(int modInt, int sum){ }
    public J6CardBuilder(List<BpInfo> bps) {
        super(430, 335);

        drawBaseRRect();
        drawUserText(bps);
        drawPieChartOverlay();
    }

    private void drawBaseRRect() {
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawUserText(List<BpInfo> bps) {
        //画数据指标
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS36 = new Font(TorusSB, 36);

        TextLine Jlu1 = TextLine.make("BPA", fontS36);

        //画左上角 BPA
        canvas.save();
        canvas.translate(20,20);
        canvas.drawTextLine(Jlu1,0,Jlu1.getHeight()-Jlu1.getXHeight(),colorWhite);
        canvas.restore();

        var x = bps.stream().collect(Collectors.groupingBy(BpInfo::getMods, Collectors.counting()));
        var s = x.entrySet()
                .stream()
                .map(e -> {
                    int c = 0;
                    for (var mod : e.getKey()) {
                        var modE = Mod.fromStr(mod);
                        if (modE == Mod.Nightcore) modE = Mod.DoubleTime;
                        c |= modE.value;
                    }

                    c &= (  Mod.Easy.value | Mod.HalfTime.value | Mod.DoubleTime.value |
                            Mod.Hidden.value | Mod.HardRock.value | Mod.Flashlight.value);
                    return new modSum(c, Math.toIntExact(e.getValue()));
                })
                .sorted(Comparator.comparingInt(modSum::sum).reversed())
                .limit(5) //只取前五
                .toList();
        //然后这里需要画饼图，我整不会

        //注意 s已经拍好序了
//        var modsStr = modStr(s.get(0).modInt); //mod 组合,包括NM
//        s.get(0).sum  //这种组合的个数

        var color = new Paint().setARGB(255,255,255,125);
        drawArc(60, 365f*s.get(0).sum/100, color);
//        ...

        //这里可以写一个方法，重复利用下面的drawUserIndex来画出指标。

    }

    String modStr(int i){
        if (i == 0){
            return "NM";
        }
        StringBuilder sb = new StringBuilder();
        if (Mod.Easy.check(i)) sb.append("EZ");
        if (Mod.HalfTime.check(i)) sb.append("HT");
        if (Mod.DoubleTime.check(i)) sb.append("DT");
        if (Mod.Hidden.check(i)) sb.append("HD");
        if (Mod.HardRock.check(i)) sb.append("HR");
        if (Mod.Flashlight.check(i)) sb.append("FL");

        return sb.toString();
    }

    void drawArc(float start, float end, Paint paint) {
        canvas.drawArc(85, 105,
                200, 200,
                start, end,
                true, paint
        );
    }

    private void drawPieChartOverlay() {
        Image PieChart = null;
        try {
            PieChart = SkiaImageUtil.getImage(java.nio.file.Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-piechart-overlay.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        canvas.save();
        canvas.translate(85,105);
        canvas.drawImage(PieChart,0,0,new Paint());
        canvas.restore();
    }


    private void drawUserIndex(String Mod, int number) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);

        TextLine Ju = TextLine.make(String.valueOf(number), fontS36);
        TextLine Jb = TextLine.make(Mod, fontS24);

        canvas.save();
        canvas.translate(- Ju.getWidth() / 2,-30);
        canvas.drawTextLine(Ju,0,Ju.getHeight()-Ju.getXHeight(),colorWhite);
        canvas.translate((Ju.getWidth() - Jb.getWidth()) / 2,36);
        canvas.drawTextLine(Jb,0,Jb.getHeight()-Jb.getXHeight(),colorGrey);
        canvas.restore();
    }

    public Image build() {
        return super.build(20);
    }
}
