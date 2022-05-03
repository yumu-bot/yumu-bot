package com.now.nowbot.game.date;

import com.now.nowbot.game.entity.Weapon;

import java.util.List;

public class User {
    List skills; // todo: 技能咋写咋写咋写咋写
    List<Weapon> weapons;
    float hp;
    float power;
    float Agility;

    public List<Weapon> getWeapons() {
        return weapons;
    }

    public void setWeapons(List<Weapon> weapons) {
        this.weapons = weapons;
    }

    public float getHp() {
        return hp;
    }

    public void setHp(float hp) {
        this.hp = hp;
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public float getAgility() {
        return Agility;
    }

    public void setAgility(float agility) {
        Agility = agility;
    }
}
