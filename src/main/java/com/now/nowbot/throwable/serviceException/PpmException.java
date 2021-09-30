package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

import java.util.Random;

public class PpmException extends TipsException {
    public enum Type{
        mode_mania(),
        mode_not_find(),
        time_short(),
        unknown(),
        ;

    }
    public PpmException(Type type){
        switch (type){
            case mode_mania:{
                setMessage("等哪天mania社区风气变好了，或许就有PPM-mania了吧...");
                break;
            }
            case mode_not_find:{
                setMessage("「邪恶的 osu! 玩家，我以 Bot 一族」…呃，这里不会读…「Bot 大魔王之名，否定你添加新模式的资格！」「除非你干掉 peppy，通过」…呃…「接受」…呃… 有几个词，波特不认识…");
                break;
            }
            case time_short:{
                setMessage("游戏时长太短了，快去多玩几局吧！");
                break;
            }
            case unknown: {
                int flag = new Random().nextInt(3);
                if (flag == 0) {
                    setMessage("发生了某种不好的事情...");
                }
                if (flag == 1) {
                    setMessage("发生了某种不好的事情...");
                }
                if (flag == 2) {
                    setMessage("发生了某种不好的事情...");
                }
                break;
            }
        }
    }
}
