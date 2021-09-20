package com.now.nowbot.entity;

import com.now.nowbot.config.Permission;

import javax.persistence.*;

@Entity
@Table(name = "permission")
public class PermissionLite {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    //QQ/Group号
    private Long number;
    private String service;
    @Enumerated(EnumType.STRING)
    private Permission.TYPE type;

    public PermissionLite(Long number, String service, Permission.TYPE type) {
        this.number = number;
        this.service = service;
        this.type = type;
    }

    public PermissionLite() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Permission.TYPE getType() {
        return type;
    }

    public void setType(Permission.TYPE type) {
        this.type = type;
    }
}
