package com.now.nowbot.model.beatmap;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.PPm.impl.PpmCatch;
import com.now.nowbot.model.PPm.impl.PpmMania;
import com.now.nowbot.model.PPm.impl.PpmOsu;
import com.now.nowbot.model.PPm.impl.PpmTaiko;
import com.now.nowbot.model.enums.OsuMode;

import java.util.List;

public abstract class Beatmap {
    protected int file_version;

    protected int circle_count;
    protected int slider_count;
    protected int spinner_count;

    protected OsuMode mode;

    protected double AR;
    protected double OD;
    protected double CS;
    protected double HP;

    protected double slider_base_velocity;
    protected double slider_tick_rate;
    protected Double stack_leniency = -1D;

    List<HitObject> hitObjects;

    public static Beatmap getInstance (OsuMode mode, List<HitObject> hitObjects){
        Beatmap beatmap = null;
        switch (mode) {
            case OSU : beatmap = new BeatmapMania(mode, hitObjects); break;
            case TAIKO : beatmap = new BeatmapMania(mode, hitObjects); break;
            case CATCH : beatmap = new BeatmapMania(mode, hitObjects); break;
            case MANIA : beatmap = new BeatmapMania(mode, hitObjects); break;
        }
        return beatmap;
    }
}
