package com.now.nowbot.model.enums;

import com.now.nowbot.throwable.ModsCatchException;

import java.util.Arrays;
import java.util.List;

public enum Mod {
    None(0, ""),
    NoFail(1, "NF"),
    Easy(1 << 1, "EZ"),
    //替换未使用的 No Video
    TouchDevice(1 << 2, "TD"),
    Hidden(1 << 3, "HD"),
    HardRock(1 << 4, "HR"),
    SuddenDeath(1 << 5, "SD"),
    DoubleTime(1 << 6, "DT"),
    Relax(1 << 7, "RL"),
    HalfTime(1 << 8, "HT"),
    //总是和 DT 一起使用 : 512 + 64 = 576
    Nightcore((1 << 9) + (DoubleTime.value), "NC"),
    Flashlight(1 << 10, "FL"),
    Autoplay(1 << 11, "AT"),
    SpunOut(1 << 12, "SO"),
    //Autopilot
    Autopilot(1 << 13, "AP"),
    Perfect(1 << 14, "PF"),
    Key4(1 << 15, "4K"),
    Key5(1 << 16, "5K"),
    Key6(1 << 17, "6K"),
    Key7(1 << 18, "7K"),
    Key8(1 << 19, "8K"),
    FadeIn(1 << 20, "FI"),
    // mania rd
    Random(1 << 21, "RD"),
    //Cinema
    Cinema(1 << 22, "CM"),
    //仅 osu!cuttingedge
    TargetPractice(1 << 23, "TP"),
    Key9(1 << 24, "9K"),
    KeyCoop(1 << 25, "CP"),
    Key1(1 << 26, "1K"),
    Key3(1 << 27, "3K"),
    Key2(1 << 28, "2K"),
    ScoreV2(1 << 29, "V2"),
    Mirror(1 << 30, "MR"),
    //    keyMod(Key1.value | Key2.value | Key3.value | Key4.value | Key5.value | Key6.value | Key7.value | Key8.value | Key9.value | KeyCoop.value),
    keyMod(521109504, "?"),
    //    FreeModAllowed(NoFail.value | Easy.value | Hidden.value | HardRock.value | SuddenDeath.value | Flashlight.value | FadeIn.value | Relax.value | Relax2.value | SpunOut.value | keyMod.value),
    FreeModAllowed(522171579, "FM"),
    //    ScoreIncreaseMods(Hidden.value | HardRock.value | Flashlight.value | DoubleTime.value | FadeIn.value)
    ScoreIncreaseMods(1049688, "IM"),
    // 其他未上传的mod
    Other(0, "?");
    public final int value;
    public final String abbreviation;

    Mod(int i, String name) {
        value = i;
        abbreviation = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static List<Mod> getModsList(String modsStr) {
        var modStrArray = getModsString(modsStr);
        var mList = Arrays.stream(modStrArray).map(Mod::fromStr).filter(e -> e != Other).toList();
        check(mList);
        return mList;
    }

    public static int getModsValue(String modsStr) {
        var modStrArray = getModsString(modsStr);
        var mList = Arrays.stream(modStrArray).map(Mod::fromStr).filter(e -> e != Other).toList();
        return getModsValue(mList);
    }

    private static String[] getModsString(String modsStr) {
        var newStr = modsStr.replaceAll("\\s+", "");
        if (newStr.length() % 2 != 0) {
            throw ModsCatchException.Create.SiseException();
        }
        return newStr.split("(?<=\\w)(?=(\\w{2})+$)");
    }

    private static void check(List<Mod> modList) {
        if (modList.contains(None) && modList.size() > 1) {
            throw ModsCatchException.Create.ConflictException(None);
        }
        if (modList.contains(DoubleTime) && modList.contains(HalfTime)) {
            throw ModsCatchException.Create.ConflictException(DoubleTime, HalfTime);
        }
        if (modList.contains(HardRock) && modList.contains(Easy)) {
            throw ModsCatchException.Create.ConflictException(HardRock, Easy);
        }
        if (modList.contains(NoFail) && (modList.contains(SuddenDeath) || modList.contains(Perfect))) {
            throw ModsCatchException.Create.ConflictException(NoFail, SuddenDeath, Perfect);
        }
    }

    public static int getModsValueFromStr(List<String> mList) {
        return getModsValue(mList.stream().map(Mod::fromStr).toList());
    }

    public static int getModsValue(List<Mod> mList) {
        check(mList);
        return mList.stream().map(m -> m.value).reduce(0, (i, s) -> s | i);
    }

    public static Mod fromStr(String modStr) {
        switch (modStr.toUpperCase()) {
            case "":
            case "NM":
                return None;
            case "NF":
                return NoFail;
            case "EZ":
                return Easy;
            case "HT":
                return HalfTime;
            case "TD":
                return TouchDevice;
            case "HR":
                return HardRock;
            case "HD":
                return Hidden;
            case "FI":
                return FadeIn;
            case "SD":
                return SuddenDeath;
            case "PF":
                return Perfect;
            case "DT":
                return DoubleTime;
            case "NC":
                return Nightcore;
            case "FL":
                return Flashlight;
            case "RL":
                return Relax;
            case "AP":
                return Autopilot;
            case "AT":
                return Autoplay;
            case "CM":
                return Cinema;
            case "SO":
                return SpunOut;
            case "CP":
                return KeyCoop;
            case "MR":
                return Mirror;
            case "RD":
                return Random;
            case "SV2":
                return ScoreV2;
            default:
                return Other;
        }
    }

    public boolean check(int i) {
        return (value & i) != 0;
    }

    public static boolean hasDt(int i) {
        return DoubleTime.check(i) || Nightcore.check(i);
    }

    public static boolean hasHt(int i) {
        return HalfTime.check(i);
    }

    public static boolean hasHr(int i) {
        return HardRock.check(i);
    }

    public static boolean hasEz(int i) {
        return Easy.check(i);
    }

    public static boolean hasChangeRating(int i) {
        return Easy.check(i) || HalfTime.check(i) ||
                HardRock.check(i) || DoubleTime.check(i) || Nightcore.check(i) || Flashlight.check(i);
    }

    public static int add(int old, Mod mod) {
        return old | mod.value;
    }

    public int add(int old) {
        return old | this.value;
    }


    public static void main(String[] args) {
        int t = SuddenDeath.value | Easy.value;
        System.out.println(t);
        System.out.println(Easy.check(t));
        System.out.println(HardRock.check(t));
    }
}
