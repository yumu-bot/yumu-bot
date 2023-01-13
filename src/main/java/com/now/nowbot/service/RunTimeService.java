package com.now.nowbot.service;


import com.now.nowbot.dao.BindDao;
import com.now.nowbot.service.MessageService.BindService;
import net.mamoe.mirai.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;

/***
 * 统一设置定时任务
 */
@Service
public class RunTimeService {
    private static final Logger log = LoggerFactory.getLogger(RunTimeService.class);
    @Autowired
    Bot bot;
    @Autowired
    BiliApiService biliApiService;
    @Autowired
    BindDao bindDao;
    //@Scheduled(cron = "0(秒) 0(分) 0(时) *(日) *(周) *(月)")  '/'步进

    /*
    @Scheduled(cron = "14 * * * * *")
    public void happynewyear(){
        biliApiService.check();
    }

    */
//    @Scheduled(cron = "2 0 0-4,20-23 * * *")
    public void say(){
        var t = bot.getGroup(582121443);
        if (t != null){
            t.sendMessage("现在是北京时间 23:00:00");
        }
    }



    /***
     * 每分钟清理未绑定的
     */
    @Scheduled(cron = "0 0/5 * * * *")
    public void clearBindMsg(){
        BindService.BIND_MSG_MAP.keySet().removeIf(k -> (k + 120 * 1000) < System.currentTimeMillis());
        log.info("清理绑定器执行 当前剩余:{}", BindService.BIND_MSG_MAP.size());
    }

    /***
     * 白天输出内存占用信息
     */
//    @Scheduled(cron = "0 0/30 8-18 * * *")
    public void alive(){
        var m = ManagementFactory.getMemoryMXBean();
        var nm = m.getNonHeapMemoryUsage();
        var t = ManagementFactory.getThreadMXBean();
        var z = ManagementFactory.getMemoryPoolMXBeans();
        log.info("方法区 已申请 {}M 已使用 {}M ",
                nm.getCommitted()/1024/1024,
                nm.getUsed()/1024/1024
        );
        log.info("堆内存上限{}M,当前内存占用{}M, 已使用{}M\n当前线程数 {} ,守护线程 {} ,峰值线程 {}",
                m.getHeapMemoryUsage().getMax()/1024/1024,
                m.getHeapMemoryUsage().getCommitted()/1024/1024,
                m.getHeapMemoryUsage().getUsed()/1024/1024,
                t.getThreadCount(),
                t.getDaemonThreadCount(),
                t.getPeakThreadCount()
        );
        for (var pool : z){
            log.info("vm内存 {} 已申请 {}M 已使用 {}M ",
                    pool.getName(),
                    pool.getUsage().getCommitted()/1024/1024,
                    pool.getUsage().getUsed()/1024/1024
            );
        }
    }
}
