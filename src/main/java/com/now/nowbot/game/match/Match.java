package com.now.nowbot.game.match;

import com.now.nowbot.game.date.User;
import com.now.nowbot.game.entity.Player;

import java.util.List;
import java.util.Random;

public class Match {
    public static Random random = new Random(System.currentTimeMillis()%163);
    Float act = null;
    Player player1;
    Player player2;
    List<State> player1State;
    List<State> player2State;

    public Match(User u1, User u2){
        player1 = new Player();
        player1.setHp(u1.getHp());
        player1.setAct(u1.getPower()*0.8f);
        player1.setSpeed(u1.getAgility());
    }

    boolean nextIsP1(float player1, float player2){
        if (act != null){
            if (act > 0){
                act -= player2;
            } else {
                act += player1;
            }
        } else {
            act = player1 - player2;
        }
        return act >= 0;
    }
}
