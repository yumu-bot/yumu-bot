package com.now.nowbot.model.beatmap;

public enum Mod {
    None(0),
    NoFail(1<<0),
    Easy(1<<1),
    //替换未使用的 No Video
    TouchDevice(1<<2),
    Hidden(1<<3),
    HardRock(1<<4),
    SuddenDeath(1<<5),
    DoubleTime(1<<6),
    Relax(1<<7),
    HalfTime(1<<8),
    //总是和 DT 一起使用 : 512 + 64 = 576
    Nightcore(1<<9),
    Flashlight(1<<10),
    Autoplay(1<<11),
    SpunOut(1<<12),
    //Autopilot
    Relax2(1<<13),
    Perfect(1<<14),
    Key4(1<<15),
    Key5(1<<16),
    Key6(1<<17),
    Key7(1<<18),
    Key8(1<<19),
    FadeIn(1<<20),
    Random(1<<21),
    //Cinema
    LastMod(1<<22),
    //仅 osu!cuttingedge
    TargetPractice(1<<23),
    Key9(1<<24),
    KeyCoop(1<<25),
    Key1(1<<26),
    Key3(1<<27),
    Key2(1<<28),
    ScoreV2(1<<29),
    Mirror(1<<30),
    //    keyMod(Key1.value | Key2.value | Key3.value | Key4.value | Key5.value | Key6.value | Key7.value | Key8.value | Key9.value | KeyCoop.value),
    keyMod(521109504),
    //    FreeModAllowed(NoFail.value | Easy.value | Hidden.value | HardRock.value | SuddenDeath.value | Flashlight.value | FadeIn.value | Relax.value | Relax2.value | SpunOut.value | keyMod.value),
    FreeModAllowed(522171579),
    //    ScoreIncreaseMods(Hidden.value | HardRock.value | Flashlight.value | DoubleTime.value | FadeIn.value)
    ScoreIncreaseMods(1049688)
    ;
    final int value;

    Mod(int i) {
        value = i;
    }
}
