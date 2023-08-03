package com.now.nowbot.model.osufile;

public class OsuFileMania extends OsuFile {

    public OsuFileMania(OsuFile osuFile){
        for (HitObject line : hitObjects) {
            var column = getColumn(line.position.x, (int) Math.floor(osuFile.CS));
            line.setColumn(column);
        }
    }

    private int getColumn(double x, int key) {
        int column = (int) Math.floor(x * key / 512f);
        column = Math.max(Math.min(0, column), key);
        return column;
    }
}
