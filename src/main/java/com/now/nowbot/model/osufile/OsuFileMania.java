package com.now.nowbot.model.osufile;

import com.now.nowbot.model.osufile.hitObject.HitObjectType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class OsuFileMania extends OsuFile {

    public OsuFileMania(String file) throws IOException {
        this(new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(
                                file.getBytes(StandardCharsets.UTF_8)
                        )
                )
        ));
    }

    public OsuFileMania(BufferedReader reader) throws IOException {
        super(reader);
        for (HitObject line : hitObjects) {
            var column = getColumn(line.position.getX(), (int) Math.floor(CS));
            line.setColumn(column);
        }
    }

    boolean parseHitObject(BufferedReader reader) throws IOException {
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
            int endTime = (type == 128) ? Integer.parseInt(entity[5]) : startTime;

            var obj = new HitObject(x, y, startTime);
            obj.setType(HitObjectType.getType(type));
            obj.setEndTime(endTime);

            hitObjects.add(obj);
        }
        return empty;
    }

    private int getColumn(double x, int key) {
        int column = (int) Math.floor(x * key / 512f);
        column = Math.min(Math.max(0, column), key - 1);
        return column;
    }
}
