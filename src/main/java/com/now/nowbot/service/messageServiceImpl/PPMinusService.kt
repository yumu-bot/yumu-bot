package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.dao.PPMinusDao
import com.now.nowbot.entity.PPMinusLite
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.model.ppminus.impl.PPMinus4
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.PPMinusService.PPMinusParam
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("PP_MINUS") class PPMinusService(
    private val scoreApiService: OsuScoreApiService,
    private val ppMinusDao: PPMinusDao,
    private val imageService: ImageService,
    newbieConfig: NewbieConfig
) : MessageService<PPMinusParam>, TencentMessageService<PPMinusParam> {

    private val killerGroup: Long = newbieConfig.killerGroup

    data class PPMinusParam(
        val isVs: Boolean,
        val me: OsuUser,
        val other: OsuUser?,
        val mode: OsuMode,
        val version: Int = 4
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<PPMinusParam>,
    ): Boolean {
        val m1 = Instruction.PP_MINUS.matcher(messageText)
        val m2 = Instruction.PP_MINUS_VS.matcher(messageText)
        val m3 = Instruction.PP_MINUS_LEGACY.matcher(messageText)

        val isVS: Boolean
        val version: Int

        val matcher = if (m1.find()) {
            isVS = false
            version = 4
            m1
        } else if (m2.find()) {
            isVS = true
            version = 4
            m2
        } else if (m3.find()) {
            isVS = false
            version = 2
            m3
        } else return false

        val inputMode = getMode(matcher)

        val users = CmdUtil.get2User(event, matcher, inputMode, isVS)

        val mode: OsuMode =
            if (event.subject.id == killerGroup && OsuMode.isDefaultOrNull(inputMode.data)) { // 在杀手群里查询，则主动认为是 osu 模式
                OsuMode.OSU
            } else {
                users.first().currentOsuMode
            }

        data.value = PPMinusParam(isVS, users.first(), if (users.size == 2) users.last() else null, mode, version)

        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: PPMinusParam) {
        val image: ByteArray

        try {
            image = if (!param.isVs && event.subject.id == killerGroup) {
                param.getPPMSpecialImage()
            } else {
                param.getPPMImage()
            }
        } catch (e: TipsException) {
            throw e
        } catch (e: Exception) {
            log.error("PP-：渲染失败：", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "PPM")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("PP-：发送失败：", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "PPM")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): PPMinusParam? {
        val m1 = OfficialInstruction.PP_MINUS.matcher(messageText)
        val m2 = OfficialInstruction.PP_MINUS_VS.matcher(messageText)
        val m3 = OfficialInstruction.PP_MINUS_LEGACY.matcher(messageText)

        val isVS: Boolean
        val version: Int

        val matcher = if (m1.find()) {
            isVS = false
            version = 4
            m1
        } else if (m2.find()) {
            isVS = true
            version = 4
            m2
        } else if (m3.find()) {
            isVS = false
            version = 2
            m3
        } else return null

        val inputMode = getMode(matcher)

        val users = CmdUtil.get2User(event, matcher, inputMode, isVS)

        val mode: OsuMode =
            if (event.subject.id == killerGroup && OsuMode.isDefaultOrNull(inputMode.data)) { // 在杀手群里查询，则主动认为是 osu 模式
                OsuMode.OSU
            } else {
                users.first().currentOsuMode
            }

        return PPMinusParam(isVS, users.first(), if (users.size == 2) users.last() else null, mode, version)
    }

    override fun reply(event: MessageEvent, param: PPMinusParam): MessageChain? {
        return QQMsgUtil.getImage(param.getPPMImage())
    }


    fun PPMinusParam.getPPMImage(): ByteArray {
        when(version) {
            2 -> {
                val my = getPPMinus2(me, scoreApiService, ppMinusDao)
                var others: PPMinus? = null

                if (isVs) {
                    others = getPPMinus2(other!!, scoreApiService, ppMinusDao)
                }
                return imageService.getPanel(getPPM2Body(me, other, my, others, mode), "B1")
            }

            4 -> {
                val my = getPPMinus4(me, scoreApiService, ppMinusDao)
                var others: PPMinus4? = null

                if (isVs) {
                    others = getPPMinus4(other!!, scoreApiService, ppMinusDao)
                }
                return imageService.getPanel(getPPM4Body(me, other, my, others, mode), "B1")
            }

            else -> return byteArrayOf()
        }
    }

    fun PPMinusParam.getPPMSpecialImage(): ByteArray {
        val my = getPPMinus2(me, scoreApiService, ppMinusDao)

        return imageService.getPanelGamma(me, mode, my)
    }



    companion object {
        private val log: Logger = LoggerFactory.getLogger(PPMinusService::class.java)

        @JvmStatic
        fun getPPMinus2(user: OsuUser, scoreApiService: OsuScoreApiService, ppMinusDao: PPMinusDao): PPMinus {
            val bests: List<LazerScore>

            try {
                bests = scoreApiService.getBestScores(user)
            } catch (e: WebClientResponseException) {
                log.error("PPM2：最好成绩获取失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, user.username)
            }

            if (user.statistics!!.playTime!! < 60 || user.statistics!!.playCount!! < 30) {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_NotEnough_PlayTime,
                    user.currentOsuMode.fullName,
                )
            }

            try {
                ppMinusDao.savePPMinus(user, bests)
            } catch (e: Exception) {
                log.error("PPM2：数据保存失败", e)
            }

            try {
                return PPMinus.getInstance(user.currentOsuMode, user, bests)
            } catch (e: Exception) {
                log.error("PPM2：数据计算失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Calculate, "PPM")
            }
        }

        @JvmStatic
        fun getPPMinus4(user: OsuUser, scoreApiService: OsuScoreApiService, ppMinusDao: PPMinusDao): PPMinus4 {
            val bests: List<LazerScore>

            try {
                bests = scoreApiService.getBestScores(user)
            } catch (e: WebClientResponseException) {
                log.error("PPM4：最好成绩获取失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, user.username)
            }

            if (user.statistics!!.playTime!! < 60 || user.statistics!!.playCount!! < 30) {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_NotEnough_PlayTime,
                    user.currentOsuMode.fullName,
                )
            }

            try {
                ppMinusDao.savePPMinus(user, bests)
            } catch (e: Exception) {
                log.error("PPM4：数据保存失败", e)
            }

            var delta = 0
            val surrounding = run {
                var surrounding: List<PPMinusLite>

                do {
                    delta += 500
                    surrounding = ppMinusDao.getSurroundingPPMinus(user, bests, delta)
                } while (delta < 3000 && surrounding.size < 50)

                return@run surrounding
            }

            try {
                return PPMinus4.getInstance(user, bests, surrounding, delta, user.currentOsuMode)!!
            } catch (e: Exception) {
                log.error("PPM4：数据计算失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Calculate, "PPM4")
            }
        }

        @JvmStatic
        fun getPPM4Body(
            me: OsuUser,
            other: OsuUser?,
            my: PPMinus4,
            others: PPMinus4?,
            mode: OsuMode,
        ): Map<String, Any> {
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

        @JvmStatic
        fun getPPM2Body(
            me: OsuUser,
            other: OsuUser?,
            my: PPMinus,
            others: PPMinus?,
            mode: OsuMode,
        ): Map<String, Any> {
            if (other != null) {
                if (other.id == 17064371L) {
                    customizePerformanceMinus(others, 999.99f)
                } else if (other.id == 19673275L) {
                    customizePerformanceMinus(others, 0f)
                }
            }

            val isVs = other != null && others != null

            val cardA1s = ArrayList<OsuUser>(2)
            cardA1s.add(me)

            if (other != null) cardA1s.add(other)

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
