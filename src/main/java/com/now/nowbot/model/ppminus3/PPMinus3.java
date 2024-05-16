package com.now.nowbot.model.ppminus3;

import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.model.ppminus3.impl.PPMinus3ManiaImpl;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class PPMinus3 {
    protected List<Double> valueList = new ArrayList<>(7);

    protected List<Double> subList = new ArrayList<>();

    protected List<String> nameList = new ArrayList<>(7);

    protected List<String> abbrList = new ArrayList<>(7);

    protected Double star = 0d;

    protected final int frac_16 = 21;

    protected final int frac_8 = 42;

    protected final int frac_6 = 55;

    protected final int frac_4 = 83;

    protected final int frac_3 = 111;

    protected final int frac_2 = 166;

    protected final int frac_1 = 333;

    protected final int beat_2 = 666;

    protected final int calculateUnit = 2500; //一个标准单位计算元的区域（毫秒）。这段时间的数据，会统计到单一一个计算元中。

    public static PPMinus3 getInstance(OsuFile file, Double clockRate) throws IOException {
        switch (file.getMode()) {
            case MANIA -> {
                var f = file.getMania();
                f.setClockRate(clockRate);

                return new PPMinus3ManiaImpl(f);
            }
            case null, default -> {
                return null;
            }
        }

    }

    // 公用算法

    // 收集数据
    @SafeVarargs
    @NonNull
    protected static List<Double> CollectData(List<Double>... lists) {
        List<Double> d = new ArrayList<>();

        for (var list : lists) {
            d.add(PPMinus3.Sum(list));
        }

        return d;
    }

    // 求值算法，y = a(cx)^b, x = ∑ list
    @NonNull
    protected static double Eval(List<Double> valueList, double multiplier, double index, @Nullable Double multiply, @Nullable Double addend) {
        double a = (addend == null) ? 0d : addend;
        double m = (multiply == null) ? 1d : multiply;

        return multiplier * Math.pow(Sum(valueList) * m + a, index);
    }

    @NonNull
    protected static double Eval(List<Double> valueList, double multiplier, double index) {
        return Eval(valueList, multiplier, index, null, null);
    }

    @NonNull
    protected static double Eval(List<Double> valueList, double multiplier, double index, double multiply) {
        return Eval(valueList, multiplier, index, multiply, null);
    }

    // 求和算法： 0.8 平均 + 0.2 最大
    @NonNull
    protected static double Sum(List<Double> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0d;
        }

        return 0.8d * list.stream().reduce(Double::sum).orElse(0d) / list.size() + 0.2d * list.stream().reduce(Double::max).orElse(0d);
    }

    /**
     * 正态分布。这个图像在 x = delta_time = standard_time 处，y = 最大值 1。
     * 这个算法可以用来计算常规值。但是计算 stream 时，还是不如之下的 ExponentFunction。
     * @param delta_time 时间差。
     * @param standard_time 标准时间（标准差）。时间差 = 标准时间，y 取到 1。
     * @param max_time 最大时间（方差的 3 倍）。截断时间太长（大于 3σ），而算出的极小值。
     * @return 难度
     */
    @NonNull
    protected static double NormalDistribution(int delta_time, int standard_time, int max_time) {
        double sigma = max_time / 3f + standard_time;
        return Math.exp(( - Math.pow(delta_time - standard_time, 2f)) / (2 * Math.pow(sigma, 2f))) / ((Math.sqrt(2f * Math.PI) * sigma));
    }

    /**
     * 获取 y = 1/x 的值。这个图像在 x = delta_time = standard_time 处，y = 1。
     * 这个算法可以用来计算 jack。
     * @param delta_time 时间差。
     * @param standard_time 标准时间。时间差 = 标准时间，y 取到 1。
     * @param max_time 最大时间。截断时间太长，而算出的极小值。
     * @param min_time 最小时间。避免算出过大的值。如果设为 0，则会出现很大的值。
     * @return 难度
     */
    @NonNull
    protected static double InverseProportionalFunction(int delta_time, int standard_time, int max_time, int min_time) {
        double x = Math.abs(delta_time);
        if (x <= 0) {
            return 0d;
        } else if (x < min_time) {
            return 1d * standard_time / min_time;
        } else if (x < max_time) {
            return 1d * standard_time / x;
        } else {
            return 0d;
        }
    }

    /**
     * 获取 y = (ex/e^x)^2 的值。这个图像在 x = delta_time/standard_time = 1 处，y = 最大值 1。这个超越函数的性质是，x ∈ (0, 1) 时，y 从 0 递增到 1，随后逐渐递减为 0。
     * 这个算法可以用来计算 stream。
     * @param delta_time 时间差。
     * @param standard_time 标准时间。时间差 = 标准时间，y 取到最大值 1。
     * @param max_time 最大时间。截断时间太长，而算出的极小值。推荐大约是标准时间的 4 倍。
     * @return 难度
     */
    @NonNull
    protected static double ExponentFunction(int delta_time, int standard_time, int max_time) {
        if (standard_time <= 0 || delta_time <= 0 || delta_time > max_time) return 0d;

        double x = Math.abs(delta_time * 1d / standard_time);
        //平方是为了方便、快速达到缩减的值
        return Math.pow(Math.E * x / Math.exp(x), 2d);
    }

    public List<Double> getValueList() {
        return valueList;
    }

    public void setValueList(List<Double> valueList) {
        this.valueList = valueList;
    }

    public List<Double> getSubList() {
        return subList;
    }

    public void setSubList(List<Double> subList) {
        this.subList = subList;
    }

    public List<String> getNameList() {
        return nameList;
    }

    public void setNameList(List<String> nameList) {
        this.nameList = nameList;
    }

    public List<String> getAbbrList() {
        return abbrList;
    }

    public void setAbbrList(List<String> abbrList) {
        this.abbrList = abbrList;
    }

    public Double getStar() {
        return star;
    }

    public void setStar(Double star) {
        this.star = star;
    }
}
