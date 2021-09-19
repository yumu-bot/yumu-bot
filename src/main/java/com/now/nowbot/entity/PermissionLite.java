package com.now.nowbot.entity;

import com.now.nowbot.config.Permission;

import javax.persistence.*;

@Entity
@Table(name = "permission")//主要是可能会有其他消息的记录,先设定表名为qq_message
public class PermissionLite {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    String service;
    Permission.TYPE type;

}
