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
    Key4(1<<15, ""),
    Key5(1<<16, ""),
    Key6(1<<17, ""),
    Key7(1<<18, ""),
    Key8(1<<19, ""),
    FadeIn(1<<20, "FI"),
    // mania rd
    Random(1<<21, "RD"),
    //Cinema
    Cinema(1<<22, "CM"),
    //仅 osu!cuttingedge
    TargetPractice(1<<23, "?"),
    Key9(1<<24, "K9"),
    KeyCoop(1<<25, "CP"),
    Key1(1<<26, "K1"),
    Key3(1<<27, "K2"),
    Key2(1<<28, "K3"),
    ScoreV2(1<<29, "SV2"),
    Mirror(1<<30, "MR"),
    //    keyMod(Key1.value | Key2.value | Key3.value | Key4.value | Key5.value | Key6.value | Key7.value | Key8.value | Key9.value | KeyCoop.value),
    keyMod(521109504, "?"),
    //    FreeModAllowed(NoFail.value | Easy.value | Hidden.value | HardRock.value | SuddenDeath.value | Flashlight.value | FadeIn.value | Relax.value | Relax2.value | SpunOut.value | keyMod.value),
    FreeModAllowed(522171579, "?"),
    //    ScoreIncreaseMods(Hidden.value | HardRock.value | Flashlight.value | DoubleTime.value | FadeIn.value)
    ScoreIncreaseMods(1049688, "?"),
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
        return switch (modStr.toUpperCase()) {
            case "" -> None;
            case "NF" -> NoFail;
            case "EZ" -> Easy;
            case "HT" -> HalfTime;
            case "TD" -> TouchDevice;
            case "HR" -> HardRock;
            case "HD" -> Hidden;
            case "FI" -> FadeIn;
            case "SD" -> SuddenDeath;
            case "PF" -> Perfect;
            case "DT" -> DoubleTime;
            case "NC" -> Nightcore;
            case "FL" -> Flashlight;
            case "RL" -> Relax;
            case "AP" -> Autopilot;
            case "AT" -> Autoplay;
            case "CM" -> Cinema;
            case "SO" -> SpunOut;
            case "CP" -> KeyCoop;
            case "MR" -> Mirror;
            case "RD" -> Random;
            case "SV2" -> ScoreV2;
            default -> Other;
        };
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
