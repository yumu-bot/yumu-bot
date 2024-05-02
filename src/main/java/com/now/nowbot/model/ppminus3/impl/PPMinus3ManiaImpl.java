package com.now.nowbot.model.ppminus3.impl;

import com.now.nowbot.model.beatmapParse.HitObject;
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType;
import com.now.nowbot.model.beatmapParse.parse.ManiaBeatmapAttributes;
import com.now.nowbot.model.ppminus3.PPMinus3;
import com.now.nowbot.model.ppminus3.data.PPMinus3ManiaData;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PPMinus3ManiaImpl extends PPMinus3 {
    // 主要六维
    List<Double> rice = new ArrayList<>();

    List<Double> ln = new ArrayList<>();

    List<Double> coordination = new ArrayList<>();

    List<Double> stamina = new ArrayList<>();

    List<Double> speed = new ArrayList<>();

    List<Double> precision = new ArrayList<>();

    List<Double> sv = new ArrayList<>();

    private double maxBurst = 0d;

    public PPMinus3ManiaImpl(ManiaBeatmapAttributes file, boolean isTest) {
        if (isTest) {
            valueList = calculateTest(file);
        } else {
            calculate(file);
            valueList = Arrays.asList(
                    1.32d * Math.pow(PPMinus3.Sum(rice), 0.37d),
                    1.68d * Math.pow(PPMinus3.Sum(ln), 0.35d),
                    Math.pow(PPMinus3.Sum(coordination), 0.48d),
                    0.43d * Math.pow(dividedByKey(PPMinus3.Sum(stamina), file.getCS().intValue()) *
                            getLengthIndex(file.getLength()), 0.83d),
                    2.65d * Math.pow(PPMinus3.Sum(speed) *
                            getBurstIndex(dividedByKey(maxBurst, file.getCS().intValue())), 0.32d),
                    0.8d * Math.pow(PPMinus3.Sum(precision), 0.54d),
                    PPMinus3.Sum(sv)
            );
        }
    }

    public PPMinus3ManiaImpl(ManiaBeatmapAttributes file) {
        new PPMinus3ManiaImpl(file, false);
    }

    private List<Double> calculateTest(ManiaBeatmapAttributes file) {

        List<Double> stream = new ArrayList<>();
        List<Double> jack = new ArrayList<>();

        List<Double> release = new ArrayList<>();
        List<Double> shield = new ArrayList<>();
        List<Double> reverseShield = new ArrayList<>();

        List<Double> bracket = new ArrayList<>();
        List<Double> handLock = new ArrayList<>();
        List<Double> overlap = new ArrayList<>();

        List<Double> riceDensity = new ArrayList<>();
        List<Double> lnDensity = new ArrayList<>();

        List<Double> speedJack = new ArrayList<>();
        List<Double> trill = new ArrayList<>();
        List<Double> burst = new ArrayList<>();

        List<Double> grace = new ArrayList<>();
        List<Double> delayedTail = new ArrayList<>();

        List<Double> bump = new ArrayList<>();
        List<Double> stop = new ArrayList<>();
        List<Double> fastJam = new ArrayList<>();
        List<Double> slowJam = new ArrayList<>();
        List<Double> teleport = new ArrayList<>();
        List<Double> negative = new ArrayList<>();

        // 下面代码的照搬
        var hitObjectList = file.getHitObjects();
        if (CollectionUtils.isEmpty(hitObjectList)) return new ArrayList<>();

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
            if (column > key) return new ArrayList<>();

            noteCategory.get(column).add(h);
        }

        // 缓存
        var d = new PPMinus3ManiaData();

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

            var next = getTopestNote(h, noteCategory.get(column));

            // 左右的边界
            if (column == 0) {
                d.add(calculateNote(h, null, getNearestNote(h, noteCategory.get(column + 1)), next));
            } else if (column == key - 1) {
                d.add(calculateNote(h, getNearestNote(h, noteCategory.get(column - 1)), null, next));
            } else {
                d.add(calculateNote(h, getNearestNote(h, noteCategory.get(column - 1)), getNearestNote(h, noteCategory.get(column + 1)), next));
            }

            d.increaseBurst();

            // 计算元已满足要求，收集数据输出
            if (now - deltaNow >= calculateUnit || h.equals(hitObjectList.getLast())) {
                deltaNow += calculateUnit;

                maxBurst = Math.max(d.getBurst(), maxBurst);

                rice.add(Math.sqrt(recordChord) * (
                                d.getStream() + d.getJack()
                        )
                );

                ln.add(Math.sqrt(recordChord) * (
                                d.getRelease() + d.getShield() + d.getReverseShield()
                        )
                );

                coordination.add(d.getBracket() + d.getHandLock() + d.getOverlap());

                stamina.add(d.getRiceDensity() + d.getLnDensity());
                speed.add(d.getSpeedJack() + d.getTrill());
                precision.add(d.getGrace() + d.getDelayedTail() + file.getOD());

                sv.add(d.getBump() + d.getFastJam() + d.getSlowJam() + d.getStop() + d.getTeleport() + d.getNegative());


                chord = 1;

                {
                    stream.add(d.getStream());
                    jack.add(d.getJack());

                    release.add(d.getRelease());
                    shield.add(d.getShield());
                    reverseShield.add(d.getReverseShield());

                    bracket.add(d.getBracket());
                    handLock.add(d.getHandLock());
                    overlap.add(d.getOverlap());

                    riceDensity.add(d.getRiceDensity());
                    lnDensity.add(d.getLnDensity());

                    speedJack.add(d.getSpeedJack());
                    trill.add(d.getTrill());
                    burst.add(d.getBurst());

                    grace.add(d.getGrace());
                    delayedTail.add(d.getDelayedTail());

                    bump.add(d.getBump());
                    stop.add(d.getStop());
                    fastJam.add(d.getFastJam());
                    slowJam.add(d.getSlowJam());
                    teleport.add(d.getTeleport());
                    negative.add(d.getNegative());
                }


                d.clear();
            }
        }

        var a = Arrays.asList(
                1.32d * Math.pow(PPMinus3.Sum(rice), 0.37d),
                1.68d * Math.pow(PPMinus3.Sum(ln), 0.35d),
                Math.pow(PPMinus3.Sum(coordination), 0.48d),
                0.43d * Math.pow(dividedByKey(PPMinus3.Sum(stamina), file.getCS().intValue()) *
                        getLengthIndex(file.getLength()), 0.83d),
                2.65d * Math.pow(PPMinus3.Sum(speed) *
                        getBurstIndex(dividedByKey(maxBurst, file.getCS().intValue())), 0.32d),
                0.8d * Math.pow(PPMinus3.Sum(precision), 0.54d),
                PPMinus3.Sum(sv)
        );

        var a1 = new ArrayList<>(a);

        List<Double> b = PPMinus3.CollectData(stream, jack, release, shield, reverseShield, bracket, handLock, overlap, riceDensity, lnDensity, speedJack, trill, burst, grace, delayedTail, bump, stop, fastJam, slowJam, teleport, negative);

        a1.addAll(b);

        return a1;
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
        var d = new PPMinus3ManiaData();

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

            var next = getTopestNote(h, noteCategory.get(column));

            // 左右的边界
            if (column == 0) {
                d.add(calculateNote(h, null, getNearestNote(h, noteCategory.get(column + 1)), next));
            } else if (column == key - 1) {
                d.add(calculateNote(h, getNearestNote(h, noteCategory.get(column - 1)), null, next));
            } else {
                d.add(calculateNote(h, getNearestNote(h, noteCategory.get(column - 1)), getNearestNote(h, noteCategory.get(column + 1)), next));
            }

            d.increaseBurst();

            // 计算元已满足要求，收集数据输出
            if (now - deltaNow >= calculateUnit || h.equals(hitObjectList.getLast())) {
                deltaNow += calculateUnit;

                maxBurst = Math.max(d.getBurst(), maxBurst);

                rice.add(Math.sqrt(recordChord) * (
                                d.getStream() + d.getJack()
                        )
                );

                ln.add(Math.sqrt(recordChord) * (
                                d.getRelease() + d.getShield() + d.getReverseShield()
                        )
                );

                coordination.add(d.getBracket() + d.getHandLock() + d.getOverlap());

                stamina.add(d.getRiceDensity() + d.getLnDensity());
                speed.add(d.getSpeedJack() + d.getTrill());
                precision.add(d.getGrace() + d.getDelayedTail() + file.getOD());

                sv.add(d.getBump() + d.getFastJam() + d.getSlowJam() + d.getStop() + d.getTeleport() + d.getNegative());

                chord = 1;
                d.clear();
            }
        }
    }

    //计算 note 的值
    private PPMinus3ManiaData calculateNote(HitObject now, @Nullable HitObject left, @Nullable HitObject right, @Nullable HitObject after) {
        var data = new PPMinus3ManiaData();

        if (after != null) {
            data.add(calculateAfter(now, after));
        }

        if (left != null && right != null) {
            data.add(calculateBetween(now, left, right));
        }

        if (right != null) {
            data.add(calculateAside(now, right));
        }

        if (left != null) {
            data.add(calculateAside(now, left));
        }

        return data;
    }

    // 比较该物件和同轨道的下一个物件
    private PPMinus3ManiaData calculateAfter(HitObject now, HitObject after) {
        // 缓存
        var data = new PPMinus3ManiaData();

        // 叠键

        data.setJack(
                calcJack(now.getStartTime(), after.getStartTime())
        );

        data.setSpeedJack(
                calcSpeedJack(now.getStartTime(), after.getStartTime())
        );

        // 盾和反盾 o o== // o== o

        switch (now.getType()) {
            case CIRCLE -> {
                if (after.getType() == HitObjectType.LONGNOTE) {
                    data.setReverseShield(
                            calcShield(now.getStartTime(), after.getStartTime())
                    );
                }
            }
            case LONGNOTE -> {

                switch (after.getType()) {
                    case CIRCLE -> data.setShield(
                            calcShield(now.getEndTime(), after.getStartTime())
                    );
                    case LONGNOTE -> data.setRelease(
                            Math.pow(Math.min(now.getEndTime() - now.getStartTime(), 100) / 100d, 2d) * calcStream(now.getEndTime(), after.getStartTime())
                    ); //这里避免超短面 （小于 100 ms） 增加 release 的值
                }
            }
        }

        return data;
    }

    // 比较该物件和周围轨道的下一个物件
    private PPMinus3ManiaData calculateBetween(HitObject now, HitObject left, HitObject right) {
        // 缓存
        var data = new PPMinus3ManiaData();

        /*
          裤衩
          o   o
            o
         */
        data.setTrill(
                calcTrill(now.getStartTime(), left.getStartTime(), right.getStartTime())
        );

        data.setBracket(
                calcBracket(now.getStartTime(), left.getStartTime(), right.getStartTime()));

        return data;
    }

    // 比较该物件和附近轨道的下一个物件
    private PPMinus3ManiaData calculateAside(HitObject now, HitObject aside) {
        // 缓存
        var data = new PPMinus3ManiaData();

        data.setStream(
                calcStream(now.getStartTime(), aside.getStartTime())
        );

        data.setGrace(
                calcGrace(now.getStartTime(), aside.getStartTime())
        );

        switch (now.getType()) {
            case CIRCLE -> {
                data.setRiceDensity(1);

                if (aside.getType() == HitObjectType.LONGNOTE) {
                    data.setHandLock(
                            calcHandLock(now.getStartTime(), aside.getStartTime(), aside.getEndTime())
                    );
                    data.setRelease(
                            calcStream(now.getStartTime(), aside.getEndTime())
                    );
                }
            }
            case LONGNOTE -> {
                data.setLnDensity(
                        calcSliderDensity(now.getStartTime(), now.getEndTime())
                );

                if (aside.getType() == HitObjectType.LONGNOTE) {
                    data.setRelease(
                            Math.pow(Math.min(now.getEndTime() - now.getStartTime(), 100) / 100d, 2d) * calcStream(now.getEndTime(), aside.getEndTime())
                    );

                    data.setDelayedTail(
                            calcDelayedTail(now.getEndTime(), aside.getEndTime())
                    );

                    data.setOverlap(
                            calcOverlap(now.getStartTime(), now.getEndTime(), aside.getStartTime(), aside.getEndTime())
                    );
                }
            }
        }

        return data;
    }

    // 返回这个物件与这一组物件对比，最靠上，或是被 LN 包围的这个 LN
    @Nullable
    private HitObject getNearestNote(HitObject now, List<HitObject> asideColumn) {
        var n = now.getStartTime();

        for (var h : asideColumn) {
            var s = h.getStartTime();
            var e = h.getEndTime();

            if (s - n >= 0 || (n >= s && e >= n)) {
                return h;
            }
        }

        return null;
    }

    // 获取同轨道靠上面的物件
    @Nullable
    private HitObject getTopestNote(HitObject now, List<HitObject> thisColumn) {
        var n = now.getStartTime();

        for (var h : thisColumn) {
            if (h.getStartTime() - n > 0) {
                return h;
            }
        }

        return null;
    }

    private double calcStream(int hit, int aside_hit) {
        double p = 0f;
        if (aside_hit - hit < frac_1) {
            p = 100 * NormalDistribution(aside_hit - hit, frac_16, frac_1);
        }
        return p;
    }

    private double calcBracket(int hit, int left_hit, int right_hit) {
        double p = 0f;
        if (Math.abs(left_hit - hit) <= frac_2 && Math.abs(right_hit - hit) <= frac_2) {
            p = 100 * NormalDistribution(left_hit - hit, frac_8, frac_2) + 100 * NormalDistribution(right_hit - hit, frac_8, frac_2); // 180bpm 1/2
        }
        return p;
    }

    private double calcGrace(int hit, int aside_hit) {
        double p = 0f;
        if (aside_hit - hit <= frac_6 && aside_hit - hit >= frac_MIN) {
            p = 200 * NormalDistribution(aside_hit - hit, frac_16, frac_6); // 180bpm 1/4
        }

        return p;
    }

    private double calcDelayedTail(int release, int aside_release) {
        double p = 0f;
        if (aside_release - release <= frac_3) {
            p = 100 * NormalDistribution(aside_release - release, frac_16, frac_3); // 180bpm 1/4
        }

        return p;
    }

    private double calcJack(int hit, int after_hit) {
        double p = 0f;
        if (after_hit - hit <= frac_2) {
            p = 200 * NormalDistribution(after_hit - hit, frac_8, frac_2); // 180bpm 1/2
        }
        return p;
    }

    private double calcShield(int release, int after_hit) {
        return 300 * NormalDistribution(after_hit - release, frac_8, frac_1); // 180bpm 1/2
    }

    private double calcSpeedJack(int hit, int after_hit) {
        double p = 0f;
        if (after_hit - hit <= frac_3) {
            p = 200 * NormalDistribution(after_hit - hit, frac_8, frac_3); // 180bpm 1/4
        }
        return p;
    }

    private double calcHandLock(int hit, int aside_hit, int aside_release){
        var isHandLock = (aside_release - frac_16 > hit && aside_hit < hit - frac_16);

        if (aside_release != 0 && aside_hit != 0 && isHandLock) {
            return 1f;
        }

        return 0f;
    }

    private double calcOverlap(int hit, int release, int aside_hit, int aside_release){
        var isOverlap = ! ((hit < aside_hit && release < aside_release) || (hit > aside_hit && release > aside_release));

        if (isOverlap) {
            var delta = (Math.min(aside_release, release) - Math.max(aside_hit, hit));

            return 1.2f - (0.2f / Math.exp(- delta / 1000f));
        }

        return 0f;
    }

    private double calcSliderDensity(int hit, int release) {
        int delta = release - hit;
        if (delta > 0) {
            return 1.4f - (0.4f / Math.exp(- delta / 1000f));
        } else {
            return 0f;
        }
    }

    private double calcTrill(int hit, int left_hit, int right_hit) {
        if (Math.abs(left_hit - hit) <= frac_8 && Math.abs(right_hit - hit) > frac_8) {
            return 150 * NormalDistribution(Math.abs(right_hit - hit), frac_8, frac_2);
        } else if (Math.abs(left_hit - hit) > frac_8 && Math.abs(right_hit - hit) <= frac_8) {
            return 150 * NormalDistribution(Math.abs(left_hit - hit), frac_8, frac_2);
        } else {
            return 0f;
        }
    }

    // 获取长度因数。一般认为 300s 的时候大概是 0.95x
    private double getLengthIndex(int length) {
        return 1 - (1 / Math.exp(length / 100d));
    }

    // 获取爆发因数。一般认为 5s 内 28 物件 的时候大概是 0.95x
    private double getBurstIndex(double burst) {
        return 1 - (1 / Math.exp(burst) / 7);
    }

    // 消除多键位带来的影响
    private double dividedByKey(double value, int key) {
        return value / Math.pow(key, 0.8);
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
        return STR."PPMinus3ManiaImpl{rice=\{rice}, ln=\{ln}, coordination=\{coordination}, stamina=\{stamina}, speed=\{speed}, precision=\{precision}, sv=\{sv}, maxBurst=\{maxBurst}, valueList=\{valueList}, nameList=\{nameList}, abbrList=\{abbrList}\{'}'}";
    }

    /*
    @Override
    public List<Double> getValueList() {
        return PPMinus3.CollectData(rice, ln, coordination, stamina, speed, precision, sv);
    }

     */

    @Override
    public List<String> getNameList() {
        return Arrays.asList("rice", "long note", "coordination", "stamina", "speed", "precision", "speed variation");
    }

    @Override
    public List<String> getAbbrList() {
        return Arrays.asList("RC", "LN", "CO", "ST", "SP", "PR", "SV");
    }
}
