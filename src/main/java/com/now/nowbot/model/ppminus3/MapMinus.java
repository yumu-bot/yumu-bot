package com.now.nowbot.model.ppminus3;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.osufile.OsuFile;
import com.now.nowbot.model.osufile.OsuFileMania;

public abstract class MapMinus {

    public static MapMinus getInstance(OsuFile file){
        MapMinus mm = null;
        if (file instanceof OsuFileMania) {
            mm = new MapMinusMania(file);
        }
        return mm;
    }
}
