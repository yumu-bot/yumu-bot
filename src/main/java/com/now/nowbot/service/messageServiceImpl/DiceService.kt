package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageReceipt
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.DiceService.DiceParam
import com.now.nowbot.throwable.serviceException.DiceException
import com.now.nowbot.util.DataUtil.isHelp
import com.now.nowbot.util.Instruction
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import kotlin.math.*

@Service("DICE")
class DiceService : MessageService<DiceParam> {
    // dice：骰子次数，默认为 1
    @JvmRecord data class DiceParam(val dice: Long?, val number: Long?, val text: String?)

    @Throws(Throwable::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<DiceParam>,
    ): Boolean {
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
                data.value = DiceParam(null, null, (number + text).trim { it <= ' ' })
                return true
            } else if (isHelp(text)) {
                throw DiceException(DiceException.Type.DICE_Instruction)
            } else {
                data.value = DiceParam(null, null, text.trim { it <= ' ' })
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

            val n =
                if (StringUtils.hasText(number)) {
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

        // throw new DiceException(DiceException.Type.DICE_Instruction);
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: DiceParam) {
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

                    receipt = event.reply(String.format(format, r))

                    // 容易被识别成 QQ
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

                    event.reply(sb.toString())
                }
            }

            if (Objects.nonNull(param.text)) {
                val message = compare(param.text)

                // 用于匹配是否被和谐
                val h = Pattern.compile("○|(\\[和谐])")
                if (h.matcher(message).find()) {
                    // 被和谐就撤回
                    receipt = event.reply(message)
                    receipt.recallIn((60 * 1000).toLong())
                } else {
                    event.reply(message)
                }
            }
        } catch (e: DiceException) {
            throw e
        } catch (e: Exception) {
            log.error("扔骰子：处理失败", e)
            throw DiceException(DiceException.Type.DICE_Send_Error)
        }
    }

    internal enum class Split(val pattern: Pattern, val onlyC3: Boolean) {
        TIME(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>多久|((几多?|多少|什么|啥|哪个|何)(时[候间]|个?(年|月|周|日子?|天|分钟?|小?时|钟[头点]|柱香|时辰|[毫微纳]秒)))(几?何|之?[后内])?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ), // 皮秒 飞秒

        RANK(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>(第几)[次个位件名只]?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ), // 第xx

        TIMES(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>((几多?|多少|什么|啥|哪个|何)?(频率|(?<![一两二三四五六七八九十百千万亿这那上下哪]|无数)[次个位件名只]数?)|第几[次个位件名只]?)(之?[后内])?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ), // 次数

        POSSIBILITY(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>((有多[少大])?的?([概几]率是?|可能[是性]?))|\\s(chance|possib(l[ey]|ility)(\\sis)?)\\s)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        ACCURACY(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>((有多[少大])?的?(准确率是?|[准精]度)|\\s?(acc(uracy)?)(\\sis)?)\\s?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        AMOUNT(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>[是有]?多少[人个件位条匹颗根辆]?|数量(?!级)|[人个件位条匹颗根辆]数)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // A和B比谁更C？
        // 正常选择
        // 当然选 X 啦！
        BETTER(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)\\s*(?<c2>(跟|和|与|并|\\s(and|or|with)\\s))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)\\s*比?(比[，,\\s]*?哪个|比[，,\\s]*?谁|哪个|谁)更?(?<c3>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),

        // A比B厉害？
        // 正常选择
        // 当然选 A 啦！，当然是 B 啦！（B厉害，这里分不开）
        COMPARE(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(比(?![赛如比拟重邻值及照目价例试上下肩方对分热画划类舍武翼意义喻作基利天推量年萨勒葫芦集速时势特体]|$)较?|(\\scompare(\\sto)?\\s)))[，,\\s]*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),

        // 选A，还是选B？
        // 正常选择
        // 当然选 X 啦！
        OR(
            Pattern.compile(
                "\\s*(?<c1>(不?是|要么|是要?)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>([：:]|[还就而]是|and|or|或|或者|要么)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),

        // 是不是   排除 爱A不A
        // A是。A不是。
        WHETHER(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*(?<!爱))?\\s*(?<c2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?])(?<m3>[不没])(?<c3>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?])[人个件位条匹颗根辆]?\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 你谁？
        // 我是 YumuBot
        AM(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>你是谁?)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 是什么？ // TODO 同时也加了 DO 的部分。以后这里可以抽象成一个单独的模块，用来推荐歌曲、抽卡、抽每日、抽老婆。
        // 我怎么知道。是哈基米。
        WHAT(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?\\s*(?<c3>(?<!你们?|[要还哪那就])[是吃做干看玩买唱喝打听抽](([你我他她它祂]们?|别人)?谁|哪[个里处位天日]|什么歌?|啥))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 为什么？
        // 我怎么知道。因为爱情。
        WHY(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?\\s*(?<c3>为(什么|何|啥))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 是谁？
        // 我怎么知道。是雪豹。
        WHO(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?\\s*(?<c3>谁是|是谁)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 是，会，要吗？
        // 是。不是。
        IS(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*?(?<c3>(?<![要还哪那就])((?<![一等开集机领误神社公工财理附利员法动应半倒标大相生体约庙云际照而融茶酒览话赴])(会不)?会|(?<![求只总如煞假利而熟皆老要凡既为倒先可])(是不)?是|(?<![摘纲刚重指打务六八提])(要不)?要|(可不)?可以)吗?|\\sis\\s)\\s*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 真的吗？
        // 我怎么知道。是真的。是假的。
        REAL(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*?(?<c3>真的吗?|\\sreal(ly)?\\s)\\s*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 并列AB
        // 当然选 X 啦！
        JUXTAPOSITION(
            Pattern.compile(
                "\\s*(?<c1>(不仅|一边|一方面|有时|既)(选?[择中好]?了?)?)\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(而且|一边|一方面|有时|又)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),

        // 宁可A，也不B
        // 偏好A
        // 当然选 X 啦！
        PREFER(
            Pattern.compile(
                "\\s*(?<c1>(宁[可愿]|尽管)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(也不[要想]?(选?[择中好]?了?)?))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),

        // 与其A，不如B
        // 偏好B
        // 当然选 X 啦！
        HESITATE(
            Pattern.compile(
                "\\s*(?<c1>(与其|虽然|尽管)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(还?不如|比不上|但是|可是|然而|却)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),

        // 即使A，也B
        // 偏好当然
        // 当然B，不会B。
        EVEN(
            Pattern.compile(
                "\\s*(?<c1>(即使|\\seven\\sif\\s)((选?[择中好]?了?)?[择中好])?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?([你我他她它祂]们?|别人)?(?<c2>([也还]会?)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),

        // 我能
        COULD(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)\\s*?(?<c2>不)?\\s*?(?<c3>([想要]|想要|能[够否]?|可以|应该))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = true
        ),

        RANGE(
            Pattern.compile(
                "(?<m1>[大多高等小少低]于(等于)?|约等于?|超过|不足|[><]=?|[＞＜≥≤≡≈]|\\s(more|less)\\s(than)?\\s)(?<c3>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*(?<m2>\\d+)"
            ), onlyC3 = false
        ),

        // 假设A，才有B。
        // 我觉得 A 也没啥。// 没有如果。
        ASSUME(
            Pattern.compile(
                "\\s*(?<c1>(如果|假使|假设|要是|\\s(if|assume)\\s))\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)[，,\\s]*?(?<c2>(那?([你我他她它祂]们?|别人)?[会要想就便么才])|([想要]|想要|能够?|可以))\\s*(?<m2>([你我他她它祂]们?|别人)?[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),


        // A是B？
        // 确实。 //不对。
        CONDITION(
            Pattern.compile(
                "\\s*(?<c1>(只要|只有|无论|不管|忽略|忽视|不(去)?想|\\sif\\s))\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)[，,\\s]*?(?<c2>(([你我他她它祂]们?|别人)?([就才都也还]能?|能)(够|是|可以)?|反正|依然))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)"
            ), onlyC3 = false
        ),


        LIKE(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*?(?<c3>喜欢|爱|\\s((dis)?like|love)\\s)\\s*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 觉得
        // 嗯。也没有吧。
        THINK(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?\\s*(?<c2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?])(?<c3>(觉得|认为))\\s*(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        NEST(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(?<c3>[!！1]d)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // ....吗？
        // ....。 不。
        QUESTION(
            Pattern.compile(
                "\\s*(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*?)?\\s*?(?<c3>吗[?？]?)\\s*?(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
            ), onlyC3 = true
        ),

        // 用于匹配是否还有关联词
        MULTIPLE(
            Pattern.compile(
                "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?(还是|或者?是?|与|\\s+)(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
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
        @JvmStatic
        @Throws(DiceException::class)
        fun compare(str: String?): String {
            val s = transferApostrophe(str)

            val result = getRandom(0)
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
                val hasC3 = sp == Split.BETTER || sp.onlyC3
                val matcher = sp.pattern.matcher(s)

                if (isPerfectMatch(matcher, hasC3, sp.onlyC3)) {
                    split = sp

                    left = matcher.group("m1")
                    right = matcher.group("m2")

                    when (split) {
                        Split.RANGE -> {
                            val range =
                                try {
                                    right.toInt()
                                } catch (e: NumberFormatException) {
                                    100
                                }

                            num =
                                if (range <= 0) {
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
                        Split.AMOUNT -> num = getRandom(100)
                        Split.TIME -> {
                            val c3 = matcher.group("c3").trim { it <= ' ' }

                            // 4% 触发彩蛋。
                            val soonList =
                                arrayOf(
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
                            if (getRandom(100) <= 4f)
                                return soonList[getRandom(soonList.size).toInt() - 1]

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
                            } else if (
                                (c3.contains("时") && !(c3.contains("时候") || c3.contains("时间"))) ||
                                    c3.contains("小时")
                            ) {
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
                                val timeList = arrayOf("年", "个月", "周", "天", "小时", "分钟", "秒")
                                num = getRandom(100)
                                iis = timeList[getRandom(timeList.size).toInt() - 1]
                            }
                        }
                        Split.RANK -> {
                            // 缩放结果，让给出的排名更靠前（小
                            num = floor(16.0 * (randomInstantly.pow(2.0))) + 1.0

                            val i = randomInstantly
                            if (i > 0.98) num = 4294967295.0 else if (i > 0.95) num = 114514.0

                            iis = "第"
                        }
                        Split.TIMES -> {
                            num = getRandom(100)

                            val i = randomInstantly

                            if (i > 0.98) num = 2147483647.0 else if (i > 0.95) num = 114514.0

                            iis = "次"
                        }
                        Split.WHETHER -> {
                            iis = matcher.group("c3")
                            not = matcher.group("m3")

                            try {
                                val is2 = matcher.group("c2")
                                // 要不要，如果不是ABA那么不能匹配
                                if (is2 != iis) {
                                    split = null
                                    continue
                                }
                                // 找不到也不行
                            } catch (e: RuntimeException) {
                                split = null
                                continue
                            }
                        }
                        Split.COULD -> {
                            iis = matcher.group("c3")
                            not = "不"
                            if (!StringUtils.hasText(left)) left = "..."
                            if (!StringUtils.hasText(right)) right = ""
                        }
                        Split.POSSIBILITY -> {
                            // 做点手脚，让 0% 和 100% 更容易出现 -4 ~ 104
                            // 7.07% 触发彩蛋。
                            num = ((getRandom(1) * 10800.0).roundToInt() / 100.0) - 4.0

                            iis = ""

                            // 钳位
                            if (num >= 102.0) {
                                // 2% 买卖理论值
                                num = 101.0
                                iis = "0000"
                            }
                            if (num >= 100f) num = 100.0
                            if (num <= 0f) num = 0.0
                        }
                        Split.ACCURACY -> {
                            // 做点手脚，让 90%-100% 和 100% 更容易出现 90 ~ 104
                            num =
                                if (randomInstantly < 0.9) {
                                    Math.round(getRandom(1) * 1400.0) / 100.0 + 90.0
                                } else {
                                    sqrt(getRandom(1)) * 9000.0 / 100.0
                                }

                            iis = ""

                            // 钳位
                            if (num >= 100.0) num = 100.0
                            if (num <= 0.0) num = 0.0
                        }
                        Split.LIKE -> iis = matcher.group("c3")
                        Split.IS -> {
                            iis = matcher.group("c3")
                            // 有时候，”是“结尾的句子并不是问是否，还可以问比如时间。
                            // 比如，“OWC 的开启时间是？”
                            if (!StringUtils.hasText(right)) return "我怎么知道。"
                        }
                        Split.REAL,
                        Split.QUESTION -> {
                            // 10% 触发彩蛋。
                            if (getRandom(100) <= 10f) return "我怎么知道。"
                        }
                        else -> {}
                    }
                    // 排除掉AB一样的选择要求
                    if (StringUtils.hasText(left) && StringUtils.hasText(right) && num == 0.0) {
                        var m = false
                        try {
                            m =
                                (left.lowercase().contains(right.lowercase()) ||
                                    right.lowercase().contains(left.lowercase())) &&
                                    (left.length >= 3) &&
                                    (right.length >= 3)
                        } catch (ignored: PatternSyntaxException) {}

                        if (m) {
                            throw DiceException(DiceException.Type.DICE_Compare_NoDifference)
                        }
                    }

                    break
                }
            }

            if (Objects.nonNull(split) && Objects.nonNull(left) && Objects.nonNull(right)) {
                leftFormat =
                    when (split) {
                        Split.MULTIPLE -> "要我选的话，我觉得，%s。"
                        Split.NEST -> "你搁这搁这呢？"
                        Split.AM -> "我是 Yumu 机器人。"
                        Split.POSSIBILITY -> "概率是：%.2f%s%%"
                        Split.ACCURACY -> "准确率是：%.2f%s%%"
                        Split.RANGE,
                        Split.AMOUNT -> "您许愿的结果是：%.0f。"
                        Split.TIME,
                        Split.TIMES -> "您许愿的结果是：%.0f %s。"
                        Split.RANK -> "您许愿的结果是：%s %.0f。"
                        Split.WHAT,
                        Split.WHY,
                        Split.WHO -> "我怎么知道。我又不是 GPT。"
                        Split.REAL -> "我觉得，是真的。"
                        Split.BETTER,
                        Split.COMPARE,
                        Split.OR,
                        Split.JUXTAPOSITION,
                        Split.PREFER,
                        Split.HESITATE,
                        Split.EVEN -> "当然%s啦！"
                        Split.ASSUME,
                        Split.LIKE,
                        Split.IS,
                        Split.QUESTION -> "%s。"
                        Split.COULD,
                        Split.WHETHER -> "%s%s%s。"
                        Split.CONDITION -> "是的。"
                        Split.THINK -> "嗯。"
                        else -> ""
                    }

                rightFormat =
                    when (split) {
                        Split.MULTIPLE -> "要我选的话，我觉得，%s。"
                        Split.NEST -> "你搁这搁这呢？"
                        Split.AM -> "别问了，我也想知道自己是谁。"
                        Split.POSSIBILITY -> "概率是：%.2f%s%%"
                        Split.ACCURACY -> "准确率是：%.2f%s%%"
                        Split.RANGE,
                        Split.AMOUNT -> "您许愿的结果是：%.0f。"
                        Split.TIME,
                        Split.TIMES -> "您许愿的结果是：%.0f %s。"
                        Split.RANK -> "您许愿的结果是：%s %.0f。"
                        Split.WHAT -> "是哈基米。\n整个宇宙都是哈基米组成的。"
                        Split.WHY -> "你不如去问问神奇海螺？"
                        Split.WHO -> "我知道，芝士雪豹。"
                        Split.REAL -> "我觉得，是假的。"
                        Split.BETTER,
                        Split.OR,
                        Split.JUXTAPOSITION,
                        Split.PREFER,
                        Split.HESITATE,
                        Split.COMPARE -> "当然%s啦！"
                        Split.EVEN -> "当然不%s啦！"
                        Split.ASSUME -> "没有如果。"
                        Split.COULD,
                        Split.WHETHER -> "%s%s%s%s。"
                        Split.CONDITION -> "不是。"
                        Split.LIKE,
                        Split.IS -> "不%s。"
                        Split.THINK -> "也没有吧。"
                        Split.QUESTION -> "不。"
                        else -> ""
                    }

                // 改变几率
                boundary =
                    when (split) {
                        Split.PREFER -> 0.35
                        Split.HESITATE -> 0.65
                        Split.EVEN -> 0.7
                        Split.WHAT,
                        Split.AM,
                        Split.WHY -> 0.8
                        else -> 0.5
                    }
            } else {
                try {
                    return chooseMultiple(s)
                } catch (e: DiceException) {
                    throw e
                } catch (e: Exception) {
                    log.info("扔骰子：${s} 匹配失败。")
                    throw DiceException(DiceException.Type.DICE_Compare_NotMatch)
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
                val lm = Split.MULTIPLE.pattern.matcher(left)
                val rm = Split.MULTIPLE.pattern.matcher(right)

                val leftHas =
                    lm.find() &&
                        (StringUtils.hasText(lm.group("m1")) || StringUtils.hasText(lm.group("m2")))
                val rightHas =
                    rm.find() &&
                        (StringUtils.hasText(rm.group("m1")) || StringUtils.hasText(rm.group("m2")))

                // 临时修改，还没有更好的解决方法
                if (split != Split.TIME && (leftHas || rightHas)) {
                    return chooseMultiple(s) // LR一样的
                }
            }

            if (result < boundary - 0.002f) {
                // 选第一个
                when (split) {
                    Split.AM -> {
                        if (StringUtils.hasText(right)) {
                            val botMatcher =
                                Pattern.compile("(?i)((\\s*Yumu\\s*)|雨沐)\\s*(机器人|Bot)?")
                                    .matcher(right)
                            return if (botMatcher.find()) {
                                // 你是 Yumu
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
                    Split.WHAT,
                    Split.WHY,
                    Split.WHO,
                    Split.CONDITION,
                    Split.THINK,
                    Split.NEST,
                    Split.REAL -> {
                        return leftFormat
                    }
                    Split.RANGE,
                    Split.AMOUNT -> {
                        return String.format(leftFormat, num)
                    }
                    Split.TIME,
                    Split.TIMES,
                    Split.POSSIBILITY,
                    Split.ACCURACY -> {
                        return String.format(leftFormat, num, iis)
                    }
                    Split.RANK -> {
                        return String.format(leftFormat, iis, num)
                    }
                    Split.BETTER,
                    Split.COMPARE,
                    Split.JUXTAPOSITION,
                    Split.PREFER,
                    Split.HESITATE,
                    Split.QUESTION -> {
                        return String.format(leftFormat, left)
                    }
                    Split.ASSUME,
                    Split.EVEN -> {
                        return String.format(leftFormat, right)
                    }
                    Split.COULD,
                    Split.WHETHER -> {
                        return String.format(leftFormat, left, iis, right)
                    }
                    Split.LIKE,
                    Split.IS -> {
                        return String.format(leftFormat, iis)
                    }
                    Split.OR -> {
                        if (left.contains("是")) {
                            leftFormat = "我觉得，%s。"
                        }
                        return String.format(leftFormat, left)
                    }
                    else -> {}
                }
            } else if (result > boundary + 0.002f) {
                // 选第二个
                when (split) {
                    Split.WHAT,
                    Split.WHY,
                    Split.WHO,
                    Split.AM,
                    Split.ASSUME,
                    Split.CONDITION,
                    Split.THINK,
                    Split.NEST,
                    Split.REAL,
                    Split.QUESTION -> {
                        return rightFormat
                    }
                    Split.RANGE,
                    Split.AMOUNT -> {
                        return String.format(rightFormat, num)
                    }
                    Split.TIME,
                    Split.TIMES,
                    Split.POSSIBILITY,
                    Split.ACCURACY -> {
                        return String.format(rightFormat, num, iis)
                    }
                    Split.RANK -> {
                        return String.format(rightFormat, iis, num)
                    }
                    Split.BETTER,
                    Split.COMPARE,
                    Split.JUXTAPOSITION,
                    Split.PREFER,
                    Split.HESITATE,
                    Split.EVEN -> {
                        return String.format(rightFormat, right)
                    }
                    Split.OR -> {
                        if (right.contains("是")) {
                            rightFormat = "我觉得，%s。"
                        }
                        return String.format(rightFormat, right)
                    }
                    Split.COULD,
                    Split.WHETHER -> {
                        return String.format(rightFormat, left, not, iis, right)
                    }
                    Split.LIKE,
                    Split.IS -> {
                        return String.format(rightFormat, iis)
                    }
                    else -> {}
                }
            } else {
                // 打平机会千分之四。彩蛋？
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
         *
         * @param str 字符串
         * @return 随机分配的结果
         * @throws DiceException 不知道该选什么
         */
        @Throws(DiceException::class)
        private fun chooseMultiple(str: String?): String {
            // A是B1还是B2还是B3？
            // 这个时候 A 是主语，不能加入匹配
            var s = str ?: ""
            val m =
                Pattern.compile(
                        "(?<m1>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)(?<c3>((?<![要还哪那就])是|喜欢|属于))(?<m2>[\\u4e00-\\u9fa5\\uf900-\\ufa2d\\w\\s.\\-:：\\[\\]_，。*()&^！？!?]*)?"
                    )
                    .matcher(s)

            if (m.matches() && Objects.nonNull(m.group("m2"))) {
                s = m.group("m2")
            }

            val strings =
                s.split(
                        "还是|\\s*(?<![A-Za-z])or(?![A-Za-z])\\s*|或者?是?|[是或与,，.。/?!、？！:：]|\\s+"
                            .toRegex()
                    )
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val stringList =
                Arrays.stream(strings).filter { s1: String -> StringUtils.hasText(s1) }.toList()

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

            val r = Math.round(getRandom(stringList.size) - 1.0).toInt()
            return String.format("当然%s啦！", changeCase(stringList[r])) // lr format一样的
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

            val m1 = Objects.nonNull(m.group("m1")) && StringUtils.hasText(m.group("m1"))
            val m2 = Objects.nonNull(m.group("m2")) && StringUtils.hasText(m.group("m2"))
            val c3 = hasC3 && Objects.nonNull(m.group("c3")) && StringUtils.hasText(m.group("c3"))

            if (onlyC3) return c3
            if (hasC3) return m1 && m2 && c3
            return m1 && m2
        }

        private val randomInstantly: Double
            get() = getRandomInstantly(0)

        /**
         * 获取短时间内的多个随机数
         *
         * @param range 范围
         * @param <T> 数字的子类 </T>
         * @return 如果范围是 1，返回 1。如果范围大于 1，返回 1-范围内的数（Double 的整数），其他则返回 0-1。
         */
        fun <T : Number?> getRandomInstantly(range: T?): Double {
            val random = Math.random()

            val r =
                try {
                    range.toString().toInt()
                } catch (e: NumberFormatException) {
                    try {
                        if (Objects.nonNull(range)) {
                            round(range!!.toDouble()).toInt()
                        } else {
                            100
                        }
                    } catch (e1: NumberFormatException) {
                        return random
                    }
                }

            return if (r > 1) {
                (random * (r - 1)).roundToInt() + 1.0
            } else {
                random
            }
        }

        /**
         * 获取随机数。注意，随机数的来源是系统毫秒，因此不能短时间内多次获取，如果要多次获取请使用 getRandomInstantly 提供的伪随机数
         *
         * @param range 范围
         * @param <T> 数字的子类 </T>
         * @return 如果范围是 1，返回 1。如果范围大于 1，返回 1-范围内的数（Double 的整数），其他则返回 0-1。
         */
        @JvmStatic
        fun <T : Number?> getRandom(range: T): Double {
            val millis = System.currentTimeMillis() % 1000

            val r =
                try {
                    range.toString().toInt()
                } catch (e: NumberFormatException) {
                    try {
                        if (Objects.nonNull(range)) {
                            round(range!!.toDouble()).toInt()
                        } else {
                            100
                        }
                    } catch (e1: NumberFormatException) {
                        return millis / 999.0
                    }
                }

            return if (r > 1) {
                (millis / 999.0 * (r - 1)).roundToInt() + 1.0
            } else {
                millis / 999.0
            }
        }

        /**
         * 改变主宾格，删除语气助词，和谐违禁词
         *
         * @param str 未和谐
         * @return 和谐
         */
        private fun changeCase(str: String?): String {
            var s = str ?: ""
            s = recoveryApostrophe(s)

            return s.trim { it <= ' ' } // 换人称
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
                    "",
                ) // 阿耶来唻了价也罗给的般则连不呸哪哇 不匹配，删去其他语气助词
                // 换句末符号

                .replace("[?？!！。.\\s]$".toRegex(), "")
                .replace(
                    "[习習]近平|[习習]?总书记|主席|国家|政治|反动|反?共(产党)?|[国國]民[党黨]|天安[門门]|极[左右](主义)?|革命|(社会)?主义|自由|解放|中[華华]民[国國]|情趣|迪克|高潮|色[诱情欲色]|擦边|露出|[蛇射受授吞]精|潮喷|成人|性交|小?男娘|小?南梁|做爱|后入|药娘|怀孕|生殖器|寄吧|几把|鸡[鸡巴]|[精卵]子|[精爱]液|子宫|阴[茎蒂唇囊道]|[逼Bb阴吊叼批肛]毛|搞基|出?脚本|[Rr]-?18|18\\s?禁|LGBT"
                        .toRegex(),
                    "[和谐]",
                )
                .replace("[黨党吊批逼操肏肛杀穴屁萎猥]".toRegex(), "○")
        }

        // 避免撇号影响结果，比如 It's time to go bed
        private fun transferApostrophe(s: String?): String {
            return (s ?: "")
                .trim { it <= ' ' }
                .replace("'".toRegex(), "\\" + "'")
                .replace("\"".toRegex(), "\\" + "\"")
        }

        // 把撇号影响的结果转换回去，比如 It's time to go bed
        private fun recoveryApostrophe(s: String?): String {
            return (s ?: "")
                .trim { it <= ' ' }
                .replace(("\\" + "'").toRegex(), "'")
                .replace(("\\" + "\"").toRegex(), "\"")
        }
    }
}
