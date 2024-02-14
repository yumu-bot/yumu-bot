package com.now.nowbot.entity;

import com.now.nowbot.model.DrawConfig;
import com.now.nowbot.model.enums.DrawGrade;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_draw")
public class DrawLogLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    private DrawGrade grade;
    private String card;
    private Long uid;
    @Column(name = "create_at")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DrawGrade getGrade() {
        return grade;
    }

    public void setGrade(DrawGrade grade) {
        this.grade = grade;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }
    public DrawLogLite(){}
    public DrawLogLite(DrawConfig.Card card, DrawGrade grade, long uid){
        this.card = card.name();
        this.grade = grade;
        this.uid = uid;
    }
}