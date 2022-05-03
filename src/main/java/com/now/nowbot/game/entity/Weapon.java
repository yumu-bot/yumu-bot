package com.now.nowbot.game.entity;

import com.now.nowbot.game.match.State;

import java.util.List;

public interface Weapon {
    enum Type{
        WEAPON,ARMOR,
    }
    int getId();
    default String getName(){
        return "";
    }
    String getText();
    short getLevel();
    Type getType();

    void use(Player p1, Player p2, List<State> states1, List<State> states2);
}
