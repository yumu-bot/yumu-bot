package com.now.nowbot.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "start")
public class StartLite {
    @Id
    int qq;

    double start;
    LocalDateTime lastTime;
}
