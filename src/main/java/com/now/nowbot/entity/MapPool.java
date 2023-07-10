package com.now.nowbot.entity;

import org.hibernate.annotations.Type;

import jakarta.persistence.*;

@Entity
@Table(name = "map_pool")
public class MapPool {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column()
    Integer fatherId;

    //@Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String info;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFatherId() {
        return fatherId;
    }

    public void setFatherId(Integer fatherId) {
        this.fatherId = fatherId;
    }
}