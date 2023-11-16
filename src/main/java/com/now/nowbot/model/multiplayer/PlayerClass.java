package com.now.nowbot.model.multiplayer;

public class PlayerClass {
    String name;
    String nameCN;
    int color;

    public PlayerClass(double ERA_index, double DRA_index, double RWS_index) {
        var c = PlayerClassEnum.getPlayerClassEnumV2(ERA_index, DRA_index, RWS_index);

        this.name = c.name;
        this.nameCN = c.nameCN;
        this.color = c.color;
    }

    public PlayerClass(String name, String nameCN, int color) {
        this.name = name;
        this.nameCN = nameCN;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameCN() {
        return nameCN;
    }

    public void setNameCN(String nameCN) {
        this.nameCN = nameCN;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
