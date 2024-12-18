package com.now.nowbot.service

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.NewbiePlayCount
import com.now.nowbot.mapper.NewbiePlayCountRepository
import com.now.nowbot.newbie.mapper.NewbieService
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import jakarta.annotation.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.time.LocalDate
import kotlin.io.path.Path


/***
 * 统一设置定时任务
 */
@Service
class RunTimeService : SchedulingConfigurer {
    @Resource
    var biliApiService: BiliApiService? = null

    @Resource
    var bindDao: BindDao? = null

    @Resource
    var maimaiApiService: MaimaiApiService? = null

    @Resource
    var chunithmApiService: ChunithmApiService ? = null

    @Resource
    var userApiService: OsuUserApiService? = null

    @Resource
    var taskExecutor: TaskExecutor? = null


    @Resource
    var applicationContext: ApplicationContext? = null

    var scheduledTaskRegistrar: ScheduledTaskRegistrar? = null


    //@Scheduled(cron = "0(秒) 0(分) 0(时) *(日) *(月) *(周) *(年,可选)")  '/'步进
    @Scheduled(cron = "0 0 4 * * *")
    fun refreshToken() {
        log.info("开始执行更新令牌任务")
        bindDao!!.refreshOldUserToken(userApiService)
    }

    @Scheduled(cron = "0 0 6 * * *")
    fun updateMaimaiSongLibrary() {
        log.info("开始执行更新 maimai 歌曲库任务")
        //maimaiApiService!!.updateMaimaiSongLibraryFile()
        maimaiApiService!!.updateMaimaiSongLibraryDatabase()
    }

    @Scheduled(cron = "20 0 6 * * *")
    fun updateMaimaiRankLibrary() {
        log.info("开始执行更新 maimai 排名库任务")
        //maimaiApiService!!.updateMaimaiRankLibraryFile()
        maimaiApiService!!.updateMaimaiRankLibraryDatabase()
    }

    @Scheduled(cron = "40 0 6 * * *")
    fun updateMaimaiFitLibrary() {
        log.info("开始执行更新 maimai 统计库任务")
        //maimaiApiService!!.updateMaimaiFitLibraryFile()
        maimaiApiService!!.updateMaimaiFitLibraryDatabase()
    }

    @Scheduled(cron = "0 1 6 * * *")
    fun updateMaimaiAliasLibrary() {
        log.info("开始执行更新 maimai 外号库任务")
        //maimaiApiService!!.updateMaimaiAliasLibraryFile()
        maimaiApiService!!.updateMaimaiAliasLibraryDatabase()
    }

    @Scheduled(cron = "20 1 6 * * *")
    fun updateChunithmSongsLibrary() {
        log.info("开始执行更新 chunithm 歌曲库任务")
        //maimaiApiService!!.updateMaimaiAliasLibraryFile()
        chunithmApiService!!.updateChunithmSongLibraryFile()
    }


    @Scheduled(cron = "0 5 10 1 9 *")
    fun count() {
        try {
            val service = applicationContext!!.getBean(NewbieService::class.java)
            val startDay = LocalDate.of(2024, 8, 12).atStartOfDay()
            val write = Files.newBufferedWriter(Path("/home/spring/res2.csv"))
            write.use {
                service.updateUserPP(
                    startDay,
                    LocalDate.of(2024, 9, 1).atStartOfDay(),
                    LocalDate.of(2024, 8, 30),
                    write
                )
            }
            log.info("统计完成")
        } catch (e: Exception) {
            log.error("统计出现异常", e)
        }
    }

