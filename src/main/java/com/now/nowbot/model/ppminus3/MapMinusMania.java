package com.now.nowbot.model.ppminus3;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ArrayListMultimap;
import com.now.nowbot.model.osufile.HitObject;
import com.now.nowbot.model.osufile.OsuFile;
import com.now.nowbot.model.osufile.hitObject.HitObjectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true, allowGetters = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class MapMinusMania extends MapMinus{
    // 谱面六维 + 额外二维
    private List<Double> stream = new ArrayList<>();
    private List<Double> jack = new ArrayList<>();
    private List<Double> variation = new ArrayList<>();
    private List<Double> coordinate = new ArrayList<>();
    private List<Double> speed = new ArrayList<>();
    private List<Double> stamina = new ArrayList<>();
    private List<Double> chaotic = new ArrayList<>();
    private List<Double> precision = new ArrayList<>();

    //细化二十多维
    private List<Double> single_stream = new ArrayList<>();
    private List<Double> jump_stream = new ArrayList<>();
    private List<Double> hand_stream = new ArrayList<>();
    private List<Double> chord_stream = new ArrayList<>();

    private List<Double> single_jack = new ArrayList<>();
    private List<Double> jump_jack = new ArrayList<>();
    private List<Double> hand_jack = new ArrayList<>();
    private List<Double> chord_jack = new ArrayList<>();

    private List<Double> bump = new ArrayList<>();
    private List<Double> fast_jam = new ArrayList<>();
    private List<Double> slow_jam = new ArrayList<>();
    private List<Double> stop = new ArrayList<>();
    private List<Double> teleport = new ArrayList<>();
    private List<Double> negative = new ArrayList<>();

    private List<Double> hand_lock = new ArrayList<>();
    private List<Double> overlap = new ArrayList<>();
    private List<Double> release = new ArrayList<>();
    private List<Double> shield = new ArrayList<>();

    private List<Double> jack_speed = new ArrayList<>();
    private List<Double> trill = new ArrayList<>();
    private List<Double> burst = new ArrayList<>();

    private List<Double> grace = new ArrayList<>();
    private List<Double> delay_tail = new ArrayList<>();
    private List<Double> stream_speed = new ArrayList<>();

    int map_start_time;
    int map_end_time;

    int frac_16 = 21;
    int frac_8 = 42;
    int frac_6 = 55;
    int frac_4 = 83;
    int frac_3 = 111;
    int frac_2 = 166;
    int frac_1 = 333;
    int calculate_unit = 2500; //一个计算元的区域（毫秒）。这其中的数据会统计到一个计算元中。

    public MapMinusMania(OsuFile file){
        var hitObjects = file.getHitObjects();
        var timings = file.getTimings();
        int key = (int) Math.floor(file.getCS());

        //index，指示一些东西

        //cache
        int now_time = 0; //指示目前算到了什么地方（毫秒）
        int calculate_max = calculate_unit;
        boolean isLeft = false; //指示左手是否有物件
        boolean isRight = false; //指示右手是否有物件
        double str; double jak; double var; double coo; double spd; double sta; double cao; double pre = 0;

        double ss = 0; double js = 0; double hs = 0; double cs = 0;
        double sj = 0; double jj = 0; double hj = 0; double cj = 0;
        double bm = 0; double fa = 0; double sa = 0; double st = 0; double tp = 0; double ne = 0;
        double hl = 0; double ol = 0; double rl = 0; double sh = 0;
        double jp = 0; double tr = 0; double br = 0;

        double gr = 0; double dt = 0; double sp = 0;

        int mid_column = -1; //指示中键的位置。4K就没中键，7K有
        int side_key = 0; //指示边键的数量
        if (key % 2 == 1) {
            mid_column = (key - 1) / 2; //假设是7K，那么中键是3号位，左右是012和456
            side_key = mid_column;
        } else {
            side_key = key / 2;
        }

        int prev_chord = 1;//指示之前多押的数量，1是没多押

        int prev_density = 0;//指示之前的密度（其实是累加过的

        //我不会用哈希表
        int[] prev_hit_times = new int[key];
        int[] prev_release_times = new int[key];

        //这个哈希表是用来存一行的物件信息的
        //为什么要搞两个表呢？是因为一行可能有多个物件，这样在计算行内物件时，不会互相干扰
        int[] now_hit_times = new int[key];
        int[] now_release_times = new int[key];

        //初始化哈希表
        for (int i = 0; i < key; i++) {
            prev_hit_times[i] = 0;
            prev_release_times[i] = 0;
            now_hit_times[i] = 0;
            now_release_times[i] = 0;
        }

        //hit是长条和单点头，release是滑条尾，flow是上一个最近的物件的方向，0是中间，1是左，-1是左，

        for (HitObject line : hitObjects) {
            var column = line.getColumn();
            var type = line.getType();
            var hit_time = line.getStartTime();
            var release_time = (type == HitObjectType.LONGNOTE) ? line.getEndTime() : 0;

            //导入缓存
            int prev_hit_time = prev_hit_times[column];
            int prev_release_time = prev_release_times[column];

            //计算元初始化
            if (now_time == 0) calculate_max += hit_time;

            //衩机制
            int left_hit_time;
            int left_release_time;
            int right_hit_time;
            int right_release_time;
            {
                if (column == 0) { //最左边
                    left_hit_time = 0;
                    left_release_time = 0;
                    right_hit_time = prev_hit_times[column + 1];
                    right_release_time = prev_release_times[column + 1];
                } else if (column >= key - 1) { //最右边
                    left_hit_time = prev_hit_times[column];
                    left_release_time = prev_release_times[column];
                    right_hit_time = 0;
                    right_release_time = 0;
                } else {
                    left_hit_time = prev_hit_times[column + 1];
                    left_release_time = prev_release_times[column + 1];
                    right_hit_time = prev_hit_times[column + 1];
                    right_release_time = prev_release_times[column + 1];
                }
            }

            //判断当前物件在哪只手
            if (column <= side_key - 1) isLeft = true;
            else if (column >= key - side_key) isRight = true;

            //如果是新行，
            if (Math.abs(hit_time - now_time) > frac_16 || now_time == 0) {
                //普通结算
                var stream = calcStream(hit_time, left_hit_time, right_hit_time);
                var jack = calcJack(hit_time, prev_hit_time);
                boolean isTwoHand = isLeft && isRight;

                switch (prev_chord) {
                    case 1 -> {
                        ss += isTwoHand ? stream : 0.5f * stream;
                        sj += isTwoHand ? jack : 0.5f * jack;
                    }
                    case 2 -> {
                        js += isTwoHand ? stream : 0.5f * stream;
                        jj += isTwoHand ? jack : 0.5f * jack;
                    }
                    case 3 -> {
                        hs += isTwoHand ? stream : 0.5f * stream;
                        hj += isTwoHand ? jack : 0.5f * jack;
                    }
                    case 4 -> {
                        cs += isTwoHand ? stream : 0.5f * stream;
                        cj += isTwoHand ? jack : 0.5f * jack;
                    }
                }


                //重置多押之前，计算交互
                int left_hand = 0;
                int left_chord = 0;
                int right_hand = 0;
                int right_chord = 0;
                for (int i = 0; i < side_key; i++) {
                    left_hand += (i + 1);
                    var v = prev_hit_times[i];
                    if (v <= frac_3) left_chord += (i + 1);

                    right_hand += (i + 1);
                    var w = prev_hit_times[i + key - side_key];
                    if (w <= frac_3) right_chord += (i + 1);
                }

                if ((left_chord == 0 && right_chord == right_hand)
                        || (left_chord == left_hand && right_chord == 0)) tr += side_key;

                //重置多押
                prev_chord = 1;

                //重置左右手
                isLeft = false;
                isRight = false;

                //哈希表内容更新
                for (int i = 0; i < key; i++) {
                    var hit = now_hit_times[i];
                    var release = now_release_times[i];

                    if (hit != 0) {
                        prev_hit_times[i] = hit; //置换
                        now_hit_times[i] = 0; //清零
                    }

                    if (release != 0) {
                        prev_release_times[i] = release; //置换
                        now_release_times[i] = 0; //清零
                    }
                }

                //如果不是新行，
            } else {
                //正常添加多押
                prev_chord = Math.min(key, prev_chord + 1);
            }

            //与以上无关的正常计算
            jp += calcSpeedJack(hit_time, prev_hit_time);
            sp += calcSpeedStream(hit_time, left_hit_time, right_hit_time);


            switch (type) {
                case CIRCLE -> {
                    gr += calcGrace(hit_time, left_hit_time, right_hit_time);
                    hl += calcHandLock(hit_time, left_hit_time, left_release_time, right_hit_time, right_release_time);
                    if (prev_release_time != 0) sh += calcShield(hit_time, prev_release_time);
                }
                case LONGNOTE -> {
                    ol += calcOverlap(hit_time, release_time, left_hit_time, left_release_time, right_hit_time, right_release_time);
                    rl += calcStream(release_time, left_release_time, right_release_time); //直接使用stream算法
                    dt += calcSpeedStream(release_time, left_release_time, right_release_time);//直接使用speed stream算法
                }
            }


            //刷新现在的缓存
            now_time = hit_time;
            now_hit_times[column] = hit_time;
            now_release_times[column] =release_time;
            
            // 如果超过了计算元，那么计算元的值清零
            if (now_time > calculate_max) {
                calculate_max = now_time + calculate_unit;

                //计算最大值，结算
                sta = Math.floor(prev_density * 0.2 + br);

                prev_density = (int) sta;

                str = ss + 2 * js + 3 * hs + 4 * cs;
                jak = sj + 2 * jj + 3 * hj + 4 * cj;
                var = bm + fa + sa + st + tp + ne;
                coo = hl + ol + rl + sh;
                spd = jp + tr + br;
                cao = gr + dt + sp;

                stream.add(str); single_stream.add(ss); jump_stream.add(js); hand_stream.add(hs); chord_stream.add(cs);
                jack.add(jak); single_jack.add(sj); jump_jack.add(jj); hand_jack.add(hj); chord_jack.add(cj);
                variation.add(var); bump.add(bm); fast_jam.add(fa); slow_jam.add(sa); stop.add(st); teleport.add(tp); negative.add(ne);
                coordinate.add(coo); hand_lock.add(hl); overlap.add(ol); release.add(rl); shield.add(sh);
                speed.add(spd); jack_speed.add(jp); trill.add(tr); burst.add(br);
                stamina.add(sta);
                chaotic.add(cao); grace.add(gr); delay_tail.add(dt); stream_speed.add(sp);
                precision.add(pre);

                //初始化

                ss = 0; js = 0; hs = 0; cs = 0;
                sj = 0; jj = 0; hj = 0; cj = 0;
                bm = 0; fa = 0; sa = 0; st = 0; tp = 0; ne = 0;
                hl = 0; ol = 0; rl = 0; sh = 0;
                jp = 0; tr = 0; br = 0;

                gr = 0; dt = 0; sp = 0;
            
            }
        }
    }

    private double calcStream(int hit, int left_hit, int right_hit) {
        double p = 0f;
        if (left_hit != 0) {
            p += calcFunctionNormal(hit - left_hit, frac_16, frac_1);
        }
        if (right_hit != 0) {
            p += calcFunctionNormal(hit - right_hit, frac_16, frac_1);
        }

        return p;
    }

    private double calcSpeedStream(int hit, int left_hit, int right_hit) {
        double p = 0f;
        if (left_hit != 0) {
            p += calcFunctionNormal(hit - left_hit, frac_16, frac_3); // 180bpm 1/4
        }
        if (right_hit != 0) {
            p += calcFunctionNormal(hit - right_hit, frac_16, frac_3); // 180bpm 1/4
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


    public List<double[]> getData() {
        List<double[]> data = new ArrayList<>();

        data.add(getStream().stream().mapToDouble(v -> v).toArray());
        data.add(getJack().stream().mapToDouble(v -> v).toArray());
        data.add(getVariation().stream().mapToDouble(v -> v).toArray());
        data.add(getCoordinate().stream().mapToDouble(v -> v).toArray());
        data.add(getSpeed().stream().mapToDouble(v -> v).toArray());
        data.add(getStamina().stream().mapToDouble(v -> v).toArray());
        data.add(getChaotic().stream().mapToDouble(v -> v).toArray());
        data.add(getPrecision().stream().mapToDouble(v -> v).toArray());

        return data;
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

    public List<Double> getVariation() {
        return variation;
    }

    public void setVariation(List<Double> variation) {
        this.variation = variation;
    }

    public List<Double> getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(List<Double> coordinate) {
        this.coordinate = coordinate;
    }

    public List<Double> getSpeed() {
        return speed;
    }

    public void setSpeed(List<Double> speed) {
        this.speed = speed;
    }

    public List<Double> getStamina() {
        return stamina;
    }

    public void setStamina(List<Double> stamina) {
        this.stamina = stamina;
    }

    public List<Double> getChaotic() {
        return chaotic;
    }

    public void setChaotic(List<Double> chaotic) {
        this.chaotic = chaotic;
    }

    public List<Double> getPrecision() {
        return precision;
    }

    public void setPrecision(List<Double> precision) {
        this.precision = precision;
    }

    public List<Double> getSingleStream() {
        return single_stream;
    }

    public void setSingleStream(List<Double> single_stream) {
        this.single_stream = single_stream;
    }

    public List<Double> getJumpStream() {
        return jump_stream;
    }

    public void setJumpStream(List<Double> jump_stream) {
        this.jump_stream = jump_stream;
    }

    public List<Double> getHandStream() {
        return hand_stream;
    }

    public void setHandStream(List<Double> hand_stream) {
        this.hand_stream = hand_stream;
    }

    public List<Double> getChordStream() {
        return chord_stream;
    }

    public void setChordStream(List<Double> chord_stream) {
        this.chord_stream = chord_stream;
    }

    public List<Double> getSingleJack() {
        return single_jack;
    }

    public void setSingleJack(List<Double> single_jack) {
        this.single_jack = single_jack;
    }

    public List<Double> getJumpJack() {
        return jump_jack;
    }

    public void setHandJack(List<Double> hand_jack) {
        this.hand_jack = hand_jack;
    }

    public List<Double> getChordJack() {
        return chord_jack;
    }

    public void setChordJack(List<Double> chord_jack) {
        this.chord_jack = chord_jack;
    }
}
