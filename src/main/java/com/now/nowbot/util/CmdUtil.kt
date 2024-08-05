package com.now.nowbot.util

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.AtMessage
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.LogException
import com.now.nowbot.throwable.ServiceException.BindException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern

object CmdUtil {
    /**
     * 获取玩家信息, 末尾没有 range
     */
    @JvmStatic
    @Throws(TipsException::class)
    fun getUserWithOutRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean,
    ): OsuUser? {
        isMyself.set(false)
        var userObj = getOsuUser(event, matcher, mode)
        if (Objects.isNull(userObj)) {
            isMyself.set(true)
            val bind = bindDao!!.getUserFromQQ(event.sender.id)
            checkOsuMode(mode, bind.osuMode)
            userObj = userApiService!!.getPlayerInfo(bind, mode.data)
        }
        return userObj
    }

    /**
     * 解析包含 @qq / username / uid= / qq= 的玩家信息, 并且解决后面的 range
     * @param mode 一个可以改变的 mode, 解决当获取到 default, 修改成用户默认绑定时的 mode 无法传出的问题
     * @param isMyself 传入一个 [AtomicBoolean] 如果是自己则结果为 true (注意, 当包含 qq= 或 uid= 时, 即使是发送者本身也是 false)
     */
    @JvmStatic
    @Throws(TipsException::class)
    fun getUserWithRange(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean
    ): CmdRange<OsuUser> {
        isMyself.set(false)
        val range = getUserAndRange(matcher, mode)
        if (Objects.isNull(range.data)) {
            range.data = getUserWithOutRange(event, matcher, mode, isMyself)
        }
        return range
    }

    /**
     * 前四个参数同 [getUserWithRange]
     * @param text 命令消息文本
     * @param cmd 需要避免的指令
     */
    @JvmStatic
    fun getUserAndRangeWithBackoff(
        event: MessageEvent,
        matcher: Matcher,
        mode: CmdObject<OsuMode>,
        isMyself: AtomicBoolean,
        text: String,
        vararg cmd: String,
    ): CmdRange<OsuUser> {
        try {
            return getUserWithRange(event, matcher, mode, isMyself)
        } catch (e: BindException) {
            if (isMyself.get() && isAvoidance(text, *cmd)) throw LogException("指令退避")
            throw e
        }
    }

    private fun getUserAndRange(matcher: Matcher, mode: CmdObject<OsuMode>): CmdRange<OsuUser> {
        require(matcher.namedGroups().containsKey(FLAG_USER_AND_RANGE)) { "Matcher 中不包含 ur 分组" }
        if (Objects.isNull(mode.data)) {
            mode.data = OsuMode.DEFAULT
        }

        val text: String = matcher.group(FLAG_USER_AND_RANGE) ?: ""
        if (text.isBlank()) {
            return CmdRange()
        }
        if (JUST_RANGE.matcher(text).matches()) {
            val range = parseRange(text)
            return CmdRange(null, range[0], range[1])
        }
        val ranges = if (text.indexOf(CHAR_HASH) > 0 || text.indexOf(CHAR_HASH_FULL) > 0) {
            parseNameAndRangeHasHash(text)
        } else {
            parseNameAndRangeWithoutHash(text)
        }

        var result = CmdRange<OsuUser>()
        for (range in ranges) {
            try {
                val id = userApiService!!.getOsuId(range.data)
                val user = getOsuUser(id, mode.data)
                result = CmdRange(user, range.start, range.end)
                break
            } catch (ignore: Exception) {
            }
        }

        // 使其顺序
        if (Objects.nonNull(result.end) &&
            Objects.nonNull(result.start) &&
            result.start!! > result.end!!
        ) {
            val temp = result.start
            result.start = result.end
            result.end = temp
        }
        return result
    }

    private fun parseNameAndRangeHasHash(text: String): LinkedList<CmdRange<String>> {
        val ranges = LinkedList<CmdRange<String>>()
        var hashIndex: Int = text.indexOf(CHAR_HASH)
        if (hashIndex < 0) hashIndex = text.indexOf(CHAR_HASH_FULL)
        var nameStr: String? = text.substring(0, hashIndex).trim { it <= ' ' }
        if (!StringUtils.hasText(nameStr)) nameStr = null
        val rangeStr = text.substring(hashIndex + 1).trim { it <= ' ' }
        val rangeInt = parseRange(rangeStr)
        ranges.add(
            CmdRange(
                nameStr, rangeInt[0], rangeInt[1]
            )
        )
        return ranges
    }

    private fun parseNameAndRangeWithoutHash(text: String): LinkedList<CmdRange<String>> {
        val ranges = LinkedList<CmdRange<String>>()
        var tempRange = CmdRange(text, null, null)
        // 保底 只有名字
        ranges.push(tempRange)
        var index = text.length - 1
        var i = 0
        var tempChar: Char
        // 第一个 range
        while (isNumber(text[index].also { tempChar = it })) {
            index--
            i++
        }

        // 对于 末尾无数字 / 数字大于3位 / 实际名称小于最小值 认为无 range
        if (i <= 0 || i > 3 || index < OSU_MIN_INDEX) {
            return ranges
        }
        val rangeN = text.substring(index + 1).toInt()
        tempRange = CmdRange(
            text.substring(0, index + 1).trim(),
            rangeN,
            null
        )
        ranges.push(tempRange)
        if (tempChar != '-' && tempChar != '－' && tempChar != ' ') {
            // 对应末尾不是 - 或者 空格, 直接忽略剩余 range
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

        while (isNumber(text[index].also { tempChar = it })) {
            index--
            i++
        }

        if (i <= 0 || i > 3 || index < OSU_MIN_INDEX) {
            // 与上面同理
            return ranges
        }

        tempRange = CmdRange(
            text.substring(0, index + 1).trim(),
            rangeN,
            text.substring(index + 1, index + i + 1).toInt()
        )

        if (tempChar != ' ') {
            // 优先认为紧贴的数字是名字的一部分, 交换位置
            val ttmp = ranges.poll()
            ranges.push(tempRange)
            ranges.push(ttmp)
        } else {
            ranges.push(tempRange)
        }

        return ranges
    }

    private fun parseRange(text: String): Array<Int?> {
        val rangeInt = arrayOf<Int?>(null, null)

        try {
            val range = text.split(SPLIT_RANGE)
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            if (range.size >= 2) {
                rangeInt[0] = range[range.size - 2].toInt()
                rangeInt[1] = range[range.size - 1].toInt()
            } else if (range.size == 1) {
                rangeInt[0] = range[0].toInt()
            }
        } catch (e: Exception) {
            log.debug("range 解析参数有误: {}", text, e)
        }

        return rangeInt
    }

    private fun isNumber(c: Char): Boolean {
        return c in '0'..'9'
    }

    @Throws(TipsException::class)
    private fun getOsuUser(event: MessageEvent, matcher: Matcher, mode: CmdObject<OsuMode>): OsuUser? {
        val at = QQMsgUtil.getType(event.message, AtMessage::class.java)

        var qq: Long = 0
        if (Objects.nonNull(at)) {
            qq = at!!.target
        } else if (matcher.namedGroups().containsKey(FLAG_QQ_ID)) {
            try {
                qq = matcher.group(FLAG_QQ_ID)?.toLong() ?: 0
            } catch (ignore: RuntimeException) {
            }
        }

        if (qq != 0L) {
            val bind = bindDao!!.getUserFromQQ(qq)
            return getOsuUser(bind, checkOsuMode(mode, bind.osuMode))
        }

        var uid: Long = 0
        if (matcher.namedGroups().containsKey(FLAG_UID)) {
            try {
                uid = matcher.group(FLAG_UID)?.toLong() ?: 0
            } catch (ignore: RuntimeException) {
            }
            if (uid != 0L) return getOsuUser(uid, mode.data)
        }

        if (matcher.namedGroups().containsKey(FLAG_NAME)) {
            val name: String = matcher.group(FLAG_NAME) ?: ""
            if (StringUtils.hasText(name)) return getOsuUser(name, mode.data)
        }
        return null
    }

    @Throws(TipsException::class)
    fun getOsuUser(user: BinUser, mode: OsuMode?): OsuUser {
        return getOsuUser({ userApiService!!.getPlayerInfo(user, mode) }, user.osuID)
    }

    @Throws(TipsException::class)
    fun getOsuUser(name: String, mode: OsuMode?): OsuUser {
        return getOsuUser({ userApiService!!.getPlayerInfo(name, mode) }, name)
    }

    @Throws(TipsException::class)
    fun getOsuUser(uid: Long, mode: OsuMode?): OsuUser {
        return getOsuUser({ userApiService!!.getPlayerInfo(uid, mode) }, uid)
    }

    @Throws(TipsException::class)
    private fun <T> getOsuUser(consumer: Supplier<T>, tips: Any): T {
        try {
            return consumer.get()
        } catch (e: WebClientResponseException.NotFound) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, tips.toString())
        } catch (e: WebClientResponseException.Forbidden) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, tips.toString())
        } catch (e: WebClientResponseException) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI)
        } catch (e: Exception) {
            log.error("HandleUtil：获取玩家信息失败！", e)
            throw TipsException("获取玩家信息失败！")
        }
    }

    @JvmStatic
    fun isAvoidance(text: String, vararg cmd: String): Boolean {
        for (c in cmd) {
            if (text.contains(c)) return true
        }
        return false
    }

    /**
     * 获取一个包装的 mode, 方便用户绑定 bot 的主 mode 覆盖 default
     */
    @JvmStatic
    fun getMode(matcher: Matcher): CmdObject<OsuMode> {
        val result = CmdObject(OsuMode.DEFAULT)
        if (matcher.namedGroups().containsKey(FLAG_MODE)) {
            result.data = OsuMode.getMode(matcher.group(FLAG_MODE) ?: "")
        }
        return result
    }
    @JvmStatic
    fun getMode(matcher: Matcher, other: OsuMode = OsuMode.DEFAULT): CmdObject<OsuMode> {
        val result = getMode(matcher)
        if (OsuMode.isDefaultOrNull(result.data) && !OsuMode.isDefaultOrNull(other)) {
            result.data = other
        }
        return result
    }

    @JvmStatic
    fun getBid(matcher: Matcher): Long {
        if (!matcher.namedGroups().containsKey(FLAG_BID)) {
            return 0
        }
        return matcher.group(FLAG_BID)?.toLong() ?: 0
    }

    @JvmStatic
    fun getMod(matcher: Matcher): String {
        if (!matcher.namedGroups().containsKey(FLAG_MOD)) {
            return ""
        }
        return matcher.group(FLAG_MOD) ?: ""
    }

    @JvmStatic
    fun processBP(bp: Iterable<Score>): Map<Int, Score> {
        val result = TreeMap<Int, Score>()
        bp.forEachIndexed { index, score ->
            result[index + 1] = score
        }
        return result
    }

    @JvmStatic
    fun checkOsuMode(mode: CmdObject<OsuMode>, other: OsuMode): OsuMode {
        if (OsuMode.isDefaultOrNull(mode.data) && !OsuMode.isDefaultOrNull(other)) {
            mode.data = other
        }
        return mode.data ?: OsuMode.DEFAULT
    }

    private const val OSU_MIN_INDEX = 2

    private val SPLIT_RANGE = "[\\-－ ]".toRegex()
    private val JUST_RANGE: Pattern = Pattern.compile("^\\s*(\\d{1,2}[\\-－ ]+)?\\d{1,3}\\s*$")
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private var bindDao: BindDao? = null
    private var userApiService: OsuUserApiService? = null
    private var scoreApiService: OsuScoreApiService? = null
    private var beatmapApiService: OsuBeatmapApiService? = null

    @JvmStatic
    fun init(applicationContext: ApplicationContext) {
        bindDao = applicationContext.getBean(BindDao::class.java)
        userApiService = applicationContext.getBean(OsuUserApiService::class.java)
        scoreApiService = applicationContext.getBean(OsuScoreApiService::class.java)
        beatmapApiService = applicationContext.getBean(OsuBeatmapApiService::class.java)
    }
}

data class CmdObject<T>(var data: T? = null)
data class CmdRange<T>(var data: T? = null, var start: Int? = null, var end: Int? = null) {
    fun allNull() = start == null && end == null
    fun getValue(default: Int = 20, important: Boolean) = if (start != null && end != null) {
        if (important) {
            start!!
        } else {
            end!!
        }
    } else if (important && start != null) {
        start!!
    } else if (important && end != null) {
        end!!
    } else {
        default
    }
}