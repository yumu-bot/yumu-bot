package com.now.nowbot.model.mappool.old;

import com.now.nowbot.model.json.BeatMap;

import java.util.List;

public class ModPool {
    String mod;
    String modColor;
    List<BeatMap> beatMaps;

    public ModPool(String mod, List<BeatMap> beatMaps) {
        this.mod = mod;
        this.beatMaps = beatMaps;
    }

    public String getModStr() {
        return mod;
    }
    public void setModStr(String mod) {
        this.mod = mod;
    }

    public Mod getMod() {
        return Mod.getModFromAbbreviation(mod);
    }

    public void setMod(Mod mod) {
        this.mod = mod.getAbbreviation();
    }

    public String getModColor() {
        return Mod.getColor(mod);
    }

    public void setModColor(String modColor) {
        this.modColor = modColor;
    }

    public List<BeatMap> getBeatMaps() {
        return beatMaps;
    }

    public void setBeatMaps(List<BeatMap> beatMaps) {
        this.beatMaps = beatMaps;
    }
}
