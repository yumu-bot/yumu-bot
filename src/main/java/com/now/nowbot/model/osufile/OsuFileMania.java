package com.now.nowbot.model.osufile;

import java.io.IOException;

public class OsuFileMania extends OsuFile {

    public OsuFileMania(String file) throws IOException {
        super(file);
        for (HitObject line : hitObjects) {
            var column = getColumn(line.position.getX(), (int) Math.floor(CS));
            line.setColumn(column);
        }
    }

    private int getColumn(double x, int key) {
        int column = (int) Math.floor(x * key / 512f);
        column = Math.max(Math.min(0, column), key);
        return column;
    }
}
