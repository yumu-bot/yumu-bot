package com.now.nowbot.model.ppminus3;

import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.model.enums.OsuMode;

import java.io.IOException;

public abstract class MapMinus {

    public static MapMinus getInstance(OsuFile file) throws IOException {
        MapMinus mm = null;
        if (file.getMode() == OsuMode.MANIA) {
            mm = new MapMinusMania(file.getMania());
        }
        return mm;
    }
}
