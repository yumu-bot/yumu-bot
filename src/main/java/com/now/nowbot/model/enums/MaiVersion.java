package com.now.nowbot.model.enums;



public enum MaiVersion {
    DEFAULT("maimai", ""),

    PLUS("maimai PLUS", "真"),
    GREEN("maimai GreeN", "超"),
    GREEN_PLUS("maimai GreeN PLUS", "檄"),
    ORANGE("maimai ORANGE", "橙"),
    ORANGE_PLUS("maimai ORANGE PLUS", "暁"),
    PINK("maimai PiNK", "桃"),
    PINK_PLUS("maimai PiNK PLUS", "櫻"),
    MURASAKI("maimai MURASAKi", "紫"),
    MURASAKI_PLUS("maimai MURASAKi PLUS", "菫"),
    MILK("maimai MiLK", "白"),
    MILK_PLUS("MiLK PLUS", "雪"),
    FINALE("maimai FiNALE", "輝"),
    ALL_FINALE("ALL FiNALE", "舞"),
    DX("maimai でらっくす", "熊"),
    DX_PLUS("maimai でらっくす PLUS", "華"),
    SPLASH("maimai でらっくす Splash", "爽"),
    SPLASH_PLUS("maimai でらっくす Splash PLUS", "煌"),
    UNIVERSE("maimai でらっくす UNiVERSE", "宙"),
    UNIVERSE_PLUS("maimai でらっくす UNiVERSE PLUS", "星"),
    FESTIVAL("maimai でらっくす FESTiVAL", "祭"),
    FESTIVAL_PLUS("maimai でらっくす FESTiVAL PLUS", "祝"),
    BUDDIES("maimai でらっくす BUDDiES", ""),
    BUDDIES_PLUS("maimai でらっくす BUDDiES PLUS", ""),
    PRISM("maimai でらっくす PRiSM", ""),
    PRISM_PLUS("maimai でらっくす PRiSM PLUS", ""),

    ;

    final String name;
    final String abbr;

    MaiVersion(String name, String abbr) {
        this.name = name;
        this.abbr = abbr;
    }

    public static MaiVersion getVersion(String name) {
        if (name == null) return DEFAULT;

        return switch (name.trim().toLowerCase()) {
            case "maimai", "mi", "0.1", "0.10" -> DEFAULT;
            case "plus", "maimaiplus", "maimai plus", "pl", "pls", "0.11" -> PLUS;
            case "green", "gr", "grn", "0.2", "0.20" -> GREEN;
            case "greenplus", "green plus", "grp", "0.21" -> GREEN_PLUS;
            case "orange", "or", "org", "0.3", "0.30" -> ORANGE;
            case "orangeplus", "orange plus", "orp", "0.31" -> ORANGE_PLUS;
            case "pink", "pk", "pnk", "0.4", "0.40" -> PINK;
            case "pinkplus", "pink plus", "pkp", "0.41" -> PINK_PLUS;
            case "murasaki", "ms", "msk", "0.5", "0.50" -> MURASAKI;
            case "murasakiplus", "murasaki plus", "msp", "0.51" -> MURASAKI_PLUS;
            case "milk", "white", "mk", "mlk", "0.6", "0.60" -> MILK;
            case "milkplus", "milk plus", "mkp", "0.61" -> MILK_PLUS;
            case "finale", "final", "fn", "fnl", "0.7", "0.70" -> FINALE;
            case "allfinale", "all finale", "finale plus", "afn", "0.71" -> ALL_FINALE;
            case "deluxe", "dx", "dlx", "1.0", "1.00" -> DX;
            case "deluxeplus", "deluxe plus", "dxp", "dlxp", "1.01" -> DX_PLUS;
            case "splash", "sp", "spl", "1.1", "1.10" -> SPLASH;
            case "splashplus", "splash plus", "spp", "splp", "1.11" -> SPLASH_PLUS;
            case "festival", "fs", "fes", "fst", "1.2", "1.20" -> FESTIVAL;
            case "festivalplus", "festival plus", "fep", "fsp", "fesp", "1.21" -> FESTIVAL_PLUS;
            case "universe", "un", "uv", "uni", "1.3", "1.30" -> UNIVERSE;
            case "universeplus", "universe plus", "unp", "uvp", "unvp", "1.31" -> UNIVERSE_PLUS;
            case "buddies", "bd", "bud", "1.4", "1.40" -> BUDDIES;
            case "buddiesplus", "buddies plus", "bdp", "budp", "1.41" -> BUDDIES_PLUS;

            default -> {
                for (var v: MaiVersion.values()) {
                    if (name.equals(v.name) || name.equals(v.abbr)) {
                        yield v;
                    }
                }
                yield DEFAULT;
            }
        };



    }

    public String getName() {
        return name;
    }

    public String getAbbr() {
        return abbr;
    }
}
