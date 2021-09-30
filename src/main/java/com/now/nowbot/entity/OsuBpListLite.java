package com.now.nowbot.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class OsuBpListLite {
    @Id
    Integer id;
    Integer OsuId;
}
