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
                    0.066d * Math.pow(PPMinus3.Sum(rice), 0.68d),
                    0.24d * Math.pow(PPMinus3.Sum(ln), 0.6d),
                    0.272d * Math.pow(PPMinus3.Sum(coordination), 0.72d),
                    0.06d * Math.pow(PPMinus3.Sum(stamina) * getLengthIndex(file.getLength()), 1.16d),
                    0.6d * Math.pow(PPMinus3.Sum(speed) * getBurstIndex(maxBurst), 0.52d),
                    0.25d * Math.pow(PPMinus3.Sum(precision), 0.73d),
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

                maxBurst = Math.max(
                        dividedByKey(d.getBurst(), key), maxBurst);

                rice.add(Math.sqrt(recordChord) * (
                                d.getStream() + d.getJack()
                        )
                );

                ln.add(Math.sqrt(recordChord) * (
                                d.getRelease() + d.getShield() + d.getReverseShield()
                        )
                );

                coordination.add(d.getBracket() + 10d * d.getHandLock() + 10d * d.getOverlap());
                stamina.add(
                        dividedByKey(d.getRiceDensity() + d.getLnDensity(), key)
                );
                speed.add(d.getSpeedJack() + d.getTrill());
                precision.add(d.getGrace() + affectedByOD(d.getDelayedTail(), file.getOD()));

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

        // todo 测试代码
        var a = Arrays.asList(
                /*
                PPMinus3.Sum(rice),
                PPMinus3.Sum(ln),
                PPMinus3.Sum(coordination),
                PPMinus3.Sum(stamina) * getLengthIndex(file.getLength()),
                PPMinus3.Sum(speed) * getBurstIndex(maxBurst),
                PPMinus3.Sum(precision),
                 */
                0.066d * Math.pow(PPMinus3.Sum(rice), 0.68d),
                0.24d * Math.pow(PPMinus3.Sum(ln), 0.6d),
                0.272d * Math.pow(PPMinus3.Sum(coordination), 0.72d),
                0.06d * Math.pow(PPMinus3.Sum(stamina) * getLengthIndex(file.getLength()), 1.16d),
                0.6d * Math.pow(PPMinus3.Sum(speed) * getBurstIndex(maxBurst), 0.52d),
                0.25d * Math.pow(PPMinus3.Sum(precision), 0.73d),

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

                maxBurst = Math.max(
                        dividedByKey(d.getBurst(), key), maxBurst);

                rice.add(Math.sqrt(recordChord) * (
                                d.getStream() + d.getJack()
                        )
                );

                ln.add(Math.sqrt(recordChord) * (
                                d.getRelease() + d.getShield() + d.getReverseShield()
                        )
                );

                coordination.add(d.getBracket() + 10d * d.getHandLock() + 10d * d.getOverlap());
                stamina.add(
                        dividedByKey(d.getRiceDensity() + d.getLnDensity(), key)
                );
                speed.add(d.getSpeedJack() + d.getTrill());
                precision.add(d.getGrace() + affectedByOD(d.getDelayedTail(), file.getOD()));

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

        switch (now.getType()) {
            case CIRCLE -> {
                // 叠键
                data.setJack(
                        calcJack(now.getStartTime(), after.getStartTime())
                );

                data.setSpeedJack(
                        calcSpeedJack(now.getStartTime(), after.getStartTime())
                );

                if (after.getType() == HitObjectType.LONGNOTE) {
                    data.setReverseShield(
                            calcReverseShield(now.getStartTime(), after.getStartTime())
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

        switch (now.getType()) {
            case CIRCLE -> {
                data.setRiceDensity(1);

                data.setStream(
                        calcStream(now.getStartTime(), aside.getStartTime())
                );

                data.setGrace(
                        calcGrace(now.getStartTime(), aside.getStartTime())
                );

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
                            calcStream(now.getEndTime(), aside.getEndTime())
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
        return 5d * ExponentFunction(aside_hit - hit, frac_4, frac_1);
    }

    private double calcBracket(int hit, int left_hit, int right_hit) {
        int lx = Math.abs(left_hit - hit);
        int rx = Math.abs(right_hit - hit);

        if (lx <= frac_2 && rx <= frac_2) {
            return ExponentFunction(lx, frac_4, frac_1) + ExponentFunction(rx, frac_4, frac_1);
        }
        return 0d;
    }

    private double calcGrace(int hit, int aside_hit) {
        if (aside_hit - hit <= frac_6) {
            return 10d * ExponentFunction(aside_hit - hit, frac_8, frac_1);
        }

        return 0d;
    }

    private double calcDelayedTail(int release, int aside_release) {
        if (aside_release - release <= frac_3) {
            return 10d * ExponentFunction(aside_release - release, frac_6, frac_2);
        }
        return 0d;
    }

    private double calcJack(int hit, int after_hit) {
        return 10d * InverseProportionalFunction(after_hit - hit, frac_2, frac_1, 0);
    }

    private double calcShield(int release, int after_hit) {
        return 5d * InverseProportionalFunction(after_hit - release, frac_2, frac_1, frac_16);
    }

    private double calcReverseShield(int hit, int after_hit) {
        return 10d * InverseProportionalFunction(after_hit - hit, frac_2, frac_1, 0);
    }

    private double calcSpeedJack(int hit, int after_hit) {
        return 5d * InverseProportionalFunction(after_hit - hit, frac_4, frac_2, 0);

    }

    private double calcHandLock(int hit, int aside_hit, int aside_release){
        var isHandLock = (aside_release - frac_8 > hit && aside_hit < hit - frac_8);

        if (aside_release > 0 && aside_hit > 0 && isHandLock) {
            return 1d;
        }

        return 0d;
    }

    private double calcOverlap(int hit, int release, int aside_hit, int aside_release){
        var isOverlap = ! ((hit < aside_hit && release < aside_release) || (hit > aside_hit && release > aside_release));

        if (isOverlap) {
            var delta = (Math.min(aside_release, release) - Math.max(aside_hit, hit));
            if (delta <= 0) return 0d;

            return 1.4d - (1.4d / Math.exp(delta * 1d / beat_2));
        }

        return 0d;
    }

    private double calcSliderDensity(int hit, int release) {
        int delta = release - hit;
        if (delta > 0) {
            return 1.4d - (0.4d / Math.exp(delta * 1d / beat_2));
        } else {
            return 0d;
        }
    }

    private double calcTrill(int hit, int left_hit, int right_hit) {
        int lx = Math.abs(left_hit - hit);
        int rx = Math.abs(right_hit - hit);

        if (lx <= frac_8 && rx > frac_8) {
            return 5d * ExponentFunction(rx, frac_4, frac_1);
        } else if (lx > frac_8 && rx <= frac_8) {
            return 5d * ExponentFunction(lx, frac_4, frac_1);
        } else {
            return 0d;
        }
    }

    // 获取长度因数。一般认为长度 = 300s 的时候，大概是 0.95x
    private double getLengthIndex(int millis) {
        return 1d - (1d / Math.exp(millis / 100_000d));
    }

    // 获取爆发因数。一般认为一计算元 5s 内 30 物件 的时候大概是 0.95x
    private double getBurstIndex(double burst) {
        return 1d - (1d / Math.exp(burst / 10d));
    }

    // 消除多键位带来的影响。4K：1.0，7K：0.755
    private double dividedByKey(double value, int key) {
        return value * 2d / Math.sqrt(key); //Math.sqrt(4d)
    }

    // 增强 OD 带来的影响。OD7: 1.0x, OD10: 4.48x
    public double affectedByOD(double value, double od) {
        return value * Math.exp(Math.max(od - 7d, 0d) / 2d);
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
