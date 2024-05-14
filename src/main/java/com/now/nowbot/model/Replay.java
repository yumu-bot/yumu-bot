package com.now.nowbot.model;

import com.now.nowbot.util.lzma.LZMAInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Replay {
    // 0 = osu!, 1 = osu!taiko, 2 = osu!catch, 3 = osu!mania
    byte      mode;
    //创建该回放文件的游戏版本 例如：20131216
    int       version;
    String    MapHash;
    String    username;
    // 回放文件的 MD5 hash
    String    RepHash;
    short     n_300;
    short     n_100;
    short     n_50;
    short     n_geki;
    short     n_katu;
    short     n_miss;
    int       score;
    short     combo;
    // full combo（1 = 没有Miss和断滑条，并且没有提前完成的滑条）
    boolean   perfect;
    int       mods;
    Map<Integer, Float> HPList;
    // 时间戳 注意这个是以公元0年开始, 不是1970年的时间戳
    long      date;
    int       dataLength;
    List<Hit> hitList;
    // 与 url 中的末尾数字不是同一个
    long      scoreId;
    double    tp;

    private Replay(ByteBuffer bf) {
        if (bf.order() == ByteOrder.BIG_ENDIAN) {
            bf.order(ByteOrder.LITTLE_ENDIAN);
        }
        mode = bf.get();
        version = bf.getInt();
        MapHash = readString(bf);
        username = readString(bf);
        RepHash = readString(bf);
        n_300 = bf.getShort();
        n_100 = bf.getShort();
        n_50 = bf.getShort();
        n_geki = bf.getShort();
        n_katu = bf.getShort();
        n_miss = bf.getShort();
        score = bf.getInt();
        combo = bf.getShort();
        perfect = bf.get() == 1;
        mods = bf.getInt();
        var Hp = readString(bf);
        date = bf.getLong();
        dataLength = bf.getInt();
        var data = new byte[dataLength];
        bf.get(data, 0, dataLength);
        hitList = hitList(data);
        scoreId = bf.getLong();
        if (bf.limit() >= 8 + bf.position()) {
            tp = bf.getDouble();
        }
        if (! Hp.isBlank()) {
            HPList = readHp(Hp);
        }
    }

    private static String readString(ByteBuffer bf) {
//        int p = (0xFF & e[offset]) |
//                (0xFF & e[offset+1])<<8 |
//                (0xFF & e[offset+2])<<16 |
//                (0xFF & e[offset+3])<<24 ;
        if (bf.get() == 11) {
            // 读取第二位 可变长int 值string byte长度
            int strLength = readLength(bf);
            //得到长度 读取string byte
            byte[] strData = new byte[strLength];
            bf.get(strData, 0, strLength);
            //转换string
            return new String(strData);
        } else {
            return "";
        }
    }

    private static List<Hit> hitList(byte[] data) {
        List<Hit> hitList = null;
        try {
            String s = new String(new LZMAInputStream(new ByteArrayInputStream(data)).readAllBytes());
            var lines = s.split(",");
            hitList = new ArrayList<>(lines.length);
            for (var line : lines) {
                var split = line.split("\\|");
                hitList.add(new Hit(
                        Long.parseLong(split[0]),
                        Float.parseFloat(split[1]),
                        Float.parseFloat(split[2]),
                        Integer.parseInt(split[3])
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hitList;
    }

    private static Map<Integer, Float> readHp(String data) {
        Map<Integer, Float> map;
        var lines = data.split(",");
        map = new LinkedHashMap<Integer, Float>(lines.length);
        for (var line : lines) {
            var k = line.split("\\|");
            Integer time = Integer.parseInt(k[0]);
            Float hp = Float.parseFloat(k[1]);

        }
        return map;
    }

    private static int readLength(ByteBuffer bf) {
        int result = 0;
        int shift = 0;
        byte b;
        do {
            b = bf.get();
            result |= (b & 0x7F) << shift;
            shift += 7;
        } while ((0x80 & b) != 0);
        return result;
    }

    public static Replay readByteToRep(ByteBuffer buffer) {
        return new Replay(buffer);
    }

    public static Replay readByteToRep(byte[] buffer) {
        return new Replay(ByteBuffer.wrap(buffer));
    }

    public List<Hit> getHitList() {
        return hitList;
    }

    public byte getMode() {
        return mode;
    }

    public int getVersion() {
        return version;
    }

    public String getMapHash() {
        return MapHash;
    }

    public String getUsername() {
        return username;
    }

    public String getRepHash() {
        return RepHash;
    }

    public short getN_300() {
        return n_300;
    }

    public short getN_100() {
        return n_100;
    }

    public short getN_50() {
        return n_50;
    }

    public short getN_geki() {
        return n_geki;
    }

    public short getN_katu() {
        return n_katu;
    }

    public short getN_miss() {
        return n_miss;
    }

    public int getScore() {
        return score;
    }

    public short getCombo() {
        return combo;
    }

    public boolean isPerfect() {
        return perfect;
    }

    public int getMods() {
        return mods;
    }

    public Map<Integer, Float> getHPList() {
        return HPList;
    }

    public long getDate() {
        return date;
    }

    public int getDataLength() {
        return dataLength;
    }

    static class Hit {
        long  befTime;
        //        鼠标的X坐标（从0到512）
        float x;
        //        鼠标的Y坐标（从0到384）
        float y;
        //鼠标、键盘按键的组合（M1 = 1, M2 = 2, K1 = 4, K2 = 8, 烟雾 = 16）（K1 总是与 M1 一起使用，K2 总是与 M2 一起使用。所以 1+4=5 2+8=10。）
        int   ket;

        public Hit(long befTime, float x, float y, int ket) {
            this.befTime = befTime;
            this.x = x;
            this.y = y;
            this.ket = ket;
        }

        public long getBefTime() {
            return befTime;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public int getKet() {
            return ket;
        }
    }

    public long getScoreId() {
        return scoreId;
    }

    public double getTp() {
        return tp;
    }
}
