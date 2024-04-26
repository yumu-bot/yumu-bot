package com.now.nowbot.model.ppminus3;

import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.ppminus3.impl.PPMinus3ManiaImpl;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class PPMinus3 {
    protected List<Double> valueList = new ArrayList<>(6);

    protected List<String> nameList = new ArrayList<>(6);

    protected List<String> abbrList = new ArrayList<>(6);


    protected final int frac_MIN = 5;

    protected final int frac_16 = 21;

    protected final int frac_8 = 42;

    protected final int frac_6 = 55;

    protected final int frac_4 = 83;

    protected final int frac_3 = 111;

    protected final int frac_2 = 166;

    protected final int frac_1 = 333;

    protected final int calculateUnit = 2500; //一个标准单位计算元的区域（毫秒）。这段时间的数据，会统计到单一一个计算元中。

    public static PPMinus3 getInstance(OsuFile file) throws IOException {
        PPMinus3 pm3 = null;
        if (file.getMode() == OsuMode.MANIA) {
            pm3 = new PPMinus3ManiaImpl(file.getMania());
        }
        return pm3;
    }

    public List<Double> getValueList() {
        return valueList;
    }

    public void setValueList(List<Double> valueList) {
        this.valueList = valueList;
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

    // 公用算法

    // 收集数据
    @SafeVarargs
    @NonNull
    protected static List<Double> CollectData(List<Double>... lists) {
        List<Double> d = new ArrayList<>();

        for (var list : lists) {
            if (CollectionUtils.isEmpty(list)) {
                d.add(0d);
            }

            d.add(0.8d * list.stream().reduce(Double::max).orElse(0d)
                    + 0.2d * list.stream().reduce(Double::sum).orElse(0d) / list.size());

        }

        return d;
    }

    @NonNull
    protected static double NormalDistribution(int delta_time, int min_time, int max_time) {
        double sigma = max_time / 3f + min_time;
        return Math.exp(( - Math.pow(delta_time - min_time, 2f)) / (2 * Math.pow(sigma, 2f))) / ((Math.sqrt(2f * Math.PI) * sigma));
    }

    //根据和之前物件的差值，获取standard_time/x函数后的难度，默认0-1 min是限制区域，小于这个区域都会是某个最大值
    @NonNull
    protected static double InverseProportionalFunction(int delta_time, int min_time, int max_time, int standard_time) {
        if (standard_time == 0) return 0f;

        if (delta_time < min_time) {
            return 1.0f * min_time / standard_time;
        } else if (delta_time > max_time) {
            return 1.0f * max_time / standard_time;
        } else {
            return 1.0f * delta_time / standard_time;
        }
    }
}
