package com.now.nowbot.service

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.lxnsApiService.LxMaiApiService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory

/***
 * 统一设置定时任务
 */
@Service
class RunTimeService(
    private val dailyStatisticsService: DailyStatisticsService,
    private val bindDao: BindDao,
    private val newbieService: NewbieService,
    private val maimaiApiService: MaimaiApiService,
    private val chunithmApiService: ChunithmApiService,
    private val lxMaiApiService: LxMaiApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    @param:Qualifier("kotlinTaskExecutor")
    private val taskExecutor: TaskExecutor,
    private val botContainer: BotContainer,
    private val newbieConfig: NewbieConfig
) : SchedulingConfigurer {
    //@Scheduled(cron = "0(秒) 0(分) 0(时) *(日) *(月) *(周) *(年,可选)")  '/'步进

    // 每 30 秒刷新一次 token
    @Scheduled(cron = "0,30 * 0-1,6-23 * * *")
    fun refreshToken() {
        bindDao.refreshOldUserToken(userApiService)
    }

    // 每几小时更新一次过期的谱面
    @Scheduled(cron = "15 0,30 0-1,6-23 * * *")
    fun updateBeatmapExtendFailTimes() {
        beatmapApiService.updateExtendedBeatmapFailTimes()
    }

    // 每天凌晨2点30统计用户信息
    @Scheduled(cron = "0 5 2 * * *")
    fun collectInfoAndScores() {
        dailyStatisticsService.collectInfoAndScores()
    }

    // 每天凌晨4点20统计玩家百分比
    @Scheduled(cron = "0 20 4 * * *")
    fun collectPercentiles() {
        dailyStatisticsService.collectPercentiles()
    }

    // 每天凌晨4点30统计新人群用户信息
    @Scheduled(cron = "0 30 4 * * *")
    fun statisticNewbieInfo() {
        val bot = botContainer.robots[newbieConfig.yumuBot] ?: botContainer.robots[newbieConfig.hydrantBot]
        if (bot == null) {
            log.error("统计新人群信息失败, 未找到机器人")
            return
        }
        val groupMembers = bot.getGroupMemberList(newbieConfig.newbieGroup)
        if (groupMembers.data.isNullOrEmpty()) {
            log.error("统计新人群信息失败, 查询群聊成员为空")
            return
        }
        val users = groupMembers.data.map { it.userId }
        val uid = bindDao.getAllQQBindUser(users).map { it.uid }
        newbieService.dailyTask(uid)
    }

    // 今天下午更新数据'
    /*
    @Scheduled(cron = "0 0 14 20 1 *")
    fun update() {
        if (LocalDate.now().year == 2025) {
            newbieService.recalculate()
        }
    }

     */

    @Scheduled(cron = "0 0 6 * * *")
    fun updateMaimaiSongLibrary() {
        log.info("开始执行更新 maimai 歌曲库任务")
        //maimaiApiService.updateMaimaiSongLibraryFile()
        maimaiApiService.updateMaimaiSongLibraryDatabase()
    }

    @Scheduled(cron = "20 0 6 * * *")
    fun updateMaimaiRankLibrary() {
        log.info("开始执行更新 maimai 排名库任务")
        //maimaiApiService.updateMaimaiRankLibraryFile()
        maimaiApiService.updateMaimaiRankLibraryDatabase()
    }

    @Scheduled(cron = "40 0 6 * * *")
    fun updateMaimaiFitLibrary() {
        log.info("开始执行更新 maimai 统计库任务")
        //maimaiApiService.updateMaimaiFitLibraryFile()
        maimaiApiService.updateMaimaiFitLibraryDatabase()
    }

    @Scheduled(cron = "0 1 6 * * *")
    fun updateMaimaiAliasLibrary() {
        log.info("开始执行更新 maimai 外号库任务")
        //maimaiApiService.updateMaimaiAliasLibraryFile()
        maimaiApiService.updateMaimaiAliasLibraryDatabase()
    }

    @Scheduled(cron = "20 1 6 * * *")
    fun updateChunithmSongsLibrary() {
        log.info("开始执行更新 chunithm 歌曲库任务")
        //chunithmApiService.updateChunithmSongLibraryFile()
        chunithmApiService.updateChunithmSongLibraryDatabase()
    }

    @Scheduled(cron = "40 1 6 * * *")
    fun updateChunithmAliasLibrary() {
        log.info("开始执行更新 chunithm 外号库任务")
        //chunithmApiService.updateChunithmAliasLibraryFile()
        chunithmApiService.updateChunithmAliasLibraryDatabase()
    }

    @Scheduled(cron = "0 2 6 * * *")
    fun updateLxnsMaiSongLibrary() {
        log.info("开始执行更新 lxns maimai 歌曲库任务")
        lxMaiApiService.saveLxMaiSongs()
    }

    @Scheduled(cron = "20 2 6 * * *")
    fun updateLxnsMaiCollectionLibrary() {
        log.info("开始执行更新 lxns maimai 收藏库任务")
        lxMaiApiService.saveLxMaiCollections()
    }

    @Scheduled(cron = "40 2 6 * * *")
    fun updateBeatMapTagsLibrary() {
        log.info("开始执行更新谱面玩家标签库任务")
        beatmapApiService.updateBeatmapTagLibraryDatabase()
    }


    /*
    // @Scheduled(cron = "0 5 10 1 9 *")
    fun count() {
        try {
            val service = applicationContext.getBean(NewbieService::class.java)
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
    */

    /***
     * 白天输出内存占用信息
     */
    @Scheduled(cron = "0 0/30 8-23 * * *")
    fun alive() {
        fun Long.toMega(): Long {
            return this / 1024L / 1024L
        }

        val m = ManagementFactory.getMemoryMXBean()
        val nm = m.nonHeapMemoryUsage
        val hm = m.heapMemoryUsage
        val t = ManagementFactory.getThreadMXBean()
        val z = ManagementFactory.getMemoryPoolMXBeans()

        log.info("""
            非堆内存：已使用 ${nm.used.toMega()} MB，已分配 ${nm.committed.toMega()} MB
            堆内存：已使用 ${hm.used.toMega()} MB，已分配 ${hm.committed.toMega()} MB，最大可用 ${hm.max.toMega()} MB
            线程：当前 ${t.threadCount} 个 (守护 ${t.daemonThreadCount}，最大 ${t.peakThreadCount})
        """.trimIndent())

        val sb = StringBuilder("虚拟机内存池：")

        for (pool in z) {
            sb.append("\n已使用 ${pool.usage.used.toMega()} MB，已分配 ${pool.usage.committed.toMega()} MB (${pool.name})")
        }

        log.info(sb.toString())
    }


    lateinit var scheduledTaskRegistrar: ScheduledTaskRegistrar

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        this.scheduledTaskRegistrar = taskRegistrar
    }

    fun addTask(task: Runnable, cron: String) {
        scheduledTaskRegistrar.addCronTask({ taskExecutor.execute(task) }, cron)
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
                log.error("获取 {} 类型的bean出错", entry.getKey().getSimpleName());
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
        val bot: Bot?
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
    */


    companion object {
        private val log: Logger = LoggerFactory.getLogger(RunTimeService::class.java)
    }
}
