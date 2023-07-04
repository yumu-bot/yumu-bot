package com.now.nowbot.model.imag;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.now.nowbot.model.enums.OsuMode;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class MapAttrGetItem implements Serializable {
    long bid;
    int mods;

    public MapAttrGetItem(long b, int m) {
        bid = b;
        mods = m;
    }

    public long getBid() {
        return bid;
    }

    public void setBid(long bid) {
        this.bid = bid;
    }

    public int getMods() {
        return mods;
    }

    public void setMods(int mods) {
        this.mods = mods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapAttrGetItem that)) return false;

        if (bid != that.bid) return false;
        return mods == that.mods;
    }

    @Override
    public int hashCode() {
        int result = (int) (bid ^ (bid >>> 32));
        result = 31 * result + mods;
        return result;
    }
}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MapAttrGet {
    int modeInt = 0;

    Set<MapAttrGetItem> maps = new HashSet<>();

    public MapAttrGet(OsuMode m) {
        if (m == OsuMode.DEFAULT) return;
        modeInt = m.getModeValue();
    }

    public MapAttrGet() {
    }

    public boolean addMap(long bid, int mods) {
        var c = new MapAttrGetItem(bid, mods);
        return maps.add(c);
    }

    public int getModeInt() {
        return modeInt;
    }

    public void setModeInt(int modeInt) {
        this.modeInt = modeInt;
    }

    public Set<MapAttrGetItem> getMaps() {
        return maps;
    }

    public void setMaps(Set<MapAttrGetItem> maps) {
        this.maps = maps;
    }
}
