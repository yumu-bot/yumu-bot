package com.now.nowbot.model.enums;

import com.now.nowbot.throwable.ModsException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static String getColor(String abbr) {
        var mod = getModFromAbbreviation(abbr);
        return mod.getColor();
    }

    public static List<Mod> getModsList(String abbr) {
        var abbrList = getModsAbbrList(abbr);
        var modList = abbrList.stream().map(Mod::getModFromAbbreviation).filter(e -> e != Other).distinct().toList();
        checkModList(modList);
        return modList;
    }

    /**
     * 不能使用计算过的 Value
     * @param value 值
     * @return 模组类列表
     */
    public static List<Mod> getModsList(int value) {
        var modList = Arrays.stream(Mod.values()).filter(e -> 0 != (e.value & value)).distinct().toList();
        checkModList(modList);
        return modList;
    }

    /**
     * 不能使用计算过的 Value
     * @param value 值
     * @return 缩写列表
     */
    public static List<String> getModsAbbrList(int value) {
        var mList = Arrays.stream(Mod.values()).filter(e -> 0 != (e.value & value)).distinct().toList();
        checkModList(mList);
        return mList.stream().map(Mod::getAbbreviation).toList();
    }

    /**
     * 不能使用计算过的 Value
     * @param value 值
     * @return 缩写组
     */
    public static String getModsAbbr(int value) {
        var mList = Arrays.stream(Mod.values()).filter(e -> 0 != (e.value & value)).distinct().toList();
        checkModList(mList);
        return String.join("", mList.stream().map(Mod::getAbbreviation).toList());
    }

    public static int getModsValue(String abbr) {
        checkAbbr(abbr);
        var abbrList = getModsAbbrList(abbr);
        return getModsValueFromAbbrList(abbrList);
    }

    /**
     * 这个没有检查，暂时专用于成绩的检查
     * @param abbrArray 模组数组
     * @return 值
     */
    public static int getModsValue(String[] abbrArray) {
        var mList = Arrays.stream(abbrArray).map(Mod::getModFromAbbreviation).filter(e -> e != Other).distinct().toList();
        return getModsValue(mList);
    }

    public static int getModsValue(List<Mod> modList) {
        if (CollectionUtils.isEmpty(modList)) return 0;

        checkModList(modList);
        return modList.stream().map(m -> m.value).reduce(0, (i, s) -> s | i);
    }

    @NonNull
    public static int getModsValueFromAbbrList(@Nullable List<String> abbrList) {
        if (CollectionUtils.isEmpty(abbrList)) return 0;
        checkAbbrList(abbrList);

        return getModsValue(abbrList.stream().map(Mod::getModFromAbbreviation).distinct().toList());
    }

    @NonNull
    public static List<String> getModsAbbrList(@Nullable String abbr) {
        if (! StringUtils.hasText(abbr)) return new ArrayList<>(0);
        var newStr = abbr.replaceAll("\\s+", "");
        if (newStr.length() % 2 != 0) {
            throw new ModsException(ModsException.Type.MOD_Receive_CharNotPaired);
        }
        var list = Arrays.stream(newStr.split("(?<=\\w)(?=(\\w{2})+$)")).toList();
        checkAbbrList(list);

        return list;
    }

    private static void checkAbbr(@Nullable String abbr) {
        if (abbr == null || abbr.isEmpty()) return;

        var abbrList = getModsAbbrList(abbr);
        var modList = new ArrayList<Mod>(abbrList.size());

        for (var a : abbrList) {
            var mod = getModFromAbbreviation(a);
            modList.add(mod);
        }

        checkModList(modList);
    }

    private static void checkAbbrList(List<String> abbrList) {
        if (abbrList.contains(None.abbreviation) && abbrList.size() > 1) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, None.abbreviation);
        }
        if (abbrList.contains(DoubleTime.abbreviation) && abbrList.contains(HalfTime.abbreviation)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{DoubleTime.abbreviation} \{HalfTime.abbreviation}");
        }
        if (abbrList.contains(HardRock.abbreviation) && abbrList.contains(Easy.abbreviation)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{HardRock.abbreviation} \{Easy.abbreviation}");
        }
        if (abbrList.contains(NoFail.abbreviation) && (abbrList.contains(SuddenDeath.abbreviation) || abbrList.contains(Perfect.abbreviation))) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{NoFail.abbreviation} \{SuddenDeath.abbreviation} \{Perfect.abbreviation}");
        }
        if (abbrList.contains(DoubleTime.abbreviation) && abbrList.contains(Nightcore.abbreviation)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{DoubleTime.abbreviation} \{Nightcore.abbreviation}");
        }
    }

    private static void checkModList(List<Mod> modList) {
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

    /**
     * 这里不能用 getModsValue，会误报重复
     * @param value 值
     * @return 倍率
     */
    public static double getModsClockRate(int value) {
        return getModsClockRate(Arrays.stream(Mod.values()).filter(e -> 0 != (e.value & value)).distinct().toList());
    }

    public static double getModsClockRate(@Nullable String abbr) {
        var modList = getModsList(abbr);
        return getModsClockRate(modList);
    }

    public static double getModsClockRate(@Nullable List<Mod> modList) {
        if (CollectionUtils.isEmpty(modList)) return 1d;

        for (var m : modList) {
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
    public static Mod getModFromAbbreviation(@NonNull String abbr) {
        return switch (abbr.toUpperCase()) {
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

    public boolean checkValue(int i) {
        return (value & i) != 0;
    }

    public static boolean hasDt(int i) {
        return DoubleTime.checkValue(i) || Nightcore.checkValue(i);
    }

    public static boolean hasHt(int i) {
        return HalfTime.checkValue(i);
    }

    public static boolean hasHr(int i) {
        return HardRock.checkValue(i);
    }

    public static boolean hasEz(int i) {
        return Easy.checkValue(i);
    }

    public static boolean hasChangeRating(int value) {
        return Easy.checkValue(value) || HalfTime.checkValue(value) ||
                HardRock.checkValue(value) || DoubleTime.checkValue(value) || Nightcore.checkValue(value) || Flashlight.checkValue(value);
    }

    public static boolean hasChangeRating(List<Mod> mods) {
        int v = getModsValue(mods);
        return hasChangeRating(v);
    }

    public static boolean hasChangeRatingFromAbbrList(List<String> abbrList) {
        int v = getModsValueFromAbbrList(abbrList);
        return hasChangeRating(v);
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

    @Override
    public String toString() {
        return this.abbreviation;
    }
}
