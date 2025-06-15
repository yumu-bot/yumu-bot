package com.now.nowbot.util

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.CmdUtil.parseNameAndRangeHasHash
import com.now.nowbot.util.CmdUtil.parseNameAndRangeWithoutHash
import com.now.nowbot.util.command.*
import kotlinx.coroutines.*
import org.springframework.context.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

/**
 * 这个类是用于快速地在命令中通过数据库获取玩家的 UID。
 *
 */

object UserIDUtil {
    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    fun getUserIDWithRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean = AtomicBoolean(false)
    ): CmdRange<Long> {
        val range = getUserIDAndRange(event, matcher, mode, isMyself)

        if (range.data == null) {
            range.data = getUserIDWithoutRange(event, matcher, mode, isMyself)
        }

        return range
    }

    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    fun getUserIDWithoutRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean = AtomicBoolean(false),
    ): Long? {

        val userID: Long?
        val me: BindUser?

        val deferred = scope.async {
            getUserID(event, matcher, mode, isMyself)
        }

        val deferred2 = scope.async {
            try {
                bindDao.getBindFromQQ(event.sender.id, true)
            } catch (ignored: BindException) {
                null
            }
        }

        runBlocking {
            userID = deferred.await()
            me = deferred2.await()

            deferred2.start()
        }

        scope.cancel()

        val myID = me?.userID

        if (userID != null) {
            return userID
        }

        if (myID != null && isMyself.get()) {
            setMode(mode, me.mode, event)
            return myID
        }

        return null
    }

    private fun getUserIDAndRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean,
    ): CmdRange<Long> {

        require(
            matcher.namedGroups().containsKey(FLAG_USER_AND_RANGE)
        ) { "Matcher 中不包含 ur 分组" }

        isMyself.set(false)

        if (mode.data == null) {
            mode.data = OsuMode.DEFAULT
        }

        val text: String? = matcher.group(FLAG_USER_AND_RANGE)?.trim()
        if (text.isNullOrBlank()) {
            return CmdRange()
        }

        val onlyRange = "^\\s*$REG_HASH?\\s*(\\d{1,3}$REG_HYPHEN+)?\\d{1,3}\\s*$".toRegex()

        if (text.contains(onlyRange)) {
            val range = parseRange(text)

            // 特殊情况，前面是某个 201~999 范围内的玩家
            if (range.first != null && range.second == null && range.first in 201..999) try {
                val user = bindDao.getBindUser(range.first.toString())

                if (user != null) {
                    setMode(mode, user.mode, event)
                } else {
                    setMode(mode, event)
                }

                return CmdRange(user?.userID)
            } catch (ignored: Exception) {}

            isMyself.set(true)
            return CmdRange(bindDao.getBindFromQQ(event.sender.id).userID, range.first, range.second)
        }

        val ranges = if (text.contains("($CHAR_HASH|$CHAR_HASH_FULL)".toRegex())) {
            parseNameAndRangeHasHash(text)
        } else {
            parseNameAndRangeWithoutHash(text)
        }

        var result = CmdRange<Long>()

        for (range in ranges) try {
            if (range.data == null) {
                result = CmdRange(null, range.start, range.end)
                break
            }

            val user = bindDao.getBindUser(range.data!!)

            if (user != null) {
                setMode(mode, user.mode, event)
            } else {
                setMode(mode, event)
            }

            val id = bindDao.getOsuID(range.data!!)

            return CmdRange(id, range.start, range.end)

        } catch (ignored: Exception) {}

        // 交换顺序
        if (result.start != null && result.end != null && result.start!! > result.end!!) {
            result.start = result.end!!.also { result.end = result.start }
        }

        return result
    }

    private fun parseRange(text: String): Pair<Int?, Int?> {
        val range = text
            .removePrefix(CHAR_HASH.toString())
            .removePrefix(CHAR_HASH_FULL.toString())
            .trim()
            .split(REG_HYPHEN.toRegex())
            .dropLastWhile { it.isEmpty() }
            .mapNotNull { it.trim().toIntOrNull() }
            .toTypedArray()

        return if (range.size >= 2) {
            Pair(range[0], range[1])
        } else if (range.size == 1) {
            Pair(range.first(), null)
        } else {
            Pair(null, null)
        }
    }

    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    private fun getUserID(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean,
    ): Long? {
        val qq = if (event.isAt) {
            event.target
        } else if (matcher.namedGroups().containsKey(FLAG_QQ_ID)) {
            try {
                matcher.group(FLAG_QQ_ID)?.toLongOrNull() ?: 0L
            } catch (ignore: RuntimeException) {
                0L
            }
        } else {
            0L
        }

        if (qq != 0L) {
            isMyself.set(qq == event.sender.id)

            try {
                return bindDao.getBindFromQQ(qq).userID
            } catch (ignored: BindException) {}
        }

        setMode(mode, event)

        if (matcher.namedGroups().containsKey(FLAG_UID)) {
            try {
                val uid = matcher.group(FLAG_UID)?.toLongOrNull() ?: 0L

                if (uid != 0L) {
                    isMyself.set(false)
                    return uid
                }
            } catch (ignore: RuntimeException) {}
        }

        if (matcher.namedGroups().containsKey(FLAG_NAME)) {
            val name: String? = matcher.group(FLAG_NAME)
            if (!name.isNullOrBlank()) {
                isMyself.set(false)
                return bindDao.getOsuID(name)
            }
        }

        if (matcher.namedGroups().containsKey(FLAG_USER_AND_RANGE)) {
            val name2: String? = matcher.group(FLAG_USER_AND_RANGE)
            if (!name2.isNullOrBlank()) {
                isMyself.set(false)
            } else {
                isMyself.set(true)
            }
        } else {
            isMyself.set(true)
        }

        return null
    }

    /**
     * 用于覆盖默认的游戏模式。优先级：mode > groupMode > selfMode
     * @param mode 玩家查询时输入的游戏模式
     * @param selfMode 一般是玩家自己绑定的游戏模式
     * @param event 可能为群聊
     */
    private fun setMode(mode: CmdObject<OsuMode>, selfMode: OsuMode, event: MessageEvent? = null) {
        mode.data = OsuMode.getMode(mode.data, selfMode, bindDao.getGroupModeConfig(event))
    }

    /**
     * 用于覆盖默认的游戏模式。优先级：mode > groupMode
     * @param mode 玩家查询时输入的游戏模式
     * @param event 可能为群聊
     */
    private fun setMode(mode: CmdObject<OsuMode>, event: MessageEvent? = null) {
        mode.data = OsuMode.getMode(mode.data, OsuMode.DEFAULT, bindDao.getGroupModeConfig(event))
    }


    private lateinit var bindDao: BindDao
    private lateinit var userApiService: OsuUserApiService
    private lateinit var scoreApiService: OsuScoreApiService
    private lateinit var beatmapApiService: OsuBeatmapApiService
    private lateinit var scope: CoroutineScope

    @JvmStatic fun init(applicationContext: ApplicationContext) {
        bindDao = applicationContext.getBean(BindDao::class.java)
        userApiService = applicationContext.getBean(OsuUserApiService::class.java)
        scoreApiService = applicationContext.getBean(OsuScoreApiService::class.java)
        beatmapApiService = applicationContext.getBean(OsuBeatmapApiService::class.java)
        scope = CoroutineScope(Dispatchers.IO.limitedParallelism(2))
    }
}