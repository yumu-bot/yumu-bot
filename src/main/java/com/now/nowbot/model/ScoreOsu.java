package com.now.nowbot.model;

import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.entity.MapSetLite;

public class ScoreOsu {
    public enum Mods{
        EZ,NF,HT,HR,SD,PF,DT,HD,FL,RL,AP,SO,AT;
    }

    class PP{
        Double total;
        Double aim;
        Double speed;
        Double accuracy;
    }
    //todo 没写完整,尽量拿到ScoreLite的全部信息

    //todo 另外可以吧beatmap跟mapset封装成完整的一个数据类,包含所有难度,这个你看看能不能写
    // 获得该难度的信息,封装成方法,因为不确定要不要用,所以可以写成取得时候再去数据库查,rank\loved图直接去库里取,不会变,没有的话再api获取,unrank图需要判断一下
    //todo 转谱还是个问题,这个之后再解决
    public BeatmapLite getBeatmap(){
        return null;
    }
    //todo 获得铺面的信息
    public MapSetLite getMapset(){
        return null;
    }
}

