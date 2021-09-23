package com.now.nowbot.model;

import ch.qos.logback.core.util.TimeUtil;
import com.alibaba.fastjson.JSONObject;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

public class Ymi {

    String name;
    String mode;
    String country;
    float pp;
    int global_rank;
    int country_rank;
    int level_current;
    int level_progress;
    int play_count;
    int total_hits;
    int play_time;
    float hit_accuracy;
    int follower_count;
    int kudosu_total;
    int support_level;
    int grade_count_ss;
    int grade_count_ssh;
    int grade_count_s;
    int grade_count_sh;
    int grade_count_a;

    int uid;

    public String getUrl(){return url;}
    public Ymi(JSONObject date){
        var user = date.getJSONObject("user");
        name = user.getString("username");
        mode = date.getString("mode");
        country = user.getString("country_code");
        
        var idate = date.getJSONObject("statistics");
            pp = idate.getFloatValue("pp");
            global_rank = idate.getIntValue("global_rank");
            country_rank = idate.getIntValue("country_rank");
            level_current = idate.getJSONObject("level").getIntValue("current");
            level_progress = idate.getJSONObject("level").getIntValue("progress");
            play_count = idate.getIntValue("play_count");
            total_hits = idate.getIntValue("total_hits");
            play_time = idate.getIntValue("play_time");
            hit_accuracy = (float) (Math.round(idate.getFloatValue("hit_accuracy")*10000)/100D);
            
            grade_count_ss = idate.getJSONObject("grade_counts").getIntValue("ss");
            grade_count_ssh = idate.getJSONObject("grade_counts").getIntValue("ssh");
            grade_count_s = idate.getJSONObject("grade_counts").getIntValue("s");
            grade_count_sh = idate.getJSONObject("grade_counts").getIntValue("sh");
            grade_count_a = idate.getJSONObject("grade_counts").getIntValue("a");
        
        follower_count = date.getIntValue("follower_count");
        kudosu_total = date.getJSONObject("kudosu").getIntValue("total");
        support_level = date.getIntValue("support_level");
        uid = date.getIntValue("id");

    }
    public static Ymi getInstance(JSONObject date){
        return new Ymi(date);
    }
    public String getOut(){
        StringBuilder sb = new StringBuilder();
        // Muziyami(standard):10086PP
        sb.append(name).append('(').append(mode).append(')').append(':').append(pp).append(' ').append("PP").append('\n');

        // #114514 CN#1919 (LV.100(32%))
        sb.append('#').append('global_rank').append(' ').append(country).append('#').append('country_rank').append(' ').append("(LV.").append('level_current').append('(').append('level_progress').append("%))").append('\n');

        // PC:2.01w TTH:743.52w 
        if (play_count>10000){
            sb.append("PC:").append(String.format("%02d",play_count/10000)).append("w").append(' ');
        }else{
            sb.append("PC:").append(play_count).append(' ');
        }

        if (total_hits>100000000){
            sb.append("TTH:").append(String.format("%02d",total_hits/100000000)).append("e").append('\n');
        }else if(total_hits>10000){
            sb.append("TTH:").append(String.format("%02d",total_hits/10000)).append("w").append('\n');
        }else{
            sb.append("TTH:").append(total_hits).append('\n');
        }
        
        // PT:24d2h7m ACC:98.16%
        sb.append("PT:").append(map_length/86400).append("d").append((map_length%86400)/3600).append("h").append((map_length%3600)/60).append("m").append(' ').append("ACC:").append(hit_accuracy).append('%').append('\n');
        
        // ♡:320 kds:245 SVIP2
        // SS:26(107) S:157(844) A:1083
        // 
        // uid:7003013
        // occupation:xxx discord:xxx interests:xxx

        //  "username"("country_code"): "mode" ("key"K)-if needed
        if ("mania".equals(mode)){
            map_hard = map_hard.replaceAll("^\\[\\d{1,2}K\\]\\s*","");
            sb.append(name).append('(').append(country).append(')').append(':').append(mode).append(' ')
                    .append('(').append(key).append("K").append(')').append('\n');
        }else {
            sb.append(name).append('(').append(country).append(')').append(':').append(mode).append('\n');
        }

        //  "artist_unicode" - "title_unicode" ["version"]
        sb.append(artist).append(" - ").append(map_name).append(' ').append('[').append(map_hard).append(']').append('\n');

        //  ★★★★★ "difficulty_rating"* mm:ss
        sb.append(star).append(' ').append(format(difficulty)).append('*').append(' ')
                .append(map_length/60).append(':').append(String.format("%02d",map_length%60)).append('\n');

        //  ["rank"] +"mods" "score" ("accuracy"%)
        sb.append('[').append(rank).append(']').append(' ');
        for (String mod : mods) {
            sb.append(mod).append(' ');
        }
        sb.append(String.valueOf(score).replaceAll("(?<=\\d)(?=(?:\\d{4})+$)","\'")).append(' ').append('(').append(format(acc)).append('%').append(')').append('\n');

        //  "pp"(###)PP  "max_combo"/###x
        sb.append(format(pp)).append("(0)PP  ").append(combo).append("/0x").append('\n');

        //   "count_300" /  "count_100" / "count_50" / "count_miss"
        switch (mode){
            default:
            case "osu":{
                sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ").append(n_0).append('\n').append('\n');
            }break;
            case "taiko":{
                sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_0).append('\n').append('\n');
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

        //DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(play_time) 格式化 ISO-8601 日期格式

        sb.append("bid:").append(bid);
        return sb.toString();
    }
    static String format(double d){
        double x = Math.round(d*100)/100D;
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(x);
    }
}
