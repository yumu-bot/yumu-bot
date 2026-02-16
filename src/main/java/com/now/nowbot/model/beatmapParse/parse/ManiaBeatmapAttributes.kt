package com.now.nowbot.model.beatmapParse.parse;

import com.now.nowbot.model.beatmapParse.HitObject;

import java.io.BufferedReader;
import java.io.IOException;

public class ManiaBeatmapAttributes extends OsuBeatmapAttributes {

    public ManiaBeatmapAttributes(BufferedReader reader, BeatmapGeneral general) throws IOException {
        super(reader, general);
        for (HitObject line : hitObjects) {
            var column = getColumn(line.getPosition().getX(), (int) Math.floor(CS));
            line.setColumn(column);
        }
    }

    private static int getColumn(double x, int key) {
        int column = (int) Math.floor(x * key / 512f);
        column = Math.min(Math.max(0, column), key - 1);
        return column;
    }
}
