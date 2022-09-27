package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public class K3CardBuilder extends PanelBuilder {

    int retry_max;
    int retry_min;
    int retry_length;
    int failed_max;
    int failed_min;
    int failed_length;

    Paint colorRRect = new Paint().setARGB(255, 56, 46, 50);
    Paint colorDarkGrey = new Paint().setARGB(255, 100, 100, 100);
    Paint colorGrey = new Paint().setARGB(255, 170, 170, 170); //Meh 漏掉的小果
    Paint colorWhite = new Paint().setARGB(255, 255, 255, 255);
    Paint colorLightBlue = new Paint().setARGB(255, 141, 207, 244); //Perfect 300 良 大果
    Paint colorYellow = new Paint().setARGB(255, 254, 246, 103); //Great 50 小果
    Paint colorGoldenYellow = new Paint().setARGB(255, 255,204,34); //金黄色
    Paint colorGreen = new Paint().setARGB(255, 121, 196, 113); //Good 100 可 中果
    Paint colorBlue = new Paint().setARGB(255, 94, 138, 198); //Ok
    Paint colorRed = new Paint().setARGB(255, 236, 107, 118); //miss 不可

    public K3CardBuilder(Score score) {
        //这是右下角的附加信息矩形
        super(1000, 270);

        drawBaseRRect();
        drawJudgeIndex(score);
        drawJudgeGraph(score);
        drawRetryFailIndex(score);
        drawRetryGraph(score);//还没有写两个折线图，折线图高80，宽520，起点在20,170，retry的颜色 Paint colorGoldenYellow，Fail颜色 Paint colorRed
        drawFailedGraph(score);
        drawBeatMapInfo(score);
        drawStarRatingRRect(score);
    }

    private void drawBaseRRect() {
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawJudgeIndex(Score score) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS18 = new Font(TorusSB, 18);

        String MapRatingStr = String.format("%.1f",score.getBeatMap().getBeatMapRating());

        TextLine Jl = TextLine.make("Judge & Difficulty", fontS18);// 标题
        TextLine Jr = TextLine.make("rating " + MapRatingStr, fontS18);// 谱面评级

        canvas.save();
        canvas.translate(20,20);
        canvas.drawTextLine(Jl, 0, Jl.getHeight() - Jl.getXHeight(), colorGrey);
        canvas.translate(520 - Jr.getWidth(),0);
        canvas.drawTextLine(Jr, 0, Jr.getHeight() - Jr.getXHeight(), colorGrey);
        canvas.restore();
    }

    private void drawJudgeGraph(Score score) {
        //还没有写柱状图，柱状图宽16，最高80，圆角8，间隔4，共26个

        int AvailableRRectCount = (int) Math.floor(score.getStatistics().getPlayTime() * 27f / score.getBeatMap().getTotalLength()); // 这里稍微放大一点，免得没法算完
        if (AvailableRRectCount > 26) AvailableRRectCount = 26;
        if (AvailableRRectCount < 0) AvailableRRectCount = 0;// 限位

        // 26等分的数据，需要传入DataUtil.readMap的数组
        int[] ObjectArray = {114,514,1919,810,114,514,1919,810,114,514,1919,810,114,514,1919,810,114,514,1919,810,114,514,1919,810,114,514};
        int ObjectMax = Arrays.stream(ObjectArray).max().getAsInt();

        canvas.save();
        canvas.translate(20,130);
        for (int i = 0; i < ObjectArray.length; i++) {

            if (AvailableRRectCount > 0) { //灰色与蓝色的区别
                canvas.drawRRect(RRect.makeXYWH(0, -85f * ObjectArray[i] / ObjectMax, 16, 85f * ObjectArray[i] / ObjectMax, 4), colorLightBlue);
            } else {
                canvas.drawRRect(RRect.makeXYWH(0, -85f * ObjectArray[i] / ObjectMax, 16, 85f * ObjectArray[i] / ObjectMax, 4), colorGrey);
            }
            AvailableRRectCount--;
            canvas.translate(20,0);
            }
        canvas.restore();
        }

    private void drawRetryFailIndex(Score score) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS18 = new Font(TorusSB, 18);

        int PassPercent;
        int FailPercent;

        if (score.getBeatMap().getPlaycount() == 0) {
            PassPercent = 0;
            FailPercent = 0;
        } else {
            PassPercent = (int) (score.getBeatMap().getPasscount() * 100f / score.getBeatMap().getPlaycount());
            FailPercent = 114514; // (int) (score.getBeatMap().getFailcount() * 100f / score.getBeatMap().getPlaycount());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("r ")
                .append(PassPercent) //重试比例
                .append("% // f ")
                .append(FailPercent) //失败比例
                .append("%");

        TextLine Jl = TextLine.make("Retry & Fail", fontS18);// 标题
        TextLine Jr = TextLine.make(sb.toString(), fontS18);// retry fail

        canvas.save();
        canvas.translate(20,140);
        canvas.drawTextLine(Jl, 0, Jl.getHeight() - Jl.getXHeight(), colorGrey);
        canvas.translate(520 - Jr.getWidth(),0);
        canvas.drawTextLine(Jr, 0, Jr.getHeight() - Jr.getXHeight(), colorGrey);
        canvas.restore();
    }


    /***
     * 借用的J2卡的折线绘制
     * @param score
     */
    private void drawRetryGraph(Score score) {
        var retry = score.getBeatMap().getBeatMapRetryList();

        //实际折线所占寬高
        int w = 525;
        int h = 90;

        //相对K3卡片的偏移
        int offset_x = 20;
        int offset_y = 160;

        if (retry.size() == 0) return;
        int retry_length_max = 0;
        int retry_length_min = 0;
        int retry_max = retry.get(0);
        int retry_min = retry_max;
        float step = 1f * w / (retry.size() - 1);

        for (var i : retry) {
            if (retry_max < i) {
                retry_max = i;
            } else if (retry_min > i) {
                retry_min = i;
            }
        }

        this.retry_max = retry_max;
        this.retry_min = retry_min;
        this.retry_length = retry.size();
        float stepY = 1f * h / (retry_max - this.retry_min);
        retry_length_max = retry.indexOf(retry_max);
        retry_length_min = retry.indexOf(this.retry_min);

        var path = new org.jetbrains.skija.Path();
        path.moveTo(0, stepY * (retry.get(0) - retry_min));
        for (int i = 1; i < retry.size() - 1; i++) {
            path.cubicTo(
                    (i - 0.5f) * step, stepY * ((retry.get(i - 1) + retry.get(i)) / 2f - retry_min),
                    i *  step, stepY * (retry.get(i) - retry_min),
                    (i + 0.5f) * step, stepY * ((retry.get(i + 1) + retry.get(i)) / 2f - retry_min)
            );
        }
        path.quadTo(
                (retry.size() - 2) * step, stepY * (retry.get(retry.size() - 2) - retry_min),
                (retry.size() - 1) * step, stepY * (retry.get(retry.size() - 1) - retry_min)
        );

        // 最大/最小点的中心坐标(注不用考虑偏移,此处针对折线区域计算)
        float max_x = retry_length_max * step;
        float max_y = h;
        //float min_x = day_min * step;
        //float min_y = 0;

        var p = new Paint()
                .setStroke(true)
                .setStrokeWidth(5)
                .setAntiAlias(true)
                .setARGB(255, 255, 204, 34);// colorgoldenyellow

        //绘图逻辑 在try内绘制文字 注意遮挡关系,代码越靠后效果越靠前
        try (path; p) {
            canvas.save();
            //折线
            canvas.translate(offset_x, offset_y);
            canvas.drawPath(path, p);

            //最大点 圆
            Typeface TorusSB = SkiaUtil.getTorusSemiBold();
            Font fontS18 = new Font(TorusSB, 18);
            TextLine retry_max_tl = TextLine.make(String.valueOf(retry_max), fontS18);
            canvas.drawTextLine(retry_max_tl, 4 + retry_max_tl.getWidth(), retry_max_tl.getHeight() - retry_max_tl.getXHeight(), colorWhite);

            canvas.drawCircle(max_x, max_y, 14, colorGoldenYellow);
            canvas.drawCircle(max_x, max_y, 8, colorRRect);

            canvas.restore();
        }
    }


    /***
     * 借用的J2卡的折线绘制
     * @param score
     */
    private void drawFailedGraph(Score score) {
        var failed = score.getBeatMap().getBeatMapFailedList();

        //实际折线所占寬高
        int w = 525;
        int h = 90;

        //相对K3卡片的偏移
        int offset_x = 20;
        int offset_y = 160;

        if (failed.size() == 0) return;
        int failed_length_max = 0;
        int failed_length_min = 0;
        int failed_max = failed.get(0);
        int failed_min = failed_max;
        float step = 1f * w / (failed.size() - 1);

        for (var i : failed) {
            if (failed_max < i) {
                failed_max = i;
            } else if (failed_min > i) {
                failed_min = i;
            }
        }

        this.failed_max = failed_max;
        this.failed_min = failed_min;
        this.failed_length = failed.size();
        float stepY = 1f * h / (failed_max - this.failed_min);
        failed_length_max = failed.indexOf(failed_max);
        failed_length_min = failed.indexOf(this.failed_min);

        var path = new org.jetbrains.skija.Path();
        path.moveTo(0, stepY * (failed.get(0) - failed_min));
        for (int i = 1; i < failed.size() - 1; i++) {
            path.cubicTo(
                    (i - 0.5f) * step, stepY * ((failed.get(i - 1) + failed.get(i)) / 2f - failed_min),
                    i *  step, stepY * (failed.get(i) - failed_min),
                    (i + 0.5f) * step, stepY * ((failed.get(i + 1) + failed.get(i)) / 2f - failed_min)
            );
        }
        path.quadTo(
                (failed.size() - 2) * step, stepY * (failed.get(failed.size() - 2) - failed_min),
                (failed.size() - 1) * step, stepY * (failed.get(failed.size() - 1) - failed_min)
        );

        // 最大/最小点的中心坐标(注不用考虑偏移,此处针对折线区域计算)
        float max_x = failed_length_max * step;
        float max_y = h;
        //float min_x = day_min * step;
        //float min_y = 0;

        var p = new Paint()
                .setStroke(true)
                .setStrokeWidth(5)
                .setAntiAlias(true)
                .setARGB(255, 236, 107, 118); //colorRed

        //绘图逻辑 在try内绘制文字 注意遮挡关系,代码越靠后效果越靠前
        try (path; p) {
            canvas.save();
            //折线
            canvas.translate(offset_x, offset_y);
            canvas.drawPath(path, p);

            //最大点 圆
            Typeface TorusSB = SkiaUtil.getTorusSemiBold();
            Font fontS18 = new Font(TorusSB, 18);
            TextLine failed_max_tl = TextLine.make(String.valueOf(failed_max), fontS18);
            canvas.drawTextLine(failed_max_tl, -4 - failed_max_tl.getWidth(), failed_max_tl.getHeight() - failed_max_tl.getXHeight(), colorWhite);

            canvas.drawCircle(max_x, max_y, 14, colorRed);
            canvas.drawCircle(max_x, max_y, 8, colorRRect);

            canvas.restore();
        }
    }

    private void drawBeatMapInfo(Score score) {
        int bpm_int = (int) Math.floor(score.getBeatMap().getBpm());
        double bpm_dec = score.getBeatMap().getBpm() - bpm_int;
        long beatLength = Math.round(60000D / score.getBeatMap().getBpm());

        int length = score.getBeatMap().getTotalLength();
        int drain = score.getBeatMap().getHitLength();

        float cs = score.getBeatMap().getCS();
        int cs_int = (int) Math.floor(cs);
        double cs_dec = cs - cs_int;
        float cs_pixel = 54.4f - 4.48f * cs;

        float ar = score.getBeatMap().getAR();
        int ar_int = (int) Math.floor(ar);
        double ar_dec = ar - ar_int;
        float ar_preempt = 0;
        if (ar < 5){
            ar_preempt = 1200 + 600 * (5 - ar) / 5;
        } else if (ar >= 5){
            ar_preempt = 1200 - 750 * (ar - 5) / 5;
        }
        
        float od = score.getBeatMap().getOD();
        int od_int = (int) Math.floor(od);
        double od_dec = od - od_int;
        float od_300window = 0;
        switch (score.getMode())
        {
            case OSU: od_300window = 80 - 6 * od; break;
            case TAIKO: od_300window = 50 - 3 * od; break;
            case CATCH: od_300window = -1 ; break;
            case MANIA: od_300window = 64 - 3 * od;
        }

        float hp = score.getBeatMap().getHP();
        int hp_int = (int) Math.floor(hp);
        double hp_dec = hp - hp_int;
            
        Image bpmII = null;
        String bpmIN = "BPM";
        String bpmLI = String.valueOf(bpm_int);
        String bpmSI;
        if (bpm_dec == 0){
            bpmSI = String.valueOf(bpm_dec).substring(1,3);
        } else {
            bpmSI = null;
        }
        String bpmAI = String.valueOf(beatLength).substring(0,5)+ "ms"; //一拍的毫秒数

        Image lengthII = null;
        String lengthIN = "Length";
        String lengthLI = String.valueOf((length - length % 60)/60);
        String lengthSI = ":" + String.format("%2d",length % 60);
        String lengthAI = (drain - drain % 60) / 60 + ":" + String.format("%2d",drain % 60); //实际时间

        Image csII = null;
        String csIN = "CS";
        String csLI = String.valueOf(cs_int);
        String csSI;
        if (bpm_dec == 0){
            csSI = String.valueOf(cs_dec).substring(1,3);
        } else {
            csSI = null;
        }
        String csAI = (int) cs_pixel + " px"; //圆圈的尺寸

        Image arII = null;
        String arIN = "AR";
        String arLI = String.valueOf(ar_int);
        String arSI;
        if (bpm_dec == 0){
            arSI = String.valueOf(ar_dec).substring(1,3);
        } else {
            arSI = null;
        }
        String arAI = ar_preempt + " ms"; //缩圈出现的总时间

        Image odII = null;
        String odIN = "HP";
        String odLI = String.valueOf(od_int);
        String odSI;
        if (bpm_dec == 0){
            odSI = String.valueOf(od_dec).substring(1,3);
        } else {
            odSI = null;
        }
        String odAI = od_300window + " ms"; //300 击打窗口

        Image hpII = null;
        String hpIN = "HP";
        String hpLI = String.valueOf(hp_int);
        String hpSI;
        if (bpm_dec == 0){
            hpSI = String.valueOf(hp_dec).substring(1,3);
        } else {
            hpSI = null;
        }
        String hpAI = "-"; //暂时不写
        
        try {
            bpmII = SkiaImageUtil.getImage(java.nio.file.Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-beatsperminute.png"));
            lengthII = SkiaImageUtil.getImage(java.nio.file.Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-length.png"));
            arII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-approachrate.png"));
            csII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-circlesize.png"));
            odII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-overalldifficulty.png"));
            hpII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-healthpoint.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        drawInfoUnit(bpmII,bpmIN,bpmLI,bpmSI,bpmAI,560,20);
        drawInfoUnit(lengthII,lengthIN,lengthLI,lengthSI,lengthAI,780,20);
        drawInfoUnit(arII,arIN,arLI,arSI,arAI,560,100);
        drawInfoUnit(csII,csIN,csLI,csSI,csAI,780,100);
        drawInfoUnit(odII,odIN,odLI,odSI,odAI,560,180);
        drawInfoUnit(hpII,hpIN,hpLI,hpSI,hpAI,780,180);
    }
    private void drawStarRatingRRect(Score score) {
        // 这里是右下角那个可以展示状态的东西，但是没想好怎么写，也可以写成和 lazer 一样的星数，也可以写pass fail
        // List<String> mod = score.getMods();

        // if (mod.contains("EZ")) ;

        canvas.save();
        canvas.translate(560,20);
        canvas.drawRRect(RRect.makeXYWH(0,0,520,4,2),colorGrey);
        canvas.restore();
    }


    private Paint getJudgeColor(String JudgeName) {
        Paint RRectPaint;

        switch (JudgeName) {
            case "o_300":
            case "t_300":
            case "c_300":
            case "m_320": RRectPaint = colorLightBlue; break;
            case "o_100":
            case "c_100":
            case "t_150":
            case "m_200": RRectPaint = colorGreen; break;
            case "o_50":
            case "c_50":
            case "m_300": RRectPaint = colorYellow; break;
            case "m_100": RRectPaint = colorBlue; break;
            case "c_dl":
            case "m_50": RRectPaint = colorGrey; break;
            case "o_0":
            case "t_0":
            case "c_0":
            case "m_0": RRectPaint = colorRed; break;
            default: RRectPaint = colorDarkGrey;
        }
        return RRectPaint;
    }

    private int getJudge (Score score, String Name) {
        int data = 0;

        int s_300 = score.getStatistics().getCount300();
        int s_100 = score.getStatistics().getCount100();
        int s_50 = score.getStatistics().getCount50();
        int s_g = score.getStatistics().getCountGeki();
        int s_k = score.getStatistics().getCountKatu();
        int s_0 = score.getStatistics().getCountMiss();

        switch (Name) {
            case "o_300":
            case "c_300":
            case "t_300": data = s_300; break;
            case "o_100":
            case "t_150": data = s_100; break;
            case "o_50": data = s_50; break;
            case "m_320": data = s_g; break;
            case "m_200":
            case "c_dl": data = s_k; break;
            case "o_0":
            case "t_0":
            case "c_0":
            case "m_0": data = s_0;
        }
        return data;
    }

    /***
     * 这是一个 200 * 50 大小的组件，因为复用较多，写成私有方法方便调用
     * @param IndexImage
     * @param IndexName
     * @param LargeInfo
     * @param SmallInfo
     * @param AssistInfo
     * @param x
     * @param y
     */
    private void drawInfoUnit (Image IndexImage, String IndexName, String LargeInfo, String SmallInfo, String AssistInfo, int x, int y) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS36 = new Font(TorusSB, 36);
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS18 = new Font(TorusSB, 18);

        TextLine U1 = TextLine.make(IndexName, fontS18);
        TextLine U2 = TextLine.make(AssistInfo, fontS18);
        TextLine U3 = TextLine.make(LargeInfo, fontS36);
        TextLine U4 = TextLine.make(SmallInfo, fontS24);

        canvas.save();
        canvas.translate(x,y);
        canvas.drawImage(IndexImage, 0, 0, new Paint()); //这里可以试着用try catch环绕一下
        canvas.translate(50,0);
        canvas.drawTextLine(U1, 0, U1.getHeight() - U1.getXHeight(), colorGrey);
        canvas.translate(150 - U2.getWidth(),0);

        // combo 的 AssistInfo 是唯一一个例外，需要完全变白
        if (Objects.equals(IndexName, "combo")){
            canvas.drawTextLine(U2, 0, U2.getHeight() - U2.getXHeight(), colorWhite);
        } else {
            canvas.drawTextLine(U2, 0, U2.getHeight() - U2.getXHeight(), colorDarkGrey);
        }

        canvas.translate(-144 + U2.getWidth(),20);
        canvas.drawTextLine(U3, 0, U3.getHeight() - U3.getXHeight(), colorWhite);
        canvas.translate(U3.getWidth(),8);
        canvas.drawTextLine(U4, 0, U4.getHeight() - U4.getXHeight(), colorWhite);
        canvas.restore();
    }

    public Image build() {
        return super.build(20);
    }
}
