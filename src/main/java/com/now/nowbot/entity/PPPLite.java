package com.now.nowbot.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

//@Entity
//@Table(name = "pp_plus")
public class PPPLite {
    @Id
    @GeneratedValue
    Long id;

    Long userId;
    //记录时间
    private LocalDateTime date;

    double Total;

    double Junp;

    double Flow;

    double Acc;

    double Sta;

    double Spd;

    double Pre;
}
