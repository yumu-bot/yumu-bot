package com.now.nowbot.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table("qq_id")
public class QQID {
    @Id
    Long Permission_id;
    Long QQ_id;
}
