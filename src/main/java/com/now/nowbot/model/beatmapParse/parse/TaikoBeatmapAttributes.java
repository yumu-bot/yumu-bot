package com.now.nowbot.model.beatmapParse.parse;

import java.io.BufferedReader;
import java.io.IOException;

public class TaikoBeatmapAttributes extends OsuBeatmapAttributes {
    /**
     * 逐行读取
     *
     * @param read    osu file
     * @param general 元信息
     * @throws io exception {@link IOException}
     */
    public TaikoBeatmapAttributes(BufferedReader read, BeatmapGeneral general) throws IOException {
        super(read, general);
    }
}
