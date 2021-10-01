package com.now.nowbot.model.bitmap;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Replay {
    // 0 = osu!, 1 = osu!taiko, 2 = osu!catch, 3 = osu!mania
    byte mode;
    //创建该回放文件的游戏版本 例如：20131216
    int version;
    String MapHash;
    String username;
    // 回放文件的 MD5 hash
    String RepHash;
    short n_300;
    short n_100;
    short n_50;
    short n_geki;
    short n_katu;
    short n_miss;
    int score;
    short combo;
    // full combo（1 = 没有Miss和断滑条，并且没有提前完成的滑条）
    boolean perfect;
    int mods;
    Map<Integer, Float> HPList;
    //时间戳
    long date;
    int dataLength;
    byte[] data;
    long scoreId;
    double tp;

    private Replay(ByteBuffer bf){
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
        date = readLong(bf);
        System.out.println(date);
        dataLength = bf.getInt();
        data = new byte[dataLength];
        bf.get(data, 0, dataLength);
        scoreId = readLong(bf);
        if (bf.limit() >= 8 + bf.position()){
            tp = bf.getDouble();
        }

        HPList = readHp(Hp);
    }
    private static String readString(ByteBuffer bf){
//        int p = (0xFF & e[offset]) |
//                (0xFF & e[offset+1])<<8 |
//                (0xFF & e[offset+2])<<16 |
//                (0xFF & e[offset+3])<<24 ;
        if (bf.get() == 11){
            // 读取第二位 可变长int 值string byte长度
            int strLength = 0;
            int b = 0;
            byte temp;
            do {
                temp = bf.get();
                strLength += (0x7F & temp) << b;
                b += 7;
            }while ((temp & 0x80) != 0);
            //得到长度 读取string byte
            byte[] strData = new byte[strLength];
            bf.get(strData, 0, strLength);
            //转换string
            return new String(strData);
        }else {
            return "";
        }
    }
    private static long readLong(ByteBuffer bf){
        long value = 0;
        for (int i = 0; i < 8; i++) {
            int shift = (7-i) << 3;
            value |= ((long)0xff << i) & ((long)bf.get() << i);
        }
        return value;
    }
    private static Map<Integer, Float> readHp(String data){
        var p = Pattern.compile("(?<time>\\d+)\\|(?<hp>(\\d+)(\\.\\d+)?),");
        var m = p.matcher(data);
        var map = new LinkedHashMap<Integer, Float>();
        while (m.find()){
            int time = Integer.parseInt(m.group("time"));
            float hp = Float.parseFloat(m.group("hp"));
            map.put(time, hp);
        }
        return map;
    }
    public static Replay readByteToRep(ByteBuffer buffer){
        return new Replay(buffer);
    }
}
