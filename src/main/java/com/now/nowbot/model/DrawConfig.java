package com.now.nowbot.model;

import com.now.nowbot.mapper.DrawLogLiteRepository;
import com.now.nowbot.model.enums.DrawKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DrawConfig {
    public record Config(String name, int width){}
    public record Card(String name, int width, String info){}
    public Map<DrawKind, Config> kindConfig;
    public Map<DrawKind, List<Card>> cardList;

    private Map<String, DrawKind> kindMap;

    public DrawConfig(){
        kindConfig = new HashMap<>();
        cardList = new HashMap<>();
    }

    public void initCache(){
        kindMap = new TreeMap<>();
        for(var kind : cardList.keySet()){
            for (var card : cardList.get(kind)){
                kindMap.put(card.info(), kind);
            }
        }

    }

    public DrawKind getKind(String cardInfo){
        return kindMap.get(cardInfo);
    }

    /***
     * 随机品级
     * @param uid
     * @param repository
     * @return
     */
    public DrawKind getRandomKind(long uid, DrawLogLiteRepository repository){
        // 可能需要的数据: 具体是什么看注释
        int countSR$SSR = repository.getKindCount(uid, 50, DrawKind.SR, DrawKind.SSR);
        int countBefSR = repository.getBeforCount(uid, DrawKind.SR);
        // todo 待实现
        return null;
    }

    /***
     * 随机卡片
     * @return
     */
    public Card getRandomCard(DrawKind kind){
        // todo 待实现
        return null;
    }
}
