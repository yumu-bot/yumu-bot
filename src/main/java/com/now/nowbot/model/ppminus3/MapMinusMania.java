package com.now.nowbot.model.ppminus3;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.now.nowbot.model.osufile.HitObject;
import com.now.nowbot.model.osufile.OsuFile;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true, allowGetters = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class MapMinusMania extends MapMinus{
    // 谱面六维 + 额外二维
    private final List<Double> rice = new ArrayList<>();
    private final List<Double> longNote = new ArrayList<>();
    private final List<Double> speedVariation = new ArrayList<>();
    private final List<Double> stamina = new ArrayList<>();
    private final List<Double> speed = new ArrayList<>();
    private final List<Double> precision = new ArrayList<>();

    //细化二十多维
    private final List<Double> stream = new ArrayList<>();
    private final List<Double> jack = new ArrayList<>();
    private final List<Double> bracket = new ArrayList<>();

    private final List<Double> handLock = new ArrayList<>();
    private final List<Double> overlap = new ArrayList<>();
    private final List<Double> release = new ArrayList<>();
    private final List<Double> shield = new ArrayList<>();

    private final List<Double> bump = new ArrayList<>();
    private final List<Double> fastJam = new ArrayList<>();
    private final List<Double> slowJam = new ArrayList<>();
    private final List<Double> stop = new ArrayList<>();
    private final List<Double> teleport = new ArrayList<>();
    private final List<Double> negative = new ArrayList<>();

    private final List<Double> riceDensity = new ArrayList<>();
    private final List<Double> longNoteDensity = new ArrayList<>();

    private final List<Double> speedJack = new ArrayList<>();
    private final List<Double> trill = new ArrayList<>();
    private final List<Double> burst = new ArrayList<>();

    private final List<Double> grace = new ArrayList<>();
    private final List<Double> delayedTail = new ArrayList<>();

    int map_start_time = 0;
    int map_end_time = 0;

    int frac_16 = 21;
    int frac_8 = 42;
    int frac_6 = 55;
    int frac_4 = 83;
    int frac_3 = 111;
    int frac_2 = 166;
    int frac_1 = 333;
    int calculate_unit = 2500; //一个计算元的区域（毫秒）。这其中的数据会统计到一个计算元中。

    public MapMinusMania(OsuFile file) {
        //data 数据，不变。
        var hitObjects = file.getHitObjects();
        var timings = file.getTimings();
        int key = (int) Math.floor(file.getCS()); // 4 - 7
        boolean hasMidColumn = (key % 2) == 1;
        int midColumn = hasMidColumn ? (key - 1) / 2 : 0; //7K下，它是0123456的3

        if (!hitObjects.isEmpty()) {
            map_start_time = hitObjects.get(0).getStartTime();
            map_end_time = hitObjects.get(hitObjects.size() - 1).getEndTime();
        }

        double RC; double LN; double SV; double ST; double SP; double PR;

        double S = 0; double J = 0; double B = 0;
        double H = 0; double O = 0; double R = 0; double E = 0;
        double M = 0; double F = 0; double W = 0; double P = 0; double T = 0; double N = 0;
        double C = 0; double D = 0;
        double K = 0; double I = 0; double U = 0;
        double G = 0; double Y = 0;

        //index 指示当前算到哪里了
        int calculate_time = map_start_time + calculate_unit; //指示计算元的位置，根据 calculate_unit 不断刷新。
        int now_time = map_start_time; //指示目前算到了什么地方（毫秒）

        boolean now_hasLeft = false; //指示左手是否有物件
        boolean now_hasRight = false; //指示右手是否有物件

        int[] now_hit_arr = new int[key];
        int[] now_release_arr = new int[key];
        int[] now_flow_arr = new int[key];
        int now_chord = 0;

        //cache 缓存前一个块和当前的块。
        //int prev_time = map_start_time; //指示之前算到了什么地方（毫秒

        boolean prev_hasLeft = false; //指示左手是否有物件
        boolean prev_hasRight = false; //指示右手是否有物件

        int[] prev_hit_arr = new int[key];
        int[] prev_release_arr = new int[key];
        int[] prev_flow_arr = new int[key]; //描述前一个物件的流向。这个流向会根据这个物件再往前的物件决定。-2是踢墙，-1是左，0是无，1是右，2是双向。
        int prev_chord = 0;

        //初始化缓存
        for (int i = 0; i < key; i++) {
            prev_hit_arr[i] = 0;
            prev_release_arr[i] = 0;
            prev_flow_arr[i] = 0;
            now_hit_arr[i] = 0;
            now_release_arr[i] = 0;
            now_flow_arr[i] = 0;
        }

        //主计算
        for (HitObject h: hitObjects) {
            var type = h.getType();
            var now_hit = h.getStartTime();
            var now_release = h.getEndTime();
            var column = h.getColumn();

            //确定物件在哪只手
            if (hasMidColumn) {
                if (column < midColumn) {
                    now_hasLeft = true;
                } else if (column > midColumn) {
                    now_hasRight = true;
                }
            } else {
                if (column < key / 2) {
                    now_hasLeft = true;
                } else {
                    now_hasRight = true;
                }
            }

            //给边界的物件的左右赋值
            int prev_hit = prev_hit_arr[column];
            int prev_release = prev_release_arr[column];
            int prev_flow = prev_flow_arr[column];

            int prev_left_hit = 0;
            int prev_left_release = 0;
            int prev_right_hit = 0;
            int prev_right_release = 0;
            int prev_left_flow = -2;
            int prev_right_flow = -2;

            if (column == 0) {
                //最左
                prev_right_hit = prev_hit_arr[1];
                prev_right_release = prev_release_arr[1];
                prev_right_flow = prev_flow_arr[1];
            } else if (column == key - 1) {
                //最右
                prev_left_hit = prev_hit_arr[key - 2];
                prev_left_release = prev_release_arr[key - 2];
                prev_left_flow = prev_flow_arr[key - 2];
            } else {
                prev_left_hit = prev_hit_arr[column - 1];
                prev_right_hit = prev_hit_arr[column + 1];
                prev_left_release = prev_release_arr[column - 1];
                prev_right_release = prev_release_arr[column + 1];
                prev_left_flow = prev_flow_arr[column - 1];
                prev_right_flow = prev_flow_arr[column + 1];
            }


            //存储物件信息
            now_hit_arr[column] = now_hit;
            now_release_arr[column] = now_release;
            now_flow_arr[column] = calcFlow(prev_left_hit, prev_right_hit, prev_left_flow, prev_right_flow);
            now_chord ++;

            //真正的主计算
            switch (type) {
                case CIRCLE -> {
                    //计算S，J，B
                    S += calcStream(now_hit, prev_left_hit, prev_right_hit);
                    J += calcJack(now_hit, prev_hit);
                    B += calcBracket(now_hit, prev_left_hit, prev_right_hit);

                    C++;
                    K += calcSpeedJack(now_hit, prev_hit);
                    G += calcGrace(now_hit, prev_left_hit, prev_right_hit);
                }
                case LONGNOTE -> {
                    //计算H，O，R，E，还有Y
                    S += calcStream(now_hit, prev_left_hit, prev_right_hit);
                    J += calcJack(now_hit, prev_hit);
                    B += calcBracket(now_hit, prev_left_hit, prev_right_hit);

                    H += calcHandLock(now_hit, prev_left_hit, prev_left_release, prev_right_hit, prev_right_release);
                    O += calcOverlap(now_hit, now_release, prev_left_hit, prev_left_release, prev_right_hit, prev_right_release);
                    R += calcStream(now_release, prev_left_release, prev_right_release);
                    E += calcShield(now_hit, prev_release);

                    D += calcSliderDensity(now_hit, now_release);
                    K += calcSpeedJack(now_hit, prev_hit);
                    G += calcGrace(now_hit, prev_left_hit, prev_right_hit);
                    Y += calcGrace(now_release, prev_left_release, prev_right_release);
                }
            }

            //如果和上一个物件差距太远，则刷新prev和now数组
            if (Math.abs(now_hit - now_time) < frac_6) {
                now_time = now_hit;
            } else {

                //计算Trill
                I += calcTrill(now_hit, prev_left_hit, prev_right_hit, now_hasLeft, now_hasRight, prev_hasLeft, prev_hasRight, now_chord, prev_chord);

                //继承
                for (int i = 0; i < key; i++) {
                    if (now_hit_arr[i] != 0) {
                        prev_hit_arr[i] = now_hit_arr[i];
                        now_hit_arr[i] = 0;
                    }

                    if (now_release_arr[i] != 0) {
                        prev_release_arr[i] = now_release_arr[i];
                        now_release_arr[i] = 0;
                    }

                    if (now_flow_arr[i] != 0) {
                        prev_flow_arr[i] = now_flow_arr[i];
                        now_flow_arr[i] = 0;
                    }
                }
                prev_chord = now_chord;
                prev_hasLeft = now_hasLeft;
                prev_hasRight = now_hasRight;

                //清空now系列
                now_time = now_hit;
                now_hasLeft = false; //指示左手是否有物件
                now_hasRight = false; //指示右手是否有物件
                now_chord = 0;
            }

            //如果超出了计算元（或者结尾了），则刷新计算元，并且给MM赋值
            if (now_time >= calculate_time || now_time >= hitObjects.get(hitObjects.size() - 1).getStartTime()) {
                calculate_time += calculate_unit;

                U = Math.max(C + D, U);

                RC = Math.sqrt(now_chord) * (S + J + B);
                LN = H + O + R + E;
                SV = M + F + W + P + T + N;
                ST = C + D;
                SP = K + I + U;
                PR = G + Y;

                stream.add(S); jack.add(J); bracket.add(B);
                handLock.add(H); overlap.add(O); release.add(R); shield.add(E);
                bump.add(M); fastJam.add(F); slowJam.add(W); stop.add(P); teleport.add(T); negative.add(N);
                riceDensity.add(C); longNoteDensity.add(D);
                speedJack.add(K); trill.add(T); burst.add(U);
                grace.add(G); delayedTail.add(Y);

                rice.add(RC); longNote.add(LN); speedVariation.add(SV); stamina.add(ST); speed.add(SP); precision.add(PR);

                S = 0;  J = 0;  B = 0;
                H = 0;  O = 0;  R = 0;  E = 0;
                M = 0;  F = 0;  W = 0;  P = 0;  T = 0;  N = 0;
                C = 0;  D = 0;
                K = 0;  I = 0;  // U = 0; 这个不需要初始化
                G = 0;  Y = 0;
                
            }
        }
    }

    private double calcStream(int hit, int left_hit, int right_hit) {
        double p = 0f;
        if (Math.abs(hit - left_hit) < frac_1) {
            p += calcFunctionNormal(hit - left_hit, frac_16, frac_1);
        }
        if (Math.abs(hit - right_hit) < frac_1) {
            p += calcFunctionNormal(hit - right_hit, frac_16, frac_1);
        }
        return p;
    }

    private double calcBracket(int hit, int left_hit, int right_hit) {
        double p = 0f;
        if (Math.abs(hit - left_hit) < frac_3 && Math.abs(hit - right_hit) < frac_3) {
            p += (calcFunctionNormal(hit - left_hit, frac_16, frac_3)
            + calcFunctionNormal(hit - right_hit, frac_16, frac_3)); // 180bpm 1/4
        }
        return p;
    }

    private double calcGrace(int hit, int left_hit, int right_hit) {
        double p = 0f;
        if (left_hit != 0) {
            p += calcFunctionNormal(hit - left_hit, frac_16, frac_6); // 180bpm 1/4
        }
        if (right_hit != 0) {
            p += calcFunctionNormal(hit - right_hit, frac_16, frac_6); // 180bpm 1/4
        }

        return p;
    }

    private double calcJack(int hit, int prev_hit) {
        double p = 0f;
        p += calcFunction1_X(hit - prev_hit, frac_16, calculate_unit, frac_2); // 180bpm 1/2
        return p;
    }

    private double calcShield(int hit, int prev_release) {
        double p = 0f;
        p += calcFunctionNormal(hit - prev_release, frac_16, frac_2); // 180bpm 1/2
        return p;
    }

    private double calcSpeedJack(int hit, int prev_hit) {
        double p = 0f;
        p += calcFunction1_X(hit - prev_hit, frac_16, calculate_unit, frac_4); // 180bpm 1/4
        return p;
    }

    private double calcHandLock(int hit, int left_hit, int left_release, int right_hit, int right_release){
        double p = 0f;
        var isLeftLN = (left_release + frac_16 > hit && left_hit - frac_16 < hit);
        var isRightLN = (right_release + frac_16 > hit && right_hit - frac_16 < hit);

        if (left_hit != 0 && left_release != 0 && isLeftLN) {
            p++;
        }
        if (right_hit != 0 && right_release != 0 && isRightLN) {
            p++;
        }

        return p;
    }

    private double calcOverlap(int hit, int release, int left_hit, int left_release, int right_hit, int right_release){
        double p = 0f;
        var isLeftLN = (left_release + frac_16 > hit && left_hit - frac_16 < hit);
        var isRightLN = (right_release + frac_16 > hit && right_hit - frac_16 < hit);

        if (left_hit != 0 && left_release != 0 && isLeftLN) {
            p += (Math.min(left_release, release) - Math.max(left_release, release));
        }
        if (right_hit != 0 && right_release != 0 && isRightLN) {
            p += (Math.min(right_release, release) - Math.max(right_release, release));
        }

        return p;
    }

    private double calcSliderDensity(int hit, int release) {
        int delta = release - hit;
        if (delta > 0) {
            return 6f / (5f + Math.exp(- delta / 1000f));
        } else {
            return 0f;
        }
    }

    private double calcTrill(int hit, int left_hit, int right_hit, boolean now_hasLeft, boolean now_hasRight, boolean prev_hasLeft, boolean prev_hasRight, int now_chord, int prev_chord) {
        double chord_index = Math.sqrt(now_chord + prev_chord);

        if (now_hasLeft && prev_hasRight && !prev_hasLeft) {
            return chord_index * calcFunctionNormal(hit - right_hit, frac_16, frac_2);
        } else if (now_hasRight && prev_hasLeft && !prev_hasRight) {
            return chord_index * calcFunctionNormal(hit - left_hit, frac_16, frac_2);
        } else {
            return 0f;
        }
    }

    //根据和之前物件的差值，获取正态分布函数后的难度，默认0-1 min是限制区域，小于这个区域都会是 1，max是 3 sigma
    private double calcFunctionNormal(int delta_time, int min_time, int max_time) {
        double sigma = max_time / 3f + min_time;
        if (delta_time > min_time) {
            return Math.exp(- Math.pow(delta_time - min_time, 2f) / 2 * Math.pow(sigma, 2f)) / (Math.sqrt(2f * Math.PI) * sigma);
        } else {
            return 1f;
        }
    }

    //根据和之前物件的差值，获取standard_time/x函数后的难度，默认0-1 min是限制区域，小于这个区域都会是某个最大值
    private double calcFunction1_X(int delta_time, int min_time, int max_time, int standard_time) {
        if (delta_time < min_time) {
            return 1.0f * min_time / standard_time;
        } else if (delta_time > max_time) {
            return 1.0f * max_time / standard_time;
        } else {
            return 1.0f * delta_time / standard_time;
        }
    }

    /*
    真值表
    L/R -2  -1  0   1   2
    -2  0   1   1   1   1
    -1  -1  -1  -1  2   -1
    0   -1  -1  0   1   -1
    1   -1  2   1   1   -1
    2   -1  1   1   1   2

     */
    private int calcFlow(int prev_left, int prev_right, int prev_left_flow, int prev_right_flow) {
        int flow = 0;

        if (prev_left + prev_right == 0){
            return 0;
        } else if (prev_left == 0){
            flow = 1;
        } else if (prev_right == 0){
            flow = -1;
        } else if (Math.abs(prev_left - prev_right) < frac_6) {
            var line1 = new int[]{0, 1, 1, 1, 1};
            var line2 = new int[]{-1, -1, -1, 2, -1};
            var line3 = new int[]{-1, -1, 0, 1, -1};
            var line4 = new int[]{-1, 2, 1, 1, -1};
            var line5 = new int[]{-1, 1, 1, 1, 2};
            var matrix = new int[][]{line1, line2, line3, line4, line5};

            flow = matrix[prev_left_flow + 2][prev_right_flow + 2];
        } else {
            if (prev_left - prev_right > frac_6) {
                flow = prev_left_flow;
            } else if (prev_right - prev_left > frac_6) {
                flow = prev_right_flow;
            }
        }

        return flow;
    }

    public List<Double> getStream() {
        return stream;
    }

    public List<Double> getJack() {
        return jack;
    }

    public List<Double> getBracket() {
        return bracket;
    }

    public List<Double> getHandLock() {
        return handLock;
    }

    public List<Double> getOverlap() {
        return overlap;
    }

    public List<Double> getRelease() {
        return release;
    }

    public List<Double> getShield() {
        return shield;
    }

    public List<Double> getBump() {
        return bump;
    }

    public List<Double> getFastJam() {
        return fastJam;
    }

    public List<Double> getSlowJam() {
        return slowJam;
    }

    public List<Double> getStop() {
        return stop;
    }

    public List<Double> getTeleport() {
        return teleport;
    }

    public List<Double> getNegative() {
        return negative;
    }

    public List<Double> getRiceDensity() {
        return riceDensity;
    }

    public List<Double> getLongNoteDensity() {
        return longNoteDensity;
    }

    public List<Double> getSpeedJack() {
        return speedJack;
    }

    public List<Double> getTrill() {
        return trill;
    }

    public List<Double> getBurst() {
        return burst;
    }

    public List<Double> getGrace() {
        return grace;
    }

    public List<Double> getDelayedTail() {
        return delayedTail;
    }

}
