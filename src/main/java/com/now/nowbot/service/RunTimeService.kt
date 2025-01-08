package com.now.nowbot.service

import com.now.nowbot.dao.BindDao
import com.now.nowbot.newbie.mapper.NewbieService
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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
class RunTimeService(
    private val dailyStatisticsService: DailyStatisticsService,
    private val bindDao: BindDao,
    private val maimaiApiService: MaimaiApiService,
    private val chunithmApiService: ChunithmApiService,
    private val userApiService: OsuUserApiService,
    @Qualifier("kotlinTaskExecutor")
    private val taskExecutor: TaskExecutor,
    private val applicationContext: ApplicationContext,
) : SchedulingConfigurer {

    //@Scheduled(cron = "0(秒) 0(分) 0(时) *(日) *(月) *(周) *(年,可选)")  '/'步进
    @Scheduled(cron = "0 0 2 * * *")
    fun refreshToken() {
        log.info("开始执行更新令牌任务")
        bindDao.refreshOldUserToken(userApiService)
    }

    // 每天凌晨4点统计用户信息
    @Scheduled(cron = "0 0 4 * * *")
    fun statisticUserInfo() {
        dailyStatisticsService.asyncTask()
    }

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
        //maimaiApiService.updateMaimaiAliasLibraryFile()
        chunithmApiService.updateChunithmSongLibraryFile()
    }


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

    /***
     * 白天输出内存占用信息
     */
    @Scheduled(cron = "0 0/30 8-18 * * *")
    fun alive() {
        val m = ManagementFactory.getMemoryMXBean()
        val nm = m.nonHeapMemoryUsage
        val t = ManagementFactory.getThreadMXBean()
        val z = ManagementFactory.getMemoryPoolMXBeans()
        log.info(
            "方法区 已申请 {}M 已使用 {}M ",
            nm.committed / 1024 / 1024,
            nm.used / 1024 / 1024
        )
        log.info(
            "堆内存上限{}M,当前内存占用{}M, 已使用{}M\n当前线程数 {} ,守护线程 {} ,峰值线程 {}",
            m.heapMemoryUsage.max / 1024 / 1024,
            m.heapMemoryUsage.committed / 1024 / 1024,
            m.heapMemoryUsage.used / 1024 / 1024,
            t.threadCount,
            t.daemonThreadCount,
            t.peakThreadCount
        )
        for (pool in z) {
            log.info(
                "vm内存 {} 已申请 {}M 已使用 {}M ",
                pool.name,
                pool.usage.committed / 1024 / 1024,
                pool.usage.used / 1024 / 1024
            )
        }
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

        @JvmStatic
        @Deprecated("建议替换", replaceWith = ReplaceWith("testNew(s)"))
        fun test(s: String) {
        }

        @JvmStatic
        fun testNew(s: String) {
        }
    }
}
