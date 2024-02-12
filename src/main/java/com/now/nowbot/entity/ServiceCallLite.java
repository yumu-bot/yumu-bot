package com.now.nowbot.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_call")
public class ServiceCallLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String service;
    Long   time;

    @Column(name = "ctime")
    LocalDateTime createTime;

    public ServiceCallLite() {
        createTime = LocalDateTime.now();
    }

    public ServiceCallLite(String service, Long time) {
        this.service = service;
        this.time = time;
        createTime = LocalDateTime.now();
    }


    public interface ServiceCallResult {
        String getService();

        Integer getSize();

        Double getAvgTime();

        Long getMinTime();

        Long getMaxTime();
    }


    public interface ServiceCallResult$80 {
        String getService();

        Long getData();
    }
}
