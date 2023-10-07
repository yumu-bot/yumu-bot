package com.now.nowbot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "note")
public class NoteLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;



    @Column(name = "key", columnDefinition = "TEXT")
    private String index;

    @Column(name = "info", columnDefinition = "TEXT")
    private String info;

    @Column(name = "corn", columnDefinition = "TEXT")
    String corn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}