package com.now.nowbot.util

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.LogException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.util.command.*
import com.yumu.core.extensions.isNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

object CmdUtil {

    /** 获取玩家信息, 末尾没有 range。在未找到匹配的玩家时，抛错 */
    @JvmStatic @Throws(TipsException::class) fun getUserWithoutRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
    ): OsuUser {
        return getUserWithoutRange(event, matcher, mode, AtomicBoolean(false))
    }

    /**
     * 获取玩家信息, 末尾没有 range
     *
     * @param isMyself: 作为返回值使用, 如果是自己则结果为 true
     */
    @JvmStatic @Throws(TipsException::class) fun getUserWithoutRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean,
    ): OsuUser {
        val bu: BindUser? = try {
            bindDao.getBindFromQQ(event.sender.id, true)
        } catch (ignored: Exception) {
            null
        }

        val user = getOsuUser(event, matcher, if (bu != null) CmdObject(bu.osuMode) else mode)

        if (user != null) {
            isMyself.set(bu?.osuID == user.userID)
            return user
        } else if (bu != null) {
            isMyself.set(true)
            return getOsuUser(bu.username, bu.osuID) {
                userApiService.getPlayerInfo(bu, checkOsuMode(mode, bu.osuMode))
            }
        } else {
            if (isMyself.get()) {
                throw BindException(BindException.Type.BIND_Me_TokenExpired)
            } else {
                throw BindException(BindException.Type.BIND_Player_TokenExpired)
            }
        }
    }

    /**
     * 解析包含 @qq / username / uid= / qq= 的玩家信息, 并且解决后面的 range
     *
     * @param mode 一个可以改变的 mode, 解决当获取到 default, 修改成用户默认绑定时的 mode 无法传出的问题
     * @param isMyself 传入一个 [AtomicBoolean] 作为返回值使用, 如果是自己则结果为 true (注意, 当包含 qq= 或 uid= 时,
     *   即使是发送者本身也是 false)
     */
    @JvmStatic @Throws(TipsException::class) fun getUserWithRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean,
    ): CmdRange<OsuUser> {
        isMyself.set(false)
        val range = getUserAndRange(matcher, mode)
        if (range.data.isNull()) {
            range.data = getUserWithoutRange(event, matcher, mode, isMyself)
        }
        return range
    }

    @JvmStatic fun getUserWithBackoff(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean,
        text: String,
        vararg ignores: String,
    ): CmdRange<OsuUser> {
        try {
            if (matcher.namedGroups().containsKey("name")) {
                val name = (matcher.group("name") ?: "")

                if (name.trim().matches("(\\d{1,2}$REG_HYPHEN)?(100|\\d{1,2})".toRegex())) {
                    // 如果人名完全符合 xx-yyy? 或 xx 的形式，则主动抛弃

                    val split = name.split(REG_HYPHEN.toRegex())

                    val start = split.firstOrNull()?.toIntOrNull()
                    val end = if (split.size == 2) split.lastOrNull()?.toIntOrNull() else null

                    return CmdRange(getOsuUser(bindDao.getBindFromQQ(event.sender.id), mode.data), start, end)
                }

                val split2 = name.trim().split("\\s+".toRegex())

                if (split2.size == 2 && split2.last().matches("100|\\d{1,2}".toRegex())) {
                    // 如果人名符合 www xx 的形式，则选择将后面的视为数字
                    return CmdRange(getOsuUser(split2.first(), mode.data), split2.last().toInt(), null)
                }
            }

            return CmdRange(getUserWithoutRange(event, matcher, mode, isMyself))
        } catch (e: Exception) {
            if (isMyself.get() && isAvoidance(text, *ignores)) {
                throw LogException("退避指令 $ignores")
            } else {
                throw e
            }
        }
    }

    /**
     * 前四个参数同 [getUserWithRange]
     *
     * @param text 命令消息文本
     * @param ignores 需要避免的指令
     */
    @JvmStatic fun getUserAndRangeWithBackoff(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean,
        text: String,
        vararg ignores: String,
    ): CmdRange<OsuUser> {
        try {
            return getUserWithRange(event, matcher, mode, isMyself)
        } catch (e: BindException) {
            if (isMyself.get() && isAvoidance(text, *ignores)) throw LogException("退避指令 $ignores")
            throw e
        }
    }

    /**
     * 获取命令中的 用户 和 range, 不包括 自己的QQ/at
     *
     * @param matcher 正则
     * @param mode 包装的模式, 如果给的 mode 非默认则返回对应 mode 的 user 信息
     * @throws [GeneralTipsException.Type.G_Null_Player] 当输入字符串包含用户名, 并且查找后无此人时抛出
     */
    @Throws(GeneralTipsException::class) private fun getUserAndRange(
        matcher: Matcher,
        mode: CmdObject<OsuMode>
    ): CmdRange<OsuUser> {
        require(matcher.namedGroups().containsKey(FLAG_USER_AND_RANGE)) { "Matcher 中不包含 ur 分组" }
        if (mode.data == null) {
            mode.data = OsuMode.DEFAULT
        }

        val text: String = (matcher.group(FLAG_USER_AND_RANGE) ?: "").trim()
        if (text.isBlank()) {
            return CmdRange()
        }

        if (JUST_RANGE.matcher(text).matches()) {
            val range = parseRange(text)
            if (range[0] != null && range[1] == null && checkRangeFirst(range[0]!!)) {
                try {
                    val name = range[0].toString()

                    val bindMode = try {
                        bindDao.getBindUser(name).osuMode
                    } catch (e: Exception) {
                        OsuMode.DEFAULT
                    }

                    val id = userApiService.getOsuId(name)
                    val user = getOsuUser(id, checkOsuMode(mode, bindMode))
                    return CmdRange(user)
                } catch (_: Exception) {

                }
            }
            return CmdRange(null, range[0], range[1])
        } // -1 才是没找到
        val ranges = if (text.indexOf(CHAR_HASH) >= 0 || text.indexOf(CHAR_HASH_FULL) >= 0) {
            parseNameAndRangeHasHash(text)
        } else {
            parseNameAndRangeWithoutHash(text)
        }

        var result = CmdRange<OsuUser>()
        for (range in ranges) {
            try {
                if (range.data == null) {
                    result = CmdRange(null, range.start, range.end)
                    break
                }

                val bindMode = try {
                    bindDao.getBindUser(range.data).osuMode
                } catch (e: Exception) {
                    OsuMode.DEFAULT
                }

                val id = userApiService.getOsuId(range.data)
                val user = getOsuUser(id, checkOsuMode(mode, bindMode))
                result = CmdRange(user, range.start, range.end)
                break
            } catch (ignore: Exception) { // 其余的忽略
            }
        }

        // 使其顺序
        if (result.end != null && result.start != null && result.start!! > result.end!!) {
            val temp = result.start
            result.start = result.end
            result.end = temp
        }

        if (result.data.isNull()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, text)
        }
        return result
    }

    /** 内部方法, 解析'#'后的 range */
    private fun parseNameAndRangeHasHash(text: String): LinkedList<CmdRange<String>> {
        val ranges = LinkedList<CmdRange<String>>()
        var hashIndex: Int = text.indexOf(CHAR_HASH)
        if (hashIndex < 0) hashIndex = text.indexOf(CHAR_HASH_FULL)
        var nameStr: String? = text.substring(0, hashIndex).trim()
        if (nameStr.isNullOrBlank()) nameStr = null
        val rangeStr = text.substring(hashIndex + 1).trim()
        val rangeInt = parseRange(rangeStr)
        ranges.add(CmdRange(nameStr, rangeInt[0], rangeInt[1]))
        return ranges
    }

    /** 内部方法, 解析 name 与 range */
    private fun parseNameAndRangeWithoutHash(text: String): LinkedList<CmdRange<String>> {
        val ranges = LinkedList<CmdRange<String>>()
        var tempRange = CmdRange(text, null, null) // 保底 只有名字
        ranges.push(tempRange)
        var index = text.length - 1
        var i = 0
        var tempChar = '0' // 第一个 range
        while (index >= 0 && isNumber(text[index].also { tempChar = it })) {
            index--
            i++
        }

        // 对于 末尾无数字 / 数字大于3位 / 实际名称小于最小值 认为无 range
        if (i <= 0 || i > 3 || index < OSU_MIN_INDEX) {
            return ranges
        }
        val rangeN = text.substring(index + 1).toInt()
        tempRange = CmdRange(text.substring(0, index + 1).trim(), rangeN, null)
        ranges.push(tempRange)
        if (tempChar != '-' && tempChar != '－' && tempChar != ' ') { // 对应末尾不是 - 或者 空格, 直接忽略剩余 range
            // 优先认为紧贴的数字是名字的一部分, 也就是目前结果集的第一个
            tempRange = ranges.pollLast()
            ranges.push(tempRange)
            return ranges
        }

        do {
            index--
        } while (text[index] == ' ')

        // 第二组数字
        i = 0

        while (index >= 0 && isNumber(text[index].also { tempChar = it })) {
            index--
            i++
        }

        if (i <= 0 || i > 3 || index < OSU_MIN_INDEX) { // 与上面同理
            return ranges
        }

        tempRange = CmdRange(
            text.substring(0, index + 1).trim(),
            rangeN,
            text.substring(index + 1, index + i + 1).toInt(),
        )

        if (tempChar != ' ') { // 优先认为紧贴的数字是名字的一部分, 交换位置
            val temp = ranges.poll()
            ranges.push(tempRange)
            ranges.push(temp)
        } else {
            ranges.push(tempRange)
        }

        return ranges
    }

    /** 内部方法 */
    @JvmStatic private fun parseRange(text: String): Array<Int?> {
        val rangeInt = arrayOf<Int?>(null, null)

        try {
            val range = text.removePrefix(CHAR_HASH.toString()).removePrefix(CHAR_HASH_FULL.toString()).trim()
                .split(SPLIT_RANGE).dropLastWhile { it.isEmpty() }.map { it.toInt() }.toTypedArray()
            if (range.size >= 2) {
                rangeInt[0] = range[0]
                rangeInt[1] = range[1]
            } else if (range.size == 1) {
                rangeInt[0] = range[0]
            }
        } catch (e: Exception) {
            log.debug("range 解析参数有误: {}", text, e)
        }

        return rangeInt
    }

    private fun isNumber(c: Char): Boolean {
        return c in '0'..'9'
    }

    /**
     * 内部方法 获取玩家信息, 优先级为 at > qq= > uid= > name, 不处理自身绑定, 如果传入 mode 为 default, 同时是 @qq 绑定, 则改为绑定的模式,
     * 否则就是对应用户的官网主模式 at / qq / uid / name。都没有找到，就返回一个没有 uid，只有 username 的 osuUser
     *
     * @param mode 如果是非默认模式则使用该模式查询 user
     */
    @Throws(TipsException::class) private fun getOsuUser(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
    ): OsuUser? {

        val qq = if (event.isAt) {
            event.target
        } else if (matcher.namedGroups().containsKey(FLAG_QQ_ID)) {
            try {
                matcher.group(FLAG_QQ_ID)?.toLong() ?: 0L
            } catch (ignore: RuntimeException) {
                0L
            }
        } else {
            0L
        }

        if (qq != 0L) {
            val bind = bindDao.getBindFromQQ(qq)
            return getOsuUser(bind, checkOsuMode(mode, bind.osuMode))
        }

        if (matcher.namedGroups().containsKey(FLAG_UID)) {
            try {
                val uid = matcher.group(FLAG_UID)?.toLong() ?: 0L
                if (uid != 0L) {
                    return getOsuUser(uid, mode.data)
                }
            } catch (ignore: RuntimeException) {
            }
        }

        if (matcher.namedGroups().containsKey(FLAG_NAME)) {
            val name: String = matcher.group(FLAG_NAME) ?: ""
            if (name.isBlank().not()) {
                return getOsuUser(name, mode.data)
            }
        }

        return null
    }

    /**
     * 获取 user 信息
     *
     * @param user 绑定
     * @param mode 指定模式
     */
    @Throws(TipsException::class) fun getOsuUser(user: BindUser, mode: OsuMode?): OsuUser {
        return getOsuUser(user.osuName, user.osuID) { userApiService.getPlayerInfo(user, mode) }
    }

    /**
     * 获取 user 信息
     *
     * @param name 用户名
     * @param mode 指定模式
     */
    @Throws(TipsException::class) fun getOsuUser(name: String, mode: OsuMode?): OsuUser {
        return getOsuUser(name) { userApiService.getPlayerInfo(name, mode) }
    }

    /**
     * 获取 user 信息
     *
     * @param uid 用户ID
     * @param mode 指定模式
     */
    @Throws(TipsException::class) fun getOsuUser(uid: Long, mode: OsuMode?): OsuUser {
        return getOsuUser("玩家 $uid", uid) { userApiService.getPlayerInfo(uid, mode) }
    }

    /** 内部方法 封装获取 user 的方法, 包装出现的异常 */
    @Throws(TipsException::class) private fun <T> getOsuUser(
        name: String,
        uid: Long? = null,
        consumer: Supplier<T>
    ): T {
        try {
            return consumer.get()
        } catch (e: WebClientResponseException.NotFound) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, name)
        } catch (e: WebClientResponseException.Forbidden) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, name)
        } catch (e: WebClientResponseException.Unauthorized) {
            uid?.let(bindDao::removeBind)
            throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Cancel)
        } catch (e: WebClientResponseException) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI)
        } catch (e: Exception) {
            log.error("HandleUtil：玩家信息获取失败！", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_PlayerInfo)
        }
    }

    /** 判断是否包含避讳指令 */
    @JvmStatic fun isAvoidance(text: String, vararg cmd: String): Boolean {
        for (c in cmd) {
            if (text.contains(c)) return true
        }
        return false
    }

    /**
     * 获取一个包装的 mode 传入的 mode 如果不是 default 且命令中没显式指定 mode 则覆盖掉结果 单纯为了java添加的重载方法, 可以不指定, 所以这里没有这个参数
     */
    @JvmStatic fun getMode(matcher: Matcher): CmdObject<OsuMode> {
        val result = CmdObject(OsuMode.DEFAULT)
        if (matcher.namedGroups().containsKey(FLAG_MODE)) {
            result.data = OsuMode.getMode(matcher.group(FLAG_MODE) ?: "")
        }
        return result
    }

    /** 获取一个包装的 mode 传入的 mode 如果不是 default 且命令中没显式指定 mode 则覆盖掉结果 */
    @JvmStatic fun getMode(matcher: Matcher, other: OsuMode = OsuMode.DEFAULT): CmdObject<OsuMode> {
        val result = getMode(matcher)
        if (OsuMode.isDefaultOrNull(result.data) && OsuMode.isNotDefaultOrNull(other)) {
            result.data = other
        }
        return result
    }

    /** 从正则中提取 bid */
    @JvmStatic fun getBid(matcher: Matcher): Long {
        if (!matcher.namedGroups().containsKey(FLAG_BID)) {
            return 0
        }
        return matcher.group(FLAG_BID)?.toLong() ?: 0
    }

    /** 从正则中提取mod (结果为字符串) */
    @JvmStatic fun getMod(matcher: Matcher): List<LazerMod> {
        if (!matcher.namedGroups().containsKey(FLAG_MOD)) {
            return listOf()
        }
        return LazerMod.getModsList(matcher.group(FLAG_MOD) ?: "")
    }

    /** 将 [LazerScore]列表转换为 indexMap 注意, 这里 index 从 1 开始 */
    @JvmStatic fun processBP(bp: List<LazerScore>): Map<Int, LazerScore> {
        val result = TreeMap<Int, LazerScore>()
        bp.forEachIndexed { index, score -> result[index + 1] = score }
        return result
    }

    /** 用于 default mode 覆盖 */
    @JvmStatic fun checkOsuMode(mode: CmdObject<OsuMode>, other: OsuMode?): OsuMode {
        if (OsuMode.isDefaultOrNull(mode.data) && OsuMode.isNotDefaultOrNull(other)) {
            mode.data = other
        }
        return mode.data ?: OsuMode.DEFAULT
    }

    private const val OSU_MIN_INDEX = 2

    private val SPLIT_RANGE = "[\\-－ ]".toRegex()
    private val JUST_RANGE: Pattern = Pattern.compile("^\\s*$REG_HASH?\\s*(\\d{1,3}[\\-－ ]+)?\\d{1,3}\\s*$")
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
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

    private fun checkRangeFirst(i: Int): Boolean {
        return when {
            i < 101 -> false
            i == 666 -> true
            i < 999 -> true
            else -> false
        }
    }
}

