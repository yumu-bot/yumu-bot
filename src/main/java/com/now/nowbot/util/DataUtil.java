package com.now.nowbot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import io.github.humbleui.skija.Typeface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class DataUtil {
    private static final Logger log = LoggerFactory.getLogger(DataUtil.class);

    private static final ObjectMapper mapper = JsonMapper.builder().build();
    
    static Typeface TORUS_REGULAR;

    static Typeface TORUS_SEMIBOLD;

    static Typeface PUHUITI;

    static Typeface PUHUITI_MEDIUM;

    static Typeface EXTRA;

    private record Range(Integer offset, Integer limit) {}

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     * @param start 开始
     * @param end 结束
     * @return offset
     */
    @NonNull
    public static int parseRange2Offset(@Nullable Integer start, @Nullable Integer end) {
        return parseRange(start, end).offset;
    }

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     * @param start 开始
     * @param end 结束
     * @return limit
     */
    @NonNull
    public static int parseRange2Limit(@Nullable Integer start, @Nullable Integer end) {
        return parseRange(start, end).limit;
    }

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     * @param start 开始
     * @param end 结束
     * @return offset-limit 对
     */
    @NonNull
    private static Range parseRange(@Nullable Integer start, @Nullable Integer end) {
        int offset;
        int limit;

        if (Objects.isNull(start) || start < 1 || start > 100) start = 1;

        if (Objects.isNull(end) || end < 1 || end > 100) {
            offset = start - 1;
            limit = 1;
        } else {
            //分流：正常，相等，相反
            if (end > start) {
                offset = start - 1;
                limit = end - start + 1;
            } else if (Objects.equals(start, end)) {
                offset = start - 1;
                limit = 1;
            } else {
                offset = end - 1;
                limit = start - end + 1;
            }
        }

        return new Range(offset, limit);
    }


    @NonNull
    public static boolean isHelp(@Nullable String str) {
        if (str == null) return false;

        return str.trim().equalsIgnoreCase("help") || str.trim().equalsIgnoreCase("帮助");
    }

    /**
     * 根据分隔符，分割玩家名
     * @param str 需要分割的，含分割符和玩家名的长文本
     * @return 分割好的玩家名
     */
    @NonNull
    public static List<String> parseUsername(@Nullable String str) {
        if (Objects.isNull(str)) return Collections.singletonList("");
        String[] split = str.trim().split("[,，、|:：]+");
        if (split.length == 0) return Collections.singletonList(str);

        return Arrays.stream(split).map(String::trim).filter(StringUtils::hasText).toList();
    }

    public static String String2Markdown(String str) {
        return str.replace("\n", "\n\n");
    }
    public static String JsonString2Markdown(String str) {
        if (Objects.isNull(str)) return null;
        return str.replace("},", "},\n\n");
    }

    public static char getRoundedNumberUnit(double number, int level) {
        char unit = '-';
        number = Math.abs(number);
        if (level < 1 || level > 2) return '-';
        int m = 1 + level;

        if (number < Math.pow(10, m)) {  //level==1->100 level==2->1000
            unit = 0;
        } else if (number < Math.pow(10, (m += 3))) {
            unit = 'K';
        } else if (number < Math.pow(10, (m += 3))) {
            unit = 'M';
        } else if (number < Math.pow(10, (m += 3))) {
            unit = 'G';
        } else if (number < Math.pow(10, (m += 3))) {
            unit = 'T';
        } else if (number < Math.pow(10, m + 3)) {
            unit = 'P';
        }

        return unit;
    }



    public static Double getRoundedNumber(double number, int level) {

        boolean s = true; //正负符号
        // lv1.保留1位小数，结果不超4位字符宽(包含单位)
        //1-99-0.1K-9.9K-10K-99K-0.1M-9.9M-10M-99M-0.1G-9.9G-10G-99G-0.1T-9.9T-10T-99T-Inf.

        // lv2.保留2位小数，结果不超7位字符宽(包含单位)
        //1-999-1.00K-999.99K-1.00M-999.99M-1.00G-999.99G-...-999.9T-Inf.

        //将负值纳入计算

        while (number >= 1000 || number <= -1000) {
            number /= 1000;
        }

        if (level == 1) {
            if (number >= 100) {
                number /= 1000;
            }
            number = (double) Math.round(number * 10) / 10D;
        } else if (level == 2) {
            number = (double) Math.round(number * 1000) / 1000D;
        }
        if (number - Math.round(number) <= 0.0001) number = (double) Math.round(number);

        return number;
    }

    public static String getRoundedNumberStr(double number, int level) {
        var c = getRoundedNumberUnit(number, level);
        boolean isInt;
        int intValue;
        if (c == 0) {
            intValue = (int) number;
            if (level == 1) {
                isInt = number - intValue <= 0.1;
            } else {
                isInt = number - intValue <= 0.001;
            }
            if (isInt) return String.valueOf(intValue);
            return String.valueOf(number);
        }

        while (number >= 1000 || number <= -1000) {
            number /= 1000;
        }

        if (level == 1) {
            if (number >= 100) {
                number /= 1000;
            }
            number = (double) Math.round(number * 10) / 10D;
        } else if (level == 2) {
            number = (double) Math.round(number * 1000) / 1000D;
        }
        intValue = (int) number;
        if (level == 1) {
            isInt = number - intValue <= 0.1;
        } else {
            isInt = number - intValue <= 0.001;
        }

        if (isInt) {
            return String.format("%d%c", intValue, c);
        }
        String out = String.format(level == 1 ? "%.1f%c" : "%.2f%c", number, c);
        if (out.charAt(out.length() - 2) == '0') {
            out = out.substring(0, out.length() - 2) + c;
        }
        return out;
    }

    public static String Time2HourAndMinient(long time) {
        if (time < 3600000) {
            return String.format("%dM", time / 60000);
        }
        var h = time / 3600000;
        var m = (time % 3600000) / 60000;
        return String.format("%dH%dM", h, m);
    }

    public static <T> T getObject(String filepath, Class<T> T) {
        try {
            return mapper.readValue(new File(filepath), T);
        } catch (IOException e) {
            log.error("读取json错误", e);
            throw new RuntimeException("见上一条");
        }
    }

    public static List<Integer> getMapObjectList(String mapStr) {
        var bucket = mapStr.split("\\[\\w+]");
        var hitObjects = bucket[bucket.length - 1].split("\\s+");
        var hitObjectStr = new ArrayList<String>();
        for (var x : hitObjects) {
            if (!x.trim().isEmpty()) {
                hitObjectStr.add(x);
            }
        }

        var p = Pattern.compile("^\\d+,\\d+,(\\d+)");

        return hitObjectStr.stream()
                .map((m) -> {
                    var m2 = p.matcher(m);
                    if (m2.find()) {
                        return Integer.parseInt(m2.group(1));
                    } else {
                        return 0;
                    }
                }).toList();
    }

    public static List<Integer> getGrouping26(List<Integer> x) {
        var steps = (x.getLast() - x.getFirst()) / 26 + 1;
        var out = new LinkedList<Integer>();
        int m = x.getFirst() + steps;
        short sum = 0;
        for (var i : x) {
            if (i < m) {
                sum++;
            } else {
                out.push((int) sum);
                sum = 0;
                m += steps;
            }
        }

        return out;
    }

    public static int AR2MS(float ar){
        if (0 < ar - 5){
            if (ar > 11) return 300;
            return  1200 - (int) (150 * (ar - 5));
        } else {
            return  1800 - (int) (120 * ar);
        }
    }

    public static float MS2AR(int ms){
        if (0 < 1200 - ms){
            if (ms < 300) return 11;
            return  5 + (1200 - ms) / 150f;
        } else {
            if (ms >= 2400) return -5;
            return  (1800 - ms) / 120f;
        }
    }

    public static float AR(float ar, int mod){
        float ms;
//      1800  -  1200  -  450  -  300
        if (OsuMod.hasHr(mod)){
            ar *= 1.4f;
        } else if (OsuMod.hasEz(mod)) {
            ar /= 2;
        }
        ar = Math.min(10f, ar);
        ms = AR2MS(ar);
        if (OsuMod.hasDt(mod)){
            ms /= (3f / 2f);
        } else if (OsuMod.hasHt(mod)) {
            ms /= (3f / 4f);
        }
        ar = MS2AR((int) ms);
        return (int)Math.ceil(ar * 100)/100f;
    }

    public static float OD2MS(float od){
        if (od > 10) return 20;
        return 80 - (6 * od);
    }

    public static float MS2OD(float ms){
        return (80 - ms) / 6f;
    }

    public static float OD(float od, int mod){
        float ms;
        if (OsuMod.hasHr(mod)){
            od *= 1.4f;
        } else if (OsuMod.hasEz(mod)) {
            od /= 2f;
        }
        ms = OD2MS(od);

        if (OsuMod.hasDt(mod)){
            ms /= (3f/2);
        } else if (OsuMod.hasHt(mod)) {
            ms /= (3f/4);
        }
        return (int) Math.ceil(MS2OD(ms) * 100) / 100f;
    }


    public static float CS(float cs, int mod){
        if (OsuMod.hasHr(mod)){
            cs *= 1.3f;
        } else if (OsuMod.hasEz(mod)) {
            cs /= 2f;
        }
        return cs;
    }

    public static float BPM(float bpm, int mod){
        if (OsuMod.hasDt(mod)){
            bpm *= 1.5f;
        } else if (OsuMod.hasHt(mod)) {
            bpm *= 0.75f;
        }
        return bpm;
    }

    public static float HP(float hp, int mod){
        if (OsuMod.hasHr(mod)){
            hp *= 1.3f;
        } else if (OsuMod.hasEz(mod)) {
            hp /= 1.3f;
        }
        return hp;
    }

    public static void setBeatMap(BeatMap beatMap, int mods) {
        if (OsuMod.hasChangeRating(mods)) {
            beatMap.setBPM(DataUtil.BPM(beatMap.getBPM(), mods));
            beatMap.setAR(DataUtil.AR(beatMap.getAR(), mods));
            beatMap.setCS(DataUtil.CS(beatMap.getCS(), mods));
            beatMap.setOD(DataUtil.OD(beatMap.getOD(), mods));
            beatMap.setHP(DataUtil.HP(beatMap.getHP(), mods));
        }
    }

    public static void main(String[] args) {
        System.out.println(OD(7, OsuMod.Easy.value));
        System.out.println(OD(7, OsuMod.add(OsuMod.Easy.value, OsuMod.HalfTime)));
        System.out.println(OD(7, OsuMod.add(OsuMod.HardRock.value, OsuMod.HalfTime)));
        System.out.println(OD(7, OsuMod.HardRock.value));
        System.out.println(OD(7, OsuMod.DoubleTime.value));
        System.out.println(OD(7, OsuMod.add(OsuMod.HardRock.value, OsuMod.DoubleTime)));
    }

    public static int getPlayedRankedMapCount(double bonusPP) {
        double v = - (bonusPP / (1000f / 2.4f)) + 1;

        if (v < 0) {
            return 0;
        } else {
            return (int) Math.round(Math.log(v) / Math.log(0.9994));
        }
    }

    public static float getBonusPP(double playerPP, List<Double> fullPP) {
        if (CollectionUtils.isEmpty(fullPP)) return 0f;

        double[] array = new double[fullPP.size()];

        for (int i = 0; i < fullPP.size(); i++) {
            array[i] = Objects.requireNonNullElse(fullPP.get(i), 0d);
        }

        return getBonusPP(playerPP, array);
    }

    /**
     * 计算bonusPP
     * 算法是最小二乘 y = kx + b
     * 输入的PP数组应该是加权之前的数组。
     */
    public static float getBonusPP(double playerPP, double[] fullPP) {
        double bonusPP, remainPP = 0, k, b, bpPP = 0, x = 0, x2 = 0, xy = 0, y = 0;

        if (fullPP == null || fullPP.length == 0d) return 0f;

        int length = fullPP.length;

        for (int i = 0; i < length; i++) {
            double weight = Math.pow(0.95f, i);
            double PP = fullPP[i];

            //只拿最后50个bp来算，这样精准
            if (i >= 50) {
                x += i;
                y += PP;
                x2 += Math.pow(i, 2f);
                xy += i * PP;
            }
            bpPP += PP * weight;//前 100 的bp上的 pp
        }

        double N = length - 50;
        // Exiyi - Nxy__ / Ex2i - Nx_2
        k = (xy - (x * y / N)) / (x2 - (Math.pow(x, 2f) / N));
        b = (y / N) - k * (x / N);

        //找零点
        int expectedX = (k == 0f) ? -1 : (int) Math.floor(- b / k);

        //这个预估的零点应该在很后面，不应该小于 100
        //如果bp没满100，那么bns直接可算得，remainPP = 0
        if (length < 100 || expectedX <= 100) {
            bonusPP = playerPP - bpPP;
        } else {
            //对离散数据求和
            for (int i = length; i <= expectedX; i++) {
                double weight = Math.pow(0.95f, i);
                remainPP += (k * i + b) * weight;
            }

            bonusPP = playerPP - bpPP - remainPP;
        }

        return (float) Math.max(Math.min(bonusPP, 413.894179759f), 0f);
    }

    public static double getV3ScoreProgress(Score score) { //下下策
        OsuMode mode = score.getMode();
        double progress;

        if(!score.getPassed()){
            progress = 1D * score.getStatistics().getCountAll(mode) / score.getBeatMap().getMaxCombo();
        } else {
            progress = 1D;
        }
        return progress;
    }

    public static String getV3Score(Score score) {
        // 算 v3 分（lazer的计分方式
        // 有个版本指出，目前 stable 的 v2 是这个算法的复杂版本，acc是10次方，转盘分数纳入mod倍率

        OsuMode mode = score.getMode();
        List<String> mods = score.getMods();

        int fc = 100_0000;
        double i = getV3ModsMultiplier(mods,mode);
        double p = getV3ScoreProgress(score); //下下策
        int c = score.getMaxCombo();
        int m = score.getBeatMap().getMaxCombo();
        double ap8 = Math.pow(score.getAccuracy(), 8f);
        double v3 = switch (score.getMode()) {
            case OSU, CATCH, DEFAULT -> fc * i * (0.7f * c / m + 0.3f * ap8) * p;
            case TAIKO -> fc * i * (0.75f * c / m + 0.25f * ap8) * p;
            case MANIA -> fc * i * (0.01f * c / m + 0.99f * ap8) * p;
        };

        return String.format("%07d",Math.round(v3)); //补 7 位达到 v3 分数的要求
    }

    // 这东西是啥?
    public static double getV3ModsMultiplier(List<String> mod, OsuMode mode) {
        double index = 1.00D;

        if (mod.contains("EZ")) index *= 0.50D;

        switch (mode){
            case OSU:{
                if (mod.contains("HT")) index *= 0.30D;
                if (mod.contains("HR")) index *= 1.10D;
                if (mod.contains("DT")) index *= 1.20D;
                if (mod.contains("NC")) index *= 1.20D;
                if (mod.contains("HD")) index *= 1.06D;
                if (mod.contains("FL")) index *= 1.12D;
                if (mod.contains("SO")) index *= 0.90D;
            } break;

            case TAIKO:{
                if (mod.contains("HT")) index *= 0.30D;
                if (mod.contains("HR")) index *= 1.06D;
                if (mod.contains("DT")) index *= 1.12D;
                if (mod.contains("NC")) index *= 1.12D;
                if (mod.contains("HD")) index *= 1.06D;
                if (mod.contains("FL")) index *= 1.12D;
            } break;

            case CATCH:{
                if (mod.contains("HT")) index *= 0.30D;
                if (mod.contains("HR")) index *= 1.12D;
                if (mod.contains("DT")) index *= 1.12D;
                if (mod.contains("NC")) index *= 1.12D;
                if (mod.contains("FL")) index *= 1.12D;
            } break;

            case MANIA: {
                if (mod.contains("HT")) index *= 0.50D;
                if (mod.contains("CO")) index *= 0.90D;
            }
        }

        return index;
    }

    /***
     * 缩短字符 220924
     * @param Str 需要被缩短的字符
     * @param MaxWidth 最大宽度
     * @return 返回已缩短的字符
     */
    public static String getShortenStr (String Str, int MaxWidth){
        StringBuilder sb = new StringBuilder();
        var Char = Str.toCharArray();

        float allWidth = 0;
        int backL = 0;

        for (var thisChar : Char) {
            if (allWidth > MaxWidth){
                break;
            }
            sb.append(thisChar);
            if ((allWidth) < MaxWidth){
                backL++;
            }
        }
        if (allWidth > MaxWidth){
            sb.delete(backL,sb.length());
            sb.append("...");
        }

        sb.delete(0,sb.length());
        allWidth = 0;
        backL = 0;

        return sb.toString();
    }

    public static Typeface getTorusRegular() {
        if (TORUS_REGULAR == null || TORUS_REGULAR.isClosed()) {
            try {
//                InputStream in = class.getClassLoader().getResourceAsStream("static/font/Torus-Regular.ttf");
//                TORUS_REGULAR = Typeface.makeFromData(Data.makeFromBytes(in.readAllBytes()));
                TORUS_REGULAR = Typeface.makeFromFile(STR."\{NowbotConfig.FONT_PATH}Torus-Regular.ttf");
            } catch (Exception e) {
                log.error("未读取到目标字体:Torus-Regular.ttf", e);
                TORUS_REGULAR = Typeface.makeDefault();
            }
        }
        return TORUS_REGULAR;
    }

    public static Typeface getTorusSemiBold() {
        if (TORUS_SEMIBOLD == null || TORUS_SEMIBOLD.isClosed()) {
            try {
                TORUS_SEMIBOLD = Typeface.makeFromFile(STR."\{NowbotConfig.FONT_PATH}Torus-SemiBold.ttf");
            } catch (Exception e) {
                log.error("未读取到目标字体:Torus-SemiBold.ttf", e);
                TORUS_SEMIBOLD = Typeface.makeDefault();
            }
        }
        return TORUS_SEMIBOLD;
    }

    public static Typeface getPUHUITI() {
        if (PUHUITI == null || PUHUITI.isClosed()) {
            try {
                PUHUITI = Typeface.makeFromFile(STR."\{NowbotConfig.FONT_PATH}Puhuiti.ttf");
            } catch (Exception e) {
                log.error("Alibaba-PuHuiTi-Medium.ttf", e);
                PUHUITI = Typeface.makeDefault();
            }
        }
        return PUHUITI;
    }

    public static Typeface getPUHUITIMedium() {
        if (PUHUITI_MEDIUM == null || PUHUITI_MEDIUM.isClosed()) {
            try {
                PUHUITI_MEDIUM = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "Alibaba-PuHuiTi-Medium.ttf");
            } catch (Exception e) {
                log.error("Alibaba-PuHuiTi-Medium.ttf", e);
                PUHUITI_MEDIUM = Typeface.makeDefault();
            }
        }
        return PUHUITI_MEDIUM;
    }

    public static Typeface getEXTRA(){
        if (EXTRA == null || EXTRA.isClosed()) {
            try {
                EXTRA = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "extra.ttf");
            } catch (Exception e) {
                log.error("未读取到目标字体:extra.ttf", e);
                throw e;
            }
        }
        return EXTRA;
    }

    /*
    public static float getBonusPP (double playerPP, double[] rawPP){
        double bonusPP, remainPP = 0, a, b, c, bpPP = 0, x = 0, x2 = 0, x3 = 0, x4 = 0, xy = 0, x2y = 0, y = 0;

        if (rawPP == null || rawPP.length == 0d) return 0f;

        int length = rawPP.length;

        for (int i = 0; i < length; i++) {
            double weight = Math.pow(0.95f, i);
            double PP = rawPP[i];

            x += i;
            x2 += Math.pow(i, 2f);
            x3 += Math.pow(i, 3f);
            x4 += Math.pow(i, 4f);
            xy += i * PP;
            x2y += Math.pow(i, 2f) * PP;
            y += PP;
            bpPP += PP * weight;//前 100 的bp上的 pp
        }

        if (length < 100) { //如果bp没满100，那么bns直接可算得，remaining = 0
            return (float) Math.min((playerPP - bpPP), 416.6667f);
        } else {

            x /= length;
            x2 /= length;
            x3 /= length;
            x4 /= length;
            xy /= length;
            x2y /= length;
            y /= length;

            double a1 = xy - x * y;
            double a2 = x3 - x * x2;
            double a3 = x2y - x2 * y;
            double a4 = x2 - Math.pow(x, 2f);
            double a5 = x4 - Math.pow(x2, 2f) * x2;

            //得到 y = ax2 + bx + c
            a = ((a1 * a2) - (a3 * a4)) / (Math.pow(a2, 2f) - (a5 * a4));
            b = (xy - (x * y) - (a * a2)) / (x2 - Math.pow(x, 2f));
            c = y - a * x2 - b * x;

            //好像不需要求导，直接找零点
            double delta = Math.pow(b, 2f) - (4 * a * c);
            if (delta < 0) {
                return 0f; //不相交
            }
            int expectedX = (int) Math.floor(( - b - Math.sqrt(delta)) / (2 * a)); //找左边的零点，而且要向下取整
            if (expectedX <= 100) {
                return (float) Math.min((playerPP - bpPP), 416.6667f); //这个预估的零点应该在很后面
            }

            //对离散数据求和
            for (int i = length; i <= expectedX; i++) {
                double weight = Math.pow(0.95f, i);
                remainPP += (a * Math.pow(i, 2f) + b * i + c) * weight;
            }

            bonusPP = playerPP - bpPP - remainPP;

            return (float) Math.min(bonusPP, 416.6667f);
        }
    }

     */

    /**
     * 获取该文件名的 Markdown 文件并转成字符串
     * @param path 文件名，和相对路径
     * @return 文件内容
     */
    public static String getMarkdownFile(String path) {
        StringBuilder sb = new StringBuilder();

        try {
            var bufferedReader = Files.newBufferedReader(
                    Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve(path)
            );

            // 逐行读取文本内容
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append('\n');
            }

            // 关闭流
            bufferedReader.close();

            return sb.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取该文件名的图片文件并转成字符串
     * @param path 图片名，和相对路径
     * @return 图片流
     */
    public static byte[] getPicture(String path) {
        if (path.isEmpty()) return null;

        try {
            return Files.readAllBytes(
                    Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve(path)
            );
        } catch (IOException e) {
            return null;
        }
    }
}
