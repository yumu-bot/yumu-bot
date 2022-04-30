package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.SkiaUtil;
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
    class intValue {
        int value = 1;

        intValue add() {
            value++;
            return this;
        }

        int value() {
            return value;
        }
    }
    class mapperDate{
        int allPP;
        int size;
        int uid;
        mapperDate(float pp, int uid){
            allPP += pp;
            size = 1;
            this.uid = uid;
        }
        mapperDate add(float pp){
            allPP += pp;
            return this;
        }

        public int getAllPP() {
            return allPP;
        }

        public int getSize() {
            return size;
        }
    }
    class modDate{
        int allPP;
        int size;
        modDate(float pp){
            allPP += pp;
            size = 1;
        }
        modDate add(float pp){
            allPP += pp;
            return this;
        }

        public int getAllPP() {
            return allPP;
        }

        public int getSize() {
            return size;
        }
    }
    public BphtPanelBuilder draw(List<BpInfo> Bps, String name, String mode){

        var dtbf = new StringBuffer(name).append('[').append(mode).append(']').append('\n');
        double allPp = 0;
        int sSum = 0;
        int xSum = 0;
        int fcSum = 0;
        TreeMap<String, intValue> modeSum = new TreeMap<>(); //各个mod的数量

        DecimalFormat decimalFormat = new DecimalFormat("0.00"); //acc格式
        for (int i = 0; i < Bps.size(); i++) {
            var jsb = Bps.get(i);
            //显示前五跟后五的数据
            if (i < 5 || i > Bps.size() - 5) {
                dtbf.append("#")
                        .append(i + 1)
                        .append(' ')
                        .append(decimalFormat.format(jsb.getPp()))
                        .append(' ')
                        .append(decimalFormat.format(100 * jsb.getAccuracy()))
//                        .append(decimalFormat.format(accCoun.getAcc(jsb)))
                        .append('%')
                        .append(' ')
                        .append(jsb.getRank());
                if (jsb.getMods().size() > 0) {
                    for (int j = 0; j < jsb.getMods().size(); j++) {
                        dtbf.append(' ').append(jsb.getMods().get(j));
                    }
                }
                dtbf.append('\n');
            } else if (i == 50) {
                dtbf.append("-------分割线-------\n");
            }
            allPp += jsb.getPp(); //统计总数
            if (jsb.getMods().size() > 0) {
                for (int j = 0; j < jsb.getMods().size(); j++) {
                    String mod = jsb.getMods().get(j);
                    if (!modeSum.containsKey(mod)) modeSum.put(mod, new intValue());
                    else modeSum.get(mod).add();
                }
            }
            if (jsb.getRank().contains("S")) sSum++;
            if (jsb.getRank().contains("X")) {
                sSum++;
                xSum++;
            }
            if (jsb.isPerfect()) fcSum++;
        }
        dtbf.append("累计mod有:\n");
        modeSum.forEach((mod, sum) -> dtbf.append(mod).append(' ').append(sum.value).append(';'));
        dtbf.append("\n您bp中S rank及以上有").append(sSum).append("个\n达到满cb的fc有").append(fcSum).append('个');
        if (xSum != 0) dtbf.append("\n其中SS数量有").append(xSum).append('个');
        dtbf.append("\n您的BP1与BP100的差为").append(decimalFormat.format(Bps.get(0).getPp() - Bps.get(Bps.size() - 1).getPp()));
        dtbf.append("\n您的平均BP为").append(decimalFormat.format(allPp / Bps.size()));


        var allstr = dtbf.toString().split("\n");
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

    public BphtPanelBuilder mf(List<BpInfo> bps, String name, String mode, OsuGetService osuGetService){
        if (bps.size() == 0) return this;
        var dtbf = new StringBuffer(name).append('[').append(mode).append(']').append('\n');

        var  t1 = bps.get(0);
        int maxBpm = 0; float maxBpmValue = t1.getBeatmap().getBpm();
        int maxCommbo = 0; int maxComboValue = t1.getMaxCombo();
        int maxLength = 0; int maxLengthValue = t1.getBeatmap().getTotalLength();

        int minBpm = 0; float minBpmValue = maxBpmValue;
        int minCommbo = 0;int minComboValue = maxComboValue;
        int minLength = 0;int minLengthValue = maxLengthValue;

        int avgLength = 0;
        int avgCombo = 0;
        int maxTimeToPp = 0; float maxTimeToPpValue = 0;
        float allPP = 0;

        TreeMap<String, modDate> modeSum = new TreeMap<>(); //各个mod的数量

        TreeMap<Integer, mapperDate> mapperSum = new TreeMap<>();
        DecimalFormat decimalFormat = new DecimalFormat("0.00"); //acc格式
        for (int i = 0; i < bps.size(); i++) {
            var jsb = bps.get(i);
            var map = jsb.getBeatmap();
            int length = map.getTotalLength();
            float bpm = map.getBpm();
            jsb.getMods().forEach(r->{
                if (modeSum.containsKey(r)){
                    modeSum.get(r).add(jsb.getPp());
                } else {
                    modeSum.put(r, new modDate(jsb.getPp()));
                }
            });
            if (jsb.getMods().contains("DT") || jsb.getMods().contains("NC")){
                length *=1.5;
                bpm *= 1.5;
            } else if (jsb.getMods().stream().anyMatch(r->r.equals("HT"))){
                length *= 0.75;
                bpm *= 0.75;
            }

            avgLength += length;

            if (bpm < minBpmValue){
                minBpm = i;
                minBpmValue = bpm;
            } else if (bpm > maxBpmValue) {
                maxBpm = i;
                maxBpmValue = bpm;
            }

            if (length < minLengthValue){
                if (jsb.getMods().stream().findAny().isPresent())
                minLength = i;
                minLengthValue = map.getTotalLength();
            } else if (length > maxLengthValue){
                maxLength = i;
                maxLengthValue = map.getTotalLength();
            }

            if (map.getMaxCombo() < minComboValue){
                minCommbo = i;
                minComboValue = jsb.getMaxCombo();
            } else if (jsb.getMaxCombo() > maxComboValue){
                maxCommbo = i;
                maxComboValue = jsb.getMaxCombo();
            }
            avgCombo += jsb.getMaxCombo();

            float tthToPp = (jsb.getPp()) / (map.getSliders() + map.getSpinners() + map.getCircles());
            if (maxTimeToPpValue < tthToPp){
                maxTimeToPp = i;
                maxTimeToPpValue = tthToPp;
            }

            if (mapperSum.containsKey(map.getUserId())) {
                mapperSum.get(map.getUserId()).add(jsb.getPp());
            } else {
                mapperSum.put(map.getUserId(), new mapperDate(jsb.getPp(), map.getUserId()));
            }

            allPP += jsb.getPp();
        }
        avgCombo /= bps.size();
        avgLength /= bps.size();

        dtbf.append("bp平均长度: ").append(getTimeStr(avgLength)).append('\n');
        dtbf.append("最长是bp").append(maxLength+1).append(' ').append(getTimeStr(maxLengthValue)).append('\n');
        dtbf.append("最短是bp").append(minLength+1).append(' ').append(getTimeStr(minLengthValue)).append('\n');

        dtbf.append("bp平均combo: ").append(avgCombo).append('\n');
        dtbf.append("最长是bp").append(maxCommbo+1).append(' ').append(maxComboValue).append('\n');
        dtbf.append("最短是bp").append(minCommbo+1).append(' ').append(minComboValue).append('\n');

        dtbf.append("pp/tth收益最大的是bp").append(maxTimeToPp+1)
                .append(" 斩获").append(decimalFormat.format(maxTimeToPpValue)).append("pp/tth").append('\n');

        dtbf.append("常打bpm:").append(decimalFormat.format(maxBpmValue)).append('-').append(decimalFormat.format(minBpmValue)).append('\n');

        dtbf.append("bp中mapper统计:\n");
        var mappers = mapperSum.values().stream().sorted(Comparator.comparing(u->u.size))
                .limit(6).collect(Collectors.toList());
        mappers.forEach(mapperDate -> {
            var user = osuGetService.getPlayerOsuInfo((long) mapperDate.uid);
            dtbf.append(user.getUsername()).append(' ').append(mapperDate.size).append('个').append("总计")
                    .append(decimalFormat.format(mapperDate.allPP)).append("PP").append('\n');
        });
        dtbf.append("累计mod有:\n");
        float finalAllPP = allPP;
        modeSum.forEach((mod, sum) -> dtbf.append(mod).append('*').append(sum.size).append(' ').append("总计")
                .append(sum.getAllPP())
                .append('[').append(decimalFormat.format(sum.getAllPP()/ finalAllPP)).append('%').append(']')
                .append('\n'));



        var allstr = dtbf.toString().split("\n");
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

    String getTimeStr(int l){
        if (l<60){
            return l+"秒";
        } else {
            return l/60+"分"+l%60+"秒";
        }
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