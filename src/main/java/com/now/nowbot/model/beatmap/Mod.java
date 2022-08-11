package com.now.nowbot.model.beatmap;

public enum Mod {
    None(0,""),
    NoFail(1, "NF"),
    Easy(1<<1, "EZ"),
    //替换未使用的 No Video
    TouchDevice(1<<2, "TD"),
    Hidden(1<<3, "HD"),
    HardRock(1<<4, "HR"),
    SuddenDeath(1<<5, "SD"),
    DoubleTime(1<<6, "DT"),
    Relax(1<<7, "RL"),
    HalfTime(1<<8, "HT"),
    //总是和 DT 一起使用 : 512 + 64 = 576
    Nightcore(1<<9, "NC"),
    Flashlight(1<<10, "FL"),
    Autoplay(1<<11, "AT"),
    SpunOut(1<<12, "SO"),
    //Autopilot
    Autopilot(1<<13, "AP"),
    Perfect(1<<14, "PF"),
    Key4(1<<15, "4K"),
    Key5(1<<16, "5K"),
    Key6(1<<17, "6K"),
    Key7(1<<18, "7K"),
    Key8(1<<19, "8K"),
    FadeIn(1<<20, "FI"),
    // mania rd
    Random(1<<21, "RD"),
    //Cinema
    Cinema(1<<22, "CM"),
    //仅 osu!cuttingedge
    TargetPractice(1<<23, "TP"),
    Key9(1<<24, "9K"),
    KeyCoop(1<<25, "CP"),
    Key1(1<<26, "1K"),
    Key3(1<<27, "3K"),
    Key2(1<<28, "2K"),
    ScoreV2(1<<29, "V2"),
    Mirror(1<<30, "MR"),
    //    keyMod(Key1.value | Key2.value | Key3.value | Key4.value | Key5.value | Key6.value | Key7.value | Key8.value | Key9.value | KeyCoop.value),
    keyMod(521109504, "?"),
    //    FreeModAllowed(NoFail.value | Easy.value | Hidden.value | HardRock.value | SuddenDeath.value | Flashlight.value | FadeIn.value | Relax.value | Relax2.value | SpunOut.value | keyMod.value),
    FreeModAllowed(522171579, "FM"),
    //    ScoreIncreaseMods(Hidden.value | HardRock.value | Flashlight.value | DoubleTime.value | FadeIn.value)
    ScoreIncreaseMods(1049688, "IM"),
    // 其他未上传的mod
    Other(0, "?")
    ;
    public final int value;
    public final String abbreviation;

    Mod(int i, String name) {
        value = i;
        abbreviation = name;
    }

    public static Mod fromStr(String modStr){
        switch (modStr.toUpperCase()) {
            case "" : return  None;
            case "NF" : return  NoFail;
            case "EZ" : return  Easy;
            case "HT" : return  HalfTime;
            case "TD" : return  TouchDevice;
            case "HR" : return  HardRock;
            case "HD" : return  Hidden;
            case "FI" : return  FadeIn;
            case "SD" : return  SuddenDeath;
            case "PF" : return  Perfect;
            case "DT" : return  DoubleTime;
            case "NC" : return  Nightcore;
            case "FL" : return  Flashlight;
            case "RL" : return  Relax;
            case "AP" : return  Autopilot;
            case "AT" : return  Autoplay;
            case "CM" : return  Cinema;
            case "SO" : return  SpunOut;
            case "CP" : return  KeyCoop;
            case "MR" : return  Mirror;
            case "RD" : return  Random;
            case "SV2" : return  ScoreV2;
            default : return  Other;
        }
    }

    public boolean check(int i){
        return (value & i) != 0;
    }



    public static void main(String[] args) {
        int t = SuddenDeath.value | Easy.value;
        System.out.println(t);
        System.out.println(Easy.check(t));
        System.out.println(HardRock.check(t));
    }
}
