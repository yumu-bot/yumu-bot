package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageReceipt
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.DiceService.DiceParam
import com.now.nowbot.service.MessageServiceImpl.DiceService.Split.*
import com.now.nowbot.throwable.ServiceException.DiceException
import com.now.nowbot.util.DataUtil.isHelp
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.round
import kotlin.math.sqrt

@Service("DICE")
class DiceService : MessageService<DiceParam> {
    // dice：骰子次数，默认为 1
    class DiceParam(val dice: Long?, val number: Long?, text: String?) {
        val text: String = text!!
    }

    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<DiceParam>): Boolean {
        val m2 = Instruction.DEPRECATED_AYACHI_NENE.matcher(messageText)
        if (m2.find()) throw DiceException(DiceException.Type.DICE_EasterEgg_0d00)

        val m = Instruction.DICE.matcher(messageText)
        if (!m.find()) return false

        val dice = m.group("dice")
        val number = m.group("number")
        val text = m.group("text")

        if (StringUtils.hasText(text)) {
            // 如果 dice 有符合，但是并不是 1，选择主动忽视
            if (StringUtils.hasText(dice)) {
                try {
                    if (dice.toLong() > 1) {
                        return false
                    }
                } catch (e: NumberFormatException) {
                    return false
                }
            }

            if (StringUtils.hasText(number)) {
                data.value = DiceParam(null, null, (number + text).trim())
                return true
            } else if (isHelp(text)) {
                throw DiceException(DiceException.Type.DICE_Instruction)
            } else {
                data.setValue(DiceParam(null, null, text.trim()))
                return true
            }
        } else if (StringUtils.hasText(dice)) {
            val d: Long

            try {
                d = dice.toLong()
            } catch (e: NumberFormatException) {
                throw DiceException(DiceException.Type.DICE_Number_ParseFailed)
            }

            // 如果 dice 有符合，但是是 0，选择主动忽视（0d2）
            if (dice.toLong() < 1) {
                throw DiceException(DiceException.Type.DICE_Number_TooSmall)
            }

            val n = if (StringUtils.hasText(number)) {
                if (number.contains("-")) {
                    throw DiceException(DiceException.Type.DICE_Number_NotSupportNegative)
                }

                try {
                    number.toLong()
                } catch (e: NumberFormatException) {
                    throw DiceException(DiceException.Type.DICE_Number_ParseFailed)
                }
            } else {
                100L
            }

            if (d > 100L) throw DiceException(DiceException.Type.DICE_Dice_TooMany, d)

            data.setValue(DiceParam(d, n, null))
        } else if (StringUtils.hasText(number)) {
            val n: Long

            if (number.contains("-")) {
                throw DiceException(DiceException.Type.DICE_Number_NotSupportNegative)
            }

            try {
                n = number.toLong()
            } catch (e: NumberFormatException) {
                throw DiceException(DiceException.Type.DICE_Number_ParseFailed)
            }

            data.setValue(DiceParam(1L, n, null))
        } else {
            data.setValue(DiceParam(1L, 100L, null))
        }
        return true

        //throw new DiceException(DiceException.Type.DICE_Instruction);
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: DiceParam) {
        val from = event.subject
        val receipt: MessageReceipt

        try {
            if (Objects.nonNull(param.number)) {
                if (param.number!! >= Int.MAX_VALUE) {
                    throw DiceException(DiceException.Type.DICE_Number_TooLarge)
                }
                if (param.number < 1L) {
                    throw DiceException(DiceException.Type.DICE_Number_TooSmall)
                }

                // 单次匹配 !d, 1d100 和多次匹配 20d100
                if (param.number == 1L) {
                    val r = getRandom<Long?>(param.number)
                    val format = if ((r < 1f)) "%.2f" else "%.0f"

                    receipt = from.sendMessage(String.format(format, r))

                    //容易被识别成 QQ
                    if (r >= 1000000f && r < 1000000000f) {
                        receipt.recallIn((60 * 1000).toLong())
                    }
                    return
                } else {
                    val sb = StringBuilder()

                    for (i in 1L..param.dice!!) {
                        val r = getRandomInstantly<Long?>(param.number)
                        val format = if ((r < 1f)) "%.2f" else "%.0f"

                        sb.append(String.format(format, r))

                        if (i != param.dice) {
                            sb.append(", ")
                        }
                    }

                    from.sendMessage(sb.toString())
                }
            }

            if (Objects.nonNull(param.text)) {
                val message = Compare(param.text)

                //用于匹配是否被和谐
                val h = Pattern.compile("○|(\\[和谐])")
                if (h.matcher(message).find()) {
                    //被和谐就撤回
                    receipt = from.sendMessage(message)
                    receipt.recallIn((60 * 1000).toLong())
                } else {
                    from.sendMessage(message)
                }
            }
        } catch (e: DiceException) {
            throw e
        } catch (e: Exception) {
            log.error("扔骰子：处理失败", e)
            throw DiceException(DiceException.Type.DICE_Send_Error)
        }
    }

    internal enum class Split(val pattern: Pattern) {
        //用于匹配是否还有关联词
        MULTIPLE(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(还是|或者?是?|与|\\s+)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        NEST(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>[!！1]d)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        RANGE(Pattern.compile("(?<m1>[大多高等小少低]于(等于)?|约等于?|超过|不足|[><]=?|[＞＜≥≤≡≈]|\\s(more|less)\\s(than)?\\s)(?<c3>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*(?<m2>\\d+)")),

        TIME(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>多久|((几多?|多少|什么|啥|哪个|何)(时[候间]|个?(年|月|周|日子?|天|分钟?|小?时|钟[头点]|柱香|时辰|[毫微纳]秒)))(几?何|之?[后内])?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),  //皮秒 飞秒

        TIMES(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>((几多?|多少|什么|啥|哪个|何)?(频率|次数?))(之?[后内])?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),  //皮秒 飞秒

        POSSIBILITY(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>((有多[少大])?的?([概几]率是?|可能[是性]?))|\\s(chance|possib(l[ey]|ility)(\\sis)?)\\s)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        ACCURACY(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>((有多[少大])?的?(准确率是?|[准精]度)|\\s?(acc(uracy)?)(\\sis)?)\\s?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        AMOUNT(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>[是有]?多少[人个件位条匹颗根辆]?|数量(?!级)|[人个件位条匹颗根辆]数)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //A和B比谁更C？
        //正常选择
        //当然选 X 啦！
        BETTER(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)\\s*(?<c2>(跟|和|与|并|\\s(and|or|with)\\s))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)\\s*比?(比[，,\\s]*?哪个|比[，,\\s]*?谁|哪个|谁)更?(?<c3>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //A比B厉害？
        //正常选择
        //当然选 A 啦！，当然是 B 啦！（B厉害，这里分不开）
        COMPARE(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(比(?![赛如比拟重邻值及照目价例试上下肩方对分热画划类舍武翼意义喻作基利天推量年萨勒葫芦集速时势特体]|$)较?|(\\scompare(\\sto)?\\s)))[，,\\s]*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //选A，还是选B？
        //正常选择
        //当然选 X 啦！
        OR(Pattern.compile("\\s*(?<c1>(不?是|要么|是要?)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>([：:]|[还就而]是|and|or|或|或者|要么)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //是不是   排除 爱A不A
        //A是。A不是。
        WHETHER(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*(?<!爱))?\\s*(?<c2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?])(?<m3>[不没])(?<c3>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?])[人个件位条匹颗根辆]?\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //你谁？
        //我是 YumuBot
        AM(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>你是谁?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //为什么？
        //我怎么知道。因为爱情。
        WHY(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?\\s*(?<c3>为(什么|何|啥))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //是谁？
        //我怎么知道。是雪豹。
        WHO(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?\\s*(?<c3>谁是|是谁)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //是什么？
        //我怎么知道。是哈基米。
        WHAT(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?\\s*(?<c3>(?<!你们?|[要还哪那就])是(([你我他她它祂]们?|别人)?谁|哪[个里处位天日]|什么|啥))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //是，会，要吗？
        //是。不是。
        IS(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*?(?<c3>(?<![要还哪那就])((?<![一等开集机领误神社公工财理附利员法动应半倒标大相生体约庙云际照而融茶酒览话赴])(会不)?会|(?<![求只总如煞假利而熟皆老要凡既为倒先可])(是不)?是|(?<![摘纲刚重指打务六八提])(要不)?要|(可不)?可以)吗?|\\sis\\s)\\s*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //真的吗？
        //我怎么知道。是真的。是假的。
        REAL(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*?(?<c3>真的吗?|\\sreal(ly)?\\s)\\s*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //并列AB
        //当然选 X 啦！
        JUXTAPOSITION(Pattern.compile("\\s*(?<c1>(不仅|一边|一方面|有时|既)(选?[择中好]?了?)?)\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(而且|一边|一方面|有时|又)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //宁可A，也不B
        //偏好A
        //当然选 X 啦！
        PREFER(Pattern.compile("\\s*(?<c1>(宁[可愿]|尽管)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(也不[要想]?(选?[择中好]?了?)?))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //与其A，不如B
        //偏好B
        //当然选 X 啦！
        HESITATE(Pattern.compile("\\s*(?<c1>(与其|虽然|尽管)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(还?不如|比不上|但是|可是|然而|却)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //即使A，也B
        //偏好当然
        //当然B，不会B。
        EVEN(Pattern.compile("\\s*(?<c1>(即使|\\seven\\sif\\s)((选?[择中好]?了?)?[择中好])?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?([你我他她它祂]们?|别人)?(?<c2>([也还]会?)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //假设A，才有B。
        //我觉得 A 也没啥。// 没有如果。
        ASSUME(Pattern.compile("\\s*(?<c1>(如果|假使|假设|要是|\\s(if|assume)\\s))\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)[，,\\s]*?(?<c2>(那?([你我他她它祂]们?|别人)?[会要想就便么才])|([想要]|想要|能够?|可以))\\s*(?<m2>([你我他她它祂]们?|别人)?[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //A是B？
        //确实。 //不对。
        CONDITION(Pattern.compile("\\s*(?<c1>(只要|只有|无论|不管|忽略|忽视|不(去)?想|\\sif\\s))\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(([你我他她它祂]们?|别人)?([就才都也还]能?|能)(够|是|可以)?|反正|依然))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        //我能
        COULD(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)\\s*?(?<c2>不)?\\s*?(?<c3>([想要]|想要|能[够否]?|可以|应该))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)")),

        LIKE(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*?(?<c3>喜欢|爱|\\s((dis)?like|love)\\s)\\s*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //觉得
        //嗯。也没有吧。
        THINK(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?\\s*(?<c2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?])(?<c3>(觉得|认为))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),

        //....吗？
        //....。 不。
        QUESTION(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*?(?<c3>吗[?？]?)\\s*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")),
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DiceService::class.java)

        // sp == TIME || sp == TIMES || sp == AMOUNT || sp == WHY || sp == WHO || sp == AM || sp == COULD || sp == WHETHER || sp == IS || sp == REAL || sp == LIKE || sp == POSSIBILITY || sp == ACCURACY || sp == THINK || sp == NEST || sp == WHAT || sp == QUESTION
        private val SPLIT_C3_SET = setOf(
            TIME,
            TIMES,
            AMOUNT,
            WHY,
            WHO,
            AM,
            COULD,
            WHETHER,
            IS,
            REAL,
            LIKE,
            POSSIBILITY,
            ACCURACY,
            THINK,
            NEST,
            WHAT,
            QUESTION
        )

        // 查询序列
        private val SPLIT_ENUM_SEQUENCE = arrayListOf(
            TIME,
            TIMES,
            POSSIBILITY,
            ACCURACY,
            AMOUNT,
            BETTER,
            COMPARE,
            OR,
            WHETHER,
            AM,
            WHAT,
            WHY,
            WHO,
            IS,
            REAL,
            JUXTAPOSITION,
            PREFER,
            HESITATE,
            EVEN,
            COULD,
            RANGE,
            ASSUME,
            CONDITION,
            LIKE,
            THINK,
            NEST,
            QUESTION
        )

        private val TIME_EASTER_EGG = arrayOf(
            "不可能。",
            "永远不会。",
            "马上。",
            "立刻。",
            "就现在。",
            "很久很久。",
            "一会儿。",
            "过不了多久。",
            "等鸡啄完了米...",
        )

        private val TIME_UNIT_LIST = arrayOf("年", "个月", "周", "天", "小时", "分钟", "秒")

        /**
         * 超级复杂™的语言学选择器
         * @param s 输入含有比较关系的文本
         * @return 返回随机一个子项
         * @throws DiceException 错
         */
        @JvmStatic
        @Throws(DiceException::class)
        fun Compare(s: String): String {
            var s = s
            s = transferApostrophe(s)

            val result = getRandom(0)
            val boundary: Float
            var left = ""
            var right = ""
            var num = 0f
            var `is` = ""
            var not = ""
            var leftFormat: String
            var rightFormat: String
            var split: Split? = null


            for (sp in SPLIT_ENUM_SEQUENCE) {
                // c3?
                val onlyC3 = SPLIT_C3_SET.contains(sp)
                val hasC3 = onlyC3 || sp == BETTER
                val matcher = sp.pattern.matcher(s)

                // 不匹配就跳过
                if (!isPerfectMatch(matcher, hasC3, onlyC3)) continue

                split = sp

                left = matcher.group("m1")
                right = matcher.group("m2")

                // 预处理
                when (split) {
                    RANGE -> {
                        val range = try {
                            right.toInt()
                        } catch (e: NumberFormatException) {
                            100
                        }

                        num = if (range <= 0) {
                            throw DiceException(DiceException.Type.DICE_Number_TooSmall)
                        } else if (range <= 100) {
                            getRandom(100)
                        } else if (range <= 10000) {
                            getRandom(10000)
                        } else if (range <= 1000000) {
                            getRandom(1000000)
                        } else {
                            throw DiceException(DiceException.Type.DICE_Number_TooLarge)
                        }
                    }

                    AMOUNT -> num = getRandom(100)
                    TIME -> {
                        val c3 = matcher.group("c3").trim()
                        when {
                            // 4% 触发彩蛋。
                            getRandom(100) <= 4f -> {
                                return TIME_EASTER_EGG.random()
                            }

                            c3.contains("年") -> {
                                num = getRandom(100)
                                `is` = "年"
                            }

                            c3.contains("月") -> {
                                num = getRandom(12)
                                `is` = "个月"
                            }

                            c3.contains("周") -> {
                                num = getRandom(52)
                                `is` = "周"
                            }

                            c3.contains("日") || c3.contains("天") -> {
                                num = getRandom(30)
                                `is` = "天"
                            }

                            c3.contains("时辰") -> {
                                num = getRandom(12)
                                `is` = "时辰"
                            }

                            c3.contains("小时") ||
                                    (c3.contains("时") && !(c3.contains("时候") || c3.contains("时间")))
                                -> {
                                num = getRandom(24)
                                `is` = "小时"
                            }

                            c3.contains("点") -> {
                                num = getRandom(24)
                                `is` = "点"
                            }

                            c3.contains("柱香") -> {
                                num = getRandom(48)
                                `is` = "柱香"
                            }

                            c3.contains("分") -> {
                                num = getRandom(60)
                                `is` = "分钟"
                            }

                            c3.contains("毫秒") -> {
                                num = getRandom(1000)
                                `is` = "毫秒"
                            }

                            c3.contains("微秒") -> {
                                num = getRandom(1000000)
                                `is` = "微秒"
                            }

                            c3.contains("纳秒") -> {
                                num = getRandom(100000000)
                                `is` = "纳秒"
                            }

                            c3.contains("秒") -> {
                                num = getRandom(60)
                                `is` = "秒"
                            }

                            else -> {
                                //未指定时间单位，比如多久
                                num = getRandom(100)
                                `is` = TIME_UNIT_LIST.random()
                            }
                        }
                    }

                    TIMES -> {
                        num = getRandom(100)

                        val i = randomInstantly

                        if (i > 0.98) num = 2147483647f
                        else if (i > 0.95) num = 114514f

                        `is` = "次"
                    }

                    WHETHER -> {
                        `is` = matcher.group("c3")
                        not = matcher.group("m3")

                        try {
                            val is2 = matcher.group("c2")
                            //要不要，如果不是ABA那么不能匹配
                            if (is2 != `is`) {
                                split = null
                                continue
                            }
                            //找不到也不行
                        } catch (e: RuntimeException) {
                            split = null
                            continue
                        }
                    }

                    COULD -> {
                        `is` = matcher.group("c3")
                        not = "不"
                        if (!StringUtils.hasText(left)) left = "..."
                        if (!StringUtils.hasText(right)) right = ""
                    }

                    POSSIBILITY -> {
                        // 做点手脚，让 0% 和 100% 更容易出现 -4 ~ 104
                        // 7.07% 触发彩蛋。
                        num = (round(getRandom(1) * 10800f) / 100f) - 4f

                        `is` = ""

                        // 钳位
                        if (num >= 102f) {
                            // 2% 买卖理论值
                            num = 101.00f
                            `is` = "00"
                        }
                        if (num >= 100f) num = 100f
                        if (num <= 0f) num = 0f
                    }

                    ACCURACY -> {
                        // 做点手脚，让 90%-100% 和 100% 更容易出现 90 ~ 104
                        num = if (randomInstantly < 0.9) {
                            (round(getRandom(1) * 1400f) / 100f) + 90f
                        } else {
                            (sqrt(
                                getRandom(1).toDouble()
                            ) * 9000f / 100f).toFloat()
                        }

                        `is` = ""

                        // 钳位
                        if (num >= 100f) num = 100f
                        if (num <= 0f) num = 0f
                    }

                    LIKE -> `is` = matcher.group("c3")
                    IS -> {
                        `is` = matcher.group("c3")
                        // 有时候，”是“结尾的句子并不是问是否，还可以问比如时间。
                        // 比如，“OWC 的开启时间是？”
                        if (!StringUtils.hasText(right)) return "我怎么知道。"
                    }

                    REAL, QUESTION -> {
                        // 10% 触发彩蛋。
                        if (getRandom(100) <= 10f) return "我怎么知道。"
                    }

                    // 其他的不需要处理
                    else -> { }
                }

                //排除掉AB一样的选择要求
                if (StringUtils.hasText(left) && StringUtils.hasText(right) && num == 0f) {
                    var m = false
                    try {
                        m = (left.lowercase(Locale.getDefault())
                            .contains(right.lowercase(Locale.getDefault())) || right.lowercase(Locale.getDefault())
                            .contains(left.lowercase(Locale.getDefault()))) && left.length >= 3 && right.length >= 3
                    } catch (ignored: PatternSyntaxException) {
                    }

                    if (m) {
                        throw DiceException(DiceException.Type.DICE_Compare_NoDifference)
                    }
                }

                break
            }

            if (Objects.nonNull(split) && Objects.nonNull(left) && Objects.nonNull(right)) {
                leftFormat = when (split) {
                    MULTIPLE -> "要我选的话，我觉得，%s。"
                    NEST -> "你搁这搁这呢？"
                    AM -> "我是 Yumu 机器人。"
                    POSSIBILITY -> "概率是：%.2f%s%%"
                    ACCURACY -> "准确率是：%.2f%s%%"
                    RANGE, AMOUNT -> "您许愿的结果是：%.0f。"
                    TIME, TIMES -> "您许愿的结果是：%.0f %s。"
                    WHAT, WHY, WHO -> "我怎么知道。我又不是 GPT。"
                    REAL -> "我觉得，是真的。"
                    BETTER, COMPARE, OR, JUXTAPOSITION, PREFER, HESITATE, EVEN -> "当然%s啦！"
                    ASSUME, LIKE, IS, QUESTION -> "%s。"
                    COULD, WHETHER -> "%s%s%s。"
                    CONDITION -> "是的。"
                    THINK -> "嗯。"
                    else -> TODO()
                }

                rightFormat = when (split) {
                    MULTIPLE -> "要我选的话，我觉得，%s。"
                    NEST -> "你搁这搁这呢？"
                    AM -> "别问了，我也想知道自己是谁。"
                    POSSIBILITY -> "概率是：%.2f%s%%"
                    ACCURACY -> "准确率是：%.2f%s%%"
                    RANGE, AMOUNT -> "您许愿的结果是：%.0f。"
                    TIME, TIMES -> "您许愿的结果是：%.0f %s。"
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
                    THINK -> "也没有吧。"
                    QUESTION -> "不。"
                }

                //改变几率
                boundary = when (split) {
                    PREFER -> 0.35f
                    HESITATE -> 0.65f
                    EVEN -> 0.7f
                    WHAT, AM, WHY -> 0.8f
                    else -> 0.5f
                }
            } else {
                try {
                    return chooseMultiple(s)
                } catch (e: DiceException) {
                    throw e
                } catch (e: Exception) {
                    log.info(
                        "扔骰子：{$s} 匹配失败。"
                    )
                    throw DiceException(DiceException.Type.DICE_Compare_NotMatch)
                }
            }

            //更换主语、和谐
            run {
                left = ChangeCase(left)
                right = ChangeCase(right)
                `is` = ChangeCase(`is`)
            }

            //如果还是有空格，那么进入多匹配模式。
            run {
                val lm = MULTIPLE.pattern.matcher(left)
                val rm = MULTIPLE.pattern.matcher(right)

                val leftHas = lm.find() && (StringUtils.hasText(lm.group("m1")) || StringUtils.hasText(lm.group("m2")))
                val rightHas = rm.find() && (StringUtils.hasText(rm.group("m1")) || StringUtils.hasText(rm.group("m2")))

                // TODO 临时修改，还没有更好的解决方法
                if (split != TIME && (leftHas || rightHas)) {
                    return chooseMultiple(s) //LR一样的
                }
            }

            if (result < boundary - 0.002f) {
                //选第一个
                when (split) {
                    AM -> {
                        if (StringUtils.hasText(right)) {
                            val botMatcher =
                                Pattern.compile("(?i)((\\s*Yumu\\s*)|雨沐)\\s*(机器人|Bot)?").matcher(right)
                            return if (botMatcher.find()) {
                                //你是 Yumu
                                if (getRandom(100) < 50) {
                                    "不不不。你才是$right。"
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

                    RANGE, AMOUNT -> {
                        return String.format(leftFormat, num)
                    }

                    TIME, TIMES, POSSIBILITY, ACCURACY -> {
                        return String.format(leftFormat, num, `is`)
                    }

                    BETTER, COMPARE, JUXTAPOSITION, PREFER, HESITATE, QUESTION -> {
                        return String.format(leftFormat, left)
                    }

                    ASSUME, EVEN -> {
                        return String.format(leftFormat, right)
                    }

                    COULD, WHETHER -> {
                        return String.format(leftFormat, left, `is`, right)
                    }

                    LIKE, IS -> {
                        return String.format(leftFormat, `is`)
                    }

                    OR -> {
                        if (left.contains("是")) {
                            leftFormat = "我觉得，%s。"
                        }
                        return String.format(leftFormat, left)
                    }

                    else -> TODO()
                }
            } else if (result > boundary + 0.002f) {
                //选第二个
                when (split) {
                    WHAT, WHY, WHO, AM, ASSUME, CONDITION, THINK, NEST, REAL, QUESTION -> {
                        return rightFormat
                    }

                    RANGE, AMOUNT -> {
                        return String.format(rightFormat, num)
                    }

                    TIME, TIMES, POSSIBILITY, ACCURACY -> {
                        return String.format(rightFormat, num, `is`)
                    }

                    BETTER, COMPARE, JUXTAPOSITION, PREFER, HESITATE, EVEN -> {
                        return String.format(rightFormat, right)
                    }

                    OR -> {
                        if (right.contains("是")) {
                            rightFormat = "我觉得，%s。"
                        }
                        return String.format(rightFormat, right)
                    }

                    COULD, WHETHER -> {
                        return String.format(rightFormat, left, not, `is`, right)
                    }

                    LIKE, IS -> {
                        return String.format(rightFormat, `is`)
                    }

                    else -> TODO()
                }
            } else {
                //打平机会千分之四。彩蛋？
                if (result > boundary + 0.001f) {
                    throw DiceException(DiceException.Type.DICE_Compare_All)
                } else {
                    throw DiceException(DiceException.Type.DICE_Compare_Tie)
                }
            }

            log.error("扔骰子：不正常结束！")
            throw DiceException(DiceException.Type.DICE_Compare_Wtf)
        }

        /**
         * 多重选择
         * @param s 字符串
         * @return 随机分配的结果
         * @throws DiceException 不知道该选什么
         */
        @Throws(DiceException::class)
        private fun chooseMultiple(s: String): String {
            // A是B1还是B2还是B3？
            // 这个时候 A 是主语，不能加入匹配
            var s = s
            val m =
                Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)(?<c3>((?<![要还哪那就])是|喜欢|属于))(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?")
                    .matcher(s)

            if (m.matches() && Objects.nonNull(m.group("m2"))) {
                s = m.group("m2")
            }

            val strings =
                s.split("还是|\\s*(?<![A-Za-z])or(?![A-Za-z])\\s*|或者?是?|[是或与,，.。/?!、？！:：]|\\s+".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
            val stringList = Arrays.stream(strings).filter { str: String? -> StringUtils.hasText(str) }.toList()

            if (stringList.isEmpty() || stringList.size == 1) {
                throw DiceException(DiceException.Type.DICE_Compare_NotMatch)
            }

            // 多选择模式的去重
            val stringSet: MutableSet<String> = HashSet()
            var same = 1

            for (l in stringList) {
                if (!stringSet.add(l)) {
                    same++
                }
            }

            if (same == stringList.size) {
                // 只有多个全部一样才抛错
                throw DiceException(DiceException.Type.DICE_Compare_NoDifference)
            }

            val r = Math.round(getRandom(stringList.size) - 1)
            return String.format("当然%s啦！", ChangeCase(stringList[r])) //lr format一样的
        }

        /**
         * 是否完美匹配
         * @param m 匹配
         * @param hasC3 含有C3
         * @param onlyC3 只有C3
         * @return 结果
         */
        private fun isPerfectMatch(m: Matcher, hasC3: Boolean, onlyC3: Boolean): Boolean {
            if (!m.find()) {
                return false
            }

            val m1 = Objects.nonNull(m.group("m1")) && StringUtils.hasText(m.group("m1"))
            val m2 = Objects.nonNull(m.group("m2")) && StringUtils.hasText(m.group("m2"))
            val c3 = hasC3 && Objects.nonNull(m.group("c3")) && StringUtils.hasText(m.group("c3"))

            if (onlyC3) return c3
            if (hasC3) return m1 && m2 && c3
            return m1 && m2
        }


        val randomInstantly: Double
            get() = getRandomInstantly(0)

        /**
         * 获取短时间内的多个随机数
         * @param range 范围
         * @return 如果范围是 1，返回 1。如果范围大于 1，返回 1-范围内的数（Float 的整数），其他则返回 0-1。
         * @param <T> 数字的子类
        </T> */
        fun <T : Number?> getRandomInstantly(@Nullable range: T): Double {
            val random = Math.random()

            var r = try {
                range.toString().toInt()
            } catch (e: NumberFormatException) {
                try {
                    if (Objects.nonNull(range)) {
                        Math.round(range!!.toFloat())
                    } else {
                        100
                    }
                } catch (e1: NumberFormatException) {
                    return random
                }
            }

            return if (r > 1) {
                Math.round(random * (r - 1)) + 1.0
            } else {
                random
            }
        }

        /**
         * 获取随机数。注意，随机数的来源是系统毫秒，因此不能短时间内多次获取，如果要多次获取请使用 getRandomInstantly 提供的伪随机数
         * @param range 范围
         * @return 如果范围是 1，返回 1。如果范围大于 1，返回 1-范围内的数（Float 的整数），其他则返回 0-1。
         * @param <T> 数字的子类
        </T> */
        fun <T : Number?> getRandom(@Nullable range: T): Float {
            val millis = System.currentTimeMillis() % 1000

            var r = try {
                range.toString().toInt()
            } catch (e: NumberFormatException) {
                try {
                    if (Objects.nonNull(range)) {
                        Math.round(range!!.toFloat())
                    } else {
                        100
                    }
                } catch (e1: NumberFormatException) {
                    return millis / 999f
                }
            }

            return if (r > 1) {
                Math.round(millis / 999f * (r - 1)) + 1f
            } else {
                millis / 999f
            }
        }

        /**
         * 改变主宾格，删除语气助词，和谐违禁词
         * @param s 未和谐
         * @return 和谐
         */
        private fun ChangeCase(@NonNull s: String): String {
            var s = s
            s = recoveryApostrophe(s)

            return s.trim()  // 换人称
                .replace("你们?".toRegex(), "雨沐")
                .replace("(?i)\\syours?\\s".toRegex(), " yumu's ")
                .replace("(?i)\\syou\\s".toRegex(), " yumu ")
                .replace("我们".toRegex(), "你们")
                .replace("我".toRegex(), "你")
                .replace("(?i)\\s([Ii]|me)\\s".toRegex(), " you ")
                .replace("(?i)\\smy\\s".toRegex(), " your ")
                .replace("(?i)\\smine\\s".toRegex(), " yours ")

                .replace(
                    "[啊呃欸呀哟欤呕噢呦嘢哦吧呗啵啦嘞哩咧咯啰喽吗嘛嚜呢呐呵兮噻哉矣焉]|[哈罢否乎么麽][?？!！。.\\s]?$".toRegex(),
                    ""
                ) //阿耶来唻了价也罗给的般则连不呸哪哇 不匹配，删去其他语气助词
                // 换句末符号

                .replace("[?？!！。.\\s]$".toRegex(), "")

                .replace(
                    "[习習]近平|[习習]?总书记|主席|国家|政治|反动|反?共(产党)?|[国國]民[党黨]|天安[門门]|极[左右](主义)?|革命|(社会)?主义|自由|解放|中[華华]民[国國]|情趣|迪克|高潮|色[诱情欲色]|擦边|露出|[蛇射受授吞]精|潮喷|成人|性交|小?男娘|小?南梁|做爱|后入|药娘|怀孕|生殖器|寄吧|几把|鸡[鸡巴]|[精卵]子|[精爱]液|子宫|阴[茎蒂唇囊道]|[逼Bb阴吊叼批肛]毛|搞基|出?脚本|[Rr]-?18|18\\s?禁|LGBT".toRegex(),
                    "[和谐]"
                )
                .replace("[黨党吊批逼操肏肛杀穴屁萎猥]".toRegex(), "○")
        }

        // 避免撇号影响结果，比如 It's time to go bed
        private fun transferApostrophe(@NonNull s: String): String {
            return s.trim().replace("'".toRegex(), "\\" + "'").replace("\"".toRegex(), "\\" + "\"")
        }

        // 把撇号影响的结果转换回去，比如 It's time to go bed
        private fun recoveryApostrophe(@NonNull s: String): String {
            return s.trim().replace(("\\" + "'").toRegex(), "'").replace(("\\" + "\"").toRegex(), "\"")
        }
    }
}
