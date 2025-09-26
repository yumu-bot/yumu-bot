package com.now.nowbot.util

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.CmdUtil.parseNameAndRangeHasHash
import com.now.nowbot.util.CmdUtil.parseNameAndRangeWithoutHash
import com.now.nowbot.util.command.*
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
        isMyself: AtomicBoolean = AtomicBoolean(false),
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
    fun getSBUserIDWithRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean = AtomicBoolean(false),
    ): CmdRange<Long> {
        val range = getSBUserIDAndRange(event, matcher, mode, isMyself)

        if (range.data == null) {
            range.data = getSBUserIDWithoutRange(event, matcher, mode, isMyself)
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

        val async = AsyncMethodExecutor.awaitPairCallableExecute(
            { getUserID(event, matcher, mode, isMyself) },
            {
                try {
                    bindDao.getBindFromQQ(event.sender.id, true)
                } catch (ignored: BindException) {
                    null
                }
            }
        )

        userID = async.first
        me = async.second

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

    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    fun getSBUserIDWithoutRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean = AtomicBoolean(false),
    ): Long? {
        val userID: Long?
        val me: SBBindUser?

        val async = AsyncMethodExecutor.awaitPairCallableExecute(
            { getSBUserID(event, matcher, mode, isMyself) },
            {
                try {
                    bindDao.getSBBindFromQQ(event.sender.id, true)
                } catch (ignored: BindException) {
                    null
                }
            }
        )

        userID = async.first
        me = async.second

        val myID = me?.userID

        if (userID != null) {
            return userID
        }

        if (myID != null && isMyself.get()) {
            setMode(mode, me.mode, null)
            return myID
        }

        return null
    }

    /**
     * 获取命令中的 用户 和 range, 不包括 自己的QQ/at
     *
     * @param matcher 正则
     * @param mode 包装的模式, 如果给的 mode 非默认则返回对应 mode 的 userID 信息
     */
    fun get2UserID(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isVS: Boolean = false,
    ): Pair<Long?, Long?> {
        require(
            matcher.namedGroups().containsKey(FLAG_2_USER)
        ) {
            "Matcher 中不包含 u2 分组"
        }

        val me = try {
            bindDao.getBindFromQQ(event.sender.id)
        } catch (ignored: Exception) {
            null
        }

        setMode(mode, me?.mode ?: OsuMode.DEFAULT, event)

        if (event.isAt) {
            return getUserIDFromQQ(event.target, me, mode, isVS)
        }

        val qq = matcher.group(FLAG_QQ_ID)?.toLong() ?: 0L
        if (qq != 0L) {
            return getUserIDFromQQ(qq, me, mode, isVS)
        }

        val uid = matcher.group(FLAG_UID)?.toLong() ?: 0L
        if (uid != 0L) {
            return getUserIDFromQQ(0L - uid, me, mode, isVS)
        }

        val g = matcher.group(FLAG_2_USER)

        if (g.isNullOrEmpty()) {
            if (me == null) {
                throw BindException.NotBindException.YouNotBind()
            }

            setMode(mode, me.mode, event)
            return me.userID to null
        }

        val gs = g.split(REG_SEPERATOR_NO_SPACE.toRegex())

        when (gs.size) {
            1 -> return getUserIDFromName(gs.first().trim(), me, mode, isVS)
            2 -> {
                val yourID = bindDao.getOsuID(gs.first().trim())
                val theyID = bindDao.getOsuID(gs.last().trim())

                return yourID to theyID
            }
            else -> {
                val bind = bindDao.getBindFromQQ(event.sender.id, true)

                setMode(mode, bind.mode, event)
                return bind.userID to null
            }
        }
    }


    /**
     * @param qq 如果是负数，则认为是 UID
     */
    private fun getUserIDFromQQ(qq: Long, me: BindUser?, mode: CmdObject<OsuMode>, isVS: Boolean): Pair<Long?, Long?> {
        val you = if (qq > 0L) {
            bindDao.getBindFromQQ(qq)
        } else {
            BindUser(- qq, "unknown")
        }

        setMode(mode, me?.mode ?: OsuMode.DEFAULT)

        return if (isVS && me != null) {
            me.userID to you.userID
        } else {
            you.userID to null
        }
    }

    private fun getUserIDFromName(name: String?, me: BindUser?, mode: CmdObject<OsuMode>, isVS: Boolean): Pair<Long?, Long?> {
        val yourID = if (name.isNullOrBlank()) {
            null
        } else try {
            bindDao.getOsuID(name)
        } catch (ignored: Exception) {
            null
        }

        setMode(mode, me?.mode ?: OsuMode.DEFAULT)

        return if (isVS && me != null) {
            me.userID to yourID
        } else {
            yourID to null
        }
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

        val hasHash = text.contains(REG_HASH.toRegex())

        if (text.matches(RANGE_ONLY) && !hasHash) {
            val range = parseRange(text)

            // 特殊情况，前面是某个 201~999 范围内的玩家
            if (range.first != null && range.second == null && range.first in 201..999) try {
                val user = bindDao.getBindUser(range.first.toString())

                return if (user != null) {
                    setMode(mode, user.mode, event)
                    CmdRange(user.userID)
                } else {
                    CmdRange(null, range.first)
                }
            } catch (ignored: Exception) {}

            isMyself.set(true)

            val me = bindDao.getBindFromQQ(event.sender.id)
            setMode(mode, me.mode, event)

            return CmdRange(me.userID, range.first, range.second)
        }

        val ranges = if (hasHash) {
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

            result = CmdRange(id, range.start, range.end)
            break
        } catch (ignored: Exception) {}

        // 交换顺序
        if (result.start != null && result.end != null && result.start!! > result.end!!) {
            result.start = result.end!!.also { result.end = result.start }
        }

        return result
    }

    private fun getSBUserIDAndRange(
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

        val hasHash = text.contains(REG_HASH.toRegex())

        if (text.matches(RANGE_ONLY) && !hasHash) {
            val range = parseRange(text)

            // 特殊情况，前面是某个 201~999 范围内的玩家
            if (range.first != null && range.second == null && range.first in 201..999) try {
                val user = try {
                    bindDao.getSBBindUser(range.first.toString())
                } catch (e: BindException) {
                    null
                }

                return if (user != null) {
                    setMode(mode, user.mode, null)
                    CmdRange(user.userID)
                } else {
                    CmdRange(null, range.first)
                }
            } catch (ignored: Exception) {}

            isMyself.set(true)

            val me = bindDao.getSBBindFromQQ(event.sender.id, true)
            setMode(mode, me.mode, null)

            return CmdRange(me.userID, range.first, range.second)
        }

        val ranges = if (hasHash) {
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

            val user = try {
                bindDao.getSBBindUser(range.data!!)
            } catch (e: BindException) {
                null
            }

            if (user != null) {
                setMode(mode, user.mode, null)
            } else {
                setMode(mode, null)
            }

            val id = bindDao.getSBUserID(range.data!!)

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
                val sb = bindDao.getBindFromQQ(qq, isMyself.get())
                setMode(mode, sb.mode, event)
                return sb.userID
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
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    private fun getSBUserID(
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
                val sb = bindDao.getSBBindFromQQ(qq, isMyself.get())
                setMode(mode, sb.mode)
                return sb.userID
            } catch (ignored: BindException) {}
        }

        // setMode(mode, event)

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
                return bindDao.getSBUserID(name)
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

    @JvmStatic fun init(applicationContext: ApplicationContext) {
        bindDao = applicationContext.getBean(BindDao::class.java)
        userApiService = applicationContext.getBean(OsuUserApiService::class.java)
        scoreApiService = applicationContext.getBean(OsuScoreApiService::class.java)
        beatmapApiService = applicationContext.getBean(OsuBeatmapApiService::class.java)
    }
}