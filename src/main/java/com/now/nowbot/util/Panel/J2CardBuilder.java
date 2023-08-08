package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.SkiaUtil;
import io.github.humbleui.skija.*;

public class J2CardBuilder extends PanelBuilder {
    int rank_max;
    int rank_min;

    int days;

    Paint colorRRect = new Paint().setARGB(255,56,46,50);
    Paint colorGolden = new Paint().setARGB(255, 255, 204, 34);
    Paint colorDarkGolden = new Paint().setARGB(255, 195, 160, 30);
    Paint colorGrey = new Paint().setARGB(255,170,170,170);
    Paint colorWhite = new Paint().setARGB(255,255,255,255);

    public J2CardBuilder(OsuUser user) {
        super(900, 335);

        drawBaseRRect();
        drawUserRankingCurve(user);
        drawCardText(user);
    }

    private void drawBaseRRect() {
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawUserRankingCurve(OsuUser user) {
        //用户Rank曲线，要求折线描边5px，颜色255,204,34，需要标出最大值。
        //最大值数字一般在曲线最大的右上角，(数字文本的左上角：曲线最大坐标：左移20px，上移20px)，24px高度，颜色同上，
        //小球半径8，大球半径14
        //
        //
        //我傻逼了，当前值有标识，以我群里发的最终截图为准

        var rankHistory = user.getRankHistory().history().stream().filter(i -> i != 0).toList();

        //实际折线所占寬高
        int w = 780;
        int h = 215;

        //相对j2卡片的偏移
        int offset_x = 60;
        int offset_y = 65;

        if (rankHistory.size() == 0) return;
        int day_max = 0;
        int day_min = 0;
        int rank_max = rankHistory.get(0);
        int rank_min = rank_max;
        float step = 1f * w / (rankHistory.size() - 1);

        for (var i : rankHistory) {
            if (rank_max < i) {
                rank_max = i;
            } else if (rank_min > i) {
                rank_min = i;
            }
        }

        this.rank_max = rank_max;
        this.rank_min = rank_min;
        this.days = rankHistory.size();
        float stepY = 1f * h / (rank_max - rank_min);
        day_max = rankHistory.indexOf(rank_max);
        day_min = rankHistory.indexOf(rank_min);

        var path = new Path();
        path.moveTo(0, stepY * (rankHistory.get(0) - rank_min));
        for (int i = 1; i < rankHistory.size() - 1; i++) {
            path.cubicTo(
                    (i - 0.5f) * step, stepY * ((rankHistory.get(i - 1) + rankHistory.get(i)) / 2f - rank_min),
                    i *  step, stepY * (rankHistory.get(i) - rank_min),
                    (i + 0.5f) * step, stepY * ((rankHistory.get(i + 1) + rankHistory.get(i)) / 2f - rank_min)
            );
        }
        path.quadTo(
                (rankHistory.size() - 2) * step, stepY * (rankHistory.get(rankHistory.size() - 2) - rank_min),
                (rankHistory.size() - 1) * step, stepY * (rankHistory.get(rankHistory.size() - 1) - rank_min)
        );

        // 最大/最小点的中心坐标(注不用考虑偏移,此处针对折线区域计算)
        float max_x = day_max * step;
        float max_y = h;
        float min_x = day_min * step;
        float min_y = 0;

        var p = new Paint()
                .setStroke(true)
                .setStrokeWidth(5)
                .setAntiAlias(true)
                .setARGB(255, 255, 204, 34);
        var rpb = new Paint()
                .setARGB(255, 255, 204, 34);
        var rps = new Paint()
                .setARGB(255, 56, 46, 50);

        //绘图逻辑 在try内绘制文字 注意遮挡关系,代码越靠后效果越靠前
        try (path; p; rpb; rps) {
            canvas.save();
            //折线
            canvas.translate(offset_x, offset_y);
            canvas.drawPath(path, p);

            //最大点 圆
            canvas.drawCircle(max_x, max_y, 14, rpb);
            canvas.drawCircle(max_x, max_y, 8, rps);

            //最小点 圆
            canvas.drawCircle(min_x, min_y, 14, rpb);
            canvas.drawCircle(min_x, min_y, 8, rps);

            canvas.restore();
        }
    }

    private void drawCardText(OsuUser user) {
        //画卡片基础信息，2号位本来是中间的坐标值，省略掉了

        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);

        String Jlu1t = "Ranking"; //左上

        double Jl1t = rank_max; //正左，坐标参数，需要根据折线数据来判断！
        //double Jl2t = 0D;
        double Jl3t = rank_min;

        double Jb1t = 0D; //正下，坐标参数
        //double Jb2t = 0D;
        double Jb3t = - days - 1;

        long Jur1t = user.getGlobalRank(); //右上
        String Jur2t = String.valueOf(user.getCountry());
        long Jur3t = user.getCountryRank();

        Jl1t = DataUtil.getRoundedNumber(Jl1t, 1);
        //Jl2t = DataUtil.getRoundedNumber(Jl2t, 1);
        Jl3t = DataUtil.getRoundedNumber(Jl3t, 1);
        //Jb1t = DataUtil.getRoundedNumber(Jb1t, 1); 这不可能需要取整，因为这天数本身就是整数啊
        //Jb2t = DataUtil.getRoundedNumber(Jb2t, 1);
        //Jb3t = DataUtil.getRoundedNumber(Jb3t, 1);

        TextLine Jlu1 = TextLine.make(Jlu1t, fontS36);

        TextLine Jl1 = TextLine.make(String.valueOf(Jl1t) + DataUtil.getRoundedNumberUnit(Jl1t, 1), fontS24);
        //TextLine Jl2 = TextLine.make(String.valueOf(Jl2t) + DataUtil.getRoundedNumberUnit(Jl2t, 1), fontS24);
        TextLine Jl3 = TextLine.make(String.valueOf(Jl3t) + DataUtil.getRoundedNumberUnit(Jl3t, 1), fontS24);

        TextLine Jb1 = TextLine.make(Jb1t + "D", fontS24);
        //TextLine Jb2 = TextLine.make(String.valueOf(Jb2t) + DataUtil.getRoundedNumberUnit(Jb2t, 1), fontS24);
        TextLine Jb3 = TextLine.make(Jb3t + "D", fontS24);

        TextLine Jur1 = TextLine.make("#" + Jur1t, fontS36);
        TextLine Jur2 = TextLine.make(Jur2t, fontS24);
        TextLine Jur3 = TextLine.make("#" + Jur3t, fontS24);


        canvas.save();
        canvas.translate(20, 20);
        canvas.drawTextLine(Jlu1, 0, Jlu1.getHeight() - Jlu1.getXHeight(), colorWhite);
        canvas.restore();

        canvas.save();
        canvas.translate(30 - (Jl1.getWidth() / 2), 56);//居中处理
        canvas.drawTextLine(Jl1, 0, Jl1.getHeight() - Jl1.getXHeight(), colorDarkGolden);
        //canvas.translate((Jl1.getWidth() - Jl2.getWidth()) / 2, 107);//居中处理
        //canvas.drawTextLine(Jl2, 0, Jl2.getHeight() - Jl2.getXHeight(), new Paint().setARGB(255, 195, 160, 30));
        //canvas.translate((Jl2.getWidth() - Jl3.getWidth()) / 2, 107);//居中处理

        canvas.translate((Jl1.getWidth() - Jl3.getWidth()) / 2, 214);//居中处理
        canvas.drawTextLine(Jl3, 0, Jl3.getHeight() - Jl3.getXHeight(), colorDarkGolden);
        canvas.restore();

        canvas.save();
        canvas.translate(60, 300);
        canvas.drawTextLine(Jb1, 0, Jb1.getHeight() - Jb1.getXHeight(), colorDarkGolden);
        //canvas.translate(330 + ((120 - Jb2.getWidth()) / 2), 0);
        //canvas.drawTextLine(Jb2, 0, Jb2.getHeight() - Jb2.getXHeight(), new Paint().setARGB(255, 195, 160, 30));
        //canvas.translate(330 + (Jb2.getWidth() / 2) + 60, 0);

        canvas.translate(780 - Jb3.getWidth(), 0);
        canvas.drawTextLine(Jb3, 0, Jb3.getHeight() - Jb3.getXHeight(), colorDarkGolden);
        canvas.restore();

        canvas.save();
        canvas.translate(880 - Jur3.getWidth(), 28);
        canvas.drawTextLine(Jur3, 0, Jur3.getHeight() - Jur3.getXHeight(), colorGrey);
        canvas.translate(0 - Jur2.getWidth(), 0);
        canvas.drawTextLine(Jur2, 0, Jur2.getHeight() - Jur2.getXHeight(), colorGrey);
        canvas.translate(-10 - Jur1.getWidth(), -8);
        canvas.drawTextLine(Jur1, 0, Jur1.getHeight() - Jur1.getXHeight(), colorGrey);
        canvas.restore();
    }

    public Image build() {
        return super.build(20);
    }
}
