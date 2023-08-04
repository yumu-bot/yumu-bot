package com.now.nowbot.model.ppminus3;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.osufile.HitObject;
import com.now.nowbot.model.osufile.OsuFile;

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

    public MapMinusMania(OsuFile file){
        var data = file;
        var hitObjects = data.getHitObjects();

        int key = (int) Math.floor(data.getCS());

        //cache
        int now_time = 0; //指示目前算到了什么地方（毫秒）
        int calculate_unit = 5000; //一个计算元的区域（毫秒）。这其中的数据会统计到一个计算元中。
        int calculate_max = calculate_unit;
        int mid_column = -1; //指示中键的位置。4K就没中键，7K有
        int chord_count = 1;//指示此时多押的数量，1是没多押
        double str = 0; double jac = 0; double svs = 0; double coo = 0; double spd = 0; double sta = 0; double pre;

        double ss = 0; double js = 0; double hs = 0; double cs = 0;
        double sj = 0; double jj = 0; double hj = 0; double cj = 0;
        double bm = 0; double fa = 0; double sa = 0; double st = 0; double tp = 0; double ne = 0;
        double hl = 0; double tr = 0; double br = 0;
        double gr = 0; double dr = 0; double sp = 0;

        if (key % 2 == 1) mid_column = (key - 1) / 2; //假设是7K，那么中键是3号位，左右是012和456

        //我不会用哈希表
        HashMap<Integer, Integer> prev_hit_times = new HashMap<>(key);
        HashMap<Integer, Integer> prev_release_times = new HashMap<>(key);
        HashMap<Integer, Integer> prev_flow_directions = new HashMap<>(key);

        //初始化哈希表
        for (int i = 0; i < key; i++) {
            prev_hit_times.put(i, 0);
            prev_release_times.put(i, 0);
            prev_flow_directions.put(i, 0);
        }

        //hit是长条和单点头，release是滑条尾，flow是上一个最近的物件的方向，0是中间，1是右，-1是左

        for (HitObject line : hitObjects) {
            var column = line.getColumn();
            var type = line.getType();
            var hit_time = line.getStartTime();
            var release_time = line.getEndTime();
            int flow_direction;

            //导入缓存
            int prev_hit_time = prev_hit_times.get(column);
            int prev_release_time = prev_release_times.get(column);
            int prev_flow_direction = prev_flow_directions.get(column);

            int left_hit_time, left_release_time, left_flow_direction, right_hit_time, right_release_time, right_flow_direction;

            //计算元初始化
            if (now_time == 0) calculate_max += hit_time;

            //衩机制
            {
                if (column == 0) {
                    left_hit_time = 0; left_release_time = 0; left_flow_direction = 0;
                    right_hit_time = prev_hit_times.get(column + 1);
                    right_release_time = prev_release_times.get(column + 1);
                    right_flow_direction = prev_flow_directions.get(column + 1);
                } else if (column == key - 1) {
                    right_hit_time = 0; right_release_time = 0; right_flow_direction = 0;
                    left_hit_time = prev_hit_times.get(column - 1);
                    left_release_time = prev_release_times.get(column - 1);
                    left_flow_direction = prev_flow_directions.get(column - 1);
                } else {
                    left_hit_time = prev_hit_times.get(column - 1);
                    left_release_time = prev_release_times.get(column - 1);
                    left_flow_direction = prev_flow_directions.get(column - 1);
                    right_hit_time = prev_hit_times.get(column + 1);
                    right_release_time = prev_release_times.get(column + 1);
                    right_flow_direction = prev_flow_directions.get(column + 1);
                }

                flow_direction = getFlowDirection(left_flow_direction, right_flow_direction);
            }


            //多押机制，
            {
                if (Math.abs(hit_time - now_time) > 20 || now_time == 0) { //180bpm 1/16
                    var stream = calcStream(hit_time, left_hit_time, right_hit_time, left_flow_direction, right_flow_direction);
                    var jack = calcJack(hit_time, prev_hit_time);

                    switch (chord_count) {
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

                    //重置多押
                    chord_count = 1;
                } else {
                    //正常添加多押
                    chord_count = Math.min(key, chord_count + 1);
                }
            }


            switch (type) {
                case CIRCLE -> {

                }
                case LONGNOTE -> {

                }
            }


            //刷新缓存
            now_time = hit_time;
            prev_hit_times.replace(column, hit_time);
            prev_release_times.replace(column, release_time);
            prev_flow_directions.replace(column, flow_direction);
        }
    }

    private double calcStream(int hit, int left_hit, int right_hit, int left_to, int right_to){
        double p = 0f;
        if (left_hit != 0 && left_to >= 0) {
            p += calcFunctionNormal(hit - left_hit, 20, 1111); // 180bpm 1/3
        }
        if (right_hit != 0 && right_to <= 0) {
            p += calcFunctionNormal(hit - right_hit, 20, 1111); // 180bpm 1/3
        }

        return p;
    }
    private double calcJack(int hit, int prev_hit){
        double p = 0f;
        p += calcFunction1_X(hit - prev_hit, 20, 5000, 833); // 180bpm 1/4
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

    /*
    真值表
    L\R -1  0   1
    -1  -1  -1  0
    0   -1  0   1
    1   0   1   1

     */
    private int getFlowDirection(int left, int right) {
        if (left == right) return left;
        else if (left == 0) return right;
        else if (right == 0) return left;
        else return 0;
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
