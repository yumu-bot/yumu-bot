package com.now.nowbot.entity;

import org.springframework.context.annotation.Primary;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class ScoreLite {
    @Id
    Long QQid;

}
