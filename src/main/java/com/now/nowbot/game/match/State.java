package com.now.nowbot.game.match;

import com.now.nowbot.game.entity.Player;

import java.util.function.Consumer;

public class State {
    boolean buff; //true buff; false debuff
    int duration; //-1 is means around the match
    Consumer<Player> beforeAct;
    Consumer<Player> afterAct;

    public State(boolean buff, int duration, Consumer<Player> before, Consumer<Player> after){
        this.buff = buff;
        this.duration = duration;
        this.beforeAct = before;
        this.afterAct = after;
    }
}