/** 包装类, 貌似只 mode 用到了 */
data class CmdObject<T>(var data: T? = null)

/** 包装类, 记录包括 range 结果 */
data class CmdRange<T>(var data: T? = null, var start: Int? = null, var end: Int? = null) {
    // 设置 ZeroToRange 时生效
    private var rangeZero = false

    private fun halfRange() = start != null && end == null

    private fun fullRange() = start != null && end != null

    fun setZeroToRange100() {
        rangeZero = (start == 0 && end == null)
    }

    fun setZeroDay() {
        if (start == 0 && end == null) {
            end = 999
        }
    }

    // 30: 30 - 1, 2-30: 2-1  // 30: 0, 2-30: 2-1
    fun getOffset(default: Int = 0, isMulti: Boolean = false): Int {
        if (rangeZero) return 0
        return if (fullRange()) {
            max(0, start!! - 1)
        } else if (halfRange()) {
            if (isMulti) {
                0
            } else {
                max(0, start!! - 1)
            }
        } else {
            default
        }
    }

    // 30: 1, 2-30: 30-1  // 30: 30, 2-30: 30-1
    fun getLimit(default: Int = 1, isMulti: Boolean = false): Int {
        if (rangeZero) {
            return 100
        }
        return if (fullRange()) {
            max(end!! - max(start!!, 1) + 1, 1)
        } else if (halfRange()) {
            if (isMulti) {
                max(start!!, 1)
            } else {
                1
            }
        } else {
            default
        }
    }

    // 30: 0, 2-30：1, 32-30：29
    fun getDayStart(default: Int = 0): Int {
        return if (fullRange()) {
            max(min(start!!, end!!) - 1, default)
        } else {
            default
        }
    }

    // 30: 30, 2-30：30, 32-30：32
    fun getDayEnd(default: Int = 1): Int {
        return if (fullRange()) {
            max(max(start!!, end!!), default)
        } else if (halfRange()) {
            max(start!!, default)
        } else {
            default
        }
    }
}
