package com.now.nowbot.model;

import ch.qos.logback.core.util.TimeUtil;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.JsonData.Score;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

public class Ymp {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SIZE_FORMATTER = DateTimeFormatter.ofPattern("m:ss");

    String name;
    String mode;
    String country;
    int map_length;
    String map_name;
    String map_hard;
    String artist;
    float difficulty;
    String star;
    String rank;
    String[] mods;
    int score;
    float acc;
    float pp;
    int combo_max;
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

    public Ymp(JSONObject date) {
        var user = date.getJSONObject("user");
        name = user.getString("username");
        mode = date.getString("mode");
        country = user.getString("country_code");
        var modsdate = date.getJSONArray("mods");
        mods = new String[modsdate.size()];
        for (int i = 0; i < mods.length; i++) {
            mods[i] = modsdate.getString(i);
        }
        map_name = date.getJSONObject("beatmapset").getString("title_unicode");
        artist = date.getJSONObject("beatmapset").getString("artist_unicode");
        map_hard = date.getJSONObject("beatmap").getString("version");
        url = date.getJSONObject("beatmapset").getJSONObject("covers").getString("card");

        difficulty = date.getJSONObject("beatmap").getFloatValue("difficulty_rating");
        map_length = date.getJSONObject("beatmap").getIntValue("total_length");
        int starmun = (int) Math.floor(difficulty);
        star = "";
        for (int i = 0; i < starmun && i < 10; i++) {
            star += '★';
        }
        if (0.5 < (difficulty - starmun) && starmun < 10) {
            star += '☆';
        }

        rank = date.getString("rank");
        score = date.getIntValue("score");
        acc = (float) (Math.round(date.getFloatValue("accuracy") * 10000) / 100D);

        try {
            pp = date.getFloat("pp");
        } catch (Exception e) {
            pp = 0;
            e.printStackTrace();
        }

        combo = date.getIntValue("max_combo");
        bid = date.getJSONObject("beatmap").getIntValue("id");
        passed = date.getBoolean("passed");
        key = date.getJSONObject("beatmap").getIntValue("cs");

        var ndate = date.getJSONObject("statistics");
        n_300 = ndate.getIntValue("count_300");
        n_100 = ndate.getIntValue("count_100");
        n_50 = ndate.getIntValue("count_50");
        n_0 = ndate.getIntValue("count_miss");
        n_geki = ndate.getIntValue("count_geki");
        n_katu = ndate.getIntValue("count_katu");
        play_time = date.getString("created_at");

        if (!passed) rank = "F";
    }

    public Ymp(Score date) {
        var user = date.getUser();
        name = user.getName();
        mode = date.getMode().getName();
        country = user.getCountryCode();
        var modsdate = date.getMods();
        mods = new String[modsdate.size()];
        for (int i = 0; i < mods.length; i++) {
            mods[i] = modsdate.get(i);
        }
        var beatmapset = date.getBeatMapSet();
        map_name = beatmapset.getTitleUTF();
        artist = beatmapset.getArtistUTF();
        url = beatmapset.getCovers().getCard();
        map_hard = date.getBeatMap().getVersion();

        difficulty = date.getBeatMap().getDifficultyRating();
        map_length = date.getBeatMap().getTotalLength();
        int starmun = (int) Math.floor(difficulty);
        star = "";
        for (int i = 0; i < starmun && i < 10; i++) {
            star += '★';
        }
        if (0.5 < (difficulty - starmun) && starmun < 10) {
            star += '☆';
        }

        rank = date.getRank();
        score = date.getScore();
        acc = (float) ((Math.round(date.getAccuracy()) * 10000) / 100D);

        if (null == date.getPP()) {
            pp = 0;
        } else {
            pp = date.getPP();
        }

        combo = date.getMaxCombo();
        bid = Math.toIntExact(date.getBeatMap().getId());
        passed = date.getPassed();
        key = date.getBeatMap().getCS().intValue();

        var ndate = date.getStatistics();
        n_300 = ndate.getCount300();
        n_100 = ndate.getCount100();
        n_50 = ndate.getCount50();
        n_0 = ndate.getCountMiss();
        n_geki = ndate.getCountGeki();
        n_katu = ndate.getCountKatu();
        play_time = date.getCreateTime().toString();

        if (!passed) rank = "F";
    }

    public static Ymp getInstance(Score date) {
        return new Ymp(date);
    }

    public String getOut() {
        StringBuilder sb = new StringBuilder();

        //  "username"("country_code"): "mode" ("key"K)-if needed
        if ("mania".equals(mode)) {
            map_hard = map_hard.replaceAll("^\\[\\d{1,2}K\\]\\s*", "");
            sb.append(name).append('(').append(country).append(')').append(':').append(mode).append(' ')
                    .append('(').append(key).append("K").append(')').append('\n');
        } else {
            sb.append(name).append('(').append(country).append(')').append(':').append(mode).append('\n');
        }

        //  "artist_unicode" - "title_unicode" ["version"]
        sb.append(artist).append(" - ").append(map_name).append(' ').append('[').append(map_hard).append(']').append('\n');

        //  ★★★★★ "difficulty_rating"* mm:ss
        sb.append(star).append(' ').append(format(difficulty)).append('*').append(' ')
                .append(map_length / 60).append(':').append(String.format("%02d", map_length % 60)).append('\n');

        //  ["rank"] +"mods" "score" ("accuracy"%)
        sb.append('[').append(rank).append(']').append(' ');
        for (String mod : mods) {
            sb.append(mod).append(' ');
        }
        sb.append(String.valueOf(score).replaceAll("(?<=\\d)(?=(?:\\d{4})+$)", "\'")).append(' ').append('(').append(format(acc)).append('%').append(')').append('\n');

        //  "pp"(###)PP  "max_combo"/###x
        sb.append(format(pp)).append("(0)PP  ").append(combo).append("/0x").append('\n');

        //   "count_300" /  "count_100" / "count_50" / "count_miss"
        switch (mode) {
            default:
            case "osu": {
                sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
            }
            break;
            case "taiko": {
                sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_0).append('\n').append('\n');
            }
            break;
            case "mania": {
                sb.append(n_300).append('+').append(n_geki).append('(');
                if (n_300 >= n_geki && n_geki != 0) {
                    sb.append(String.format("%.1f", (1F * n_300 / n_geki))).append(':').append(1);
                } else if (n_300 < n_geki && n_300 != 0) {
                    sb.append(1).append(':').append(String.format("%.1f", (1F * n_geki / n_300)));
                } else {
                    sb.append('-');
                }
                sb.append(')').append(" / ").append(n_katu).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
            }
            break;
            case "catch":
            case "fruits": {
                sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('(').append('-').append(n_katu).append(')').append('\n').append('\n');
            }
            break;
        }

        //DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(play_time) 格式化 ISO-8601 日期格式

        sb.append("bid:").append(bid);
        return sb.toString();
    }

    static String format(double d) {
        double x = Math.round(d * 100) / 100D;
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(x);
    }
}
