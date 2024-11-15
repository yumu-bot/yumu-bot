package com.now.nowbot.model.mapminus.impl;

import com.now.nowbot.model.beatmapParse.HitObject;
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType;
import com.now.nowbot.model.beatmapParse.parse.ManiaBeatmapAttributes;
import com.now.nowbot.model.mapminus.PPMinus3;
import com.now.nowbot.model.mapminus.data.PPMinus3ManiaData;
import org.springframework.lang.NonNull;
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

    // 子参数
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

    private double maxBurst = 0d;

    private final ManiaBeatmapAttributes file;

    public PPMinus3ManiaImpl(ManiaBeatmapAttributes file) {
        this.file = file;
        calculate();
    }

    // 主计算
    private void calculate() {
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
            if (key <= 1) {
                //1K 没有左右
                d.add(calculateNote(h, null, null, next));
            } else if (column == 0) {
                d.add(calculateNote(h, null, getNearestNote(h, noteCategory.get(column + 1)), next));
            } else if (column == key - 1) {
                d.add(calculateNote(h, getNearestNote(h, noteCategory.get(column - 1)), null, next));
            } else if (column >= key) {
                continue;
            } else {
                d.add(calculateNote(h, getNearestNote(h, noteCategory.get(column - 1)), getNearestNote(h, noteCategory.get(column + 1)), next));
            }

            d.increaseBurst();

            // 计算元已满足要求，收集数据输出
            if (now - deltaNow >= calculateUnit || h.equals(hitObjectList.getLast())) {
                deltaNow += calculateUnit;

                maxBurst = Math.max(d.getBurst(), maxBurst);

                rice.add(
                        Math.sqrt(recordChord) * (d.getStream() + d.getJack())
                );
                ln.add(
                        Math.sqrt(recordChord) * (d.getRelease() + d.getShield() + d.getReverseShield())
                );
                coordination.add(d.getBracket() + 10d * d.getHandLock() + 10d * d.getOverlap());
                stamina.add(d.getRiceDensity() + d.getLnDensity());
                speed.add(d.getSpeedJack() + d.getTrill());
                precision.add(d.getGrace() + affectedByOD(d.getDelayedTail(), file.getOD()));
                sv.add(d.getBump() + d.getFastJam() + d.getSlowJam() + d.getStop() + d.getTeleport() + d.getNegative());

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

                chord = 1;
                d.clear();
            }
        }
    }

    @NonNull
    //计算 note 的值
    private PPMinus3ManiaData calculateNote(@NonNull HitObject now, @Nullable HitObject left, @Nullable HitObject right, @Nullable HitObject after) {
        var data = new PPMinus3ManiaData();

        //计算自己
        data.add(calculateThis(now));

        //计算同轨：叠、盾
        if (after != null) {
            data.add(calculateAfter(now, after));
        }

        //计算交叉轨：交互、裤衩
        if (left != null && right != null) {
            data.add(calculateBetween(now, left, right));
        }

        //计算侧轨：滑键/楼梯、切换
        if (right != null) {
            data.add(calculateAside(now, right));
        }

        if (left != null) {
            data.add(calculateAside(now, left));
        }

        return data;
    }

    // 返回此物件的一些特征值
    private PPMinus3ManiaData calculateThis(@NonNull HitObject now) {
        // 缓存
        var data = new PPMinus3ManiaData();
        switch (now.getType()) {
            case CIRCLE ->
                    data.setRiceDensity(1);
            case LONGNOTE ->
                    data.setLnDensity(
                            calcLnDensity(now.getStartTime(), now.getEndTime())
                    );
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

    // 返回这个物件与这一组物件对比，最靠上，或是被 LN 包围的这个 LN，使用二分法查询
    @Nullable
    private HitObject getNearestNote(HitObject now, @Nullable List<HitObject> asideColumn) {
        if (CollectionUtils.isEmpty(asideColumn)) return null;

        var n = now.getStartTime();

        int min = 0;
        int max = asideColumn.size() - 1;

        // 筛除超过区域的物件
        if (asideColumn.getFirst().getStartTime() > n) return asideColumn.getFirst();
        if (asideColumn.getLast().getEndTime() < n) return null;

        while (max - min > 1) {
            int mid = (min + max) / 2;
            int m = asideColumn.get(mid).getStartTime();

            if (n < m) {
                max = mid;
            } else {
                //n >= m
                min = mid;
            }
        }

        int xs = asideColumn.get(max).getStartTime();
        int xe = asideColumn.get(max).getEndTime();
        int ms = asideColumn.get(min).getStartTime();
        int me = asideColumn.get(min).getEndTime();

        if (min == asideColumn.size() - 1) return null;

        if (n >= ms) {
            if (n <= me) return asideColumn.get(min);
            if (n <= xs || n <= xe) return asideColumn.get(max); //n <= xe 是针对最后的物件做的
        }


        /*

        for (var h : asideColumn) {
            var s = h.getStartTime();
            var e = h.getEndTime();

            if (s - n >= 0 || (n >= s && e >= n)) {
                return h;
            }
        }

         */

        return null;
    }

    // 获取同轨道靠上面的物件，使用二分法查询
    @Nullable
    private HitObject getTopestNote(HitObject now, List<HitObject> thisColumn) {
        var n = now.getStartTime();

        int min = 0;
        int max = thisColumn.size() - 1;

        // 筛除超过区域的物件（最后一个）
        if (thisColumn.getLast().getStartTime() == n) return null;

        while (max - min > 1) {
            int mid = (min + max) / 2;
            int m = thisColumn.get(mid).getStartTime();

            if (n < m) {
                max = mid;
            } else {
                //n >= m
                min = mid;
            }
        }

        return thisColumn.get(max);

        /*
        for (var h : thisColumn) {
            if (h.getStartTime() - n > 0) {
                return h;
            }
        }

        return null;
         */

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
        return 5d * ExponentFunction(aside_hit - hit, frac_12, frac_6);
    }

    private double calcDelayedTail(int release, int aside_release) {
        return 10d * ExponentFunction(aside_release - release, frac_6, frac_3);
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

    private double calcLnDensity(int hit, int release) {
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

    // 由于爆发是记录的最大值，在低难度内，爆发需要缩减至 0。
    private double decreaseLowBurst(double burst) {
        if (burst >= 30) return burst;

        return burst * (Math.E * (burst / 30) / Math.exp(burst / 30));
    }

    // 增强 OD 带来的影响。OD7: 1.0x, OD10: 4.48x
    public double affectedByOD(double value, double od) {
        return value * Math.exp(Math.max(od - 7d, 0d) / 2d);
    }

    // kotlin 读不到下面的东西？
    public List<Double> getValues() {
        return getValueList();
    }

    // kotlin 读不到下面的东西？
    public Double getSR() {
        return getStar();
    }

    // 重写
    @Override
    public List<Double> getValueList() {
        return Arrays.asList(
                // v0.7

                PPMinus3.Eval(rice, 0.06367296d, 0.66588798d),
                PPMinus3.Eval(ln, 0.5184874d, 0.4541511d),
                PPMinus3.Eval(coordination, 0.1309091d
                        , 0.8785463d),
                PPMinus3.Eval(precision, 0.1974495d, 0.7550981d),
                PPMinus3.Eval(speed, 0.1584118d, 0.7111474d, null, decreaseLowBurst(maxBurst)),
                PPMinus3.Eval(stamina, 0.17030721d, 0.9559876d, getLengthIndex(file.getLength())),
                PPMinus3.Eval(sv, 1d, 1d)

                /*
                // v0.6.2

                PPMinus3.Eval(rice, 0.066d, 0.68d),
                PPMinus3.Eval(ln, 0.24d, 0.6d),
                PPMinus3.Eval(coordination, 0.272d, 0.72d),
                PPMinus3.Eval(stamina, 0.068d, 1.24d, getLengthIndex(file.getLength())),
                PPMinus3.Eval(speed, 0.112d, 0.8d, null, decreaseLowBurst(maxBurst)),
                PPMinus3.Eval(precision, 0.25d, 0.73d),
                PPMinus3.Eval(sv, 1d, 1d)

                 */

                /*

                // test
                PPMinus3.Eval(rice),
                PPMinus3.Eval(ln),
                PPMinus3.Eval(coordination),
                PPMinus3.Eval(stamina, 1d, 1d, getLengthIndex(file.getLength())),
                PPMinus3.Eval(speed, 1d, 1d, null, decreaseLowBurst(maxBurst)),
                PPMinus3.Eval(precision),
                PPMinus3.Eval(sv)

                 */
        );
    }

    @Override
    public List<Double> getSubList() {
        return PPMinus3.CollectData(stream, jack, release, shield, reverseShield, bracket, handLock, overlap, riceDensity, lnDensity, speedJack, trill, burst, grace, delayedTail, bump, stop, fastJam, slowJam, teleport, negative);
    }

    @Override
    public List<String> getNameList() {
        return Arrays.asList("rice", "long note", "coordination", "stamina", "speed", "precision", "speed variation");
    }

    @Override
    public List<String> getAbbrList() {
        return Arrays.asList("RC", "LN", "CO", "PR", "SP", "ST", "SV");
    }

    // 新版获取星数的方法
    @Override
    public Double getStar() {
        if (CollectionUtils.isEmpty(getValueList()) || getValueList().size() < 6) return 0d;

        List<Double> values = getValueList().subList(0, 6);
        List<Double> powers = Arrays.asList(0.8d, 0.8d, 0.8d, 0.4d, 0.6d, 1.2d);
        double divided = 3.6d;

        double star = 0d;

        for (int i = 0; i < 6; i++) {
            var p = powers.get(i);
            var v = values.get(i);

            star += p * v;
        }

        star /= divided;

        return 0.6 * star + 0.4 * values.stream().sorted().toList().get(4);
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

    public List<Double> getCoordination() {
        return coordination;
    }

    public void setCoordination(List<Double> coordination) {
        this.coordination = coordination;
    }

    public List<Double> getStream() {
        return stream;
    }

    public void setStream(List<Double> stream) {
        this.stream = stream;
    }

    public List<Double> getJack() {
        return jack;
    }

    public void setJack(List<Double> jack) {
        this.jack = jack;
    }

    public List<Double> getRelease() {
        return release;
    }

    public void setRelease(List<Double> release) {
        this.release = release;
    }

    public List<Double> getShield() {
        return shield;
    }

    public void setShield(List<Double> shield) {
        this.shield = shield;
    }

    public List<Double> getReverseShield() {
        return reverseShield;
    }

    public void setReverseShield(List<Double> reverseShield) {
        this.reverseShield = reverseShield;
    }

    public List<Double> getBracket() {
        return bracket;
    }

    public void setBracket(List<Double> bracket) {
        this.bracket = bracket;
    }

    public List<Double> getHandLock() {
        return handLock;
    }

    public void setHandLock(List<Double> handLock) {
        this.handLock = handLock;
    }

    public List<Double> getOverlap() {
        return overlap;
    }

    public void setOverlap(List<Double> overlap) {
        this.overlap = overlap;
    }

    public List<Double> getRiceDensity() {
        return riceDensity;
    }

    public void setRiceDensity(List<Double> riceDensity) {
        this.riceDensity = riceDensity;
    }

    public List<Double> getLnDensity() {
        return lnDensity;
    }

    public void setLnDensity(List<Double> lnDensity) {
        this.lnDensity = lnDensity;
    }

    public List<Double> getSpeedJack() {
        return speedJack;
    }

    public void setSpeedJack(List<Double> speedJack) {
        this.speedJack = speedJack;
    }

    public List<Double> getTrill() {
        return trill;
    }

    public void setTrill(List<Double> trill) {
        this.trill = trill;
    }

    public List<Double> getBurst() {
        return burst;
    }

    public void setBurst(List<Double> burst) {
        this.burst = burst;
    }

    public List<Double> getGrace() {
        return grace;
    }

    public void setGrace(List<Double> grace) {
        this.grace = grace;
    }

    public List<Double> getDelayedTail() {
        return delayedTail;
    }

    public void setDelayedTail(List<Double> delayedTail) {
        this.delayedTail = delayedTail;
    }

    public List<Double> getBump() {
        return bump;
    }

    public void setBump(List<Double> bump) {
        this.bump = bump;
    }

    public List<Double> getStop() {
        return stop;
    }

    public void setStop(List<Double> stop) {
        this.stop = stop;
    }

    public List<Double> getFastJam() {
        return fastJam;
    }

    public void setFastJam(List<Double> fastJam) {
        this.fastJam = fastJam;
    }

    public List<Double> getSlowJam() {
        return slowJam;
    }

    public void setSlowJam(List<Double> slowJam) {
        this.slowJam = slowJam;
    }

    public List<Double> getTeleport() {
        return teleport;
    }

    public void setTeleport(List<Double> teleport) {
        this.teleport = teleport;
    }

    public List<Double> getNegative() {
        return negative;
    }

    public void setNegative(List<Double> negative) {
        this.negative = negative;
    }

    @Override
    public String toString() {
        return STR."PPMinus3ManiaImpl{rice=\{rice}, ln=\{ln}, coordination=\{coordination}, stamina=\{stamina}, speed=\{speed}, precision=\{precision}, sv=\{sv}, stream=\{stream}, jack=\{jack}, release=\{release}, shield=\{shield}, reverseShield=\{reverseShield}, bracket=\{bracket}, handLock=\{handLock}, overlap=\{overlap}, riceDensity=\{riceDensity}, lnDensity=\{lnDensity}, speedJack=\{speedJack}, trill=\{trill}, burst=\{burst}, grace=\{grace}, delayedTail=\{delayedTail}, bump=\{bump}, stop=\{stop}, fastJam=\{fastJam}, slowJam=\{slowJam}, teleport=\{teleport}, negative=\{negative}, maxBurst=\{maxBurst}}";
    }
}
