package com.now.nowbot.entity;

import com.now.nowbot.model.enums.OsuMode;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "osu_user_mode_score", indexes = {
        @Index(name = "osu_id", columnList = "osu_id"),
        @Index(name = "usermod_find", columnList = "osu_id,mode")
})
public class OsuUserModeScoreLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "osu_id")
    private Integer osuID;
    private OsuMode mode;

    private LocalDateTime time;

    // ',' 分割
    //@Lob
    @Column(columnDefinition = "TEXT")
    private String rank_history;
    //等级
    private Integer level_current;
    private Integer level_progress;
    //rank
    private Long global_rank;
    private Long country_rank;
    private Double pp;
    private Double hit_accuracy;

    private Long play_count;
    private Long play_time;

    private Long ranked_score;
    private Long total_score;

    private Integer maximum_combo;
    private Boolean is_ranked;

    private Integer grade_counts_ss;
    private Integer grade_counts_ssh;
    private Integer grade_counts_s;
    private Integer grade_counts_sh;
    private Integer grade_counts_a;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOsuID() {
        return osuID;
    }

    public void setOsuID(Integer osuID) {
        this.osuID = osuID;
    }

    public OsuMode getMode() {
        return mode;
    }

    public void setMode(OsuMode mode) {
        this.mode = mode;
    }

    public String getRank_history() {
        return rank_history;
    }

    public void setRank_history(String rank_history) {
        this.rank_history = rank_history;
    }

    public Integer getLevel_current() {
        return level_current;
    }

    public void setLevel_current(Integer level_current) {
        this.level_current = level_current;
    }

    public Integer getLevel_progress() {
        return level_progress;
    }

    public void setLevel_progress(Integer level_progress) {
        this.level_progress = level_progress;
    }

    public Long getGlobal_rank() {
        return global_rank;
    }

    public void setGlobal_rank(Long global_rank) {
        this.global_rank = global_rank;
    }

    public Long getCountry_rank() {
        return country_rank;
    }

    public void setCountry_rank(Long country_rank) {
        this.country_rank = country_rank;
    }

    public Double getPP() {
        return pp;
    }

    public void setPP(Double pp) {
        this.pp = pp;
    }

    public Double getHit_accuracy() {
        return hit_accuracy;
    }

    public void setHit_accuracy(Double hit_accuracy) {
        this.hit_accuracy = hit_accuracy;
    }

    public Long getPlay_count() {
        return play_count;
    }

    public void setPlay_count(Long play_count) {
        this.play_count = play_count;
    }

    public Long getPlay_time() {
        return play_time;
    }

    public void setPlay_time(Long play_time) {
        this.play_time = play_time;
    }

    public Long getRanked_score() {
        return ranked_score;
    }

    public void setRanked_score(Long ranked_score) {
        this.ranked_score = ranked_score;
    }

    public Long getTotal_score() {
        return total_score;
    }

    public void setTotal_score(Long total_score) {
        this.total_score = total_score;
    }

    public Integer getMaximum_combo() {
        return maximum_combo;
    }

    public void setMaximum_combo(Integer maximum_combo) {
        this.maximum_combo = maximum_combo;
    }

    public Boolean getIs_ranked() {
        return is_ranked;
    }

    public void setIs_ranked(Boolean is_ranked) {
        this.is_ranked = is_ranked;
    }

    public Integer getGrade_counts_ss() {
        return grade_counts_ss;
    }

    public void setGrade_counts_ss(Integer grade_counts_ss) {
        this.grade_counts_ss = grade_counts_ss;
    }

    public Integer getGrade_counts_ssh() {
        return grade_counts_ssh;
    }

    public void setGrade_counts_ssh(Integer grade_counts_ssh) {
        this.grade_counts_ssh = grade_counts_ssh;
    }

    public Integer getGrade_counts_s() {
        return grade_counts_s;
    }

    public void setGrade_counts_s(Integer grade_counts_s) {
        this.grade_counts_s = grade_counts_s;
    }

    public Integer getGrade_counts_sh() {
        return grade_counts_sh;
    }

    public void setGrade_counts_sh(Integer grade_counts_sh) {
        this.grade_counts_sh = grade_counts_sh;
    }

    public Integer getGrade_counts_a() {
        return grade_counts_a;
    }

    public void setGrade_counts_a(Integer grade_counts_a) {
        this.grade_counts_a = grade_counts_a;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
