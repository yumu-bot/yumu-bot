package com.now.nowbot.util

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.isDefaultOrNull
import com.now.nowbot.model.enums.OsuMode.Companion.orElse
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.command.*
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

/**
 * 这个类是用于快速地在命令中通过数据库获取玩家的 UID。
 *
 */
@Component
class UserIDUtil(
    private val bindDao: BindDao
) {
    @PostConstruct
    fun init() {
        // 在 Spring 初始化完成后，把实例赋值给静态变量
        instance = this
    }

    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    private fun getUserIDWithRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: InstructionObject<OsuMode>,
        isMyself: AtomicBoolean = AtomicBoolean(false),
        maximum: Int = 200,
    ): InstructionRange<Long> {
        val range = getUserIDAndRange(event, matcher, mode, isMyself, maximum)

        if (range.data == null) {
            range.data = getUserIDWithoutRange(event, matcher, mode, isMyself, maximum)
        }

        return range
    }
    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    private fun getSBUserIDWithRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: InstructionObject<OsuMode>,
        isMyself: AtomicBoolean = AtomicBoolean(false),
        maximum: Int = 200
    ): InstructionRange<Long> {
        val range = getSBUserIDAndRange(event, matcher, mode, isMyself, maximum)

        if (range.data == null) {
            range.data = getSBUserIDWithoutRange(event, matcher, mode, isMyself)
        }

        return range
    }

    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    private fun getUserIDWithoutRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: InstructionObject<OsuMode>,
        isMyself: AtomicBoolean = AtomicBoolean(false),
        maximum: Int = 200,
    ): Long? {
        val userID: Long?
        val me: BindUser?

        val async = AsyncMethodExecutor.awaitPair(
            { getUserID(event, matcher, mode, isMyself, maximum) },
            { bindDao.getBindFromQQOrNull(event.sender.contactID) }
        )

        userID = async.first
        me = async.second

        val myID = me?.userID

        if (userID != null) {
            return userID
        }

        if (myID != null && isMyself.get()) {
            setMode(mode, event, me.mode)
            return myID
        }

        return null
    }

    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    private fun getSBUserIDWithoutRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: InstructionObject<OsuMode>,
        isMyself: AtomicBoolean = AtomicBoolean(false),
    ): Long? {
        val userID: Long?
        val me: SBBindUser?

        val async = AsyncMethodExecutor.awaitPair(
            { getSBUserID(event, matcher, mode, isMyself) },
            {
                try {
                    bindDao.getSBBindFromQQ(event.sender.contactID, true)
                } catch (_: BindException) {
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
            setMode(mode, selfMode = me.mode)
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
    private fun get2UserID(
        event: MessageEvent,
        matcher: Matcher,
        mode: InstructionObject<OsuMode>,
        isVS: Boolean = false,
    ): Pair<Long?, Long?> {
        require(
            matcher.namedGroups().containsKey(FLAG_2_USER)
        ) {
            "Matcher 中不包含 u2 分组"
        }

        val me = bindDao.getBindFromQQOrNull(event.sender.contactID)

        setMode(mode, event, me?.mode ?: OsuMode.DEFAULT)

        if (event.hasAt()) {
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

            setMode(mode, event, me.mode)
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
                val bind = bindDao.getBindFromQQ(event.sender.contactID, true)

                setMode(mode, event, bind.mode)
                return bind.userID to null
            }
        }
    }


    /**
     * @param qq 如果是负数，则认为是 UID
     */
    private fun getUserIDFromQQ(qq: Long, me: BindUser?, mode: InstructionObject<OsuMode>, isVS: Boolean): Pair<Long?, Long?> {
        val you = if (qq > 0L) {
            bindDao.getBindFromQQ(qq)
        } else {
            BindUser(- qq, "unknown")
        }

        setMode(mode, selfMode = me?.mode ?: OsuMode.DEFAULT)

        return if (isVS && me != null) {
            me.userID to you.userID
        } else {
            you.userID to null
        }
    }

    private fun getUserIDFromName(name: String?, me: BindUser?, mode: InstructionObject<OsuMode>, isVS: Boolean): Pair<Long?, Long?> {
        val yourID = if (name.isNullOrBlank()) {
            null
        } else try {
            bindDao.getOsuID(name)
        } catch (_: Exception) {
            null
        }

        setMode(mode, selfMode = me?.mode ?: OsuMode.DEFAULT)

        return if (isVS && me != null) {
            me.userID to yourID
        } else {
            yourID to null
        }
    }

    private fun getUserIDAndRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: InstructionObject<OsuMode>,
        isMyself: AtomicBoolean,
        maximum: Int = 200,
    ): InstructionRange<Long> {

        require(
            matcher.namedGroups().containsKey(FLAG_USER_AND_RANGE)
        ) { "Matcher 中不包含 ur 分组" }

        isMyself.set(false)

        if (mode.data == null) {
            mode.data = OsuMode.DEFAULT
        }

        val text: String? = matcher.group(FLAG_USER_AND_RANGE)?.trim()
        if (text.isNullOrBlank()) {
            return InstructionRange()
        }

        val hasHash = text.contains(REG_HASH.toRegex())

        if (text.matches(RANGE_ONLY.toRegex()) && !hasHash) {
            val range = parseRange(text)

            // 特殊情况，前面是某个 201~999 范围内的玩家
            if (range.first != null && range.second == null && range.first in 201..999) try {
                val user = bindDao.getBindUser(range.first.toString())

                return if (user != null) {
                    setMode(mode, event, user.mode)
                    InstructionRange(user.userID)
                } else {
                    InstructionRange(null, range.first)
                }
            } catch (_: Exception) {}

            isMyself.set(true)

            val me = bindDao.getBindFromQQ(event.sender.contactID)
            setMode(mode, event, me.mode)

            return InstructionRange(me.userID, range.first, range.second)
        }

        val range = if (hasHash) {
            InstructionUtil.parseNameWithHashedRange(text, maximum)
        } else {
            InstructionUtil.parseNameWithRange(text, maximum)
        }

        var result: InstructionRange<Long>

        try {
            if (range.data == null) {
                result = InstructionRange(null, range.start, range.end)
            } else {
                val user = bindDao.getBindUser(range.data!!)

                if (user != null) {
                    setMode(mode, event, user.mode)
                } else {
                    setMode(mode, event)
                }

                val id = bindDao.getOsuID(range.data!!)

                result = InstructionRange(id, range.start, range.end)
            }
        } catch (_: Exception) {
            result = InstructionRange()
        }

        return result
    }

    private fun getSBUserIDAndRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: InstructionObject<OsuMode>,
        isMyself: AtomicBoolean,
        maximum: Int = 200
    ): InstructionRange<Long> {

        require(
            matcher.namedGroups().containsKey(FLAG_USER_AND_RANGE)
        ) { "Matcher 中不包含 ur 分组" }

        isMyself.set(false)

        if (mode.data == null) {
            mode.data = OsuMode.DEFAULT
        }

        val text: String? = matcher.group(FLAG_USER_AND_RANGE)?.trim()
        if (text.isNullOrBlank()) {
            return InstructionRange()
        }

        val hasHash = text.contains(REG_HASH.toRegex())

        if (text.matches(RANGE_ONLY.toRegex()) && !hasHash) {
            val range = parseRange(text)

            // 特殊情况，前面是某个 201~999 范围内的玩家
            if (range.first != null && range.second == null && range.first in 201..999) try {
                val user = try {
                    bindDao.getSBBindUser(range.first.toString())
                } catch (_: BindException) {
                    null
                }

                return if (user != null) {
                    setMode(mode, selfMode = user.mode)
                    InstructionRange(user.userID)
                } else {
                    InstructionRange(null, range.first)
                }
            } catch (_: Exception) {}

            isMyself.set(true)

            val me = bindDao.getSBBindFromQQ(event.sender.contactID, true)
            setMode(mode, selfMode = me.mode)

            return InstructionRange(me.userID, range.first, range.second)
        }

        val range = if (hasHash) {
            InstructionUtil.parseNameWithHashedRange(text, maximum)
        } else {
            InstructionUtil.parseNameWithRange(text, maximum)
        }

        var result: InstructionRange<Long>

        try {
            if (range.data == null) {
                result = InstructionRange(null, range.start, range.end)
            } else {
                val user = try {
                    bindDao.getSBBindUser(range.data!!)
                } catch (_: BindException) {
                    null
                }

                if (user != null) {
                    setMode(mode, selfMode = user.mode)
                } else {
                    setMode(mode, null)
                }

                val id = bindDao.getSBUserID(range.data!!)

                return InstructionRange(id, range.start, range.end)
            }
        } catch (_: Exception) {
            result = InstructionRange()
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
        mode: InstructionObject<OsuMode>,
        isMyself: AtomicBoolean,
        maximum: Int = 200,
    ): Long? {
        val qq = if (event.hasAt()) {
            event.target
        } else if (matcher.namedGroups().containsKey(FLAG_QQ_ID)) {
            matcher.group(FLAG_QQ_ID)?.toLongOrNull()
        } else {
            null
        }

        if (qq != null) {
            isMyself.set(qq == event.sender.contactID)

            // 必须提前返回，否则对方没绑定时会掉到后面去
            val sb = bindDao.getBindFromQQOrNull(qq) ?: return null

            setMode(mode, event, sb.mode)
            return sb.userID
        }

        setMode(mode, event)

        if (matcher.namedGroups().containsKey(FLAG_UID)) {
            val uid = matcher.group(FLAG_UID)?.toLongOrNull() ?: 0L

            if (uid != 0L) {
                isMyself.set(false)
                return uid
            }
        }

        if (matcher.namedGroups().containsKey(FLAG_NAME)) {
            val name: String? = matcher.group(FLAG_NAME)
            if (!name.isNullOrBlank()) {
                isMyself.set(false)
                return bindDao.getOsuID(name)
            }
        }

        if (matcher.namedGroups().containsKey(FLAG_USER_AND_RANGE)) {
            val name2: String = matcher.group(FLAG_USER_AND_RANGE) ?: ""

            val onlyRange = name2.matches(RANGE_ONLY.toRegex())

            val hasOthers = !name2.isBlank() && !onlyRange

            if (hasOthers || (name2.toIntOrNull() ?: Int.MIN_VALUE) > maximum) {
                isMyself.set(false)
                return null
            }
        }

        isMyself.set(true)
        return null
    }

    /**
     * @param isMyself 这里的布尔值仅用于返回当前 parse 的效果
     */
    private fun getSBUserID(
        event: MessageEvent,
        matcher: Matcher,
        mode: InstructionObject<OsuMode>,
        isMyself: AtomicBoolean,
    ): Long? {
        val qq = if (event.hasAt()) {
            event.target
        } else if (matcher.namedGroups().containsKey(FLAG_QQ_ID)) {
            try {
                matcher.group(FLAG_QQ_ID)?.toLongOrNull() ?: 0L
            } catch (_: RuntimeException) {
                0L
            }
        } else {
            0L
        }

        if (qq != 0L) {
            isMyself.set(qq == event.sender.contactID)

            try {
                val sb = bindDao.getSBBindFromQQ(qq, isMyself.get())
                setMode(mode, selfMode = sb.mode)
                return sb.userID
            } catch (_: BindException) {}
        }

        // setMode(mode, event)

        if (matcher.namedGroups().containsKey(FLAG_UID)) {
            try {
                val uid = matcher.group(FLAG_UID)?.toLongOrNull() ?: 0L

                if (uid != 0L) {
                    isMyself.set(false)
                    return uid
                }
            } catch (_: RuntimeException) {}
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
     * 用于覆盖默认的游戏模式。优先级：mode > selfMode
     * @param mode 玩家查询时输入的游戏模式
     * @param selfMode 一般是玩家自己的游戏模式
     */
    private fun setMode(mode: InstructionObject<OsuMode>, selfMode: OsuMode) {
        if (mode.data.isDefaultOrNull()) {
            mode.data = selfMode
        }
    }

    /**
     * 用于覆盖默认的游戏模式。优先级：mode > groupMode > selfMode
     * @param mode 玩家查询时输入的游戏模式
     * @param event 可能为群聊
     * @param selfMode 一般是玩家自己的游戏模式
     */
    private fun setMode(mode: InstructionObject<OsuMode>, event: MessageEvent, selfMode: OsuMode) {
        if (mode.data.isDefaultOrNull()) {
            mode.data = bindDao.getGroupMode(event).orElse(selfMode)
        }
    }

    /**
     * 用于覆盖默认的游戏模式。优先级：mode > groupMode
     * @param mode 玩家查询时输入的游戏模式
     * @param event 可能为群聊
     */
    private fun setMode(mode: InstructionObject<OsuMode>, event: MessageEvent? = null) {
        if (mode.data.isDefaultOrNull()) {
            mode.data = bindDao.getGroupMode(event)
        }
    }

    companion object {
        private lateinit var instance: UserIDUtil

        fun getUserIDWithRange(
            event: MessageEvent,
            matcher: Matcher,
            mode: InstructionObject<OsuMode>,
            isMyself: AtomicBoolean = AtomicBoolean(false),
            maximum: Int = 200,
        ): InstructionRange<Long> {
            return instance.getUserIDWithRange(event, matcher, mode, isMyself, maximum)
        }


        fun getSBUserIDWithRange(
            event: MessageEvent,
            matcher: Matcher,
            mode: InstructionObject<OsuMode>,
            isMyself: AtomicBoolean = AtomicBoolean(false),
            maximum: Int = 200
        ): InstructionRange<Long> {
            return instance.getSBUserIDWithRange(event, matcher, mode, isMyself, maximum)
        }

        fun getUserIDWithoutRange(
            event: MessageEvent,
            matcher: Matcher,
            mode: InstructionObject<OsuMode>,
            isMyself: AtomicBoolean = AtomicBoolean(false),
            maximum: Int = 200
        ): Long? {
            return instance.getUserIDWithoutRange(event, matcher, mode, isMyself, maximum)
        }

        fun getSBUserIDWithoutRange(
            event: MessageEvent,
            matcher: Matcher,
            mode: InstructionObject<OsuMode>,
            isMyself: AtomicBoolean = AtomicBoolean(false),
        ): Long? {
            return instance.getSBUserIDWithoutRange(event, matcher, mode, isMyself)
        }

        fun get2UserID(
            event: MessageEvent,
            matcher: Matcher,
            mode: InstructionObject<OsuMode>,
            isVS: Boolean = false,
        ): Pair<Long?, Long?> {
            return instance.get2UserID(event, matcher, mode, isVS)
        }

    }
}