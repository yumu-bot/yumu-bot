package com.now.nowbot.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ServiceSwitchLite {
    @Id
    @Column(name = "service", nullable = false, length = 40)
    private String Service;
    boolean Switch;

    public ServiceSwitchLite() {
    }

    public ServiceSwitchLite(String service, boolean aSwitch) {
        Service = service;
        Switch = aSwitch;
    }

    public String getService() {
        return Service;
    }

    public void setService(String service) {
        this.Service = service;
    }

    public boolean isSwitch() {
        return Switch;
    }

    public void setSwitch(boolean aSwitch) {
        Switch = aSwitch;
    }
}
