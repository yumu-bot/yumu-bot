package com.now.nowbot.model.enums;

import com.now.nowbot.throwable.ModsException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public enum Mod {
    None(0, "", "#22AC38", 10),
    NoFail(1, "NF", "#00A0E9", 10),
    Easy(1 << 1, "EZ", None.color, 10),
    //替换未使用的 No Video
    TouchDevice(1 << 2, "TD", "#7ECEF4", 0),
    Hidden(1 << 3, "HD", "#F8B551", 18),
    HardRock(1 << 4, "HR", "#D32F2F", 17),
    SuddenDeath(1 << 5, "SD", "#FF9800", 20),
    DoubleTime(1 << 6, "DT", "#0068B7", 19),
    Relax(1 << 7, "RL", "#8FC31F", 30),
    HalfTime(1 << 8, "HT", "#BDBDBD", 20),
    //总是和 DT 一起使用 : 512 + 64 = 576
    Nightcore((1 << 9) + (DoubleTime.value), "NC", "#601986", 20),
    Flashlight(1 << 10, "FL", "#313131", 20),
    Autoplay(1 << 11, "AT", "#00B7EE", 20),
    SpunOut(1 << 12, "SO", "#B28850", 30),
    //Autopilot
    Autopilot(1 << 13, "AP", "#B3D465", 30),
    Perfect(1 << 14, "PF", "#FFF100", 20),
    Key4(1 << 15, "4K", "#616161", 30),
    Key5(1 << 16, "5K", "#616161", 30),
    Key6(1 << 17, "6K", "#616161", 30),
    Key7(1 << 18, "7K", "#616161", 30),
    Key8(1 << 19, "8K", "#616161", 30),
    FadeIn(1 << 20, "FI", Hidden.color, 20),
    // mania rd
    Random(1 << 21, "RD", "#009944", 30),
    //Cinema
    Cinema(1 << 22, "CM", Autoplay.color, 30),
    //仅 osu!cuttingedge
    TargetPractice(1 << 23, "TP", "#920783", 30),
    Key9(1 << 24, "9K", "#616161", 30),
    KeyCoop(1 << 25, "CP", "#EA68A2", 30),
    Key1(1 << 26, "1K", "#616161", 30),
    Key3(1 << 27, "3K", "#616161", 30),
    Key2(1 << 28, "2K", "#616161", 30),
    ScoreV2(1 << 29, "V2", "#2A2226", 30),
    Mirror(1 << 30, "MR", "#007130", 30),
    //    keyMod(Key1.value | Key2.value | Key3.value | Key4.value | Key5.value | Key6.value | Key7.value | Key8.value | Key9.value | KeyCoop.value),
    keyMod(521109504, "KEY", "#616161", 30),
    //    FreeModAllowed(NoFail.value | Easy.value | Hidden.value | HardRock.value | SuddenDeath.value | Flashlight.value | FadeIn.value | Relax.value | Autopilot.value | SpunOut.value | keyMod.value),
    ScoreIncreaseMods(1049688, "IM", "#9922EE", 0),

    //给谱面用的 Mod
    FreeMod(522171579, "FM", ScoreIncreaseMods.color, 30),
    //    ScoreIncreaseMods(Hidden.value | HardRock.value | Flashlight.value | DoubleTime.value | FadeIn.value)
    LongNote(-1, "LN", NoFail.color, 20),
    NoMod(-1, "NM", Easy.color, 10),
    Rice(-1, "RC", Easy.color, 10),
    Hybrid(-1, "HB", Hidden.color, 30),
    Extra(-1, "EX", SuddenDeath.color, 50),
    TieBreaker(-1, "TB", Flashlight.color, 100),
    SpeedVariation(-1, "SV", ScoreIncreaseMods.color, 40),

    // 其他未上传的mod (Fun Mods)
    Other(-1, "OTHER", "#EA68A2", 90);

    public final int value;
    public final String abbreviation;
    public final String color;
    public final int priority;

    Mod(int i, String name, String color, int priority) {
        value = i;
        abbreviation = name;
        this.color = color;
        this.priority = priority;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getColor() {
        return color;
    }
    public int getPriority() {
        return priority;
    }

    public static String getColor(String modStr) {
        var m = fromStr(modStr);
        return m.getColor();
    }

    public static List<Mod> getModsList(String modsStr) {
        var modStrArray = getModsString(modsStr);
        var mList = Arrays.stream(modStrArray).map(Mod::fromStr).filter(e -> e != Other).distinct().toList();
        check(mList);
        return mList;
    }

    public static List<Mod> getModsList(int mods) {
        var mList = Arrays.stream(Mod.values()).filter(e -> 0 != (e.value & mods)).distinct().toList();
        check(mList);
        return mList;
    }

    public static List<String> getModsStrList(Integer mods) {
        if (Objects.isNull(mods)) return new ArrayList<>();
        var mList = Arrays.stream(Mod.values()).filter(e -> 0 != (e.value & mods)).distinct().toList();
        check(mList);
        return mList.stream().map(Mod::getAbbreviation).toList();
    }

    public static String getModsStr(Integer mods) {
        if (Objects.isNull(mods)) return "";
        var mList = Arrays.stream(Mod.values()).filter(e -> 0 != (e.value & mods)).distinct().toList();
        check(mList);
        return String.join("", mList.stream().map(Mod::getAbbreviation).toList());
    }

    public static int getModsValue(String modsStr) {
        var modStrArray = getModsString(modsStr);
        var mList = Arrays.stream(modStrArray).map(Mod::fromStr).filter(e -> e != Other).distinct().toList();
        return getModsValue(mList);
    }
    public static int getModsValue(String[] modsStr) {
        var mList = Arrays.stream(modsStr).map(Mod::fromStr).filter(e -> e != Other).distinct().toList();
        return getModsValue(mList);
    }

    private static String[] getModsString(String modsStr) {
        if (! StringUtils.hasText(modsStr)) return new String[]{};
        var newStr = modsStr.replaceAll("\\s+", "");
        if (newStr.length() % 2 != 0) {
            throw new ModsException(ModsException.Type.MOD_Receive_CharNotPaired);
        }
        return newStr.split("(?<=\\w)(?=(\\w{2})+$)");
    }

    private static void check(List<Mod> modList) {
        if (modList.contains(None) && modList.size() > 1) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, None.abbreviation);
        }
        if (modList.contains(DoubleTime) && modList.contains(HalfTime)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{DoubleTime.abbreviation} \{HalfTime.abbreviation}");
        }
        if (modList.contains(HardRock) && modList.contains(Easy)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{HardRock.abbreviation} \{Easy.abbreviation}");
        }
        if (modList.contains(NoFail) && (modList.contains(SuddenDeath) || modList.contains(Perfect))) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{NoFail.abbreviation} \{SuddenDeath.abbreviation} \{Perfect.abbreviation}");
        }
        if (modList.contains(DoubleTime) && modList.contains(Nightcore)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{DoubleTime.abbreviation} \{Nightcore.abbreviation}");
        }
    }

    public static double getModsClockRate(int modsValue) {
        // 这里不能用 getModsValue，会误报重复
        return getModsClockRate(Arrays.stream(Mod.values()).filter(e -> 0 != (e.value & modsValue)).distinct().toList());
    }

    public static double getModsClockRate(List<Mod> mList) {
        for (var m : mList) {
            switch (m) {
                case HalfTime -> {
                    return 0.75d;
                }
                case DoubleTime, Nightcore -> {
                    return 1.5d;
                }
            }
        }

        return 1d;
    }

    @NonNull
    public static int getModsValueFromStr(@Nullable List<String> mList) {
        if (CollectionUtils.isEmpty(mList)) return 0;

        return getModsValue(mList.stream().map(Mod::fromStr).distinct().toList());
    }

    public static int getModsValue(List<Mod> mList) {
        check(mList);
        return mList.stream().map(m -> m.value).reduce(0, (i, s) -> s | i);
    }

    public static Mod fromStr(String modStr) {
        return switch (modStr.toUpperCase()) {
            case "", "NM" -> None;
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

    public static boolean hasChangeRating(List<String> mods) {
        int i = getModsValueFromStr(mods);
        return Easy.check(i) || HalfTime.check(i) ||
                HardRock.check(i) || DoubleTime.check(i) || Nightcore.check(i) || Flashlight.check(i);
    }

    public static int add(int old, Mod mod) {
        return old | mod.value;
    }

    public static int sub(int old, Mod mod) {
        return old & ~mod.value;
    }

    public int add(int old) {
        return old | this.value;
    }


}
