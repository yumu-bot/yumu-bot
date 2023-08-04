package com.now.nowbot.model.ppminus3;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.osufile.HitObject;
import com.now.nowbot.model.osufile.OsuFile;
import com.now.nowbot.model.osufile.hitObject.HitObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MapMinusMania extends MapMinus{
    // 谱面六维 + 额外二维
    List<Double> stream = new ArrayList<>();
    List<Double> jack = new ArrayList<>();
    List<Double> variation = new ArrayList<>();
    List<Double> coordinate = new ArrayList<>();
    List<Double> speed = new ArrayList<>();
    List<Double> stamina = new ArrayList<>();
    List<Double> chaotic = new ArrayList<>();
    List<Double> precision = new ArrayList<>();

    //细化二十多维
    List<Double> single_stream = new ArrayList<>();
    List<Double> jump_stream = new ArrayList<>();
    List<Double> hand_stream = new ArrayList<>();
    List<Double> chord_stream = new ArrayList<>();

    List<Double> single_jack = new ArrayList<>();
    List<Double> jump_jack = new ArrayList<>();
    List<Double> hand_jack = new ArrayList<>();
    List<Double> chord_jack = new ArrayList<>();

    List<Double> bump = new ArrayList<>();
    List<Double> fast_jam = new ArrayList<>();
    List<Double> slow_jam = new ArrayList<>();
    List<Double> stop = new ArrayList<>();
    List<Double> teleport = new ArrayList<>();
    List<Double> negative = new ArrayList<>();

    List<Double> hand_lock = new ArrayList<>();
    List<Double> overlap = new ArrayList<>();
    List<Double> release = new ArrayList<>();
    List<Double> shield = new ArrayList<>();

    List<Double> jack_speed = new ArrayList<>();
    List<Double> trill = new ArrayList<>();
    List<Double> burst = new ArrayList<>();

    List<Double> grace = new ArrayList<>();
    List<Double> delay_tail = new ArrayList<>();
    List<Double> stream_speed = new ArrayList<>();

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
        var data = file;
        var hitObjects = data.getHitObjects();
        int key = (int) Math.floor(data.getCS());

        //index，指示一些东西

        //cache
        int now_time = 0; //指示目前算到了什么地方（毫秒）
        int calculate_max = calculate_unit;
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
        HashMap<Integer, Integer> prev_hit_times = new HashMap<>(key);
        HashMap<Integer, Integer> prev_release_times = new HashMap<>(key);

        //这个哈希表是用来存一行的物件信息的
        //为什么要搞两个表呢？是因为一行可能有多个物件，这样在计算行内物件时，不会互相干扰
        HashMap<Integer, Integer> now_hit_times = new HashMap<>(key);
        HashMap<Integer, Integer> now_release_times = new HashMap<>(key);

        //初始化哈希表
        for (int i = 0; i < key; i++) {
            prev_hit_times.put(i, 0);
            prev_release_times.put(i, 0);
            now_hit_times.put(i, 0);
            now_release_times.put(i, 0);
        }

        //hit是长条和单点头，release是滑条尾，flow是上一个最近的物件的方向，0是中间，1是左，-1是左，

        for (HitObject line : hitObjects) {
            var column = line.getColumn();
            var type = line.getType();
            var hit_time = line.getStartTime();
            var release_time = (type == HitObjectType.LONGNOTE) ? line.getEndTime() : 0;

            //导入缓存
            int prev_hit_time = prev_hit_times.get(column);
            int prev_release_time = prev_release_times.get(column);

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
                    right_hit_time = prev_hit_times.get(column + 1);
                    right_release_time = prev_release_times.get(column + 1);
                } else if (column == key - 1) { //最右边
                    left_hit_time = prev_hit_times.get(column - 1);
                    left_release_time = prev_release_times.get(column - 1);
                    right_hit_time = 0;
                    right_release_time = 0;
                } else {
                    left_hit_time = prev_hit_times.get(column - 1);
                    left_release_time = prev_release_times.get(column - 1);
                    right_hit_time = prev_hit_times.get(column + 1);
                    right_release_time = prev_release_times.get(column + 1);
                }
            }


            //如果是新行，
            if (Math.abs(hit_time - now_time) > frac_16 || now_time == 0) {
                //普通结算
                var stream = calcStream(hit_time, left_hit_time, right_hit_time);
                var jack = calcJack(hit_time, prev_hit_time);

                switch (prev_chord) {
                    case 1 -> {
                        ss += stream;
                        sj += jack;
                    }
                    case 2 -> {
                        js += stream;
                        jj += jack;
                    }
                    case 3 -> {
                        hs += stream;
                        hj += jack;
                    }
                    case 4 -> {
                        cs += stream;
                        cj += jack;
                    }
                }


                //重置多押之前，计算交互
                int left_hand = 0;
                int left_chord = 0;
                int right_hand = 0;
                int right_chord = 0;
                for (int i = 0; i < side_key; i++) {
                    left_hand += (i + 1);
                    var v = prev_hit_times.get(i);
                    if (v <= frac_3) left_chord += (i + 1);

                    right_hand += (i + 1);
                    var w = prev_hit_times.get(i + key - side_key);
                    if (w <= frac_3) right_chord += (i + 1);
                }

                if (left_chord == 0 && right_chord == right_hand) tr += side_key;
                else if (left_chord == left_hand && right_chord == 0) tr += side_key;
                else tr += 0;


                //重置多押
                prev_chord = 1;

                //哈希表内容更新
                for (int i = 0; i < key; i++) {
                    var hit = now_hit_times.get(i);
                    var release = now_release_times.get(i);

                    if (hit != 0) {
                        prev_hit_times.replace(i, hit); //置换
                        now_hit_times.replace(i, 0); //清零
                    }

                    if (release != 0) {
                        prev_release_times.replace(i, release); //置换
                        now_release_times.replace(i, 0); //清零
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
                }
            }



            //刷新现在的缓存
            now_time = hit_time;
            now_hit_times.replace(column, hit_time);
            now_release_times.replace(column, release_time);
            
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
}
