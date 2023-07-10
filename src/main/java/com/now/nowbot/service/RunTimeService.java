package com.now.nowbot.service;


import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.MessageService.BindService;
import com.now.nowbot.throwable.ServiceException.BindException;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Resource
    OsuGetService osuGetService;

    //@Scheduled(cron = "0(秒) 0(分) 0(时) *(日) *(月) *(周) *(年,可选)")  '/'步进

    /*
    @Scheduled(cron = "14 * * * * *")
    public void happynewyear(){
        biliApiService.check();
    }

    */
//    @Scheduled(cron = "16 0 0 * * *")
    public void checkBPlist() throws BindException {
        var qq = 3054_7298_60L;
        var u = bindDao.getUser(qq);
        LocalDateTime dayBefore = LocalDateTime.now().plusDays(-1);
        var bpList = osuGetService.getBestPerformance(u, OsuMode.OSU, 0,100);
        var dayBpList = bpList.stream().filter(e -> dayBefore.isBefore(e.getCreateTime())).toList();
        var group = bot.getGroup(9289_3625_5);
        if (dayBpList.size() > 0) {
            group.sendMessage(new At(365246692).plus(new At(3054729860L)).plus("今天刷到了bp数: " + dayBpList.size()));
        } else {
            group.sendMessage(new At(365246692).plus(new At(3054729860L)).plus("今天没刷到pp,杀!"));
        }
    }

    public void sayBp1() {
        var group = bot.getGroup(928936255);
        var devGroup = bot.getGroup(746671531);
        ContactList<NormalMember> users = null;
        if (group != null && devGroup != null) {
            users = group.getMembers();
            devGroup.sendMessage("开始统计进阶群 bp1");
        }
        if (users == null || users.size() == 0) {
            log.warn("no users");
            return;
        }
        record QQUser(long qq, String name){};
        var qqUserList = users.stream().map(e -> new QQUser(e.getId(),e.getNameCard())).toList();
        record UserLog(long qq, String name, float pp) {
        }
        var dataMap = new ArrayList<UserLog>();
        var p = Pattern.compile("\"pp\":(?<pp>\\d+(.\\d+)?),");
        var name = Pattern.compile("^(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))");
        for (var qqUser : qqUserList) {
            log.warn("获取qq[{}]信息", qqUser);
            try {
                BinUser u = null;
                try {
                    u = bindDao.getUser(qqUser.qq);
                } catch (BindException e) {
                    var m = name.matcher(qqUser.name);
                    if (m.find() && !m.group("name").trim().equals("")){
                        var nu = osuGetService.getPlayerInfo(m.group("name").trim());
                        u = new BinUser();
                        u.setOsuID(nu.getId());
                        u.setOsuName(nu.getUsername());
                    } else {
                        throw e;
                    }
                }
                var url = String.format("https://osu.ppy.sh/users/%dl/scores/best?mode=osu&limit=1&offset=0", u.getOsuID());
                var data = restTemplate.getForObject(url, String.class);
                var m = p.matcher(data);
                if (m.find()) {
                    dataMap.add(new UserLog(qqUser.qq, u.getOsuName(), Float.parseFloat(m.group("pp"))));
                }
                log.warn("结束,[{}, {}, {}]", qqUser, u.getOsuName(), m.group("pp"));
                Thread.sleep(((Double) (Math.random() * 10000 + 10000)).longValue());
            } catch (Exception e) {
                if (e instanceof BindException) {
                    dataMap.add(new UserLog(qqUser.qq, "未绑定", 0));
                } else if (e instanceof NumberFormatException) {
                    dataMap.add(new UserLog(qqUser.qq, "PP读取错误", 0));
                } else if (e instanceof NullPointerException nullerr) {
                    dataMap.add(new UserLog(qqUser.qq, "未知错误,详见日志:query#" + qqUser, 0));
                    log.error("错误日志: query#{}", qqUser, nullerr);
                } else {
                    dataMap.add(new UserLog(qqUser.qq, "未知错误,详见日志:query#" + qqUser, 0));
                    log.error("错误日志: query#{}", qqUser, e);
                }
            }
        }
        var n = dataMap.stream().sorted(Comparator.comparing(UserLog::pp).reversed()).toList();
        var dataFormat = DateTimeFormatter.ofPattern("MM-dd");
        var sb = new StringBuilder("QQ,name,pp\n");
        log.warn("处理结果中");
        for (var u : n) {
            sb.append(u.qq).append(',')
                    .append(u.name).append(',')
                    .append(u.pp).append('\n');
        }
        log.warn("完成: {}", sb);
        devGroup.getFiles().uploadNewFile(LocalDate.now().format(dataFormat) + ".csv", ExternalResource.create(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    /***
     * 每分钟清理未绑定的
     */
    @Scheduled(cron = "0 0/5 * * * *")
    public void clearBindMsg() {
        BindService.BIND_MSG_MAP.keySet().removeIf(k -> (k + 120 * 1000) < System.currentTimeMillis());
        log.info("清理绑定器执行 当前剩余:{}", BindService.BIND_MSG_MAP.size());
    }

    /***
     * 白天输出内存占用信息
     */
    @Scheduled(cron = "0 0/30 8-18 * * *")
    public void alive() {
        var m = ManagementFactory.getMemoryMXBean();
        var nm = m.getNonHeapMemoryUsage();
        var t = ManagementFactory.getThreadMXBean();
        var z = ManagementFactory.getMemoryPoolMXBeans();
        log.info("方法区 已申请 {}M 已使用 {}M ",
                nm.getCommitted() / 1024 / 1024,
                nm.getUsed() / 1024 / 1024
        );
        log.info("堆内存上限{}M,当前内存占用{}M, 已使用{}M\n当前线程数 {} ,守护线程 {} ,峰值线程 {}",
                m.getHeapMemoryUsage().getMax() / 1024 / 1024,
                m.getHeapMemoryUsage().getCommitted() / 1024 / 1024,
                m.getHeapMemoryUsage().getUsed() / 1024 / 1024,
                t.getThreadCount(),
                t.getDaemonThreadCount(),
                t.getPeakThreadCount()
        );
        for (var pool : z) {
            log.info("vm内存 {} 已申请 {}M 已使用 {}M ",
                    pool.getName(),
                    pool.getUsage().getCommitted() / 1024 / 1024,
                    pool.getUsage().getUsed() / 1024 / 1024
            );
        }
    }
}
