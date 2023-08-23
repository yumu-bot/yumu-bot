package com.now.nowbot.model.beatmapParse.stars;

import com.now.nowbot.model.beatmapParse.parse.OsuBeatmapAttributes;
import com.now.nowbot.model.enums.Mod;

import java.util.List;

public class OsuStars {


    OsuBeatmapAttributes beatmap;
    Integer mods;
    Integer passedObjectsCount;
    double clockRate = 1f;
    boolean clockRateNoChange(){
        if (Mod.hasDt(mods) || Mod.hasHt(mods)) {
            return false;
        } else {
            return Math.abs(clockRate - 1f) < 1e-6f;
        }
    }

    public List<Mod> getModsList() {
        return Mod.getModsList(mods);
    }

    public Integer getMods() {
        return mods;
    }

    public void setMods(Integer mods) {
        this.mods = mods;
    }

    public void addMod(Mod m) {
        this.mods |= m.value;
    }

    public float getModClockRate() {
        if (Mod.hasDt(mods)) {
            return 1.5f;
        } else if (Mod.hasHt(mods)){
            return 0.75f;
        } else {
            return 1f;
        }
    }

    public void setClockRate(float clockRate) {
        this.clockRate = Math.max(clockRate, 0.001f);
    }

    public void calculate() {
        int take = beatmap.getHitObjects().size();

    }


}
