package com.now.nowbot.model.ppminus3;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.osufile.OsuFile;

public abstract class MapMinus {

    public static MapMinus getInstance(OsuMode mode, OsuFile file){
        MapMinus mm = null;
        switch (mode) {
            case OSU : mm = new MapMinusMania(mode, file); break;
            case TAIKO : mm = new MapMinusMania(mode, file); break;
            case CATCH : mm = new MapMinusMania(mode, file); break;
            case MANIA : mm = new MapMinusMania(mode, file); break;
        };
        return mm;
    }
}
