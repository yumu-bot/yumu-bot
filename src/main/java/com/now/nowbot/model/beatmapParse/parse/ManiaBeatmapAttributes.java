package com.now.nowbot.model.beatmapParse.parse;

import com.now.nowbot.model.beatmapParse.HitObject;
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;

public class ManiaBeatmapAttributes extends OsuBeatmapAttributes {

    public ManiaBeatmapAttributes(BufferedReader reader, BeatmapGeneral general) throws IOException {
        super(reader, general);
        for (HitObject line : hitObjects) {
            var column = getColumn(line.getPosition().getX(), (int) Math.floor(CS));
            line.setColumn(column);
        }
    }

    boolean parseHitObject(BufferedReader reader) throws IOException {
        boolean empty = true;
        String line;
        hitObjects = new LinkedList<>();
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            var entity = line.split(",");
            if (entity.length < 5) throw new IOException("解析 [HitObjects] 错误");
            int x = Integer.parseInt(entity[0]);
            int y = Integer.parseInt(entity[1]);
            int startTime = Integer.parseInt(entity[2]);
            int type = Integer.parseInt(entity[3]);
            int endTime = (type == 128) ? Integer.parseInt(entity[5].split(":")[0]) : startTime;

            var obj = new HitObject(x, y, startTime);
            obj.setType(HitObjectType.getType(type));
            obj.setEndTime(endTime);

            hitObjects.add(obj);
        }

        if (! CollectionUtils.isEmpty(hitObjects)) {
            length = hitObjects.getLast().getEndTime() - hitObjects.getFirst().getStartTime();
        }

        return empty;
    }

    private static int getColumn(double x, int key) {
        int column = (int) Math.floor(x * key / 512f);
        column = Math.min(Math.max(0, column), key - 1);
        return column;
    }
}
