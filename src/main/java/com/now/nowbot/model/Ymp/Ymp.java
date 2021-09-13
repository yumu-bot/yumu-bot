package com.now.nowbot.model.Ymp;

import com.alibaba.fastjson.JSONObject;

import java.text.NumberFormat;

public class Ymp {
    String name;
    String mode;
    String country;
    String map_name;
    String map_hard;
    String artist;
    float difficulty;
    String star;
    String rank;
    String [] mods;
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

    public String getUrl(){return url;}
    public Ymp(JSONObject date){
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
        int starmun = (int) Math.floor(difficulty);
        star = "";
        for (int i = 0; i < starmun; i++) {
            star += '★';
        }
        if (0.5<(difficulty-starmun)){
            star += '☆';
        }

        rank = date.getString("rank");
        score = date.getIntValue("score");
        acc = (float) (Math.round(date.getFloatValue("accuracy")*10000)/100D);
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
        
        if (!passed) rank = "F";
    }
    public static Ymp getInstance(JSONObject date){
        Ymp rdate;
        switch (date.getString("mode")){
            default:
            case "osu":{
                rdate = new Ymp(date);
            }
        }
        return rdate;
    }
    public String getOut(){
        StringBuilder sb = new StringBuilder();
        /*
         "username"("country_code"): "mode" ("key"K)-if needed
         "artist_unicode" - "title_unicode" ["version"]
         ★★★★★ "difficulty_rating"*
         ["rank"] +"mods" "score" ("accuracy"%)
         "pp"(###)PP  "max_combo"/###x
         "count_300" /  "count_100" / "count_50" / "count_miss"
         */
        switch (mode){
            default:
            case "osu":
            case "taiko":
            case "catch":{
                sb.append(name).append('(').append(country).append(')').append(':').append(mode).append('\n');
            }break;
            case "mania":{
                sb.append(name).append('(').append(country).append(')').append(':').append(mode).append(' ').append('(').append(key).append("K").append(')').append('\n');
            }break;
        }
        
        sb.append(artist).append(" - ").append(map_name).append(' ').append('[').append(map_hard).append(']').append('\n');
        sb.append(star).append(' ').append(format(difficulty)).append('*').append('\n');
        sb.append('[').append(rank).append(']').append(' ');
        for (String mod : mods) {
            sb.append(mod).append(' ');
        }
        sb.append(score).append(' ').append('(').append(format(acc)).append('%').append(')').append('\n');
        sb.append(format(pp)).append('(').append("###").append(')').append("PP ").append(combo).append('/').append("###").append('X').append('\n');

        switch (mode){
            default:
            case "osu":{
                sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
            }break;
            case "taiko":{
                sb.append(n_300).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
            }break;
            case "mania":{
                sb.append(n_300).append('+').append(n_geki).append('(');
                if (n_300 >= n_geki && n_geki != 0){
                    sb.append(String.format("%.1f",(1F*n_300/n_geki))).append(':').append(1);
                }else if(n_300 < n_geki && n_300 != 0) {
                    sb.append(1).append(':').append(String.format("%.1f",(1F*n_geki/n_300)));
                }else{
                    sb.append('-');
                }
                sb.append(')').append(" / ").append(n_katu).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
            }break;
            case "catch":
            case "fruits":{
                sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('(').append('-').append(n_katu).append(')').append('\n').append('\n');
            }break;
        }
        sb.append("bid:").append(bid);
        return sb.toString();
    }
    static String format(double d){
        double x = Math.round(d*100)/100D;
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(x);
    }
}
