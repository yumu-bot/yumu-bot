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
    if (support_level=null) support_level=0;

    }
    public static Ymi getInstance(JSONObject date){
        return new Ymi(date);
    }
    public String getOut(){
        StringBuilder sb = new StringBuilder();
        // Muziyami(standard):10086PP
        sb.append(name).append('(').append(mode).append(')').append(':').append(pp).append(' ').append("PP").append('\n');

        // #114514 CN#1919 (LV.100(32%))
        sb.append('#').append(global_rank).append(' ').append(country).append('#').append(country_rank).append(' ').append("(LV.").append(level_current).append('(').append(level_progress).append("%))").append('\n');

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
        sb.append("♡:").append(follower_count).append(' ').append("kds:").append(kudosu_total).append(' ');
        if (support_level!=0){
        sb.append("SVIP").append(support_level).append('\n');
        }else{
            sb.append('\n');
        }

        // SS:26(107) S:157(844) A:1083
        sb.append("SS:").append(grade_count_ss).append('(').append(grade_count_ssh).append(')').append(' ').append("S:").append(grade_count_s).append('(').append(grade_count_sh).append(' ').append("A:").append(grade_count_a).append('\n').append('\n');
        // 
        // uid:7003013
        sb.append("uid:").append(uid);
        // occupation:xxx discord:xxx interests:xxx
        }
        //DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(play_time)
    }
