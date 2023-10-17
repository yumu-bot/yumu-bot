package com.now.nowbot.model;

import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.service.OsuGetService;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

public class ScoreLegacy {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SIZE_FORMATTER = DateTimeFormatter.ofPattern("m:ss");

    String name;
    String mode;
    String country;
    int map_length;
    String map_name;
    String difficulty_name;
    String artist;
    float star_rating;
    String star_str;
    String rank;
    String[] mods;
    int score;
    float acc;
    float pp;
    int max_combo;
    int combo;
    int bid;

    int n_300;
    int n_100;
    int n_50;
    int n_geki;
    int n_katu;
    int n_0;
    boolean passed = true;
    String url;
    int key;
    String play_time;

    public String getUrl() {
        return url;
    }

    public ScoreLegacy(Score score, OsuGetService osuGetService) {
        var user = score.getUser();
        bid = Math.toIntExact(score.getBeatMap().getId());

        var beatMap = osuGetService.getBeatMapInfo(bid);
        var beatMapSet = beatMap.getBeatMapSet();
        var modsList = score.getMods();

        name = user.getUserName();
        mode = score.getMode().getName();
        country = user.getCountryCode();
        mods = new String[modsList.size()];
        for (int i = 0; i < mods.length; i++) {
            mods[i] = modsList.get(i);
        }
        map_name = beatMapSet.getTitleUTF();
        artist = beatMapSet.getArtistUTF();
        url = beatMapSet.getCovers().getCard();
        max_combo = beatMap.getMaxCombo();
        difficulty_name = beatMap.getVersion();

        star_rating = beatMap.getDifficultyRating();
        map_length = beatMap.getTotalLength();

        int sr_floor = (int) Math.floor(star_rating);
        star_str = "";
        for (int i = 0; i < sr_floor && i < 10; i++) {
            star_str += '★';
        }
        if (0.5 < (star_rating - sr_floor) && sr_floor < 10) {
            star_str += '☆';
        }

        rank = score.getRank();
        this.score = score.getScore();
        acc = (float) ((Math.round(score.getAccuracy() * 10000)) / 100D);

        if (score.getPP() == null) {
            pp = 0;
        } else {
            pp = score.getPP();
        }

        combo = score.getMaxCombo();
        passed = score.getPassed();
        key = score.getBeatMap().getCS().intValue();
        play_time = score.getCreateTime().toString();

        var stat = score.getStatistics();
        n_300 = stat.getCount300();
        n_100 = stat.getCount100();
        n_50 = stat.getCount50();
        n_0 = stat.getCountMiss();
        n_geki = stat.getCountGeki();
        n_katu = stat.getCountKatu();

        if (!passed) rank = "F";
    }

    public static ScoreLegacy getInstance(Score score, OsuGetService osuGetService) {
        return new ScoreLegacy(score, osuGetService);
    }

    public String getScoreLegacyOutput() {
        StringBuilder sb = new StringBuilder();

        //  "username" ("country_code"): "mode" ("key"K)-if needed

        sb.append(name).append(' ').append('(').append(country).append(')').append(':').append(' ').append(mode);

        if (mode.equals("mania")) {
            difficulty_name = difficulty_name.replaceAll("^\\[\\d{1,2}K\\]\\s*", "");
            sb.append(' ').append('(').append(key).append("K").append(')').append('\n');
        } else {
            sb.append('\n');
        }

        //  "artist_unicode" - "title_unicode" ["version"]
        sb.append(artist).append(" - ").append(map_name).append(' ').append('[').append(difficulty_name).append(']').append('\n');

        //  ★★★★★ "difficulty_rating"* mm:ss
        sb.append(star_str).append(' ').append(format(star_rating)).append('*').append(' ')
                .append(map_length / 60).append(':').append(String.format("%02d", map_length % 60)).append('\n');

        //  ["rank"] +"mods" "score" "pp"(###)PP
        sb.append('[').append(rank).append(']').append(' ');

        if (mods.length >= 1) {
            sb.append('+');
            for (String mod : mods) {
                sb.append(mod).append(' ');
            }
        }

        sb.append(String.valueOf(score).replaceAll("(?<=\\d)(?=(?:\\d{4})+$)", "'")).append(' ').append('(').append(format(pp)).append("PP").append(')').append('\n');

        //  "max_combo"/###x "accuracy"%
        sb.append(combo).append('x').append(' ').append('/').append(' ').append(max_combo).append('x')
                .append(' ').append('/').append('/').append(' ').append(format(acc)).append('%').append('\n');

        //  "count_300" /  "count_100" / "count_50" / "count_miss"
        switch (mode) {
            // "osu" 与其他走默认分支
            default -> sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
            case "taiko" -> sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_0).append('\n').append('\n');
            case "mania" -> {
                sb.append(n_300).append('+').append(n_geki).append('(');
                if (n_300 >= n_geki && n_geki != 0) {
                    sb.append(String.format("%.2f", (1F * n_geki / n_300)));
                } else if (n_300 < n_geki && n_300 != 0) {
                    sb.append(String.format("%.1f", (1F * n_geki / n_300)));
                } else {
                    sb.append('-');
                }
                sb.append(')').append(" / ").append(n_katu).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
            }
            case "catch", "fruits" ->  sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('(').append('-').append(n_katu).append(')').append('\n').append('\n');

        }

        //DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(play_time) 格式化 ISO-8601 日期格式

        sb.append("bid: ").append(bid);
        return sb.toString();
    }

    static String format(double d) {
        double x = Math.round(d * 100) / 100D;
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(x);
    }
}
