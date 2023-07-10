package com.now.nowbot.entity;

import com.now.nowbot.config.PermissionType;

import jakarta.persistence.*;

@Entity
@Table(name = "permission")
public class PermissionLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //service名为 PERMISSION_ALL 则是全局名单,所有的服务都要经过此权限表
    private String service;
    @Enumerated(EnumType.STRING)
    private PermissionType type;

    public PermissionLite(String service, PermissionType type) {
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

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public PermissionType getType() {
        return type;
    }

    public void setType(PermissionType type) {
        this.type = type;
    }
}
