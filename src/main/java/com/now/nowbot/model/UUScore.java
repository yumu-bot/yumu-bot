package com.now.nowbot.model;

import com.now.nowbot.model.json.BeatMap;
import com.now.nowbot.model.json.Score;
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class UUScore {
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
    boolean passed;
    String url;
    int key;
    String play_time;

    public String getUrl() {
        return url;
    }

    public UUScore(Score score, OsuBeatmapApiService osuBeatmapApiService) throws GeneralTipsException {
        var user = score.getUser();
        bid = Math.toIntExact(score.getBeatMap().getBeatMapID());

        BeatMap b;

        try {
            b = osuBeatmapApiService.getBeatMapFromDataBase(bid);
        } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me);
        }

        var s = b.getBeatMapSet();
        var modsList = score.getMods();

        name = user.getUserName();
        mode = score.getMode().getName();
        country = user.getCountryCode();
        mods = new String[modsList.size()];
        for (int i = 0; i < mods.length; i++) {
            mods[i] = modsList.get(i);
        }
        if (s != null) {
            map_name = s.getTitleUnicode();
            artist = s.getArtistUnicode();
            url = s.getCovers().getCard();
        }
        max_combo = b.getMaxCombo();
        difficulty_name = b.getDifficultyName();

        star_rating = b.getStarRating();
        map_length = b.getTotalLength();

        int sr_floor = (int) Math.floor(star_rating);
        var sr_str = new StringBuilder();
        for (int i = 0; i < sr_floor && i < 10; i++) {
            sr_str.append('★');
        }
        if (0.5 < (star_rating - sr_floor) && sr_floor < 10) {
            sr_str.append('☆');
        }
        star_str = sr_str.toString();

        rank = score.getRank();
        this.score = score.getScore();
        acc = (float) ((Math.round(score.getAccuracy() * 10000)) / 100D);

        pp = Objects.nonNull(score.getPP()) ? score.getPP() : 0;

        combo = score.getMaxCombo();
        passed = score.getPassed();
        key = score.getBeatMap().getCS().intValue();
        play_time = score.getCreateTimePretty().toString();

        var stat = score.getStatistics();
        n_300 = stat.getCount300();
        n_100 = stat.getCount100();
        n_50 = stat.getCount50();
        n_geki = stat.getCountGeki();
        n_katu = stat.getCountKatu();
        n_0 = stat.getCountMiss();

        if (!passed) rank = "F";
    }

    public static UUScore getInstance(Score score, OsuBeatmapApiService beatmapApiService) throws GeneralTipsException {
        return new UUScore(score, beatmapApiService);
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

            default -> sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
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
