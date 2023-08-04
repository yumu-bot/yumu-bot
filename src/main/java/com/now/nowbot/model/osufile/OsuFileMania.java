package com.now.nowbot.model.osufile;

import com.now.nowbot.model.osufile.hitObject.HitObjectType;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;

public class OsuFileMania extends OsuFile {

    public OsuFileMania(String file) throws IOException {
        super(file);
        for (HitObject line : hitObjects) {
            var column = getColumn(line.position.getX(), (int) Math.floor(CS));
            line.setColumn(column);
        }
    }

    public OsuFileMania(BufferedReader reader) throws IOException {
        super(reader);
    }

    private boolean parseHitObject(BufferedReader reader) throws IOException {
        boolean empty = true;
        String line;
        hitObjects = new LinkedList<>();
        while ((line = reader.readLine()) != null && !line.equals("")) {
            var entity = line.split(",");
            if (entity.length < 5) throw new IOException("解析 [HitObjects] 错误");
            int x = Integer.parseInt(entity[0]);
            int y = Integer.parseInt(entity[1]);
            int startTime = Integer.parseInt(entity[2]);
            int type = Integer.parseInt(entity[3]);
            int endTime = Integer.parseInt(entity[5]);

            var obj = new HitObject(x, y, startTime);
            obj.setEndTime(endTime);

            obj.setType(HitObjectType.getType(type));
            hitObjects.add(obj);
        }
        return empty;
    }

    private int getColumn(double x, int key) {
        int column = (int) Math.floor(x * key / 512f);
        column = Math.max(Math.min(0, column), key);
        return column;
    }
}
