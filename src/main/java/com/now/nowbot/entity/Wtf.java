package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(name = "no_use")
public class Wtf {
    public Wtf(){}
    public Wtf(String text){
        this.text = text;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @Column(length = 1000)
    String text;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
