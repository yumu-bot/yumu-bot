package com.now.nowbot.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "account")
public class UserAccountLite {
    @Id
    private Long uid;
    private String username;
    private String akV1;
    private String akV2;
    private String password;
    @Column(length = 1000)
    private String session;
    private Integer timeZone;


    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAkV1() {
        return akV1;
    }

    public void setAkV1(String akV1) {
        this.akV1 = akV1;
    }

    public String getAkV2() {
        return akV2;
    }

    public void setAkV2(String akV2) {
        this.akV2 = akV2;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public Integer getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(Integer timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAccountLite)) return false;
        UserAccountLite that = (UserAccountLite) o;
        return Objects.equals(uid, that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }
}