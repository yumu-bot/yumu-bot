package com.now.nowbot.model.enums;

import com.now.nowbot.throwable.ModsException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.print.attribute.standard.MediaSize;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum OsuMod {
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
    keyMod(521109504, "KEY"),
    //    FreeModAllowed(NoFail.value | Easy.value | Hidden.value | HardRock.value | SuddenDeath.value | Flashlight.value | FadeIn.value | Relax.value | Autopilot.value | SpunOut.value | keyMod.value),
    FreeMod(522171579, "FM"),
    //    ScoreIncreaseMods(Hidden.value | HardRock.value | Flashlight.value | DoubleTime.value | FadeIn.value)
    ScoreIncreaseMods(1049688, "IM"),
    Other(-1,"OTHER");


    public final int value;
    public final String abbreviation;

    OsuMod(int i, String name) {
        value = i;
        abbreviation = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }



    @NonNull
    public static List<OsuMod> getModsList(@Nullable String abbr) {
        if (! StringUtils.hasText(abbr)) return new ArrayList<>(0);

        var abbrList = getModsAbbrList(abbr.toUpperCase());
        var modList = abbrList.stream().map(OsuMod::getModFromAbbreviation).filter(e -> e != Other).distinct().toList();
        checkModList(modList);
        return modList;
    }

    /**
     * 不能使用计算过的 Value
     * @param value 值
     * @return 模组类列表
     */
    public static List<OsuMod> getModsList(int value) {
        var modList = Arrays.stream(OsuMod.values()).filter(e -> 0 != (e.value & value)).distinct().toList();
        checkModList(modList);
        return modList;
    }

    /**
     * 不能使用计算过的 Value
     * @param value 值
     * @return 缩写列表
     */
    public static List<String> getModsAbbrList(int value) {
        var modList = getModsList(value);
        return modList.stream().map(OsuMod::getAbbreviation).toList();
    }

    /**
     * 不能使用计算过的 Value
     * @param value 值
     * @return 缩写组
     */
    public static String getModsAbbr(int value) {
        var modList = getModsList(value);
        return String.join("", modList.stream().map(OsuMod::getAbbreviation).toList());
    }

    public static int getModsValue(String abbr) {
        checkAbbr(abbr);
        var abbrList = getModsAbbrList(abbr.toUpperCase());
        return getModsValueFromAbbrList(abbrList);
    }

    /**
     * 这个没有检查，暂时专用于成绩的检查
     * @param abbrArray 模组数组
     * @return 值
     */
    public static int getModsValue(@Nullable String[] abbrArray) {
        if (abbrArray == null) return 0;

        var mList = Arrays.stream(abbrArray).map(String::toUpperCase).map(OsuMod::getModFromAbbreviation).filter(e -> e != Other).distinct().toList();
        return getModsValue(mList);
    }

    public static int getModsValue(@Nullable List<OsuMod> osuModList) {
        if (CollectionUtils.isEmpty(osuModList)) return 0;

        checkModList(osuModList);
        return osuModList.stream().map(m -> m.value).reduce(0, (i, s) -> s | i);
    }

    @NonNull
    public static int getModsValueFromAbbrList(@Nullable List<String> abbrList) {
        if (CollectionUtils.isEmpty(abbrList)) return 0;
        checkAbbrList(abbrList);

        return getModsValue(abbrList.stream().map(String::toUpperCase).map(OsuMod::getModFromAbbreviation).distinct().toList());
    }

    @NonNull
    public static List<String> getModsAbbrList(@Nullable String abbr) {
        if (! StringUtils.hasText(abbr)) return new ArrayList<>(0);

        var newStr = abbr.toUpperCase().replaceAll("\\s+", "");
        if (newStr.length() % 2 != 0) {
            throw new ModsException(ModsException.Type.MOD_Receive_CharNotPaired);
        }
        var list = Arrays.stream(newStr.split("(?<=\\w)(?=(\\w{2})+$)")).toList();
        checkAbbrList(list);

        return list;
    }

    private static void checkAbbr(@Nullable String abbr) {
        if (abbr == null || abbr.isEmpty()) return;

        var abbrList = getModsAbbrList(abbr.toUpperCase());
        var modList = new ArrayList<OsuMod>(abbrList.size());

        for (var a : abbrList) {
            var mod = getModFromAbbreviation(a);
            modList.add(mod);
        }

        checkModList(modList);
    }

    private static void checkAbbrList(@Nullable List<String> abbrList) {
        if (CollectionUtils.isEmpty(abbrList)) return;

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

    private static void checkModList(@Nullable List<OsuMod> osuModList) {
        if (CollectionUtils.isEmpty(osuModList)) return;

        if (osuModList.contains(None) && osuModList.size() > 1) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, None.abbreviation);
        }
        if (osuModList.contains(DoubleTime) && osuModList.contains(HalfTime)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{DoubleTime.abbreviation} \{HalfTime.abbreviation}");
        }
        if (osuModList.contains(HardRock) && osuModList.contains(Easy)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{HardRock.abbreviation} \{Easy.abbreviation}");
        }
        if (osuModList.contains(NoFail) && (osuModList.contains(SuddenDeath) || osuModList.contains(Perfect))) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{NoFail.abbreviation} \{SuddenDeath.abbreviation} \{Perfect.abbreviation}");
        }
        if (osuModList.contains(DoubleTime) && osuModList.contains(Nightcore)) {
            throw new ModsException(ModsException.Type.MOD_Receive_Conflict, STR."\{DoubleTime.abbreviation} \{Nightcore.abbreviation}");
        }
    }

    /**
     * 这里不能用 getModsValue，会误报重复
     * @param value 值
     * @return 倍率
     */
    @NonNull
    public static double getModsClockRate(int value) {
        return getModsClockRate(Arrays.stream(OsuMod.values()).filter(e -> 0 != (e.value & value)).distinct().toList());
    }

    @NonNull
    public static double getModsClockRate(@Nullable String abbr) {
        var modList = getModsList(abbr);
        return getModsClockRate(modList);
    }

    @NonNull
    public static double getModsClockRate(@Nullable List<OsuMod> osuModList) {
        if (CollectionUtils.isEmpty(osuModList)) return 1d;

        for (var m : osuModList) {
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
    public static OsuMod getModFromAbbreviation(@NonNull String abbr) {
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

    private static final int changeRatingValue = Easy.value | HalfTime.value | TouchDevice.value |
                    HardRock.value | DoubleTime.value | Nightcore.value | Flashlight.value;

    public static boolean hasChangeRating(int value) {
        return (changeRatingValue & value) != 0;
    }

    public static boolean hasChangeRating(List<OsuMod> osuMods) {
        int v = getModsValue(osuMods);
        return hasChangeRating(v);
    }

    public static boolean hasChangeRatingFromAbbrList(List<String> abbrList) {
        int v = getModsValueFromAbbrList(abbrList);
        return hasChangeRating(v);
    }

    public static int add(int old, OsuMod osuMod) {
        return old | osuMod.value;
    }

    public static int sub(int old, OsuMod osuMod) {
        return old & ~osuMod.value;
    }

    public int add(int old) {
        return old | this.value;
    }

    @Override
    public String toString() {
        return this.abbreviation;
    }
}
