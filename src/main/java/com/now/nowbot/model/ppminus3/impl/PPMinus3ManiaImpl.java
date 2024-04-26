package com.now.nowbot.model.ppminus3.impl;

import com.now.nowbot.model.beatmapParse.HitObject;
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType;
import com.now.nowbot.model.beatmapParse.parse.ManiaBeatmapAttributes;
import com.now.nowbot.model.ppminus3.PPMinus3;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PPMinus3ManiaImpl extends PPMinus3 {
    // 主要六维
    List<Double> rice = new ArrayList<>();

    List<Double> ln = new ArrayList<>();

    List<Double> sv = new ArrayList<>();

    List<Double> stamina = new ArrayList<>();

    List<Double> speed = new ArrayList<>();

    List<Double> precision = new ArrayList<>();

    private double U = 0d;

    public PPMinus3ManiaImpl(ManiaBeatmapAttributes file) {
        calculate(file);

        valueList = PPMinus3.CollectData(rice, ln, sv, stamina, speed, precision);
        nameList = Arrays.asList("rice", "long note", "speed variation", "stamina", "speed", "precision");
        abbrList = Arrays.asList("RC", "LN", "SV", "STA", "SPD", "PRE");
    }

    // 主计算
    private void calculate(ManiaBeatmapAttributes file) {
        var hitObjectList = file.getHitObjects();
        if (CollectionUtils.isEmpty(hitObjectList)) return;

        // 初始化
        int now = hitObjectList.getFirst().getStartTime();
        int deltaNow = calculateUnit;
        int chord = 1;

        int key = file.getCS().intValue(); // 1~9

        List<List<HitObject>> noteCategory = new ArrayList<>(key);

        for (int i = 0; i < key; i++) {
            noteCategory.add(new ArrayList<>());
        }

        // 遍历数据，并存储在 noteCategory 中
        for (var h : hitObjectList) {
            int column = h.getColumn();
            if (column > key) return;

            noteCategory.get(column).add(h);
        }

        // 缓存
        var cache = new Cache(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ,0, 0, 0, 0, 0,0, 0, 0);

        // 遍历数据，开始计算
        for (var h : hitObjectList) {
            var recordChord = 1; //将被记录的多押数量
            if (h.getStartTime() == now) {
                chord++;
            } else {
                recordChord = chord;
                chord = 1;
            }

            now = h.getStartTime();
            var column = h.getColumn();

            var next = getNearestNote(h, noteCategory.get(column));

            // 左右的边界
            if (column == 0) {
                cache = merge2Cache(cache, calculateNote(h, null, getNearestNote(h, noteCategory.get(column + 1)), next));
            } else if (column == key - 1) {
                cache = merge2Cache(cache, calculateNote(h, getNearestNote(h, noteCategory.get(column - 1)), null, next));
            } else {
                cache = merge2Cache(cache, calculateNote(h, getNearestNote(h, noteCategory.get(column - 1)), getNearestNote(h, noteCategory.get(column + 1)), next));
            }


            // 计算元已满足要求，收集数据输出
            if (now - deltaNow >= calculateUnit || h.equals(hitObjectList.getLast())) {
                deltaNow += calculateUnit;

                U = Math.max(cache.C + cache.D, U);

                rice.add(Math.sqrt(recordChord) * (cache.C + cache.J + cache.B));
                ln.add(Math.sqrt(recordChord) * (cache.H + cache.O + cache.R + cache.E * 100f));
                sv.add(cache.M + cache.F + cache.W + cache.P + cache.T + cache.N);
                stamina.add(cache.C + cache.D);
                speed.add(cache.K * 5f + cache.I * 1000f);
                precision.add(cache.G * 5f + cache.Y);

                chord = 1;
                cache = new Cache(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ,0, 0, 0, 0, 0,0, 0, 0);
            }
        }
    }

    //计算 note 的值
    private Cache calculateNote(HitObject now, @Nullable HitObject left, @Nullable HitObject right, @Nullable HitObject after) {
        var cache = new Cache(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ,0, 0, 0, 0, 0,0, 0, 0);

        if (after != null) {
            cache = merge2Cache(cache, calculateAfter(now, after));
        }

        if (left != null && right != null) {
            cache = merge2Cache(cache, calculateBetween(now, left, right));
        }

        if (right != null) {
            cache = merge2Cache(cache, calculateAside(now, right));
        }

        if (left != null) {
            cache = merge2Cache(cache, calculateAside(now, left));
        }

        return cache;
    }

    // 比较该物件和同轨道的下一个物件
    private Cache calculateAfter(HitObject now, HitObject after) {
        // 缓存
        double S = 0d; double J = 0d; double B = 0d;
        double H = 0d; double O = 0d; double R = 0d; double E = 0d;
        double M = 0d; double F = 0d; double W = 0d; double P = 0d; double T = 0d; double N = 0d;
        double C = 0d; double D = 0d;
        double K = 0d; double I = 0d; double U = 0d;
        double G = 0d; double Y = 0d;

        // 叠键
        J += calcJack(now.getStartTime(), after.getStartTime());
        K += calcSpeedJack(now.getStartTime(), after.getStartTime());

        // 盾和反盾 o o== // o== o
        if (now.getType() == HitObjectType.LONGNOTE) {
            E += calcShield(now.getEndTime(), after.getStartTime());

        } else if (after.getType() == HitObjectType.LONGNOTE) {
            Y += calcShield(now.getStartTime(), after.getEndTime());
        }

        return new Cache(S, J, B, H, O, R, E, M, F, W, P, T, N, C, D, K, I, U, G, Y);
    }

    // 比较该物件和周围轨道的下一个物件
    private Cache calculateBetween(HitObject now, HitObject left, HitObject right) {
        // 缓存
        double S = 0d; double J = 0d; double B = 0d;
        double H = 0d; double O = 0d; double R = 0d; double E = 0d;
        double M = 0d; double F = 0d; double W = 0d; double P = 0d; double T = 0d; double N = 0d;
        double C = 0d; double D = 0d;
        double K = 0d; double I = 0d; double U = 0d;
        double G = 0d; double Y = 0d;

        /*
          裤衩
          o   o
            o
         */
        I += calcTrill(now.getStartTime(), left.getStartTime(), right.getStartTime());
        B += calcBracket(now.getStartTime(), left.getStartTime(), right.getStartTime());

        return new Cache(S, J, B, H, O, R, E, M, F, W, P, T, N, C, D, K, I, U, G, Y);
    }

    // 比较该物件和附近轨道的下一个物件
    private Cache calculateAside(HitObject now, HitObject aside) {
        // 缓存
        double S = 0d; double J = 0d; double B = 0d;
        double H = 0d; double O = 0d; double R = 0d; double E = 0d;
        double M = 0d; double F = 0d; double W = 0d; double P = 0d; double T = 0d; double N = 0d;
        double C = 0d; double D = 0d;
        double K = 0d; double I = 0d; double U = 0d;
        double G = 0d; double Y = 0d;

        S += calcStream(now.getStartTime(), aside.getStartTime());
        G += calcGrace(now.getStartTime(), aside.getStartTime());

        switch (now.getType()) {
            case CIRCLE -> {
                C++;

                if (aside.getType() == HitObjectType.LONGNOTE) {
                    H += calcHandLock(now.getStartTime(), aside.getStartTime(), aside.getEndTime());
                    R += calcStream(now.getStartTime(), aside.getEndTime());
                }
            }
            case LONGNOTE -> {
                D++;
                R += calcStream(now.getStartTime(), aside.getStartTime());

                if (aside.getType() == HitObjectType.LONGNOTE) {
                    Y += calcDelayedTail(now.getEndTime(), aside.getEndTime());
                    O += calcOverlap(now.getStartTime(), now.getEndTime(), aside.getStartTime(), aside.getEndTime());
                }
            }
        }

        return new Cache(S, J, B, H, O, R, E, M, F, W, P, T, N, C, D, K, I, U, G, Y);
    }

    // 返回这个物件与这一组物件对比，最靠上，或是被 LN 包围的这个 LN
    @Nullable
    private HitObject getNearestNote(HitObject now, List<HitObject> nearColumn) {
        var n = now.getStartTime();

        for (var h : nearColumn) {
            var s = h.getStartTime();
            var e = h.getEndTime();

            if (s - n >= 0 || (n >= s && e >= n)) {
                return h;
            }
        }

        return null;
    }

    private double calcStream(int hit, int aside_hit) {
        double p = 0f;
        if (aside_hit - hit < frac_1) {
            p = NormalDistribution(aside_hit - hit, frac_16, frac_1);
        }
        return p;
    }

    private double calcBracket(int hit, int left_hit, int right_hit) {
        double p = 0f;
        if (Math.abs(left_hit - hit) < frac_3 && Math.abs(right_hit - hit) < frac_3) {
            p += (NormalDistribution(left_hit - hit, frac_16, frac_3) + NormalDistribution(right_hit - hit, frac_16, frac_3)); // 180bpm 1/4
        }
        return p;
    }

    private double calcGrace(int hit, int aside_hit) {
        double p = 0f;
        if (aside_hit - hit <= frac_6 && aside_hit - hit >= frac_MIN) {
            p = NormalDistribution(aside_hit - hit, frac_16, frac_6); // 180bpm 1/4
        }

        return p;
    }

    private double calcDelayedTail(int release, int aside_release) {
        double p = 0f;
        if (aside_release - release <= frac_3) {
            p = NormalDistribution(aside_release - release, frac_16, frac_3); // 180bpm 1/4
        }

        return p;
    }

    private double calcJack(int hit, int after_hit) {
        double p = 0f;
        if (after_hit - hit <= frac_2) {
            p += InverseProportionalFunction(after_hit - hit, frac_8, calculateUnit, frac_4); // 180bpm 1/2
        }
        return p;
    }

    private double calcShield(int release, int after_hit) {
        return NormalDistribution(after_hit - release, frac_16, frac_1); // 180bpm 1/2
    }

    private double calcSpeedJack(int hit, int after_hit) {
        double p = 0f;
        if (after_hit - hit <= frac_4) {
            p += InverseProportionalFunction(after_hit - hit, frac_16, calculateUnit, frac_4); // 180bpm 1/4
        }
        return p;
    }

    private double calcHandLock(int hit, int aside_hit, int aside_release){
        double p = 0f;
        var isHandLock = (aside_release - frac_16 > hit && aside_hit < hit - frac_16);

        if (aside_release != 0 && aside_hit != 0 && isHandLock) {
            p++;
        }

        return p;
    }

    private double calcOverlap(int hit, int release, int aside_hit, int aside_release){
        double p;
        double delta = 0f;
        var isOverlap = ! ((hit < aside_hit && release < aside_release) || (hit > aside_hit && release > aside_release));

        if (isOverlap) {
            delta = (Math.min(aside_release, release) - Math.max(aside_hit, hit));
        }

        p = 6f / (5f + Math.exp(- delta / 1000f));

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

    private double calcTrill(int hit, int left_hit, int right_hit) {
        if (Math.abs(left_hit - hit) <= frac_8 && Math.abs(right_hit - hit) > frac_8) {
            return NormalDistribution(Math.abs(right_hit - hit), frac_8, frac_2);
        } else if (Math.abs(left_hit - hit) > frac_8 && Math.abs(right_hit - hit) <= frac_8) {
            return NormalDistribution(Math.abs(left_hit - hit), frac_8, frac_2);
        } else {
            return 0f;
        }
    }


    public List<Double> getRice() {
        return rice;
    }

    public void setRice(List<Double> rice) {
        this.rice = rice;
    }

    public List<Double> getLn() {
        return ln;
    }

    public void setLn(List<Double> ln) {
        this.ln = ln;
    }

    public List<Double> getSv() {
        return sv;
    }

    public void setSv(List<Double> sv) {
        this.sv = sv;
    }

    public List<Double> getStamina() {
        return stamina;
    }

    public void setStamina(List<Double> stamina) {
        this.stamina = stamina;
    }

    public List<Double> getSpeed() {
        return speed;
    }

    public void setSpeed(List<Double> speed) {
        this.speed = speed;
    }

    public List<Double> getPrecision() {
        return precision;
    }

    public void setPrecision(List<Double> precision) {
        this.precision = precision;
    }

    @Override
    public String toString() {
        return STR."PPMinus3ManiaImpl{rice=\{rice}, ln=\{ln}, sv=\{sv}, stamina=\{stamina}, speed=\{speed}, precision=\{precision}\{'}'}";
    }

    private record Cache(
            double S, double J, double B,
            double H, double O, double R, double E,
            double M, double F, double W, double P, double T, double N,
            double C, double D,
            double K, double I, double U,
            double G, double Y) {
    }

    private Cache merge2Cache(Cache c1, Cache c2) {
        return new Cache(
                c1.S + c2.S,
                c1.J + c2.J,
                c1.B + c2.B,
                c1.H + c2.H,
                c1.O + c2.O,
                c1.R + c2.R,
                c1.E + c2.E,
                c1.M + c2.M,
                c1.F + c2.F,
                c1.W + c2.W,
                c1.P + c2.P,
                c1.T + c2.T,
                c1.N + c2.N,
                c1.C + c2.C,
                c1.D + c2.D,
                c1.K + c2.K,
                c1.I + c2.I,
                c1.U + c2.U,
                c1.G + c2.G,
                c1.Y + c2.Y
        );
    }
}
