package com.now.nowbot.service;


import com.now.nowbot.dao.BindDao;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.management.ManagementFactory;

/***
 * 统一设置定时任务
 */
@Service
public class RunTimeService implements SchedulingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(RunTimeService.class);

    @Resource
    BiliApiService biliApiService;
    @Resource
    BindDao bindDao;
    @Resource
    RestTemplate restTemplate;
    @Resource
    OsuUserApiService userApiService;
    @Resource
    TaskExecutor taskExecutor;

    @Resource
    ApplicationContext applicationContext;

    ScheduledTaskRegistrar scheduledTaskRegistrar;

    //@Scheduled(cron = "0(秒) 0(分) 0(时) *(日) *(月) *(周) *(年,可选)")  '/'步进


    @Scheduled(cron = "0 0 6 * * *")
    public void refreshToken() {
        log.info("开始执行更新令牌任务");
        // bindDao.refreshOldUserToken(userApiService);
    }


    public void sayBp1() {
        /*
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

         */
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
        log.debug("方法区 已申请 {}M 已使用 {}M ",
                nm.getCommitted() / 1024 / 1024,
                nm.getUsed() / 1024 / 1024
        );
        log.debug("堆内存上限{}M,当前内存占用{}M, 已使用{}M\n当前线程数 {} ,守护线程 {} ,峰值线程 {}",
                m.getHeapMemoryUsage().getMax() / 1024 / 1024,
                m.getHeapMemoryUsage().getCommitted() / 1024 / 1024,
                m.getHeapMemoryUsage().getUsed() / 1024 / 1024,
                t.getThreadCount(),
                t.getDaemonThreadCount(),
                t.getPeakThreadCount()
        );
        for (var pool : z) {
            log.debug("vm内存 {} 已申请 {}M 已使用 {}M ",
                    pool.getName(),
                    pool.getUsage().getCommitted() / 1024 / 1024,
                    pool.getUsage().getUsed() / 1024 / 1024
            );
        }
    }



    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.scheduledTaskRegistrar = taskRegistrar;
    }

    public void addTask(Runnable task, String cron) {
        scheduledTaskRegistrar.addCronTask(() -> taskExecutor.execute(task), cron);
    }

    /*

     public void example() {
        try {
            var code = """
                    jakarta.persistence.Query q = manager.createNativeQuery("$sql");
                    Object r = q.getResultList();
                    String data = com.now.nowbot.util.JacksonUtil.objectToJsonPretty(r);
                    System.out.println(data);
                    System.out.println(r.getClass().getSimpleName());
                    """.replace("$sql", "select version();");
            Map<Class, String> arg = Map.of(Class.forName("jakarta.persistence.EntityManager"), "manager");
            executeCode(code, arg);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public void executeCode(String code, Map<Class, String> autowrite) {
        var sc = new ScriptEvaluator();
        List<Object> args = new ArrayList<>(autowrite.size());
        for (var entry : autowrite.entrySet()) {
            try {
                var bean = applicationContext.getBean(entry.getKey());
                args.add(bean);
            } catch (BeansException e) {
                log.error("获取 [{}] 类型的bean出错", entry.getKey().getSimpleName());
                args.add(null);
            }
        }
        sc.setParameters(autowrite.values().toArray(new String[0]), autowrite.keySet().toArray(new Class[0]));
        try {
            sc.cook(code);
            sc.evaluate(args.toArray(new Object[0]));
        } catch (CompileException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    */
}
