package com.now.nowbot.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

//@Entity
//@Table(catalog = "qq_id")
public class QQID {
    @Id
    Long id;

    Long PermissionId;
    Long QQ;
}
