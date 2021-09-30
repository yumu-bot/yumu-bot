package com.now.nowbot.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class QQUserLite {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "osu_id")
    private Long osuId;
    @Column(name = "osu_name")
    private String osuName;
    private Long qq;
    @Column(name = "access_token")
    private String accessToken;
    @Column(name = "refresh_token")
    private String refreshToken;
    private Long time;

    //一些额外信息
    //创号时间
    @Column(name = "join_date")
    private LocalDateTime joinDate;
    //主模式
    @Column(name = "play_mode")
    private String playMode;
}
