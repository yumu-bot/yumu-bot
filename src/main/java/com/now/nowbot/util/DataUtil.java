package com.now.nowbot.util;

import java.math.BigDecimal;

public class DataUtil {
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

    public static String getRoundedNumberStr(double n, int level){
        var c = getRoundedNumberUnit(n, level);
        if (c == 0){
            return String.valueOf(n);
        }
        var p = getRoundedNumber(n, level);
        if (p - Math.round(p) < 0.0001) {
            return String.format("%d%c", Math.round(p), c);
        }
        return String.format(level == 1 ? "%.1f%c" : "%.2f%c", getRoundedNumber(n, level), c);
    }
    public static String Time2HourAndMinient(long time){
        if (time < 3600000){
            return String.format("%dM",time / 60000);
        }
        var h = time/3600000;
        var m = (time%3600000) / 60000;
        return String.format("%dH%dM",h,m);
    }

}
