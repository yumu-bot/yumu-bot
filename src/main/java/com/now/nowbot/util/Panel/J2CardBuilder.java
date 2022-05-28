package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.RRect;

import java.io.IOException;

public class J2CardBuilder extends PanelBuilder{

    public J2CardBuilder(OsuUser user) throws IOException {
        super(900, 355);

        drawBaseRRect();
    }

    private void drawBaseRRect() {
        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0, 0, 900, 355, 20), new Paint().setARGB(255, 56, 46, 50));
        canvas.restore();
    }

    private void drawUserRankingCurve(OsuUser user) {
        //用户Rank曲线，要求折线描边5px，颜色255,204,34，需要标出最大值和最小值。
        //最大值数字一般在曲线最大的右上角，(数字文本的左上角：曲线最大坐标：左移20px，上移20px)，24px高度，颜色同上，
        //但如果最大值出现在靠近当前天数的五天内，则改为在曲线最大坐标左上角显示。)
        //当前值数字一般在曲线当前值位置的右上角(要求同上)
        //但如果当前值与最大值靠得太近，则改为在曲线最大坐标右下角显示。移动方法也是像上面那样的20px、20px。
        //当前值和最大值所用的小标识我会用小png来表示。
        //注意，这里还需要 2 套大数字省略方法，具体内容如下：
        //1-99-0.1K-9.9K-10K-99K-0.1M-9.9M-10M-99M-0.1G-9.9G-10G-99G-0.1T-9.9T-10T-99T-Inf.
        //1-999-1.00K-999.99K-1.00M-999.99M-1.00G-999.99G-...-999.9T-Inf
    }
}
