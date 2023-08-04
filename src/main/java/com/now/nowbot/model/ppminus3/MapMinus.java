package com.now.nowbot.model.ppminus3;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.osufile.OsuFile;
import com.now.nowbot.model.osufile.OsuFileMania;

public abstract class MapMinus {

    public static MapMinus getInstance(OsuFile file){
        MapMinus mm = null;
        switch (file) {
            case OsuFileMania maniaFile: mm = new MapMinusMania(maniaFile); break;
            // ...
            default:
                throw new IllegalStateException("Unexpected value: " + file);
        };
        return mm;
    }
}
