package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.enums.MaiVersion.*
import com.now.nowbot.permission.TokenBucketRateLimiter
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.lxnsApiService.LxMaiApiService
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.command.FLAG_ANY
import org.springframework.stereotype.Service
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service("FETCH")
class FetchService(
    private val lxMaiApiService: LxMaiApiService
): MessageService<FetchService.FetchParam> {

    enum class FetchType {
        MAIMAI;

        companion object {
            fun getType(string: String?): FetchType {
                return when(string?.trim()) {
                    "m", "mai", "maimai", "" -> MAIMAI

                    else -> throw UnsupportedOperationException("""
                        请输入需要获取的种类：
                        
                        m -> maimai: musicDB.json
                        限制 2 分钟获取一次。
                    """.trimIndent())
                }
            }
        }
    }

    abstract class FetchParam

    data class FetchMaiSongParam(
        val boolean: Boolean = true
    ): FetchParam()

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<FetchParam>
    ): Boolean {
        val matcher = Instruction.FETCH.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val any: String? = matcher.group(FLAG_ANY)

        if (Permission.isGroupAdmin(event)) {
            val type = FetchType.getType(any)

            data.value = when(type) {
                FetchType.MAIMAI -> FetchMaiSongParam(boolean = true)
            }
            return true
        } else return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: FetchParam
    ): ServiceCallStatistic? {
        rateLimiter.checkOrThrow("FETCH")

        when(param) {
            is FetchMaiSongParam -> fetchMaiMusicDatabaseJson(event, lxMaiApiService)

            else -> throw UnsupportedOperationException.Invalid()
        }

        return null
    }

    companion object {
        val rateLimiter = TokenBucketRateLimiter(1, 2.toDuration(DurationUnit.MINUTES))

        private fun fetchMaiMusicDatabaseJson(event: MessageEvent, lxMaiApiService: LxMaiApiService) {
            val l = lxMaiApiService.getMaiSongs()
                .associateBy { it.songID }

            fun getVersionInt(maiVersion: MaiVersion): Int {
                return when(maiVersion) {
                    DEFAULT -> 0
                    MAIMAI -> 0
                    PLUS -> 1
                    GREEN -> 2
                    GREEN_PLUS -> 3
                    ORANGE -> 4
                    ORANGE_PLUS -> 5
                    PINK -> 6
                    PINK_PLUS -> 7
                    MURASAKI -> 8
                    MURASAKI_PLUS -> 9
                    MILK -> 10
                    MILK_PLUS -> 11
                    FINALE -> 12
                    ALL_FINALE -> 0
                    DX -> 13
                    DX_PLUS -> 13
                    SPLASH -> 14
                    SPLASH_PLUS -> 15
                    UNIVERSE -> 16
                    UNIVERSE_PLUS -> 17
                    FESTIVAL -> 18
                    FESTIVAL_PLUS -> 19
                    BUDDIES -> 20
                    BUDDIES_PLUS -> 21
                    PRISM -> 22
                    PRISM_PLUS -> 23
                    CIRCLE -> 24
                    CIRCLE_PLUS -> 25
                }
            }

            val map = l
                .toList()
                .sortedBy { it.first }
                .associate { entry ->
                    val s = entry.second

                    entry.first.toString() to mapOf(
                        "name" to s.title,
                        "version" to getVersionInt(MaiVersion.getVersion(s.info.version))

                    )
                }

            val fileArray = JacksonUtil.objectToJsonPretty(map).toByteArray(Charsets.UTF_8)

            event.replyFileInGroup(fileArray, "musicDB.json")
        }
    }
}