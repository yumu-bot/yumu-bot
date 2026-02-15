package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.dao.PPMinusDao
import com.now.nowbot.entity.PPMinusLite
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.model.ppminus.PPMinus4
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.PPMinusService.PPMinusParam
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.InstructionUtil.getMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("PP_MINUS") class PPMinusService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val ppMinusDao: PPMinusDao,
    private val imageService: ImageService,
    newbieConfig: NewbieConfig
) : MessageService<PPMinusParam>, TencentMessageService<PPMinusParam> {

    private val killerGroup: Long = newbieConfig.killerGroup

    data class PPMinusParam(
        val isVs: Boolean,
        val me: OsuUser,
        val myBests: List<LazerScore>,
        val other: OsuUser?,
        val otherBests: List<LazerScore>?,
        val mode: OsuMode,
        val version: Int = 4
    ) {
        fun toMap(dao: PPMinusDao): Map<String, Any> {
            when(version) {
                2 -> {
                    val my: PPMinus
                    val others: PPMinus?

                    if (isVs) {
                        val async = AsyncMethodExecutor.awaitPairCallableExecute(
                            { getPPMinus2(me, myBests, dao) },
                            { getPPMinus2(other!!, otherBests!!, dao) }
                        )
                        my = async.first
                        others = async.second
                    } else {
                        my = getPPMinus2(me, myBests, dao)
                        others = null
                    }

                    if (other != null) {
                        if (other.id == 17064371L) {
                            customizePerformanceMinus(others, 999.99f)
                        } else if (other.id == 19673275L) {
                            customizePerformanceMinus(others, 0f)
                        }
                    }

                    val cardA1s = ArrayList<OsuUser>(2)
                    cardA1s.add(me)

                    if (isVs) cardA1s.add(other!!)

                    val cardB1 = mapOf(
                        "ACC" to my.value1,
                        "PTT" to my.value2,
                        "STA" to my.value3,
                        (if (mode == OsuMode.MANIA) "PRE" else "STB") to my.value4,
                        "EFT" to my.value5,
                        "STH" to my.value6,
                        "OVA" to my.value7,
                        "SAN" to my.value8
                    )
                    val cardB2 = if (isVs) mapOf(
                        "ACC" to others!!.value1,
                        "PTT" to others.value2,
                        "STA" to others.value3,
                        (if (mode == OsuMode.MANIA) "PRE" else "STB") to others.value4,
                        "EFT" to others.value5,
                        "STH" to others.value6,
                        "OVA" to others.value7,
                        "SAN" to others.value8
                    ) else null

                    val statistics: Map<String, Any> = mapOf("is_vs" to isVs, "mode_int" to mode.modeValue)

                    val body = HashMap<String, Any>(4)

                    body["users"] = cardA1s
                    body["my"] = cardB1

                    if (cardB2 != null) body["others"] = cardB2

                    body["stat"] = statistics
                    body["panel"] = "PM2"

                    return body
                }

                4 -> {

                    val my: PPMinus4
                    val others: PPMinus4?

                    if (isVs) {
                        val async = AsyncMethodExecutor.awaitPairCallableExecute(
                            { getPPMinus4(me, myBests, dao) },
                            { getPPMinus4(other!!, otherBests!!, dao) }
                        )
                        my = async.first
                        others = async.second
                    } else {
                        my = getPPMinus4(me, myBests, dao)
                        others = null
                    }

                    val cardA1s = ArrayList<OsuUser>(2)
                    cardA1s.add(me)

                    if (other != null) cardA1s.add(other)

                    val titles =
                        listOf("ACC", "PTT", "STA", if (mode == OsuMode.MANIA) "PRE" else "STB", "EFT", "STH", "OVA", "SAN")
                    val cardB1 = my.values.mapIndexed { i, it -> titles[i] to it }.toMap()

                    val cardB2 = others?.values?.mapIndexed { i, it -> titles[i] to it }?.toMap()

                    val statistics: Map<String, Any> = mapOf("is_vs" to (other != null), "mode_int" to mode.modeValue)

                    val body = mutableMapOf(
                        "users" to cardA1s, "my" to cardB1, "stat" to statistics, "count" to my.count, "delta" to my.delta, "panel" to "PM4"
                    )

                    if (other != null && other.id == 17064371L) {
                        body["others"] = List(others!!.values.size) { i -> titles[i] to 999 }.toMap()
                    } else if (cardB2 != null)  {
                        body["others"] = cardB2
                    }

                    return body
                }

                else -> {
                    val my = getPPMinus2(me, myBests, dao, -1)

                    val users = listOf(me)

                    val ppm2 = mapOf(
                        "ACC" to my.value1,
                        "PTT" to my.value2,
                        "STA" to my.value3,
                        (if (mode == OsuMode.MANIA) "PRE" else "STB") to my.value4,
                        "EFT" to my.value5,
                        "STH" to my.value6,
                        "OVA" to my.value7,
                        "SAN" to my.value8
                    )

                    val statistics: Map<String, Any> = mapOf("is_vs" to false, "mode_int" to mode.modeValue)

                    return mapOf(
                        "users" to users, "my" to ppm2, "stat" to statistics, "panel" to "sanity"
                    )
                }
            }
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<PPMinusParam>,
    ): Boolean {
        val m1 = Instruction.PP_MINUS.matcher(messageText)
        val m2 = Instruction.PP_MINUS_VS.matcher(messageText)
        val m3 = Instruction.PP_MINUS_LEGACY.matcher(messageText)

        val isVs: Boolean
        val version: Int

        val matcher = if (m1.find()) {
            isVs = false
            version = 4
            m1
        } else if (m2.find()) {
            isVs = true
            version = 4
            m2
        } else if (m3.find()) {
            isVs = false
            version = 2
            m3
        } else return false

        val ver = if (event.subject.contactID == killerGroup && !isVs) {
            -1
        } else {
            version
        }

        data.value = getParam(event, matcher, isVs, ver)

        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: PPMinusParam): ServiceCallStatistic? {
        val image = param.getPPMImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("PP-：发送失败：", e)
            throw IllegalStateException.Send("PPM")
        }

        return if (param.isVs) {
            ServiceCallStatistic.builds(event,
                userIDs = listOf(param.me.userID, param.other!!.userID),
                modes = listOf(param.mode)
            )
        } else {
            ServiceCallStatistic.build(event,
                userID = param.me.userID,
                mode = param.mode
            )
        }
    }

    override fun accept(event: MessageEvent, messageText: String): PPMinusParam? {
        val m1 = OfficialInstruction.PP_MINUS.matcher(messageText)
        val m2 = OfficialInstruction.PP_MINUS_VS.matcher(messageText)
        val m3 = OfficialInstruction.PP_MINUS_LEGACY.matcher(messageText)

        val isVs: Boolean
        val version: Int

        val matcher = if (m1.find()) {
            isVs = false
            version = 4
            m1
        } else if (m2.find()) {
            isVs = true
            version = 4
            m2
        } else if (m3.find()) {
            isVs = false
            version = 2
            m3
        } else return null

        return getParam(event, matcher, isVs, version)
    }

    override fun reply(event: MessageEvent, param: PPMinusParam): MessageChain? {
        return MessageChain(param.getPPMImage())
    }


    fun PPMinusParam.getPPMImage(): ByteArray {
        return when(version) {
            2, 4 -> imageService.getPanel(this.toMap(ppMinusDao), "B1")
            else -> imageService.getPanel(this.toMap(ppMinusDao), "Gamma")
        }
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, isVs: Boolean = false, version: Int = 4): PPMinusParam {
        val inputMode = getMode(matcher)

        val ids = UserIDUtil.get2UserID(event, matcher, inputMode, isVs)

        val me: OsuUser
        val other: OsuUser?
        val myBests: List<LazerScore>
        val otherBests: List<LazerScore>?
        val mode: OsuMode

        if (isVs) {
            if (ids.first != null && ids.second != null) {
                // 双人模式

                mode = if (version == -1) {
                    OsuMode.OSU
                } else {
                    inputMode.data!!
                }

                val async = AsyncMethodExecutor.awaitQuadCallableExecute(
                    { userApiService.getOsuUser(ids.first!!, mode) },
                    { scoreApiService.getBestScores(ids.first!!, mode, 0, 100) },
                    { userApiService.getOsuUser(ids.second!!, mode) },
                    { scoreApiService.getBestScores(ids.second!!, mode, 0, 100) },
                )

                me = async.first.first
                other = async.second.first

                myBests = async.first.second
                otherBests = async.second.second
            } else {
                // 缺东西，走常规路线
                val users = InstructionUtil.get2User(event, matcher, inputMode, true)

                mode = if (version == -1) {
                    OsuMode.OSU
                } else {
                    users.first().currentOsuMode
                }

                me = users.first()
                other = if (users.size == 2) users.last() else null

                myBests = scoreApiService.getBestScores(me.userID, mode, 0, 100)
                otherBests = if (other != null) scoreApiService.getBestScores(other.userID, mode, 0, 100) else null
            }
        } else {
            if (ids.first != null && ids.second != null) {
                // 双人模式

                mode = if (version == -1) {
                    OsuMode.OSU
                } else {
                    inputMode.data!!
                }

                val async = AsyncMethodExecutor.awaitQuadCallableExecute(
                    { userApiService.getOsuUser(ids.first!!, mode) },
                    { scoreApiService.getBestScores(ids.first!!, mode, 0, 100) },
                    { userApiService.getOsuUser(ids.second!!, mode) },
                    { scoreApiService.getBestScores(ids.second!!, mode, 0, 100) },
                )

                me = async.first.first
                other = async.second.first

                myBests = async.first.second
                otherBests = async.second.second

            } else if (ids.first != null) {
                // 单人模式

                mode = if (version == -1) {
                    OsuMode.OSU
                } else {
                    inputMode.data!!
                }

                val async = AsyncMethodExecutor.awaitPairCallableExecute(
                    { userApiService.getOsuUser(ids.first!!, mode) },
                    { scoreApiService.getBestScores(ids.first!!, mode, 0, 100) },
                )

                me = async.first
                other = null

                myBests = async.second
                otherBests = null

            } else {
                // 缺东西，走常规路线

                val users = InstructionUtil.get2User(event, matcher, inputMode, false)

                mode = if (version == -1) {
                    OsuMode.OSU
                } else {
                    users.first().currentOsuMode
                }

                me = users.first()
                other = if (users.size == 2) users.last() else null

                myBests = scoreApiService.getBestScores(me.userID, mode, 0, 100)
                otherBests = if (other != null) scoreApiService.getBestScores(other.userID, mode, 0, 100) else null
            }
        }

        return PPMinusParam(other != null, me, myBests, other, otherBests, mode, version)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PPMinusService::class.java)

        @JvmStatic
        fun getPPMinus2(user: OsuUser, bests: List<LazerScore>, ppMinusDao: PPMinusDao, version: Int = 2): PPMinus {
            if (version != -1) {
                if (user.statistics!!.playTime!! < 60 || user.statistics!!.playCount!! < 30) {
                    throw NoSuchElementException.PlayerPlayWithMode(user.username, user.currentOsuMode)
                }

                AsyncMethodExecutor.asyncRunnableExecute {
                    try {
                        ppMinusDao.savePPMinus(user, bests)
                    } catch (e: Exception) {
                        log.error("PPM2：数据保存失败", e)
                    }
                }
            }

            try {
                return PPMinus.getInstance(user.currentOsuMode, user, bests)
            } catch (e: Exception) {
                log.error("PPM2：数据计算失败", e)
                throw IllegalStateException.Calculate("PPM")
            }
        }

        @JvmStatic
        fun getPPMinus4(user: OsuUser, bests: List<LazerScore>, ppMinusDao: PPMinusDao): PPMinus4 {
            if (user.statistics!!.playTime!! < 60 || user.statistics!!.playCount!! < 30) {
                throw NoSuchElementException.PlayerPlayWithMode(user.username, user.currentOsuMode)
            }

            AsyncMethodExecutor.asyncRunnableExecute {
                try {
                    ppMinusDao.savePPMinus(user, bests)
                } catch (e: Exception) {
                    log.error("PPM4：数据保存失败", e)
                }
            }

            var delta = 0
            val surrounding = run {
                var surrounding: List<PPMinusLite>

                do {
                    delta += 200
                    surrounding = ppMinusDao.getSurroundingPPMinus(user, bests, delta)
                } while (delta < 3000 && surrounding.size < 50)

                return@run surrounding
            }

            try {
                return PPMinus4.getInstance(user, bests, surrounding, delta, user.currentOsuMode)!!
            } catch (e: Exception) {
                log.error("PPM4：数据计算失败", e)
                throw IllegalStateException.Calculate("PPM4")
            }
        }

        private fun customizePerformanceMinus(minus: PPMinus?, value: Float) {
            if (minus == null) return

            val ppmClass = Class.forName("com.now.nowbot.model.ppminus.PPMinus")
            val valueFields = arrayOf(
                ppmClass.getDeclaredField("value1"),
                ppmClass.getDeclaredField("value2"),
                ppmClass.getDeclaredField("value3"),
                ppmClass.getDeclaredField("value4"),
                ppmClass.getDeclaredField("value5"),
                ppmClass.getDeclaredField("value6"),
                ppmClass.getDeclaredField("value7"),
                ppmClass.getDeclaredField("value8"),
            )
            for (i in valueFields) {
                i.isAccessible = true
                i[minus] = value
            }
        }
    }
}
