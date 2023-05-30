package com.now.nowbot.util.Panel;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.skija.*;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BphtPanelBuilder{
    private static final int FONT_SIZE = 30;
    private Font font;
    Image image;

    public BphtPanelBuilder() {
    }
    @NotNull
    public BphtPanelBuilder drowLine(String[] allstr) {
        TextLine[] lines = new TextLine[allstr.length];
        float maxWidth = 0;
        for (int i = 0; i < allstr.length; i++) {
            lines[i] = TextLine.make(allstr[i], getFont());
            if (maxWidth < lines[i].getWidth()) {
                maxWidth = lines[i].getWidth();
            }
        }
        int w = (int) maxWidth + 50;
        int h = (int) ((lines.length + 1) * lines[0].getHeight()) + 50;

        Surface surface = Surface.makeRasterN32Premul(w, h);
        Shader shader = Shader.makeLinearGradient(0, 0, 0, h, SkiaUtil.getRandomColors());
        try (surface; shader) {
            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(38, 51, 57));
            canvas.translate(25, 40);
            for (TextLine line : lines) {
                canvas.drawTextLine(line, 0, line.getCapHeight() + FONT_SIZE * 0.2f, new Paint().setColor(SkiaUtil.getRandomColor()));
                canvas.translate(0, line.getHeight());
            }
            image = surface.makeImageSnapshot();
        } finally {
            for (var line : lines) {
                line.close();
            }
        }
        return this;
    }


    public Image build() {
        //这里是自定义输出
        return image;
    }

    private Font getFont() {
        if (font == null) {
            font = new Font(SkiaUtil.getPUHUITI(), FONT_SIZE);
        }
        return font;
    }
}