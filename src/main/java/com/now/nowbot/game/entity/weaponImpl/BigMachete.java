package com.now.nowbot.game.entity.weaponImpl;

import com.now.nowbot.game.entity.Player;
import com.now.nowbot.game.entity.Weapon;
import com.now.nowbot.game.match.State;

import java.util.List;

public class BigMachete implements Weapon {
    short lv;
    public BigMachete(short level){
        super();
        lv = level;
    }

    public String getText() {
        return """
                造成伤害,并附带流血效果
                lv1 中等伤害 3回合流血效果 每回合减少自己5%的最大生命值
                lv2 大量伤害 3回合流血效果 每回合减少自己5%的最大生命值
                lv3 大量伤害 1回合流血效果 每回合减少自己5%的最大生命值""";
    }

    public short getLevel() {
        return 0;
    }

    public Type getType() {
        return Type.WEAPON;
    }

    public int getId() {
        return 1;
    }

    @Override
    public String getName() {
        return "大砍刀";
    }

    @Override
    public void use(Player p1, Player p2, List<State> states1, List<State> states2) {
        switch (lv){
            case 1 -> {
                states1.add(new State(false, 3, player -> player.setHp(0.95f * player.getHp()), player -> {}));
                p2.setHp(p2.getHp() - p1.getAct() * 1.5f);
            }
            case 2 -> {
                states1.add(new State(false, 3, player -> player.setHp(0.95f * player.getHp()), player -> {
                }));
                p2.setHp(p2.getHp() - p1.getAct() * 2f);
            }
            case 3 -> {
                states1.add(new State(false, 1, player -> player.setHp(0.95f * player.getHp()), player -> {
                }));
                p2.setHp(p2.getHp() - p1.getAct() * 2f);
            }
        }

    }


}
