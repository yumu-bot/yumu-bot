package com.now.nowbot.service;


import com.now.nowbot.dao.BindDao;
import com.now.nowbot.service.MessageService.BindService;
import com.now.nowbot.throwable.ServiceException.BindException;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.utils.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;

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
    @Resource
    RestTemplate restTemplate;

    //@Scheduled(cron = "0(秒) 0(分) 0(时) *(日) *(周) *(月)")  '/'步进

    /*
    @Scheduled(cron = "14 * * * * *")
    public void happynewyear(){
        biliApiService.check();
    }

    */
    @Scheduled(cron = "2 8 4 * * *")
    public void sayBp1(){
        var t = bot.getGroup(928936255);
        ContactList<NormalMember> users = null;
        if (t != null) {
            users = t.getMembers();
            t.sendMessage("开始统计进阶群 bp1");
        }
        if (users == null){
            return;
        }
        var idList = users.stream().map(NormalMember::getId).toList();
        record User(long qq, String name, float pp){}
        var dataMap = new ArrayList<User>();
        var p = Pattern.compile("\"pp\":(?<pp>\\d+(.\\d+)?),");
        for(var qq : idList){
            try {
                var u = bindDao.getUser(qq);
                var url = String.format("https://osu.ppy.sh/users/%dl/scores/best?mode=osu&limit=1&offset=0", u.getOsuID());
                var data = restTemplate.getForObject(url, String.class);
                var m = p.matcher(data);
                if (m.find()){
                    dataMap.add(new User(qq, u.getOsuName(), Float.parseFloat(m.group("pp"))));
                }
                Thread.sleep(((Double)(Math.random() * 500)).longValue());
            } catch (Exception e) {
                switch (e){
                    case BindException ignore-> dataMap.add(new User(qq, "未绑定", 0));
                    case NumberFormatException ignore -> dataMap.add(new User(qq, "PP读取错误", 0));
                    case NullPointerException nullerr -> {
                        dataMap.add(new User(qq, "未知错误,详见日志:query#"+qq, 0));
                        log.error("错误日志: query#{}", qq, nullerr);
                    }
                    default -> {
                        dataMap.add(new User(qq, "未知错误,详见日志:query#"+qq, 0));
                        log.error("错误日志: query#{}", qq, e);
                    }
                }
            }
        }
        var n = dataMap.stream().sorted(Comparator.comparing(User::pp).reversed()).toList();
        var dataFormat = DateTimeFormatter.ofPattern("MM-dd");
        var sb = new StringBuilder("qq,name,pp\n");
        for (var u : n){
            sb.append(u.qq).append(',')
                    .append(u.name).append(',')
                    .append(u.pp).append('\n');
        }
        t.getFiles().uploadNewFile(LocalDate.now().format(dataFormat) + ".csv", ExternalResource.create(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    @Scheduled(cron = "0 50 12 21 * 5")
    void testA(){
        sayBp1();
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
    @Scheduled(cron = "0 0/30 8-18 * * *")
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
