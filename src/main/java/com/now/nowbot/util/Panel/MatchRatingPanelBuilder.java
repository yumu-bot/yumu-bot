package com.now.nowbot.util.Panel;

import com.now.nowbot.model.match.UserMatchData;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.RRect;

import java.util.List;

public class MatchRatingPanelBuilder extends PanelBuilder{
    private static final Paint c2 = new Paint().setARGB(255,42,34,38);
    public MatchRatingPanelBuilder(int size){
        super(1920, 330 + 150 * Math.round(size/2f));
    }

    public MatchRatingPanelBuilder drawBanner(Image banner){
        drawImage(banner);
        return this;
    }
    public MatchRatingPanelBuilder mainCrawCard(Image main){
        drawImage(main, 40, 40);
        return this;
    }
    public MatchRatingPanelBuilder drawUser(List<UserMatchData> userMatchData){
        canvas.drawRRect(RRect.makeXYWH(0,290,1920,150 * Math.round(userMatchData.size()/2f) + 80, 30), c2);
        int evenNum = (userMatchData.size()%2 == 0) ? userMatchData.size() : userMatchData.size()-1;
        canvas.save();
        canvas.translate(40, 180);
        for (int i = 0; i < evenNum; i+=2) {
            canvas.translate(0,150);
            canvas.drawImage(new H2CardBuilder(userMatchData.get(i)).build(), 0, 0);
        }
        canvas.restore();
        canvas.save();
        canvas.translate(980, 180);
        for (int i = 1; i < evenNum; i+=2) {
            canvas.translate(0,150);
            canvas.drawImage(new H2CardBuilder(userMatchData.get(i)).build(), 0, 0);
        }
        canvas.restore();
        if (evenNum < userMatchData.size()){
            canvas.drawImage(new H2CardBuilder(userMatchData.get(evenNum)).build(),510,330+75*evenNum);
        }
        return this;
    }
    public Image build() {
        drawName("MRA");
        return super.build(20, "match rating");
    }
}