    fun newbiePlayCount() {
        log.info("开始执行新人统计任务")
        val newbiePlayCount: NewbiePlayCountRepository
        val newbieService = try {
            newbiePlayCount = applicationContext!!.getBean(NewbiePlayCountRepository::class.java)
            applicationContext!!.getBean(NewbieService::class.java)
        } catch (e: Exception) {
            log.warn("未找到统计服务, 结束任务", e)
            return
        }
        val bot: com.mikuac.shiro.core.Bot?
        val users = try {
            val botContainer = applicationContext!!.getBean(BotContainer::class.java)
            bot = botContainer.robots[365246692] ?: botContainer.robots.values.find {
                it.groupList.data.find { g -> g.groupId == 595985887L } != null
            }
            bot?.getGroupMemberList(595985887L)?.data?.map { it.userId }
        } catch (e: Exception) {
            log.warn("未找到主bot机器人, 结束任务", e)
            return
        }
        if (users == null) {
            log.info("未找到目标群, 结束任务")
            return
        }


        val startDay = LocalDate.of(2024, 8, 12).atStartOfDay()
        val today = LocalDate.now().atStartOfDay()

        newbieService
            .countDataByQQ(users, startDay, today) {
                val user = NewbiePlayCount(it)
                newbiePlayCount.saveAndFlush(user)

                log.info("统计 ${it.id} 完成")
            }


        log.info("任务结束")
    }

    fun sayBp1() {
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
        record QQUser(long qq, String data){};
        var qqUserList = users.stream().map(e -> new QQUser(e.getId(),e.getNameCard())).toList();
        record UserLog(long qq, String data, float pp) {
        }
        var dataMap = new ArrayList<UserLog>();
        var p = Pattern.compile("\"pp\":(?<pp>\\d+(.\\d+)?),");
        var data = Pattern.compile("^(\\s+(?<data>[0-9a-zA-Z\\[\\]\\-_ ]*))");
        for (var qqUser : qqUserList) {
            log.warn("获取qq[{}]信息", qqUser);
            try {
                BinUser u = null;
                try {
                    u = bindDao.getUser(qqUser.qq);
                } catch (BindException e) {
                    var m = data.matcher(qqUser.data);
                    if (m.find() && !m.group("data").trim().equals("")){
                        var nu = osuGetService.getPlayerInfo(m.group("data").trim());
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
        var sb = new StringBuilder("QQ,data,pp\n");
        log.warn("处理结果中");
        for (var u : n) {
            sb.append(u.qq).append(',')
                    .append(u.data).append(',')
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
    fun alive() {
        val m = ManagementFactory.getMemoryMXBean()
        val nm = m.nonHeapMemoryUsage
        val t = ManagementFactory.getThreadMXBean()
        val z = ManagementFactory.getMemoryPoolMXBeans()
        log.debug(
            "方法区 已申请 {}M 已使用 {}M ",
            nm.committed / 1024 / 1024,
            nm.used / 1024 / 1024
        )
        log.debug(
            "堆内存上限{}M,当前内存占用{}M, 已使用{}M\n当前线程数 {} ,守护线程 {} ,峰值线程 {}",
            m.heapMemoryUsage.max / 1024 / 1024,
            m.heapMemoryUsage.committed / 1024 / 1024,
            m.heapMemoryUsage.used / 1024 / 1024,
            t.threadCount,
            t.daemonThreadCount,
            t.peakThreadCount
        )
        for (pool in z) {
            log.debug(
                "vm内存 {} 已申请 {}M 已使用 {}M ",
                pool.name,
                pool.usage.committed / 1024 / 1024,
                pool.usage.used / 1024 / 1024
            )
        }
    }


    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        this.scheduledTaskRegistrar = taskRegistrar
    }

    fun addTask(task: Runnable?, cron: String?) {
        scheduledTaskRegistrar!!.addCronTask({ taskExecutor!!.execute(task!!) }, cron!!)
    } /*

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


    companion object {
        private val log: Logger = LoggerFactory.getLogger(RunTimeService::class.java)

        @JvmStatic
        @Deprecated("建议替换", replaceWith = ReplaceWith("testNew(s)"))
        fun test(s: String) {
        }

        @JvmStatic
        fun testNew(s: String) {
        }
    }
}
