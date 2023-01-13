package com.now.nowbot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.enums.Mod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class DataUtil {

    private static final ObjectMapper mapper = JsonMapper.builder().build();

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
            NowbotApplication.log.error("读取json错误", e);
            throw new RuntimeException("见上一条");
        }
    }

    public static List<Integer> getMapObjectList(String mapStr) {
        var bucket = mapStr.split("\\[\\w+]");
        var hitObjects = bucket[bucket.length - 1].split("\\s+");
        var hitObjectStr = new ArrayList<String>();
        for (var x : hitObjects) {
            if (!x.trim().equals("")) {
                hitObjectStr.add(x);
            }
        }

        var p = Pattern.compile("^\\d+,\\d+,(\\d+)");

        var times = hitObjectStr.stream()
                .map((m) -> {
                    var m2 = p.matcher(m);
                    if (m2.find()) {
                        return Integer.parseInt(m2.group(1));
                    } else {
                        return 0;
                    }
                }).toList();

        return times;
    }

    public static List<Integer> getGrouping26(List<Integer> x) {
        var steps = (x.get(x.size() - 1) - x.get(0)) / 26 + 1;
        var out = new LinkedList<Integer>();
        int m = x.get(0) + steps;
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

    private static int AR2MS(float ar){
        if (0 < ar - 5){
            if (ar > 11) return 300;
            return  1200 - (int) (150 * (ar - 5));
        } else {
            return  1800 - (int) (120 * ar);
        }
    }

    private static float MS2AR(int ms){
        if (0 < 1200 - ms){
            if (ms < 300) return 11;
            return  5 + (1200 - ms) / 150f;
        } else {
            if (ms >= 2400) return -5;
            return  (1800 - ms) / 120f;
        }
    }

    public static float AR(float ar, int mod){
        int ms;
//      1800  -  1200  -  450  -  300
        if (Mod.hasHr(mod)){
            ar *= 1.4;
        } else if (Mod.hasEz(mod)) {
            ar /= 2;
        }
        ms = AR2MS(ar);
        if (Mod.hasDt(mod)){
            ms /= (3d/2);
        } else if (Mod.hasHt(mod)) {
            ms /= (3d/4);
        }
        ar = MS2AR(ms);
        return (int)Math.ceil(ar * 100)/100f;
    }

    private static float OD2MS(float od){
        if (od > 10) return 20;
        return 80 - (6 * od);
    }

    private static float MS2OD(float ms){
        return (80 - ms) / 6f;
    }

    public static float OD(float od, int mod){
        float ms;
        if (Mod.hasHr(mod)){
            od *= 1.4f;
        } else if (Mod.hasEz(mod)) {
            od /= 2f;
        }
        ms = OD2MS(od);

        if (Mod.hasDt(mod)){
            ms /= (3d/2);
        } else if (Mod.hasHt(mod)) {
            ms /= (3d/4);
        }
        return (int)Math.ceil(MS2OD(ms)*100) / 100f;
    }


    public static float CS(float cs, int mod){
        if (Mod.hasHr(mod)){
            cs *= 1.3f;
        } else if (Mod.hasEz(mod)) {
            cs /= 2f;
        }
        return cs;
    }
    public static float HP(float hp, int mod){
        if (Mod.hasHr(mod)){
            hp *= 1.3f;
        } else if (Mod.hasEz(mod)) {
            hp /= 1.3f;
        }
        return hp;
    }
    public static void main(String[] args) {
        System.out.println(OD(7, Mod.Easy.value));
        System.out.println(OD(7, Mod.add(Mod.Easy.value, Mod.HalfTime)));
        System.out.println(OD(7, Mod.add(Mod.HardRock.value, Mod.HalfTime)));
        System.out.println(OD(7, Mod.HardRock.value));
        System.out.println(OD(7, Mod.DoubleTime.value));
        System.out.println(OD(7, Mod.add(Mod.HardRock.value, Mod.DoubleTime)));
    }

//    public static void main(String[] args) throws IOException {
//        NowbotConfig.FONT_PATH = "/home/spring/cache/nowbot/font/";
//        IocAllReadyRunner.initFountWidth();
//        var data = DataUtil.getObject("/home/spring/data.json", OsuUser.class);
//        var j1 = new J1CardBuilder(data, new ArrayList<>(0));
//        Files.write(Path.of("/home/spring/p1.png"), j1.build().encodeToData().getBytes());
//    }
}
