package com.now.nowbot.service;


import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@Service
public class RunTimeService {
    private static final Logger log = LoggerFactory.getLogger(RunTimeService.class);
    @Autowired
    Bot bot;
    @Scheduled(cron = "0 0 0 * * *")
    public void sleep(){
        bot.getGroups().forEach(group -> {
            var gs = group.getMembers();
            try {
                var n = gs.getOrFail(1529813731L);
                group.sendMessage(new At(n.getId()).plus("快去睡啦"));
            } catch (Exception e) {
            }
        });
    }
    @Scheduled(cron = "0 0/30 8-18 * * *")
    public void alive(){
        var m = ManagementFactory.getMemoryMXBean();
        log.info("堆内存上限{}M，当前内存占用{}M, 已使用{}M",
                m.getHeapMemoryUsage().getMax()/1024/1024,
                m.getHeapMemoryUsage().getCommitted()/1024/1024,
                m.getHeapMemoryUsage().getUsed()/1024/1024);
    }
}
