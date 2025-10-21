package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.DiceService.DiceParam
import com.now.nowbot.service.messageServiceImpl.DiceService.Split.*
import com.now.nowbot.throwable.botRuntimeException.DiceException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_COLON
import com.now.nowbot.util.command.REG_NUMBER_DECIMAL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.*
import kotlin.random.Random

@Service("DICE") class DiceService : MessageService<DiceParam> {
    // dice：骰子次数，默认为 1
    data class DiceParam(val dice: Long?, val number: Long?, val text: String?)

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<DiceParam>,
    ): Boolean {
        val m3 = Instruction.EASTER_WHAT.matcher(messageText)
        if (m3.find()) {
            throw DiceException.ForWhat()
        }

        val m2 = Instruction.EASTER_AYACHI_NENE.matcher(messageText)
        if (m2.find()) {
            throw DiceException.JerkOff()
        }

        val m = Instruction.DICE.matcher(messageText)
        if (!m.find()) return false

        val dice = m.group("dice")
        val number = m.group("number")
        val text = m.group("text")

        if (text.isNullOrBlank().not()) { // 如果 dice 有符合，但是并不是 1，选择主动忽视
            if (dice.isNullOrBlank().not() && (dice.toLongOrNull() ?: return false) > 1L) {
                return false
            } else if (number.isNullOrBlank().not()) {
                data.value = DiceParam(null, null, (number + text).trim())
                return true
            } else if (text.trim().matches("^$REG_NUMBER_DECIMAL$".toRegex())) {
                // !roll 4
                data.value = DiceParam(1L, text.toLongOrNull() ?: 100L, null)
                return true
            } else {
                data.value = DiceParam(null, null, text.trim())
                return true
            }
        } else if (dice.isNullOrBlank().not()) {
            val d: Long = dice.toLongOrNull()
                ?: dice.toDoubleOrNull()?.toLong() ?: throw DiceException.Exceed()

            // 如果 dice 有符合，但是是 0，选择主动忽视（0d2）
            if (d < 1) {
                throw DiceException.TooSmall()
            } else if (d > 100L) {
                throw DiceException.DiceTooMany(d)
            }

            val n = if (number.isNullOrBlank().not()) {
                if (number.contains("-")) {
                    throw DiceException.Negative()
                }

                number.toLongOrNull()
                    ?: number.toDoubleOrNull()?.coerceAtLeast(1.0)?.toLong()
                    ?: throw DiceException.Exceed()
            } else {
                100L
            }

            data.value = DiceParam(d, n, null)
            return true
        } else if (number.isNullOrBlank().not()) {

            val n = number.toLongOrNull() ?: throw DiceException.Exceed()

            if (n < 0L) {
                throw DiceException.Negative()
            }

            data.value = DiceParam(1L, n, null)
            return true
        } else {
            data.value = DiceParam(1L, 100L, null)
            return true
        }

        // throw DiceException(DiceException.Type.DICE_Instruction);
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: DiceParam): ServiceCallStatistic? {
        try {
            if (param.number != null) {
                if (param.number >= Int.MAX_VALUE) {
                    throw DiceException.TooLarge()
                }

                if (param.number < 1L) {
                    throw DiceException.TooSmall()
                }

                // 单次匹配 !d, 1d100 和多次匹配 20d100
                if (param.dice == 1L || param.dice == null) {
                    val r = getRandom<Long?>(param.number)
                    val format = if ((r < 1f)) "%.2f" else "%.0f"
                    val result = String.format(format, r)

                    val receipt = event.reply(result)

                    // 容易被识别成 QQ
                    if (r in 1000000.0..<1000000000.0) {
                        receipt.recallIn((60 * 1000).toLong())
                    }

                    return ServiceCallStatistic.building(event) {
                        setParam(
                            mapOf(
                                "src" to event.rawMessage.trim(),
                                "dices" to listOf(result)
                            )
                        )
                    }
                } else {
                    val sb = StringBuilder()
                    val dices = ArrayList<String>(param.dice.toInt())

                    for (i in 1L..param.dice) {
                        val r = getRandom(param.number)
                        val format = if ((r < 1f)) "%.2f" else "%.0f"
                        val result = String.format(format, r)
                        dices += result

                        sb.append(result)

                        if (i != param.dice) {
                            sb.append(", ")
                        }
                    }

                    event.reply(sb.toString())
                    return ServiceCallStatistic.building(event) {
                        setParam(
                            mapOf(
                                "src" to event.rawMessage.trim(),
                                "dices" to dices
                            )
                        )
                    }
                }
            }

            if (param.text.isNullOrBlank().not()) {
                val message = compare(param.text)

                // 用于匹配是否被和谐
                val harmonised = "○|(\\[和谐])".toRegex()
                if (message.contains(harmonised)) { // 被和谐就撤回
                    event.reply(message).recallIn((60 * 1000).toLong())
                } else {
                    event.reply(message)
                }

                return ServiceCallStatistic.building(event) {
                    setParam(
                        mapOf(
                            "src" to event.rawMessage.trim(),
                            "select" to message
                        )
                    )
                }
            }
        } catch (e: DiceException) {
            throw e
        } catch (e: Exception) {
            log.error("扔骰子：处理失败", e)
            throw DiceException.Unexpected()
        }

        return ServiceCallStatistic.building(event)
    }

    internal enum class Split(val pattern: Pattern, val onlyC3: Boolean) {
        TIME(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>多久|((几多?|多[少长]|什么|啥|哪个|何)(时[候间]|个?(年|月|周|日子?|天|分钟?|小?时|钟[头点]|柱香|时辰|[毫微纳]秒))|几点)(几?何|之?[后内])?|when(?!\\w))(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ), // 皮秒 飞秒

        RANK(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>(第几)[次个位件名只]?)(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ), // 第xx

        TIMES(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>(((?<![一两二三四五六七八九十百千万亿这那上下哪点排报成出提命匿爆真假实大小])(几多?|多少|什么|啥|哪个|何)?(频率|无数)[次个位件名只发层人枚字章节分指大小级科]数?)|第几[次个位件名只发层人枚字章节分指大小级科]?)(之?[后内])?)(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ), // 次数

        POSSIBILITY(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>((有多[少大])?的?([概几]率是?|可能[是性]?))|\\s(chance|possib(l[ey]|ility)(\\sis)?)\\s)(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        ACCURACY(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>((有多[少大])?的?((准确|概|几)率是?|[准精]度)|\\s?(acc(uracy)?)(\\sis)?)\\s?)(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        AGE(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>(岁数?)|(年龄)|(?<!\\w)age(?!\\w))(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        AMOUNT(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>(([是有]?多少[次个位件名只发层岁人枚字章节]?)|(数量(?!级))|((?<![一两二三四五六七八九十百千万亿这那上下哪点排报成出提命匿爆真假实])([次个位件名只发层人枚字章节分指大小级科]数))|(?<![茶未无刀知万方])几))(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // A和B比谁更C？
        // 正常选择
        // 当然选 X 啦！
        BETTER(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*)\\s*(?<c2>(跟|和|与|并|\\s(?<![A-Za-z])(and|or|with)(?![A-Za-z])\\s))\\s*(?<m2>[\\S\\s]*?)\\s*(?<![叠面傻装牛菜想提分排开小对阿科此豆死伦攀反字])比?(比[，,\\s]*?哪个|比[，,\\s]*?谁|哪个|谁)更?(?<c3>[\\S\\s]*)"
            ), onlyC3 = false
        ),

        // 比分 2比4
        SCORE(
            Pattern.compile(
                "\\s*(?<m1>$REG_NUMBER_DECIMAL)\\s*(?<c2>((?<![叠面傻装牛菜想提分排开小对阿科此豆死伦攀反字])比)|$REG_COLON)(?<m2>$REG_NUMBER_DECIMAL)"
            ), onlyC3 = false
        ),

        // A比B厉害？
        // 正常选择
        // 当然选 A 啦！，当然是 B 啦！（B厉害，这里分不开）
        COMPARE(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*)[，,\\s]*?(?<![叠面傻装牛菜想提分排开小对阿科此豆死伦攀反字])(?<c2>(比(?![赛如比拟重邻值及照目价例试上下肩方对分热画划类舍武翼意义喻作基利天推量年萨勒葫芦集速时势特体]|$)较?|(\\scompare(\\sto)?\\s)))[，,\\s]*?(?<m2>[\\S\\s]*)"
            ), onlyC3 = false
        ),

        // 选A，还是选B？
        // 正常选择
        // 当然选 X 啦！
        OR(
            Pattern.compile(
                "\\s*(?<c1>(不?是|要么|是要?)(选?[择中好]?了?)?)?\\s*(?<m1>[\\S\\s]*)[，,\\s]*?(?<c2>([：:]|[还就而]是|and|(?<![A-Za-z])or(?![A-Za-z])|或|或者|要么)(选?[择中好]?了?)?)\\s*(?<m2>[\\S\\s]*)"
            ), onlyC3 = false
        ),

        // 是不是   排除 爱A不A
        // A是。A不是。
        WHETHER(
            Pattern.compile(
                "\\s*(?<m1>[^不]*(?<!爱))?\\s*(?<c2>[\\S\\s])(?<m3>[不没])(?<c3>[\\S\\s])[人个件位条匹颗根辆]?\\s*(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 你谁？
        // 我是 YumuBot
        AM(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>你是谁?|\\bwho\\b\\s*\\b('re|are)\\b)(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 是什么？ // TODO 同时也加了 DO 的部分。以后这里可以抽象成一个单独的模块，用来推荐歌曲、抽卡、抽每日、抽老婆。
        // 我怎么知道。是哈基米。
        WHAT(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*)?\\s*(?<c3>(?<!你们?|[要还哪那就])[是吃做干看玩买唱喝打听抽](([你我他她它祂]们?|别人)?谁|哪[个里处位天日]|什么歌?|啥|\\bwhat\\b))\\s*(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 为什么？
        // 我怎么知道。因为爱情。
        WHY(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*)?\\s*(?<c3>为(什么|何|啥)|\\bwhy\\b)\\s*(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 是谁？
        // 我怎么知道。是雪豹。
        WHO(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*)?\\s*(?<c3>谁是|是(谁|哪位|哪个)|\\bwho\\b\\s*\\b('s|is)\\b)\\s*(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 是，会，要吗？
        // 是。不是。
        IS(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*?)?\\s*?(?<c3>(?<![要还哪那就])((?<![一等开集机领误神社公工财理附利员法动应半倒标大相生体约庙云际照而融茶酒览话赴])(会不)?会|(?<![求只总如煞假利而熟皆老要凡既为倒先可])(是不)?是|(可不)?可以)吗?|\\bis\\b)\\s*?(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 有。没有。
        HAS(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*?)?\\s*?(?<c3>(?<![要还哪那就])((?<![求只总如煞假利而熟皆老要凡既为倒先可])(是不)?是|(?<![占己拥存国私仅乌还所尽其必具富稀不之保享岂凡总现])(有没)?有)吗?|\\bhas\\b)\\s*?(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 真的吗？
        // 我怎么知道。是真的。是假的。
        REAL(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*?)?\\s*?(?<c3>真的吗?|\\sreal(ly)?\\s)\\s*?(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 并列AB
        // 当然选 X 啦！
        JUXTAPOSITION(
            Pattern.compile(
                "\\s*(?<c1>(不仅|一边|一方面|有时|既)(选?[择中好]?了?)?)\\s*(?<m1>[\\S\\s]*)[，,\\s]*?(?<c2>(而且|一边|一方面|有时|又)(选?[择中好]?了?)?)\\s*(?<m2>[\\S\\s]*)"
            ), onlyC3 = false
        ),

        // 宁可A，也不B
        // 偏好A
        // 当然选 X 啦！
        PREFER(
            Pattern.compile(
                "\\s*(?<c1>(宁[可愿]|尽管)(选?[择中好]?了?)?)?\\s*(?<m1>[\\S\\s]*)[，,\\s]*?(?<c2>(也不[要想]?(选?[择中好]?了?)?))\\s*(?<m2>[\\S\\s]*)"
            ), onlyC3 = false
        ),

        // 与其A，不如B
        // 偏好B
        // 当然选 X 啦！
        HESITATE(
            Pattern.compile(
                "\\s*(?<c1>(与其|虽然|尽管)(选?[择中好]?了?)?)?\\s*(?<m1>[\\S\\s]*)[，,\\s]*?(?<c2>(还?不如|比不上|但是|可是|然而|却)(选?[择中好]?了?)?)\\s*(?<m2>[\\S\\s]*)"
            ), onlyC3 = false
        ),

        // 即使A，也B
        // 偏好当然
        // 当然B，不会B。
        EVEN(
            Pattern.compile(
                "\\s*(?<c1>(即使|\\seven\\sif\\s)((选?[择中好]?了?)?[择中好])?)?\\s*(?<m1>[\\S\\s]*)[，,\\s]*?([你我他她它祂]们?|别人)?(?<c2>([也还]会?)(选?[择中好]?了?)?)\\s*(?<m2>[\\S\\s]*)"
            ), onlyC3 = false
        ),

        // 我能
        COULD(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*?)\\s*?(?<c2>不)?\\s*?(?<c3>([想要]|想要|能[够否]?|可以|应该))\\s*(?<m2>[\\S\\s]*)"
            ), onlyC3 = true
        ),

        RANGE(
            Pattern.compile(
                "(?<m1>[大多高等小少低]于(等于)?|约等于?|超过|不足|[><]=?|[＞＜≥≤≡≈]|\\s(more|less)\\s(than)?\\s)(?<c3>[\\S\\s]*?)?\\s*(?<m2>\\d+)"
            ), onlyC3 = false
        ),

        // 假设A，才有B。
        // 我觉得 A 也没啥。// 没有如果。
        ASSUME(
            Pattern.compile(
                "\\s*(?<c1>(如果|假使|假设|要是|\\s(if|assume)\\s))\\s*(?<m1>[\\S\\s]*?)[，,\\s]*?(?<c2>(那?([你我他她它祂]们?|别人)?[会要想就便么才])|([想要]|想要|能够?|可以))\\s*(?<m2>([你我他她它祂]们?|别人)?[\\S\\s]*)"
            ), onlyC3 = false
        ),


        // A是B？
        // 确实。 //不对。
        CONDITION(
            Pattern.compile(
                "\\s*(?<c1>(只要|只有|无论|不管|忽略|忽视|不(去)?想|\\sif\\s))\\s*(?<m1>[\\S\\s]*)[，,\\s]*?(?<c2>(([你我他她它祂]们?|别人)?([就才都也还]能?|能)(够|是|可以)?|反正|依然))\\s*(?<m2>[\\S\\s]*)"
            ), onlyC3 = false
        ),

        LIKE(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*?)?\\s*?(?<c3>喜欢|((?<![可怜恋做被性])爱)|\\s((dis)?like|love)\\s)\\s*?(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 觉得
        // 嗯。也没有吧。
        THINK(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*)?\\s*(?<c2>[\\S\\s])(?<c3>(觉得|认为))\\s*(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        NEST(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(?<c3>[!！1]d)(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // ....吗？
        // ....。 不。
        QUESTION(
            Pattern.compile(
                "\\s*(?<m1>[\\S\\s]*?)?\\s*?(?<c3>(吗[?？]?)|([?？]))\\s*?(?<m2>[\\S\\s]*)?"
            ), onlyC3 = true
        ),

        // 用于匹配是否还有关联词
        MULTIPLE(
            Pattern.compile(
                "(?<m1>[\\S\\s]*)?(还是|或者?是?|与|\\s+)(?<m2>[\\S\\s]*)?"
            ), onlyC3 = false
        ),
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DiceService::class.java)

        /**
         * 超级复杂™的语言学选择器
         *
         * @param str 输入含有比较关系的文本
         * @return 返回随机一个子项
         * @throws DiceException 错
         */
        @JvmStatic @Throws(DiceException::class) fun compare(str: String?): String {
            val s = transferApostrophe(str)

            val result = getRandom()
            val boundary: Double
            var left = ""
            var right = ""
            var num = 0.0
            var iis = ""
            var not = ""
            var leftFormat: String
            var rightFormat: String
            var split: Split? = null

            for (sp in Split.entries) {
                val hasC3 = sp == BETTER || sp.onlyC3
                val matcher = sp.pattern.matcher(s)

                if (isPerfectMatch(matcher, hasC3, sp.onlyC3)) {
                    split = sp

                    left = matcher.group("m1")
                    right = matcher.group("m2")

                    when (split) {
                        RANGE -> {
                            val range =  right.toIntOrNull() ?: 100

                            num = if (range <= 0) {
                                throw DiceException.TooSmall()
                            } else if (range <= 100) {
                                getRandom(100)
                            } else if (range <= 10000) {
                                getRandom(10000)
                            } else if (range <= 1000000) {
                                getRandom(1000000)
                            } else {
                                throw DiceException.TooLarge()
                            }
                        }

                        AMOUNT, AGE -> num = getRandom(120)
                        TIME -> {
                            val c3 = matcher.group("c3").trim()

                            // 4% 触发彩蛋。
                            val cannot = arrayOf(
                                "不可能。",
                                "永远不会。",
                                "等鸡啄完了米，狗舔完了面，火烧断了锁...",
                                "等到世界末日了也没可能。",
                                "这辈子就别想了。",
                            )

                            if (getRandom(100) <= 4f) {
                                return cannot[getRandom(cannot.size).toInt() - 1]
                            }

                            if (c3.contains("年")) {
                                num = getRandom(100)
                                iis = "年"
                            } else if (c3.contains("月")) {
                                num = getRandom(12)
                                iis = "个月"
                            } else if (c3.contains("周")) {
                                num = getRandom(52)
                                iis = "周"
                            } else if (c3.contains("日") || c3.contains("天")) {
                                num = getRandom(30)
                                iis = "天"
                            } else if (c3.contains("时辰")) {
                                num = getRandom(12)
                                iis = "时辰"
                            } else if ((c3.contains("时") && !(c3.contains("时候") || c3.contains("时间"))) || c3.contains(
                                    "小时"
                                )) {
                                num = getRandom(24)
                                iis = "小时"
                            } else if (c3.contains("点")) {
                                num = getRandom(24)
                                iis = "点"
                            } else if (c3.contains("柱香")) {
                                num = getRandom(48)
                                iis = "柱香"
                            } else if (c3.contains("分")) {
                                num = getRandom(60)
                                iis = "分钟"
                            } else if (c3.contains("毫秒")) {
                                num = getRandom(1000)
                                iis = "毫秒"
                            } else if (c3.contains("微秒")) {
                                num = getRandom(1000000)
                                iis = "微秒"
                            } else if (c3.contains("纳秒")) {
                                num = getRandom(100000000)
                                iis = "纳秒"
                            } else if (c3.contains("秒")) {
                                num = getRandom(60)
                                iis = "秒"
                            } else {
                                // 未指定时间单位，比如多久

                                // 4% 触发彩蛋。
                                val soon = arrayOf(
                                    "马上。",
                                    "立刻。",
                                    "就现在。",
                                    "很久很久。",
                                    "一会儿。",
                                    "过不了多久。",
                                )

                                if (getRandom(100) <= 4f) {
                                    return soon[getRandom(soon.size).toInt() - 1]
                                }

                                val timeList = arrayOf("年", "个月", "周", "天", "小时", "分钟", "秒")
                                num = getRandom(100)
                                iis = timeList[getRandom(timeList.size).toInt() - 1]
                            }
                        }

                        SCORE -> {
                            num = - 1.0
                        }

                        RANK -> { // 缩放结果，让给出的排名更靠前（小
                            num = floor(16.0 * (getRandom().pow(2.0))) + 1.0

                            val i = getRandom()
                            if (i > 0.98) num = Long.MAX_VALUE.toDouble() else if (i > 0.95) num = 114514.0

                            iis = "第"
                        }

                        TIMES -> {
                            val c3 = matcher.group("c3").trim()

                            num = getRandom(100)

                            val i = getRandom()

                            if (i > 0.98) num = Int.MAX_VALUE.toDouble() else if (i > 0.95) num = 114514.0

                            iis = if (c3.matches("[次个位件名只发层岁人枚字章节]".toRegex())) {
                                c3
                            } else {
                                "次"
                            }
                        }

                        WHETHER -> {
                            iis = matcher.group("c3")
                            not = matcher.group("m3")

                            try {
                                val is2 = matcher.group("c2") // 要不要，如果不是ABA那么不能匹配
                                if (is2 != iis) {
                                    split = null
                                    continue
                                } // 找不到也不行
                            } catch (_: RuntimeException) {
                                split = null
                                continue
                            }
                        }

                        COULD -> {
                            iis = matcher.group("c3")
                            not = "不"
                            if (left.isBlank()) left = "..."
                            if (right.isBlank()) right = ""
                        }

                        POSSIBILITY -> { // 做点手脚，让 0% 和 100% 更容易出现 -4 ~ 104
                            // 7.07% 触发彩蛋。
                            num = ((getRandom(1) * 10800.0).roundToInt() / 100.0) - 4.0

                            iis = ""

                            if (num >= 101.0) { // 2% 买卖理论值
                                num = 101.0
                                iis = "0000"
                            }
                            num = num.coerceIn(0.0, 100.0)
                        }

                        ACCURACY -> { // 做点手脚，让 90%-100% 和 100% 更容易出现 90 ~ 104
                            num = if (getRandom() < 0.9) {
                                round(getRandom(1) * 1400.0) / 100.0 + 90.0
                            } else {
                                sqrt(getRandom(1)) * 9000.0 / 100.0
                            }.coerceIn(0.0, 100.0)

                            iis = ""
                        }

                        LIKE -> iis = matcher.group("c3")
                        IS, HAS -> {
                            iis = matcher.group("c3") // 有时候，”是“结尾的句子并不是问是否，还可以问比如时间。
                            // 比如，“OWC 的开启时间是？”
                            if (right.isBlank()) return "我怎么知道。"
                        }

                        REAL, QUESTION -> { // 10% 触发彩蛋。
                            if (getRandom(100) <= 10f) return "我怎么知道。"
                        }

                        else -> {}
                    }

                    // 排除掉AB一样的选择要求
                    if (left.isNotBlank() && right.isNotBlank() && num == 0.0) {
                        val isSame: Boolean = try {
                            val l = left.lowercase().trim()
                            val r = right.lowercase().trim()

                            (l == r) || ((l.contains(r) || r.contains(l)) && l.length >= 3 && r.length >= 3)
                        } catch (_: PatternSyntaxException) {
                            false
                        }

                        if (isSame) {
                            if (getRandom(100) < 30) {
                                throw DiceException.NoDifferenceEveryday(left.trim(), right.trim())
                            } else {
                                throw DiceException.NoDifference()
                            }
                        }
                    }

                    break
                }
            }

            if (split != null && (split.onlyC3 || (left.isNotBlank() && right.isNotBlank()))) {
                leftFormat = when (split) {
                    MULTIPLE -> "要我选的话，我觉得，%s。"
                    NEST -> "你搁这搁这呢？"
                    AM -> "我是 Yumu 机器人。"
                    POSSIBILITY -> "概率是：%.2f%s%%"
                    ACCURACY -> "准确率是：%.2f%s%%"
                    RANGE, AMOUNT -> "您许愿的结果是：%.0f。"
                    AGE -> "您许愿的岁数是：%.0f。"
                    TIME, TIMES -> "您许愿的结果是：%.0f %s。"
                    RANK -> "您许愿的结果是：%s %.0f。"
                    WHAT, WHY, WHO -> "我怎么知道。我又不是 deepseek。"
                    REAL -> "我觉得，是真的。"
                    BETTER, COMPARE, OR, JUXTAPOSITION, PREFER, HESITATE, EVEN -> "当然%s啦！"
                    ASSUME, LIKE, IS, HAS, QUESTION -> "%s。"
                    COULD, WHETHER -> "%s%s%s。"
                    CONDITION -> "是的。"
                    THINK -> "嗯。"
                    SCORE -> "您许愿的比分是：%s:%s。"
                }

                rightFormat = when (split) {
                    MULTIPLE -> "要我选的话，我觉得，%s。"
                    NEST -> "你搁这搁这呢？"
                    AM -> "别问了，我也想知道自己是谁。"
                    POSSIBILITY -> "概率是：%.2f%s%%"
                    ACCURACY -> "准确率是：%.2f%s%%"
                    RANGE, AMOUNT -> "您许愿的结果是：%.0f。"
                    AGE -> "您许愿的岁数是：%.0f。"
                    TIME, TIMES -> "您许愿的结果是：%.0f %s。"
                    RANK -> "您许愿的结果是：%s %.0f。"
                    WHAT -> "是哈基米。\n整个宇宙都是哈基米组成的。"
                    WHY -> "你不如去问问神奇海螺？"
                    WHO -> "我知道，芝士雪豹。"
                    REAL -> "我觉得，是假的。"
                    BETTER, OR, JUXTAPOSITION, PREFER, HESITATE, COMPARE -> "当然%s啦！"
                    EVEN -> "当然不%s啦！"
                    ASSUME -> "没有如果。"
                    COULD, WHETHER -> "%s%s%s%s。"
                    CONDITION -> "不是。"
                    LIKE, IS -> "不%s。"
                    HAS -> "没%s。"
                    THINK -> "也没有吧。"
                    QUESTION -> "不。"
                    SCORE -> "您许愿的比分是：%s:%s。"
                }

                // 改变几率
                boundary = when (split) {
                    PREFER -> 0.35
                    HESITATE -> 0.65
                    EVEN -> 0.7
                    WHAT, AM, WHY -> 0.8
                    else -> 0.5
                }
            } else {
                try {
                    return chooseMultiple(s)
                } catch (e: DiceException) {
                    throw e
                } catch (_: Exception) {
                    log.info("扔骰子：$s 匹配失败。")
                    throw DiceException.NotMatched()
                }
            }

            // 更换主语、和谐
            run {
                left = changeCase(left)
                right = changeCase(right)
                iis = changeCase(iis)
            }

            // 如果还是有空格，那么进入多匹配模式。
            run {
                val lm = MULTIPLE.pattern.matcher(left)
                val rm = MULTIPLE.pattern.matcher(right)

                val leftHas = lm.find() && (lm.group("m1").isNullOrBlank().not() || lm.group("m2").isNullOrBlank().not())
                val rightHas = rm.find() && (rm.group("m1").isNullOrBlank().not() || rm.group("m2").isNullOrBlank().not())

                // 临时修改，还没有更好的解决方法
                // 也就是说，如果这里列出的枚举可以匹配，就不进入多选择模式
                if (split != TIME && split != COULD && split != QUESTION && (leftHas || rightHas)) {
                    return chooseMultiple(s) // LR一样的
                }
            }

            if (result < boundary - 0.002f) { // 选第一个
                when (split) {
                    AM -> {
                        if (right.isNotBlank()) {
                            val botMatcher =
                                Pattern.compile("(?i)((\\s*Yumu\\s*)|雨沐)\\s*(机器人|Bot)?").matcher(right)
                            return if (botMatcher.find()) { // 你是 Yumu
                                if (getRandom(100) < 50) {
                                    "不不不。你才是${right}。"
                                } else {
                                    "我还以为你不知道呢。"
                                }
                            } else {
                                "${leftFormat}不是${right}。"
                            }
                        }
                        return leftFormat
                    }

                    WHAT, WHY, WHO, CONDITION, THINK, NEST, REAL -> {
                        return leftFormat
                    }

                    RANGE, AMOUNT, AGE -> {
                        return String.format(leftFormat, num)
                    }

                    TIME, TIMES, POSSIBILITY, ACCURACY -> {
                        return String.format(leftFormat, num, iis)
                    }

                    RANK -> {
                        return String.format(leftFormat, iis, num)
                    }

                    SCORE -> {
                        return String.format(leftFormat, ((left.toIntOrNull() ?: 0) + 1).toString(), right)
                    }

                    BETTER, COMPARE, JUXTAPOSITION, PREFER, HESITATE, QUESTION, MULTIPLE -> {
                        return String.format(leftFormat, left)
                    }

                    ASSUME, EVEN -> {
                        return String.format(leftFormat, right)
                    }

                    COULD, WHETHER -> {
                        return String.format(leftFormat, left, iis, right)
                    }

                    LIKE, IS, HAS -> {
                        return String.format(leftFormat, iis)
                    }

                    OR -> {
                        if (left.contains("是")) {
                            leftFormat = "我觉得，%s。"
                        }
                        return String.format(leftFormat, left)
                    }
                }
            } else if (result > boundary + 0.002f) { // 选第二个
                when (split) {
                    WHAT, WHY, WHO, AM, ASSUME, CONDITION, THINK, NEST, REAL, QUESTION -> {
                        return rightFormat
                    }

                    RANGE, AMOUNT, AGE -> {
                        return String.format(rightFormat, num)
                    }

                    TIME, TIMES, POSSIBILITY, ACCURACY -> {
                        return String.format(rightFormat, num, iis)
                    }

                    RANK -> {
                        return String.format(rightFormat, iis, num)
                    }

                    SCORE -> {
                        return String.format(rightFormat, left, ((right.toIntOrNull() ?: 0) + 1).toString())
                    }

                    BETTER, COMPARE, JUXTAPOSITION, PREFER, HESITATE, EVEN, MULTIPLE -> {
                        return String.format(rightFormat, right)
                    }

                    OR -> {
                        if (right.contains("是")) {
                            rightFormat = "我觉得，%s。"
                        }
                        return String.format(rightFormat, right)
                    }

                    COULD, WHETHER -> {
                        return String.format(rightFormat, left, not, iis, right)
                    }

                    LIKE, IS, HAS -> {
                        return String.format(rightFormat, iis)
                    }
                }
            } else { // 打平机会千分之四。彩蛋？
                if (result > boundary + 0.001f) {
                    throw DiceException.All()
                } else {
                    throw DiceException.Tie()
                }
            }

            // log.error("扔骰子：不正常结束！")
            // throw DiceException(DiceException.Type.DICE_Compare_Wtf)
        }

        /**
         * 多重选择
         *
         * @param str 字符串
         * @return 随机分配的结果
         * @throws DiceException 不知道该选什么
         */
        @Throws(DiceException::class) private fun chooseMultiple(str: String?): String { // A是B1还是B2还是B3？
            // 这个时候 A 是主语，不能加入匹配
            var s = str ?: ""
            val m =
                Pattern.compile("(?<m1>[\\S\\s]*)(?<c3>((?<![要还哪那就])是|喜欢|属于))(?<m2>[\\S\\s]*)?").matcher(s)

            if (m.matches() && m.group("m2") != null) {
                s = m.group("m2")
            }

            val strings = s.split(
                "还是|\\s*(?<![A-Za-z])or(?![A-Za-z])\\s*|或者?是?|[是或与,，.。/?!、？！:：]|\\s+".toRegex()
            ).dropLastWhile { it.isEmpty() }.filter { it.isNotBlank() }

            if (strings.isEmpty() || strings.size == 1) {
                throw DiceException.NotMatched()
            }

            // 多选择模式的去重
            val stringSet: MutableSet<String> = HashSet()
            var same = 1

            for (l in strings) {
                if (!stringSet.add(l)) {
                    same++
                }
            }

            if (same == strings.size) { // 只有多个全部一样才抛错
                if (getRandom(100) < 30) {
                    throw DiceException.NoDifferenceEveryday(stringSet.first(), stringSet.first())
                } else {
                    throw DiceException.NoDifference()
                }
            }

            val r = round(getRandom(strings.size) - 1.0).toInt()
            return String.format("当然%s啦！", changeCase(strings[r])) // lr format一样的
        }

        /**
         * 是否完美匹配
         *
         * @param m 匹配
         * @param hasC3 含有C3
         * @param onlyC3 只有C3
         * @return 结果
         */
        private fun isPerfectMatch(m: Matcher, hasC3: Boolean, onlyC3: Boolean): Boolean {
            if (!m.find()) {
                return false
            }

            val m1 = m.group("m1").isNullOrBlank().not()
            val m2 = m.group("m2").isNullOrBlank().not()
            val c3 = hasC3 && m.group("c3").isNullOrBlank().not()

            if (onlyC3) return c3
            if (hasC3) return m1 && m2 && c3
            return m1 && m2
        }

        @JvmStatic fun getRandom() : Double {
            return getRandom(0)
        }

        /**
         * 获取随机数。
         *
         * @param range 范围
         * @param T 数字的子类
         * @return 如果范围大于 1，返回 1-范围内的数（Double 的整数），其他则返回 0-1 之间的小数。
         */
        @JvmStatic fun <T : Number?> getRandom(range: T): Double {
            val r = range.toString().toIntOrNull() ?: (range?.toDouble() ?: 1.0).roundToInt()

            return if (r > 1) {
                Random.nextInt(1, r + 1).toDouble()
            } else {
                Random.nextDouble()
            }
        }

        /**
         * 改变主宾格，删除语气助词，和谐违禁词
         *
         * @param str 未和谐
         * @return 和谐
         */
        private fun changeCase(str: String?): String {
            val s = recoveryApostrophe(str ?: "")

            return s.trim() // 换人称
                .replace("你们?".toRegex(), "雨沐")
                .replace("(?i)\\syours?\\s".toRegex(), " yumu's ")
                .replace("(?i)\\syou\\s".toRegex(), " yumu ")
                .replace("我们".toRegex(), "你们")
                .replace("我".toRegex(), "你")
                .replace("(?i)\\s([Ii]|me)\\s".toRegex(), " you ")
                .replace("(?i)\\smy\\s".toRegex(), " your ")
                .replace("(?i)\\smine\\s".toRegex(), " yours ")
                .replace(
                    "[啊呃欸呀哟欤呕噢呦嘢哦吧呗啵啦嘞哩咧咯啰喽吗嘛嚜呢呐呵兮噻哉矣焉]|[罢否乎][?？!！。.\\s]?$".toRegex(),
                    "",
                ) // 阿耶来唻了价也罗给的般则连不呸哪哇 不匹配，删去其他语气助词
                // 换句末符号

                .replace("[?？!！。.\\s]$".toRegex(), "")
                .replace(
                    "[习習]近平|[习習]?总书记|主席|国家|政治|反动|反?共(产党)?|[国國]民[党黨]|天安[門门]|极[左右](主义)?|革命|(社会)?主义|自由|解放|中[華华]民[国國]|情趣|迪克|高潮|色[诱情欲色]|擦边|露出|[蛇射受授吞]精|潮喷|成人|性交|小?男娘|小?南梁|做爱|后入|药娘|怀孕|生殖器|寄吧|几把|鸡[鸡巴]|[精卵]子|[精爱]液|子宫|阴[茎蒂唇囊道]|[逼Bb阴吊叼批肛]毛|搞基|出?脚本|[Rr]-?18|18\\s?禁|LGBT".toRegex(),
                    "[和谐]",
                )
                .replace("[黨党吊批逼操肏肛杀穴屁萎猥]".toRegex(), "○")
                .replace("((电?[棍昆滚])|otto)\\s*的?\\s*((老?[木母]亲?)|([妈老]?妈)|(mom)|(mother))".toRegex(), "    ")
        }

        // 避免撇号影响结果，比如 It's time to go bed
        private fun transferApostrophe(s: String?): String {
            return (s ?: "").trim().replace("'".toRegex(), "\\" + "'").replace("\"".toRegex(), "\\" + "\"")
        }

        // 把撇号影响的结果转换回去，比如 It's time to go bed
        private fun recoveryApostrophe(s: String?): String {
            return (s ?: "").trim().replace(("\\" + "'").toRegex(), "'")
                .replace(("\\" + "\"").toRegex(), "\"")
        }
    }
}
