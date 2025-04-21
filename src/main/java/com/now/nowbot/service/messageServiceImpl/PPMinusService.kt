package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.dao.PPMinusDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
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
import java.util.*

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
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<PPMinusParam>,
    ): Boolean {
        val m1 = Instruction.PP_MINUS.matcher(messageText)
        val m2 = Instruction.PP_MINUS_VS.matcher(messageText)

        val isVS: Boolean
        val matcher = if (m1.find()) {
            isVS = false
            m1
        } else if (m2.find()) {
            isVS = true
            m2
        } else return false

        val inputMode = getMode(matcher)

        val users = CmdUtil.get2User(event, matcher, inputMode, isVS)

        val mode: OsuMode =
            if (event.subject.id == killerGroup && OsuMode.isDefaultOrNull(inputMode.data)) { // 在杀手群里查询，则主动认为是 osu 模式
                OsuMode.OSU
            } else {
                users.first().currentOsuMode
            }

        data.value = PPMinusParam(isVS, users.first(), if (users.size == 2) users.last() else null, mode)

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

        val isVS: Boolean
        val matcher = if (m1.find()) {
            isVS = false
            m1
        } else if (m2.find()) {
            isVS = true
            m2
        } else return null

        val inputMode = getMode(matcher)

        val users = CmdUtil.get2User(event, matcher, inputMode, isVS)

        val mode: OsuMode =
            if (event.subject.id == killerGroup && OsuMode.isDefaultOrNull(inputMode.data)) { // 在杀手群里查询，则主动认为是 osu 模式
                OsuMode.OSU
            } else {
                users.first().currentOsuMode
            }

        return PPMinusParam(isVS, users.first(), if (users.size == 2) users.last() else null, mode)
    }

    override fun reply(event: MessageEvent, param: PPMinusParam): MessageChain? {
        return QQMsgUtil.getImage(param.getPPMImage())
    }

    /**
     * 获取 PPM 信息重写
     *
     * @param user 玩家信息
     * @return PPM 实例
     */
    private fun getPPMinus(user: OsuUser): PPMinus {
        val bests: List<LazerScore>

        try {
            bests = scoreApiService.getBestScores(user)
        } catch (e: WebClientResponseException) {
            log.error("PP-：最好成绩获取失败", e)
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
            log.error("PP-：数据保存失败", e)
        }

        try {
            return PPMinus.getInstance(user.currentOsuMode, user, bests)
        } catch (e: Exception) {
            log.error("PP-：数据计算失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Calculate, "PPM")
        }
    }

    fun PPMinusParam.getPPMImage(): ByteArray {
        val my = getPPMinus(me)

        var others: PPMinus? = null

        if (isVs) {
            others = getPPMinus(other!!)
        }
        return imageService.getVS(me, other, my, others, mode)
    }

    fun PPMinusParam.getPPMSpecialImage(): ByteArray {
        val my = getPPMinus(me)

        return imageService.getPanelGamma(me, mode, my)
    }

    fun ImageService.getVS(
        me: OsuUser,
        other: OsuUser?,
        my: PPMinus,
        others: PPMinus?,
        mode: OsuMode,
    ): ByteArray {
        if (other != null) {
            if (other.id == 17064371L) {
                customizePerformanceMinus(others, 999.99f)
            } else if (other.id == 19673275L) {
                customizePerformanceMinus(others, 0f)
            }
        }
        return getPanelB1(me, other, my, others, mode)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PPMinusService::class.java)

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
