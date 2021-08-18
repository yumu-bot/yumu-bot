package com.now.nowbot.util;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.entity.BinUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class BindingUtil {
    private static final Logger log = LoggerFactory.getLogger(BindingUtil.class);
    public static void writeUser(BinUser user) {
        Path pt = Path.of(NowbotConfig.BIN_PATH + user.getQq() + ".json");
        try {
            if (!Files.isRegularFile(pt)){
                Files.createFile(pt);
            }
            Files.writeString(pt, JSONObject.toJSONString(user));
        } catch (IOException e) {
            log.error("写入文件异常",e);
        }
        System.gc();
    }
    public static BinUser readUser(long qq){
        Path pt = Path.of(NowbotConfig.BIN_PATH + qq + ".json");
        BinUser date = null;
        if(Files.isRegularFile(pt)) {
            try {
                String s = Files.readString(pt);
                date = JSONObject.parseObject(s,BinUser.class);
            } catch (IOException e) {
                log.error("用户文件读取异常",e);
            }
        }
        System.gc();
        return date;
    }

    public static void writeOsuID(String name, int id){
        Path pt = Path.of(NowbotConfig.OSU_ID + name);
        try {
            if(!Files.isRegularFile(pt)) Files.createFile(pt);
            Files.write(pt, new byte[]{
                    (byte) ((id >> 24) & 0xFF),
                    (byte) ((id >> 16) & 0xFF),
                    (byte) ((id >> 8) & 0xFF),
                    (byte) (id & 0xFF)
            });
        } catch (IOException e) {
            log.error("osu id文件写入异常",e);
        }
        System.gc();
    }
    public static int readOsuID(String name){
        Path pt = Path.of(NowbotConfig.OSU_ID + name);
        int id = 0;
        try {
            if(Files.isRegularFile(pt)) {
                var b = Files.readAllBytes(pt);
                switch (b.length){
                    case 4:id += (b[3]&0xFF);
                    case 3:id += ((b[2]&0xFF)<<8);
                    case 2:id += ((b[1]&0xFF)<<16);
                    case 1:id += ((b[0]&0xFF)<<24);break;
                    default:return 0;
                }
            }

        } catch (IOException e) {
            log.error("osu id文件读取异常",e);
        }
        System.gc();
        return id;
    }
}
