package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.Locale
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.random.RandomGenerator

@Service("LOGIN")
class LoginService(private val bindDao: BindDao) : MessageService<String> {
    init {
        Thread.startVirtualThread {
            while (true) {
                try {
                    Thread.sleep(Duration.ofSeconds(120))
                } catch (_: InterruptedException) {
                }
                val t = System.currentTimeMillis()
                LOGIN_USER_MAP.entries.removeIf { entry: MutableMap.MutableEntry<String, LoginUser> ->
                    t.minus(entry.value.time) > 60 * 1000
                }
            }
        }
    }

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<String>
    ): Boolean {
        return "!login" == messageText
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {
        val qq = event.sender.contactID
        val u = bindDao.getBindFromQQ(qq)
        var code: String?
        // 防止key重复, 循环构造随机字符串
        while (LOGIN_USER_MAP.containsKey(
                (getRoStr().also { code = it }).uppercase())) {}
        event.reply("您的登录验证码: $code")
        LOGIN_USER_MAP[code!!.uppercase(Locale.getDefault())] = LoginUser(u.userID, u.username, System.currentTimeMillis())

        return null
    }

    @JvmRecord data class LoginUser(val uid: Long, val name: String, val time: Long)

    companion object {
        @JvmField
        val LOGIN_USER_MAP: MutableMap<String, LoginUser> =
                ConcurrentHashMap<String, LoginUser>()
        private const val CODE_SIZE = 6
        var random: Random = Random.from(RandomGenerator.getDefault())

        fun getRoStr(): String {
            val t = CharArray(CODE_SIZE)
            for (i in 0 until CODE_SIZE) {
                var temp = random.nextInt(0, 36)
                if (temp < 10) {
                    t[i] = ('0'.code + temp).toChar()
                } else {
                    // 防止 'l' 与 'I', 0 与 'O' 混淆
                    if (temp == 'o'.code - 'a'.code) temp += 3
                    temp -= if (temp != 18 && (temp == 21 || random.nextBoolean())) {
                        10
                    } else {
                        42
                    }
                    t[i] = ('a'.code + temp).toChar()
                }
            }
            return String(t)
        }
    }
}
