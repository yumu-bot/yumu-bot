package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.util.SkiaImageUtil;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;

import java.io.IOException;
import java.nio.file.Path;

public class K0CardBuilder extends PanelBuilder {

    Paint colorRRect = new Paint().setARGB(255,56,46,50);

    public K0CardBuilder(Score score) {
        super(1920,790);
        String Rank = score.getRank();
        drawImage(getScoreRankImage(Rank));
    }

    private Image getScoreRankImage(String Rank){
        Image ScoreRankImage = null;
        try {
            ScoreRankImage = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "object-score-backimage-" + Rank + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ScoreRankImage;
    }

    public Image build() {
        return super.build(20);
    }
}
