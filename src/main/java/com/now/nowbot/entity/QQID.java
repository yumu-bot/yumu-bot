package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(catalog = "qq_id")
public class QQID {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    //true group   |    false friend
    @Column(name = "type")
    boolean isGroup;
    Long PermissionId;
    Long QQ;
}
