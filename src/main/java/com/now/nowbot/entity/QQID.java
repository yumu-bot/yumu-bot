package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(name = "qq_id")
public class QQID {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //true group   |    false friend
    @Column(name = "type")
    private Boolean isGroup;
    private Long permissionId;
    private Long QQ;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getGroup() {
        return isGroup;
    }

    public void setGroup(Boolean group) {
        isGroup = group;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        permissionId = permissionId;
    }

    public Long getQQ() {
        return QQ;
    }

    public void setQQ(Long QQ) {
        this.QQ = QQ;
    }
}
