package com.now.nowbot.aop

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.OsuBindUserLite
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.entity.UserProfileLite
import com.now.nowbot.mapper.UserProfileRepository
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Aspect
@Component
class CheckAspect(
    // private val serviceCall: ServiceCallRepository?,
    private val serviceCallStatisticsDao: ServiceCallStatisticsDao,
    private val userProfileRepository: UserProfileRepository
) {
    //所有实现 MessageService 的 HandMessage 方法切入点
    @Pointcut("within(com.now.nowbot.service.MessageService+) &&  execution(* handleMessage(com.now.nowbot.qq.event.MessageEvent, ..))")
    fun servicePoint() {
    }

    @Pointcut("within(org.springframework.web.client.RestTemplate) && !execution(void *(..))")
    fun restTemplate() {
    }

    @Pointcut("execution(* com.now.nowbot.mapper.BindUserMapper.save(..))")
    fun userSave() {
    }

    @Pointcut(
        ("execution(* com.now.nowbot.service.osuApiService.OsuBeatmapApiService.*(..)) ||" +
                "execution(* com.now.nowbot.service.osuApiService.OsuUserApiService.*(..)) ||" +
                "execution(* com.now.nowbot.service.osuApiService.OsuMatchApiService.*(..)) ||" +
                "execution(* com.now.nowbot.service.osuApiService.OsuDiscussionApiService.*(..)) ||" +
                "execution(* com.now.nowbot.service.osuApiService.OsuScoreApiService.*(..))")
    )
    fun apiService() {
    }

    @Pointcut("execution(* com.now.nowbot.service.ImageService.get*(..))")
    fun imageService() {
    }


    @Before(value = "userSave()")
    fun userSaveLogger(point: JoinPoint): Array<Any> {
        val args = point.args

        val u = args.firstOrNull()

        if (u is OsuBindUserLite) {
            if (u.userID != 0L) {
                log.info("新增用户：{} ({})", u.userID, u.username)
            } else {
                log.info(
                    "新增绑定关系 ({})",
                    u.accessToken?.take(15) ?: "null"
                )
            }
        }
        return args
    }

    @Before("servicePoint() && @annotation(ServiceLimit)")
    fun serviceLimit(point: JoinPoint, serviceLimit: ServiceLimit): Any {
        val limit = serviceLimit.cooldownMillis
        if (limit == 0L) return point.args
        val now = System.currentTimeMillis()
        val time = SERVICE_LIMIT_MAP.getOrDefault(serviceLimit, 0L)
        if (now - time > limit) {
            SERVICE_LIMIT_MAP[serviceLimit] = now
            return point.args
        }
        throw PermissionException("请求过于频繁")
    }

    object UserProfileContext {
        private val HOLDER = ThreadLocal<UserProfileLite?>()

        fun set(profile: UserProfileLite?) {
            HOLDER.set(profile)
        }

        fun get(): UserProfileLite? {
            return HOLDER.get()
        }

        fun remove() {
            HOLDER.remove()
        }
    }

    @Around("imageService()")
    @Throws(Throwable::class)
    fun beforeGetImage(point: ProceedingJoinPoint): Any? {
        val args = point.args
        for (arg in args) {
            if (arg is LazerScore) {
                if (arg.user.userID != 0L) {
                    val profile = userProfileRepository.findTopById(arg.user.userID)
                    if (profile != null) {
                        UserProfileContext.set(profile)
                    }
                }
            }
        }

        try {
            return point.proceed()
        } finally {
            UserProfileContext.remove()
        }
    }

    @Around(value = "servicePoint()", argNames = "pjp")
    @Throws(Throwable::class)
    fun setContext(pjp: ProceedingJoinPoint) {
        val ser = pjp.target.javaClass.getAnnotation<Service?>(Service::class.java)
        var name = "unknown"
        if (ser != null) {
            name = ser.value
        }

        val e = pjp.args.firstOrNull()

        if (e is MessageEvent) {
            if (e.subject.contactID < 0) {
                log.debug("官方bot [uid {}] 调用 -> {}", -e.sender.contactID, name)
            } else {
                log.debug("{} 调用 -> {}", e.sender.contactID, name)
            }
        }
        var result: Any? = null
        val start = System.currentTimeMillis()
        try {
            result = pjp.proceed(pjp.getArgs())
        } finally {
            val end = System.currentTimeMillis()
            val duration = end - start
            if (result is ServiceCallStatistic) {
                // 新版的统计
                result.setOther(name, start, duration)
                serviceCallStatisticsDao.saveService(result)
            }

            // 原来的可以下线了
            //serviceCall.saveCall(name, duration);
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CheckAspect::class.java)
        private val SERVICE_LIMIT_MAP: MutableMap<ServiceLimit, Long> = ConcurrentHashMap<ServiceLimit, Long>()
    }
}
